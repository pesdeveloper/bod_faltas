package ar.gob.malvinas.faltas.core.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Comando interno para informar el pago de condena.
 *
 * Actor proviene exclusivamente del sub del JWT; no se acepta actor del body.
 * Un re-informe valido sobre PENDIENTE/INFORMADO/OBSERVADO emite un nuevo PCOINF.
 * Un pago en CONFIRMADO rechaza el re-informe.
 * No cierra el acta ni emite CIERRA.
 */
public record InformarPagoCondenaCommand(
        @NotNull Long actaId,
        @NotNull BigDecimal monto,
        @NotBlank String referenciaPago,
        String observaciones,
        @NotBlank @Size(max = 36) String actor
) {}
