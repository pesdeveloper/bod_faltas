package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.MotivoAnulacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento;

public record RevertirMovimientoPagoRequest(
        Long movimientoOriginalId,
        MotivoAnulacionPago motivo,
        String referenciaExterna,
        OrigenMovimiento origenMovimiento
) {}