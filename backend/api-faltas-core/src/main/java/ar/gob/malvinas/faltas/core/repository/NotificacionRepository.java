package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de notificaciones del expediente.
 */
public interface NotificacionRepository {
    FalNotificacion guardar(FalNotificacion notificacion);
    Optional<FalNotificacion> buscarPorId(String id);
    List<FalNotificacion> buscarPorActa(Long idActa);
    List<FalNotificacion> buscarPorDocumento(String idDocumento);
}

