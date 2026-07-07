package ar.gob.malvinas.faltas.core.domain.exception;

public class PersonaNoEncontradaException extends RuntimeException {
    public PersonaNoEncontradaException(Long id) {
        super("Persona no encontrada: " + id);
    }
}
