package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoNotificacion;
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

    /**
     * Retorna la unica notificacion activa para el documento indicado.
     * Una notificacion es activa mientras su estado no sea SIN_EFECTO.
     * Debe existir como maximo una por documento.
     */
    Optional<FalNotificacion> buscarActivaPorDocumento(Long idDocumento);

    /**
     * Retorna todas las notificaciones en el estado indicado, ordenadas por id.
     */
    List<FalNotificacion> buscarPorEstado(EstadoNotificacion estado);
}
