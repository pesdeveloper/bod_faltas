package ar.gob.malvinas.faltas.core.domain.exception;

public class MovimientoPagoDuplicadoException extends RuntimeException {
    public MovimientoPagoDuplicadoException(String referenciaExterna) {
        super("Movimiento de pago duplicado: referenciaExterna=" + referenciaExterna);
    }
    public MovimientoPagoDuplicadoException(String campo, Object valor) {
        super("Movimiento de pago duplicado: " + campo + "=" + valor);
    }
}
