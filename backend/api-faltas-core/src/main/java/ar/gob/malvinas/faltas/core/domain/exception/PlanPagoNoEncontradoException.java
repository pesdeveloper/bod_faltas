package ar.gob.malvinas.faltas.core.domain.exception;

public class PlanPagoNoEncontradoException extends RuntimeException {
    public PlanPagoNoEncontradoException(Long id) {
        super("Plan de pago no encontrado: id=" + id);
    }
    public PlanPagoNoEncontradoException(String msg) {
        super("Plan de pago no encontrado: " + msg);
    }
}
