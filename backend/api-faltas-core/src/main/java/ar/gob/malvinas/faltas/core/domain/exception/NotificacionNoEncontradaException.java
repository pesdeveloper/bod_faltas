package ar.gob.malvinas.faltas.core.domain.exception;

public class NotificacionNoEncontradaException extends RuntimeException {
    public NotificacionNoEncontradaException(String idNotificacion) {
        super("Notificacion no encontrada: " + idNotificacion);
    }
}
