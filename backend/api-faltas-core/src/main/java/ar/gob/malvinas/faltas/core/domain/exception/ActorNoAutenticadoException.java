package ar.gob.malvinas.faltas.core.domain.exception;

/**
 * Se lanza cuando se requiere un actor autenticado y no hay ninguno en el contexto.
 * Traducida por GlobalFaltasControllerAdvice a HTTP 401 con ErrorResponse canónico.
 */
public class ActorNoAutenticadoException extends RuntimeException {
    public ActorNoAutenticadoException() {
        super("Autenticacion requerida: no hay actor en el contexto de seguridad");
    }
}
