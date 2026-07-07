package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalTarifarioUnidadFaltas;
import ar.gob.malvinas.faltas.core.repository.TarifarioUnidadFaltasRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryTarifarioUnidadFaltasRepository
        implements TarifarioUnidadFaltasRepository, ResettableInMemoryRepository {

    private final Map<Long, FalTarifarioUnidadFaltas> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    /**
     * Guarda con validacion atomica de no-superposicion de rangos activos para el mismo tipoUnidad.
     * fhVigHasta es exclusivo: [fhVigDesde, fhVigHasta).
     * Rangos contiguos [d1, h1) y [h1, d2) NO se superponen.
     */
    @Override
    public synchronized FalTarifarioUnidadFaltas save(FalTarifarioUnidadFaltas tarifario) {
        if (tarifario.isSiActiva()) {
            for (FalTarifarioUnidadFaltas existing : store.values()) {
                if (existing.getId().equals(tarifario.getId())) continue;
                if (existing.getTipoUnidad() != tarifario.getTipoUnidad()) continue;
                if (!existing.isSiActiva()) continue;
                if (solapan(tarifario.getFhVigDesde(), tarifario.getFhVigHasta(),
                        existing.getFhVigDesde(), existing.getFhVigHasta())) {
                    throw new PrecondicionVioladaException(
                            "Superposicion de vigencia con tarifario id=" + existing.getId()
                                    + " para tipoUnidad=" + tarifario.getTipoUnidad()
                                    + ". fhVigHasta es exclusivo.");
                }
            }
        }
        FalTarifarioUnidadFaltas copia = tarifario.copia();
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalTarifarioUnidadFaltas> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalTarifarioUnidadFaltas::copia);
    }

    @Override
    public List<FalTarifarioUnidadFaltas> findAll() {
        return store.values().stream().map(FalTarifarioUnidadFaltas::copia).collect(Collectors.toList());
    }

    @Override
    public List<FalTarifarioUnidadFaltas> findByTipoUnidad(TipoUnidadFaltas tipoUnidad) {
        return store.values().stream()
                .filter(t -> tipoUnidad == t.getTipoUnidad())
                .map(FalTarifarioUnidadFaltas::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalTarifarioUnidadFaltas> findVigentes(TipoUnidadFaltas tipoUnidad, LocalDate fecha) {
        return store.values().stream()
                .filter(t -> tipoUnidad == t.getTipoUnidad() && t.esVigenteEn(fecha))
                .map(FalTarifarioUnidadFaltas::copia)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FalTarifarioUnidadFaltas> findUltimoVigente(TipoUnidadFaltas tipoUnidad, LocalDate fecha) {
        List<FalTarifarioUnidadFaltas> vigentes = store.values().stream()
                .filter(t -> tipoUnidad == t.getTipoUnidad() && t.esVigenteEn(fecha))
                .map(FalTarifarioUnidadFaltas::copia)
                .collect(Collectors.toList());
        if (vigentes.size() > 1) {
            throw new IllegalStateException(
                    "Invariante rota: " + vigentes.size()
                            + " tarifarios activos y vigentes para tipoUnidad=" + tipoUnidad
                            + " en fecha=" + fecha);
        }
        return vigentes.isEmpty() ? Optional.empty() : Optional.of(vigentes.get(0));
    }

    public void cargarSeed(List<FalTarifarioUnidadFaltas> lista) {
        long maxId = 0;
        for (FalTarifarioUnidadFaltas t : lista) {
            store.put(t.getId(), t.copia());
            if (t.getId() > maxId) maxId = t.getId();
        }
        idCounter.set(maxId + 1);
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "tarifarios"; }

    @Override
    public int size() { return store.size(); }

    /** fhVigHasta es exclusivo. [d1,h1) y [d2,h2) solapan si d1 < h2 && d2 < h1. */
    private boolean solapan(LocalDate d1, LocalDate h1, LocalDate d2, LocalDate h2) {
        LocalDate fin1 = (h1 == null) ? LocalDate.MAX : h1;
        LocalDate fin2 = (h2 == null) ? LocalDate.MAX : h2;
        return d1.isBefore(fin2) && d2.isBefore(fin1);
    }
}
