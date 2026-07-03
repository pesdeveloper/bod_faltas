package ar.gob.malvinas.faltas.core.application.command;

import java.math.BigDecimal;

public record InformarPagoCondenaCommand(
        Long actaId,
        BigDecimal monto,
        String referenciaPago,
        String observaciones
) {}

