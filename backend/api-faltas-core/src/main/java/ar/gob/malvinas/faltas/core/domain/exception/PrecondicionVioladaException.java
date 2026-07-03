package ar.gob.malvinas.faltas.core.domain.exception;

/**
 * Se lanza cuando una acción no puede ejecutarse porque no se cumplen
 * las precondiciones del dominio (bloque incorrecto, estado inválido,
 * documento sin firma, etc.).
 */
public class PrecondicionVioladaException extends RuntimeException {
    public PrecondicionVioladaException(String mensaje) {
        super(mensaje);
    }
}
