package ar.gob.malvinas.faltas.core.domain.exception;
public class ActaContravencionNoEncontradaException extends RuntimeException {
    public ActaContravencionNoEncontradaException(Long actaId) { super("Contravencion de acta no encontrada: actaId=" + actaId); }
}
