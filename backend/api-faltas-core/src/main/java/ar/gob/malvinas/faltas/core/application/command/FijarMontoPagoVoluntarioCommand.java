package ar.gob.malvinas.faltas.core.application.command;

import java.math.BigDecimal;

public record FijarMontoPagoVoluntarioCommand(
        Long actaId,
        BigDecimal monto,
        String observaciones
) {}
