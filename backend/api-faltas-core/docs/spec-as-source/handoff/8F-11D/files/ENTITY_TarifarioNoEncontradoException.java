package ar.gob.malvinas.faltas.core.domain.exception;

public class TarifarioNoEncontradoException extends RuntimeException {
    public TarifarioNoEncontradoException(Long id) {
        super("Tarifario de unidad no encontrado: id=" + id);
    }
}
