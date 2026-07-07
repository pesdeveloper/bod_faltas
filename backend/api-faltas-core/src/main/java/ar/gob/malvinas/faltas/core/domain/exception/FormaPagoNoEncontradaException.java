package ar.gob.malvinas.faltas.core.domain.exception;

public class FormaPagoNoEncontradaException extends RuntimeException {
    public FormaPagoNoEncontradaException(Long id) {
        super("Forma de pago no encontrada: id=" + id);
    }
    public FormaPagoNoEncontradaException(String msg) {
        super("Forma de pago no encontrada: " + msg);
    }
}
