package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.ClasificacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenConfirmacion;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoMovimientoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record NotificarMovimientoPagoCommand(
        Long obligacionPagoId,
        Long formaPagoId,
        Long planPagoRefId,
        TipoMovimientoPago tipoMovimiento,
        OrigenMovimiento origenMovimiento,
        OrigenConfirmacion origenConfirmacion,
        Long evidenciaDocumentoId,
        ClasificacionPago clasificacionPago,
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
