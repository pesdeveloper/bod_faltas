package ar.gob.malvinas.faltas.core.domain.exception;
public class ActaSustanciasNoEncontradaException extends RuntimeException {
    public ActaSustanciasNoEncontradaException(Long actaId) { super("Sustancias alimenticias de acta no encontradas: actaId=" + actaId); }
}
