package ar.gob.malvinas.faltas.core.domain.exception;

public class NotificacionIntentoNoEncontradoException extends RuntimeException {
    public NotificacionIntentoNoEncontradoException(Long id) {
        super("NotificacionIntento no encontrado: id=" + id);
    }
    public NotificacionIntentoNoEncontradoException(Long notificacionId, short nroIntento) {
        super("NotificacionIntento no encontrado: notificacionId=" + notificacionId + ", nroIntento=" + nroIntento);
    }
}
