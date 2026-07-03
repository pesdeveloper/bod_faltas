package ar.gob.malvinas.faltas.core.domain.exception;
public class ArticuloNormativaNoEncontradoException extends RuntimeException {
    public ArticuloNormativaNoEncontradoException(Long id) { super("Articulo de normativa no encontrado: " + id); }
}
