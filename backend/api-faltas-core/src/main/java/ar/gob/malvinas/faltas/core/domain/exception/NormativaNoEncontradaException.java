package ar.gob.malvinas.faltas.core.domain.exception;
public class NormativaNoEncontradaException extends RuntimeException {
    public NormativaNoEncontradaException(Long id) { super("Normativa no encontrada: " + id); }
    public NormativaNoEncontradaException(String cod, int ver) { super("Normativa no encontrada: " + cod + " v" + ver); }
}
