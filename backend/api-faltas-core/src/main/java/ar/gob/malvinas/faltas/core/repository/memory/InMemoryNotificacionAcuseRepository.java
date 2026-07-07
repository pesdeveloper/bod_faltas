package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.TipoAcuse;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacionAcuse;
import ar.gob.malvinas.faltas.core.repository.NotificacionAcuseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryNotificacionAcuseRepository
        implements NotificacionAcuseRepository, ResettableInMemoryRepository {

    private final Map<Long, FalNotificacionAcuse> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalNotificacionAcuse guardar(FalNotificacionAcuse acuse) {
        FalNotificacionAcuse copia = acuse.copia();
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalNotificacionAcuse> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalNotificacionAcuse::copia);
    }

    @Override
    public List<FalNotificacionAcuse> buscarPorNotificacion(Long notificacionId) {
        return store.values().stream()
                .filter(a -> notificacionId.equals(a.getNotificacionId()))
                .map(FalNotificacionAcuse::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalNotificacionAcuse> buscarPorIntento(Long intentoId) {
        return store.values().stream()
                .filter(a -> intentoId.equals(a.getIntentoId()))
                .map(FalNotificacionAcuse::copia)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FalNotificacionAcuse> buscarPorIdempotencia(Long notificacionId, Long intentoId, TipoAcuse tipoAcuse) {
        return store.values().stream()
                .filter(a -> notificacionId.equals(a.getNotificacionId())
                        && java.util.Objects.equals(intentoId, a.getIntentoId())
                        && tipoAcuse == a.getTipoAcuse()
                        && a.estaActivo())
                .map(FalNotificacionAcuse::copia)
                .findFirst();
    }

    public void cargarSeed(List<FalNotificacionAcuse> lista) {
        long maxId = 0;
        for (FalNotificacionAcuse a : lista) {
            store.put(a.getId(), a.copia());
            if (a.getId() > maxId) maxId = a.getId();
        }
        idCounter.set(maxId + 1);
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "notificacion-acuses"; }

    @Override
    public int size() { return store.size(); }
}
