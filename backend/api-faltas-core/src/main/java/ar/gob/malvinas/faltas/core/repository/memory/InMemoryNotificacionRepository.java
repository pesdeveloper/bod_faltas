package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoNotificacion;
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
    public synchronized FalNotificacion guardar(FalNotificacion notificacion) {
        if (notificacion.estaActiva()) {
            for (FalNotificacion existente : store.values()) {
                if (existente.getIdDocumento().equals(notificacion.getIdDocumento())
                        && !existente.getId().equals(notificacion.getId())
                        && existente.estaActiva()) {
                    throw new ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException(
                            "Ya existe una notificacion activa (id=" + existente.getId()
                            + ") para el documento " + notificacion.getIdDocumento()
                            + ". No puede existir una segunda notificacion activa.");
                }
            }
        }
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
    public Optional<FalNotificacion> buscarActivaPorDocumento(Long idDocumento) {
        return store.values().stream()
                .filter(n -> idDocumento.equals(n.getIdDocumento()) && n.estaActiva())
                .findFirst();
    }

    @Override
    public List<FalNotificacion> buscarPorEstado(EstadoNotificacion estado) {
        return store.values().stream()
                .filter(n -> estado.equals(n.getEstado()))
                .sorted(java.util.Comparator.comparing(FalNotificacion::getId))
                .toList();
    }

    @Override
    public void reset() { store.clear(); idGen.set(1); }

    @Override
    public String nombre() { return "Notificacion"; }

    @Override
    public int size() { return store.size(); }
}
