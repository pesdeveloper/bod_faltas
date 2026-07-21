package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPlanPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFormaPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPlanPagoRef;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PagoMovimientoReducer {

    private final EconomiaProyeccionRecalculador recalculador;

    public PagoMovimientoReducer(EconomiaProyeccionRecalculador recalculador) {
        this.recalculador = recalculador;
    }

    public EstadoObligacionPago proyectarEstadoObligacion(FalActaObligacionPago obligacion, List<FalActaPagoMovimiento> movimientos) {
        return recalculador.proyectarEstadoObligacion(obligacion, movimientos);
    }

    public EstadoFormaPago proyectarEstadoForma(FalActaFormaPago forma, List<FalActaPagoMovimiento> movimientosForma) {
        return recalculador.proyectarEstadoForma(forma, movimientosForma);
    }

    public EstadoPlanPago proyectarEstadoPlan(FalActaPlanPagoRef plan, List<FalActaPagoMovimiento> movimientosPlan) {
        return recalculador.proyectarEstadoPlan(plan, movimientosPlan);
    }

    public boolean hayPagoProcesadoActivo(List<FalActaPagoMovimiento> movimientos) {
        return recalculador.hayPagoProcesadoActivo(movimientos);
    }

    public boolean hayPagoConfirmadoActivo(List<FalActaPagoMovimiento> movimientos) {
        return recalculador.hayPagoConfirmadoActivo(movimientos);
    }
}
