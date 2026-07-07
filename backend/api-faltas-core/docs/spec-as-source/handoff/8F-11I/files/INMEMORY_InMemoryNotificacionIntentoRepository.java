package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalNotificacionIntento;
import ar.gob.malvinas.faltas.core.repository.NotificacionIntentoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Repositorio InMemory de intentos de notificacion.
 * El correlativo nroIntento es atomico por notificacionId via ConcurrentHashMap<Long, AtomicInteger>.
 */
@Repository
public class InMemoryNotificacionIntentoRepository
        implements NotificacionIntentoRepository, ResettableInMemoryRepository {

    private final Map<Long, FalNotificacionIntento> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);
    private final Map<Long, AtomicInteger> correlativoPorNotif = new ConcurrentHashMap<>();
    private final Map<String, Long> referenciasClamadas = new ConcurrentHashMap<>();

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public short siguienteNroIntento(Long notificacionId) {
        return (short) correlativoPorNotif
                .computeIfAbsent(notificacionId, k -> new AtomicInteger(1))
                .getAndIncrement();
    }

    @Override
    public FalNotificacionIntento guardar(FalNotificacionIntento intento) {
        FalNotificacionIntento copia = intento.copia();
        store.put(copia.getId(), copia);
        // Sync correlativo counter: next siguienteNroIntento must return at least nroIntento+1
        int minNext = (int) copia.getNroIntento() + 1;
        correlativoPorNotif.compute(copia.getNotificacionId(), (k, existing) -> {
            if (existing == null) return new AtomicInteger(minNext);
            existing.updateAndGet(v -> Math.max(v, minNext));
            return existing;
        });
        return copia;
    }

    @Override
    public Optional<FalNotificacionIntento> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalNotificacionIntento::copia);
    }

    @Override
    public List<FalNotificacionIntento> buscarPorNotificacion(Long notificacionId) {
        return store.values().stream()
                .filter(i -> notificacionId.equals(i.getNotificacionId()))
                .sorted(java.util.Comparator.comparingInt(FalNotificacionIntento::getNroIntento))
                .map(FalNotificacionIntento::copia)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FalNotificacionIntento> buscarPorNroIntento(Long notificacionId, short nroIntento) {
        return store.values().stream()
                .filter(i -> notificacionId.equals(i.getNotificacionId()) && i.getNroIntento() == nroIntento)
                .map(FalNotificacionIntento::copia)
                .findFirst();
    }

    @Override
    public Optional<FalNotificacionIntento> buscarPorReferenciaExterna(String referenciaExterna) {
        if (referenciaExterna == null) return Optional.empty();
        return store.values().stream()
                .filter(i -> referenciaExterna.equals(i.getReferenciaExterna()))
                .map(FalNotificacionIntento::copia)
                .findFirst();
    }

    @Override
    public boolean claimReferenciaExterna(String ref) {
        return referenciasClamadas.putIfAbsent(ref, 1L) == null;
    }

    public void cargarSeed(List<FalNotificacionIntento> lista) {
        long maxId = 0;
        Map<Long, Integer> maxNroPorNotif = new ConcurrentHashMap<>();
        for (FalNotificacionIntento i : lista) {
            store.put(i.getId(), i.copia());
            if (i.getId() > maxId) maxId = i.getId();
            maxNroPorNotif.merge(i.getNotificacionId(), (int) i.getNroIntento(), Math::max);
        }
        idCounter.set(maxId + 1);
        maxNroPorNotif.forEach((notifId, maxNro) ->
                correlativoPorNotif.put(notifId, new AtomicInteger(maxNro + 1)));
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
        correlativoPorNotif.clear();
        referenciasClamadas.clear();
    }

    @Override
    public String nombre() { return "notificacion-intentos"; }

    @Override
    public int size() { return store.size(); }
}
