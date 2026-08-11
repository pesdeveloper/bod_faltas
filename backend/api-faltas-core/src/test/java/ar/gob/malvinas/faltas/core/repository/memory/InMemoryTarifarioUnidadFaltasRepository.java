package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalTarifarioUnidadFaltas;
import ar.gob.malvinas.faltas.core.repository.TarifarioUnidadFaltasRepository;
import org.springframework.context.annotation.Profile;
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
@Profile("test")
public class InMemoryTarifarioUnidadFaltasRepository
        implements TarifarioUnidadFaltasRepository, ResettableInMemoryRepository {

    private final Map<Long, FalTarifarioUnidadFaltas> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    /** Como maximo una formula activa por articulo (refleja la UK real del DDL). */
    @Override
    public synchronized FalTarifarioUnidadFaltas save(FalTarifarioUnidadFaltas tarifario) {
        if (tarifario.isSiActiva()) {
            for (FalTarifarioUnidadFaltas existing : store.values()) {
                if (existing.getId().equals(tarifario.getId())) continue;
                if (!existing.getArticuloId().equals(tarifario.getArticuloId())) continue;
                if (!existing.isSiActiva()) continue;
                throw new PrecondicionVioladaException(
                        "Ya existe una formula activa (id=" + existing.getId()
                                + ") para el articulo id=" + tarifario.getArticuloId()
                                + ". Cerrar su vigencia antes de activar una nueva.");
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
    public List<FalTarifarioUnidadFaltas> findAllByArticulo(Long articuloId) {
        return store.values().stream()
                .filter(t -> articuloId.equals(t.getArticuloId()))
                .sorted(Comparator.comparing(FalTarifarioUnidadFaltas::getFhVigDesde))
                .map(FalTarifarioUnidadFaltas::copia)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FalTarifarioUnidadFaltas> findActivaByArticulo(Long articuloId) {
        List<FalTarifarioUnidadFaltas> activas = store.values().stream()
                .filter(t -> articuloId.equals(t.getArticuloId()) && t.isSiActiva())
                .toList();
        if (activas.size() > 1) {
            throw new IllegalStateException(
                    "Invariante rota: " + activas.size() + " formulas activas para articuloId=" + articuloId);
        }
        return activas.isEmpty() ? Optional.empty() : Optional.of(activas.get(0).copia());
    }

    @Override
    public Optional<FalTarifarioUnidadFaltas> findVigenteByArticulo(Long articuloId, LocalDate fecha) {
        return store.values().stream()
                .filter(t -> articuloId.equals(t.getArticuloId()) && t.esVigenteEn(fecha))
                .max(Comparator.comparing(FalTarifarioUnidadFaltas::getFhVigDesde))
                .map(FalTarifarioUnidadFaltas::copia);
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
}
