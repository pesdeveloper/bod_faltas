package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.enums.TipoAcuse;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacionAcuse;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acuses de notificacion.
 */
public interface NotificacionAcuseRepository {
    Long nextId();
    FalNotificacionAcuse guardar(FalNotificacionAcuse acuse);
    Optional<FalNotificacionAcuse> buscarPorId(Long id);
    List<FalNotificacionAcuse> buscarPorNotificacion(Long notificacionId);
    List<FalNotificacionAcuse> buscarPorIntento(Long intentoId);
    Optional<FalNotificacionAcuse> buscarPorIdempotencia(Long notificacionId, Long intentoId, TipoAcuse tipoAcuse);
}
