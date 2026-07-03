package ar.gob.malvinas.faltas.core.web.dto;

import java.math.BigDecimal;

public record InformarPagoCondenaRequest(
        BigDecimal monto,
        String referenciaPago,
        String observaciones
) {}
