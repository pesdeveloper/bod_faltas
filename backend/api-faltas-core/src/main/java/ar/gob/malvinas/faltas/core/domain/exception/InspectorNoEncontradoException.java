package ar.gob.malvinas.faltas.core.domain.exception;

public class InspectorNoEncontradoException extends RuntimeException {
    public InspectorNoEncontradoException(Long idInsp) {
        super("Inspector no encontrado: " + idInsp);
    }
}
