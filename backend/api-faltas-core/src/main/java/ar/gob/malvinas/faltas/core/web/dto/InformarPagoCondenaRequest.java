package ar.gob.malvinas.faltas.core.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InformarPagoCondenaRequest(
        @NotNull BigDecimal monto,
        @NotBlank String referenciaPago,
        String observaciones
) {}
