package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de notificaciones del expediente.
 */
public interface NotificacionRepository {
    Long nextId();
    FalNotificacion guardar(FalNotificacion notificacion);
    Optional<FalNotificacion> buscarPorId(Long id);
    List<FalNotificacion> buscarPorActa(Long idActa);
    List<FalNotificacion> buscarPorDocumento(Long idDocumento);
}