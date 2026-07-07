package ar.gob.malvinas.faltas.core.domain.exception;
public class ActaTransitoAlcoholemiaNoEncontradaException extends RuntimeException {
    public ActaTransitoAlcoholemiaNoEncontradaException(Long id) { super("Medicion alcoholemia no encontrada: id=" + id); }
    public ActaTransitoAlcoholemiaNoEncontradaException(String msg) { super(msg); }
}