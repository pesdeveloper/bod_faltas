package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPlanPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoMovimientoPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFormaPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPlanPagoRef;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Reductor determinista de estado operativo de pago.
 *
 * Proyecta estado de obligacion, forma y plan desde la lista de movimientos
 * ordenados por fhMovimiento (y por id como desempate).
 * Soporta fuera-de-orden: siempre recalcula desde el historial completo.
 * Movimientos anulados no contribuyen a la proyeccion activa.
 */
@Component
public class PagoMovimientoReducer {

    /**
     * Proyecta el estado de la obligacion desde los movimientos.
     * No modifica la obligacion; devuelve el estado calculado.
     */
    public EstadoObligacionPago proyectarEstadoObligacion(
            FalActaObligacionPago obligacion,
            List<FalActaPagoMovimiento> movimientos) {
        List<FalActaPagoMovimiento> ordenados = ordenar(movimientos);
        EstadoObligacionPago estado = EstadoObligacionPago.DETERMINADA;
        for (FalActaPagoMovimiento m : ordenados) {
            TipoMovimientoPago tipo = m.getTipoMovimiento();
            switch (tipo) {
                case DEUDA_EMITIDA -> estado = EstadoObligacionPago.DEUDA_EMITIDA;
                case PAGO_CONTADO_GENERADO -> estado = EstadoObligacionPago.CON_FORMA_PAGO;
                case PLAN_GENERADO -> estado = EstadoObligacionPago.EN_PLAN;
                case PLAN_REFINANCIADO -> estado = EstadoObligacionPago.REFINANCIADA;
                case OBLIGACION_CANCELADA -> estado = EstadoObligacionPago.CANCELADA;
                case PAGO_ANULADO, OPERACION_TESORERIA_ANULADA -> {
                    if (estado == EstadoObligacionPago.CANCELADA) {
                        estado = EstadoObligacionPago.CON_FORMA_PAGO;
                    }
                }
                case CONTRACARGO_REGISTRADO -> {
                    if (estado == EstadoObligacionPago.CANCELADA) {
                        estado = EstadoObligacionPago.CON_FORMA_PAGO;
                    }
                }
                default -> { /* otros movimientos no cambian estado de obligacion */ }
            }
        }
        return estado;
    }

    /**
     * Proyecta el estado de la forma de pago desde los movimientos asociados.
     */
    public EstadoFormaPago proyectarEstadoForma(
            FalActaFormaPago forma,
            List<FalActaPagoMovimiento> movimientosForma) {
        List<FalActaPagoMovimiento> ordenados = ordenar(movimientosForma);
        EstadoFormaPago estado = EstadoFormaPago.GENERADA;
        for (FalActaPagoMovimiento m : ordenados) {
            TipoMovimientoPago tipo = m.getTipoMovimiento();
            switch (tipo) {
                case PAGO_PROCESADO, CUOTA_PAGO_PROCESADO -> estado = EstadoFormaPago.PROCESADA;
                case PAGO_CONFIRMADO_TESORERIA, CUOTA_PAGO_CONFIRMADO_TESORERIA ->
                        estado = EstadoFormaPago.CONFIRMADA;
                case PAGO_PROCESADO_ANULADO, CUOTA_PAGO_PROCESADO_ANULADO -> {
                    if (estado == EstadoFormaPago.PROCESADA) estado = EstadoFormaPago.GENERADA;
                }
                case PAGO_CONFIRMADO_TESORERIA_ANULADO, CUOTA_PAGO_CONFIRMADO_TESORERIA_ANULADO -> {
                    if (estado == EstadoFormaPago.CONFIRMADA) estado = EstadoFormaPago.PROCESADA;
                }
                case PLAN_REFINANCIADO -> estado = EstadoFormaPago.REEMPLAZADA;
                default -> { }
            }
        }
        return estado;
    }

    /**
     * Proyecta el estado del plan de pago desde los movimientos asociados.
     */
    public EstadoPlanPago proyectarEstadoPlan(
            FalActaPlanPagoRef plan,
            List<FalActaPagoMovimiento> movimientosPlan) {
        List<FalActaPagoMovimiento> ordenados = ordenar(movimientosPlan);
        EstadoPlanPago estado = EstadoPlanPago.ACTIVO;
        for (FalActaPagoMovimiento m : ordenados) {
            TipoMovimientoPago tipo = m.getTipoMovimiento();
            switch (tipo) {
                case PLAN_EN_MORA -> estado = EstadoPlanPago.EN_MORA;
                case PLAN_VENCIDO -> estado = EstadoPlanPago.VENCIDO;
                case PLAN_CAIDO -> estado = EstadoPlanPago.CAIDO;
                case PLAN_REFINANCIADO -> estado = EstadoPlanPago.REFINANCIADO;
                case PLAN_CANCELADO -> estado = EstadoPlanPago.CANCELADO;
                case CUOTA_PAGO_PROCESADO, CUOTA_PAGO_CONFIRMADO_TESORERIA -> {
                    if (estado == EstadoPlanPago.EN_MORA || estado == EstadoPlanPago.VENCIDO) {
                        estado = EstadoPlanPago.ACTIVO;
                    }
                }
                default -> { }
            }
        }
        return estado;
    }

    /**
     * Indica si hay pago procesado activo (no anulado).
     */
    public boolean hayPagoProcesadoActivo(List<FalActaPagoMovimiento> movimientos) {
        List<FalActaPagoMovimiento> ordenados = ordenar(movimientos);
        boolean procesado = false;
        for (FalActaPagoMovimiento m : ordenados) {
            switch (m.getTipoMovimiento()) {
                case PAGO_PROCESADO -> procesado = true;
                case PAGO_PROCESADO_ANULADO -> procesado = false;
                default -> { }
            }
        }
        return procesado;
    }

    /**
     * Indica si hay pago confirmado activo (no anulado).
     */
    public boolean hayPagoConfirmadoActivo(List<FalActaPagoMovimiento> movimientos) {
        List<FalActaPagoMovimiento> ordenados = ordenar(movimientos);
        boolean confirmado = false;
        for (FalActaPagoMovimiento m : ordenados) {
            switch (m.getTipoMovimiento()) {
                case PAGO_CONFIRMADO_TESORERIA -> confirmado = true;
                case PAGO_CONFIRMADO_TESORERIA_ANULADO, CONTRACARGO_REGISTRADO -> confirmado = false;
                default -> { }
            }
        }
        return confirmado;
    }

    private List<FalActaPagoMovimiento> ordenar(List<FalActaPagoMovimiento> movimientos) {
        return movimientos.stream()
                .sorted(Comparator.comparing(FalActaPagoMovimiento::getFhMovimiento)
                        .thenComparingLong(FalActaPagoMovimiento::getId))
                .toList();
    }
}
