package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryNotificacionRepository implements NotificacionRepository {

    private final Map<String, FalNotificacion> store = new ConcurrentHashMap<>();

    @Override
    public FalNotificacion guardar(FalNotificacion notificacion) {
        store.put(notificacion.getId(), notificacion);
        return notificacion;
    }

    @Override
    public Optional<FalNotificacion> buscarPorId(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<FalNotificacion> buscarPorActa(Long idActa) {
        return store.values().stream()
                .filter(n -> idActa.equals(n.getIdActa()))
                .toList();
    }

    @Override
    public List<FalNotificacion> buscarPorDocumento(String idDocumento) {
        return store.values().stream()
                .filter(n -> idDocumento.equals(n.getIdDocumento()))
                .toList();
    }
}

