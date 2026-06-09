package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.math.BigDecimal;

public record ConfirmarPagoVoluntarioExternoAccionRequest(
        String origen,
        String referenciaPago,
        String medioPago,
        String fechaPago,
        BigDecimal monto,
        String observaciones) {
}
