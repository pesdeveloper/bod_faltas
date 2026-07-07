package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryNotificacionRepository implements NotificacionRepository, ResettableInMemoryRepository {

    private final AtomicLong idGen = new AtomicLong(1);
    private final Map<Long, FalNotificacion> store = new ConcurrentHashMap<>();

    @Override
    public Long nextId() {
        return idGen.getAndIncrement();
    }

    @Override
    public FalNotificacion guardar(FalNotificacion notificacion) {
        store.put(notificacion.getId(), notificacion);
        return notificacion;
    }

    @Override
    public Optional<FalNotificacion> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<FalNotificacion> buscarPorActa(Long idActa) {
        return store.values().stream()
                .filter(n -> idActa.equals(n.getIdActa()))
                .toList();
    }

    @Override
    public List<FalNotificacion> buscarPorDocumento(Long idDocumento) {
        return store.values().stream()
                .filter(n -> idDocumento.equals(n.getIdDocumento()))
                .toList();
    }

    @Override
    public void reset() { store.clear(); idGen.set(1); }

    @Override
    public String nombre() { return "Notificacion"; }

    @Override
    public int size() { return store.size(); }
}