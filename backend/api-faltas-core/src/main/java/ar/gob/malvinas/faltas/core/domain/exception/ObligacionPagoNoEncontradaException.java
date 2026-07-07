package ar.gob.malvinas.faltas.core.domain.exception;

public class ObligacionPagoNoEncontradaException extends RuntimeException {
    public ObligacionPagoNoEncontradaException(Long id) {
        super("Obligacion de pago no encontrada: id=" + id);
    }
    public ObligacionPagoNoEncontradaException(String msg) {
        super("Obligacion de pago no encontrada: " + msg);
    }
}
