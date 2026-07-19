package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repositorio InMemory de FalNotificacion con semantica OCC real (copy-on-read/write).
 *
 * Contrato OCC (DECISION_DDL-NOTI-01):
 *   1. guardar version 0 (nueva).
 *   2. leer devuelve copia detached — mutar la copia no afecta el store.
 *   3. guardar existente: verifica versionRow; si coincide, incrementa y persiste copia.
 *   4. guardar con versionRow desactualizado lanza ConcurrenciaConflictoException.
 */
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
        FalNotificacion existing = store.get(notificacion.getId());
        if (existing == null) {
            if (notificacion.getVersionRow() != 0) {
                throw new ConcurrenciaConflictoException("FalNotificacion", notificacion.getId(),
                        0, notificacion.getVersionRow());
            }
            store.put(notificacion.getId(), notificacion.copia());
            return notificacion;
        }
        if (existing.getVersionRow() != notificacion.getVersionRow())
            throw new ConcurrenciaConflictoException("FalNotificacion", notificacion.getId(),
                    existing.getVersionRow(), notificacion.getVersionRow());
        notificacion.setVersionRow(existing.getVersionRow() + 1);
        store.put(notificacion.getId(), notificacion.copia());
        return notificacion;
    }

    @Override
    public Optional<FalNotificacion> buscarPorId(Long id) {
        FalNotificacion stored = store.get(id);
        return stored == null ? Optional.empty() : Optional.of(stored.copia());
    }

    @Override
    public List<FalNotificacion> buscarPorActa(Long idActa) {
        return store.values().stream()
                .filter(n -> idActa.equals(n.getIdActa()))
                .map(FalNotificacion::copia)
                .toList();
    }

    @Override
    public List<FalNotificacion> buscarPorDocumento(Long idDocumento) {
        return store.values().stream()
                .filter(n -> idDocumento.equals(n.getIdDocumento()))
                .map(FalNotificacion::copia)
                .toList();
    }

    @Override
    public Optional<FalNotificacion> buscarActivaPorDocumento(Long idDocumento) {
        return store.values().stream()
                .filter(n -> idDocumento.equals(n.getIdDocumento()) && n.estaActiva())
                .map(FalNotificacion::copia)
                .findFirst();
    }

    @Override
    public List<FalNotificacion> buscarPorEstado(EstadoNotificacion estado) {
        return store.values().stream()
                .filter(n -> estado.equals(n.getEstado()))
                .sorted(java.util.Comparator.comparing(FalNotificacion::getId))
                .map(FalNotificacion::copia)
                .toList();
    }

    @Override
    public void reset() { store.clear(); idGen.set(1); }

    @Override
    public String nombre() { return "Notificacion"; }

    @Override
    public int size() { return store.size(); }
}
