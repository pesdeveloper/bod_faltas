package ar.gob.malvinas.faltas.core.domain.exception;

public class ActaArticuloInfringidoNoEncontradoException extends RuntimeException {
    public ActaArticuloInfringidoNoEncontradoException(Long id) {
        super("Artículo imputado no encontrado: id=" + id);
    }
}
