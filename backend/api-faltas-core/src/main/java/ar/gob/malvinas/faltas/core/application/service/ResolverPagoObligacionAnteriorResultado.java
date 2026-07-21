package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resultado no persistido de ResolverPagoObligacionAnteriorService.resolver.
 *
 * No existe una tabla fal_acta_pago_resolucion: el efecto de la resolucion es
 * unicamente el movimientoAplicado (PAGO_CONFIRMADO, clasificacionPago=NORMAL,
 * movimientoOrigenId=movimientoOriginal.id) mas el evento PAGRES. Este record
 * es una vista de conveniencia calculada en el momento de la respuesta.
 *
 * En un reintento idempotente movimientoOriginal/movimientoAplicado/
 * importeAplicado son exactos (movimientoAplicado es inmutable), pero
 * saldoResultante/importeExcedente se recalculan contra el estado economico
 * actual de la obligacion aplicada (no hay snapshot historico persistido de
 * esos dos campos).
 */
public record ResolverPagoObligacionAnteriorResultado(
        FalActaPagoMovimiento movimientoOriginal,
        FalActaPagoMovimiento movimientoAplicado,
        Long obligacionOrigenId,
        Long obligacionAplicadaId,
        BigDecimal importeAplicado,
        BigDecimal saldoResultante,
        BigDecimal importeExcedente,
        String motivo,
        String actor,
        LocalDateTime fhResolucion
) {}
