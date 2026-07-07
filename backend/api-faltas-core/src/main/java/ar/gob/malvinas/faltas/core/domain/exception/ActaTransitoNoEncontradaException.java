package ar.gob.malvinas.faltas.core.domain.exception;
public class ActaTransitoNoEncontradaException extends RuntimeException {
    public ActaTransitoNoEncontradaException(Long actaId) { super("Acta transito no encontrada: actaId=" + actaId); }
}
