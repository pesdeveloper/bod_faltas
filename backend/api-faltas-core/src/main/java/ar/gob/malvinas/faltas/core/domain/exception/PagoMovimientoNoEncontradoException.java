package ar.gob.malvinas.faltas.core.domain.exception;

public class PagoMovimientoNoEncontradoException extends RuntimeException {
    public PagoMovimientoNoEncontradoException(Long id) {
        super("Movimiento de pago no encontrado: id=" + id);
    }
    public PagoMovimientoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
