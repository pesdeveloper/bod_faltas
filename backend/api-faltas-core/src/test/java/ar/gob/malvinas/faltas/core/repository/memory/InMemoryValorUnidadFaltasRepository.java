package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalValorUnidadFaltas;
import ar.gob.malvinas.faltas.core.repository.ValorUnidadFaltasRepository;
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
public class InMemoryValorUnidadFaltasRepository
        implements ValorUnidadFaltasRepository, ResettableInMemoryRepository {

    private final Map<Long, FalValorUnidadFaltas> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() { return idCounter.getAndIncrement(); }

    @Override
    public synchronized FalValorUnidadFaltas save(FalValorUnidadFaltas valorUnidad) {
        if (valorUnidad.isSiActiva()) {
            for (FalValorUnidadFaltas existing : store.values()) {
                if (existing.getId().equals(valorUnidad.getId())) continue;
                if (existing.getTipoUnidad() != valorUnidad.getTipoUnidad()) continue;
                if (!existing.isSiActiva()) continue;
                throw new PrecondicionVioladaException(
                        "Ya existe un valor activo (id=" + existing.getId()
                                + ") para tipoUnidad=" + valorUnidad.getTipoUnidad()
                                + ". Cerrar su vigencia antes de activar uno nuevo.");
            }
        }
        FalValorUnidadFaltas copia = valorUnidad.copia();
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalValorUnidadFaltas> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalValorUnidadFaltas::copia);
    }

    @Override
    public List<FalValorUnidadFaltas> findAllByTipo(TipoUnidadFaltas tipo) {
        return store.values().stream()
                .filter(v -> v.getTipoUnidad() == tipo)
                .sorted(Comparator.comparing(FalValorUnidadFaltas::getFhVigDesde))
                .map(FalValorUnidadFaltas::copia)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FalValorUnidadFaltas> findActivoByTipo(TipoUnidadFaltas tipo) {
        List<FalValorUnidadFaltas> activos = store.values().stream()
                .filter(v -> v.getTipoUnidad() == tipo && v.isSiActiva())
                .toList();
        if (activos.size() > 1) {
            throw new IllegalStateException(
                    "Invariante rota: " + activos.size() + " valores activos para tipoUnidad=" + tipo);
        }
        return activos.isEmpty() ? Optional.empty() : Optional.of(activos.get(0).copia());
    }

    @Override
    public Optional<FalValorUnidadFaltas> findVigenteByTipo(TipoUnidadFaltas tipo, LocalDate fecha) {
        return store.values().stream()
                .filter(v -> v.getTipoUnidad() == tipo && v.esVigenteEn(fecha))
                .max(Comparator.comparing(FalValorUnidadFaltas::getFhVigDesde))
                .map(FalValorUnidadFaltas::copia);
    }

    public void cargarSeed(List<FalValorUnidadFaltas> lista) {
        long maxId = 0;
        for (FalValorUnidadFaltas v : lista) {
            store.put(v.getId(), v.copia());
            if (v.getId() > maxId) maxId = v.getId();
        }
        idCounter.set(maxId + 1);
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "valoresUnidad"; }

    @Override
    public int size() { return store.size(); }
}
