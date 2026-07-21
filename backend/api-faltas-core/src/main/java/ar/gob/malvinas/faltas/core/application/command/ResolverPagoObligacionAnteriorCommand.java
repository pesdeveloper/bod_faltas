package ar.gob.malvinas.faltas.core.application.command;

/**
 * Resuelve administrativamente un pago (FalActaPagoMovimiento PAGO_CONFIRMADO,
 * clasificacion OBLIGACION_ANTERIOR, evento PAGANT ya registrado) contra la
 * obligacion vigente del acta.
 *
 * No duplica datos obtenibles de entidades relacionadas: la obligacion
 * anterior/origen se deriva de movimientoPagoId.obligacionPagoId y la
 * obligacion vigente contra la que se aplica se deriva de actaId. El importe
 * aplicado es siempre el importe total del movimiento original, completo y
 * sin recortar contra el saldo vigente (el saldo resultante puede quedar en
 * cero o negativo, representando un excedente informativo).
 *
 * No se crea ninguna obligacion nueva ni tabla de resolucion propia: el
 * efecto de esta resolucion es un unico movimiento de aplicacion
 * (PAGO_CONFIRMADO, clasificacionPago=NORMAL) contra la obligacion vigente,
 * enlazado al movimiento original via movimientoOrigenId, mas el evento
 * PAGRES.
 *
 * Idempotencia: unicidad de aplicacion por movimientoOrigenId en
 * FalActaPagoMovimiento. Un reintento exacto (mismo movimientoPagoId, misma
 * obligacion destino y motivo equivalente) devuelve el mismo resultado sin
 * efectos adicionales; un reintento incompatible (distinta obligacion
 * destino o motivo) es un conflicto (409).
 */
public record ResolverPagoObligacionAnteriorCommand(
        Long actaId,
        Long movimientoPagoId,
        String motivo,
        String idUser
) {}
