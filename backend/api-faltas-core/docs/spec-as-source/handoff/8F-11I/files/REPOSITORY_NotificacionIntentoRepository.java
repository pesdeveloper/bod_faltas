package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalNotificacionIntento;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de intentos de notificacion.
 * El correlativo nroIntento es unico por notificacionId y se asigna atomicamente.
 */
public interface NotificacionIntentoRepository {
    Long nextId();
    short siguienteNroIntento(Long notificacionId);
    FalNotificacionIntento guardar(FalNotificacionIntento intento);
    Optional<FalNotificacionIntento> buscarPorId(Long id);
    List<FalNotificacionIntento> buscarPorNotificacion(Long notificacionId);
    Optional<FalNotificacionIntento> buscarPorNroIntento(Long notificacionId, short nroIntento);
    Optional<FalNotificacionIntento> buscarPorReferenciaExterna(String referenciaExterna);
    /** Atomically claims a referenciaExterna. Returns true if claimed, false if already taken. */
    boolean claimReferenciaExterna(String ref);
}
