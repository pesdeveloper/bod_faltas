package ar.gob.malvinas.faltas.core.domain.exception;

public class ValorizacionNoEncontradaException extends RuntimeException {
    public ValorizacionNoEncontradaException(Long id) {
        super("Valorización no encontrada: id=" + id);
    }
    public ValorizacionNoEncontradaException(String msg) {
        super("Valorización no encontrada: " + msg);
    }
}
