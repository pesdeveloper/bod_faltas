package ar.gob.malvinas.faltas.core.domain.exception;

public class DomicilioPersonaNoEncontradoException extends RuntimeException {
    public DomicilioPersonaNoEncontradoException(Long id) {
        super("Domicilio de persona no encontrado: " + id);
    }
}
