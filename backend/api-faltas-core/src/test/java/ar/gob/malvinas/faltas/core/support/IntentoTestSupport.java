package ar.gob.malvinas.faltas.core.support;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacionIntento;
import ar.gob.malvinas.faltas.core.repository.NotificacionIntentoRepository;

import java.util.List;

/**
 * Soporte de test para resolver el intento concreto sobre el que se registra un
 * resultado notificatorio positivo (CMD-FALLO-004).
 *
 * Exige exactamente un intento activo (EN_PROCESO, sin resultado) en la cabecera indicada,
 * conforme al contrato del slice. Si hay 0 o mas de 1, es un error de fixture y se rechaza.
 */
public final class IntentoTestSupport {

    private IntentoTestSupport() {}

    public static Long intentoActivo(NotificacionIntentoRepository repo, long idNotificacion) {
        List<Long> activos = repo.buscarPorNotificacion(idNotificacion).stream()
                .filter(i -> i.getEstadoIntento() == EstadoNotificacion.EN_PROCESO
                        && i.getResultadoIntento() == null)
                .map(FalNotificacionIntento::getId)
                .toList();
        if (activos.size() != 1)
            throw new IllegalStateException(
                    "Se esperaba exactamente un intento EN_PROCESO sin resultado para la notificacion "
                            + idNotificacion + ", encontrados: " + activos.size() + ".");
        return activos.get(0);
    }
}
