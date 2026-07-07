package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.TipoMovimientoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Command para notificar un movimiento de pago externo (Ingresos/Tesoreria).
 * Equivalente a POST /api/faltas/pagos/notificar-movimiento.
 * referenciaExterna garantiza idempotencia cuando esta presente.
 */
public record NotificarMovimientoPagoCommand(
        Long obligacionPagoId,
        Long formaPagoId,
        Long planPagoRefId,
        TipoMovimientoPago tipoMovimiento,
        Short nroCuota,
        BigDecimal importeCapital,
        BigDecimal importeRima,
        BigDecimal importeTotal,
        String cmteEM,
        Short prefEM,
        Integer nroEM,
        String cmtePG,
        Short prefPG,
        Integer nroPG,
        Long idCierre,
        Long idOpe,
        LocalDateTime fhPagoProcesado,
        LocalDateTime fhPagoConfirmado,
        String referenciaExterna,
        LocalDateTime fhMovimiento,
        String idUser
) {}
