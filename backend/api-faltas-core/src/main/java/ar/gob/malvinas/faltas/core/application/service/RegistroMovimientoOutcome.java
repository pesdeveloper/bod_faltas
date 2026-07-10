
package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.MovimientoRegistroResult;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;

public record RegistroMovimientoOutcome(MovimientoRegistroResult resultado, FalActaPagoMovimiento movimiento) {
}
