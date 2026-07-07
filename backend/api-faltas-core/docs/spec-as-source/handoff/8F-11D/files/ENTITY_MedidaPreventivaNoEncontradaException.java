package ar.gob.malvinas.faltas.core.domain.exception;

public class MedidaPreventivaNoEncontradaException extends RuntimeException {
    public MedidaPreventivaNoEncontradaException(String msg) {
        super("Medida preventiva no encontrada: " + msg);
    }
    public MedidaPreventivaNoEncontradaException(Long id) {
        super("Medida preventiva no encontrada: id=" + id);
    }
}
