package ar.gob.malvinas.faltas.core.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body de POST /api/faltas/pagos/resolver-pago-anterior.
 *
 * El actor no viaja en el body: se resuelve desde el JWT sub via
 * ActorContextHolder, igual que el resto de /api/faltas/pagos/**.
 */
public record ResolverPagoObligacionAnteriorRequest(
        @NotNull Long actaId,
        @NotNull Long movimientoPagoId,
        String motivo
) {}
