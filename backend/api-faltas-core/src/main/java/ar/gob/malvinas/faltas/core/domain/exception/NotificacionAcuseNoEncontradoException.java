package ar.gob.malvinas.faltas.core.domain.exception;

public class NotificacionAcuseNoEncontradoException extends RuntimeException {
    public NotificacionAcuseNoEncontradoException(Long id) {
        super("NotificacionAcuse no encontrado: id=" + id);
    }
}
