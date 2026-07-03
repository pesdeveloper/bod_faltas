package ar.gob.malvinas.faltas.core.domain.exception;

public class ActaNoEncontradaException extends RuntimeException {
    public ActaNoEncontradaException(Long idActa) {
        super("Acta no encontrada: " + idActa);
    }
    public ActaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
