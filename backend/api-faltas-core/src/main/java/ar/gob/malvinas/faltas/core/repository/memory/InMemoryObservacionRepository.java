package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.model.FalObservacion;
import ar.gob.malvinas.faltas.core.repository.ObservacionRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryObservacionRepository implements ObservacionRepository, ResettableInMemoryRepository {

    private final Map<Long, FalObservacion> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalObservacion guardar(FalObservacion obs) {
        FalObservacion copia = obs.copia();
        store.put(copia.getId(), copia);
        return copia.copia();
    }

    @Override
    public Optional<FalObservacion> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalObservacion::copia);
    }

    @Override
    public List<FalObservacion> listarPorEntidad(EntidadTipoObservada tipo, Long entidadId) {
        return store.values().stream()
                .filter(o -> o.getEntidadTipo() == tipo && o.getEntidadId().equals(entidadId))
                .map(FalObservacion::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalObservacion> listarActivasPorEntidad(EntidadTipoObservada tipo, Long entidadId) {
        return store.values().stream()
                .filter(o -> o.getEntidadTipo() == tipo && o.getEntidadId().equals(entidadId) && o.isSiActiva())
                .map(FalObservacion::copia)
                .collect(Collectors.toList());
    }

    @Override
    public void desactivar(Long id) {
        FalObservacion obs = store.get(id);
        if (obs != null) {
            obs.setSiActiva(false);
        }
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "observaciones"; }

    @Override
    public int size() { return store.size(); }
}
