package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.math.BigDecimal;

/**
 * Body de la acción administrativa "Pago voluntario" (habilitación con
 * monto fijado por Dirección de Faltas). El monto es obligatorio y debe
 * ser estrictamente mayor a cero; la validación de rango la aplica el
 * controller antes de invocar el store.
 *
 * <p>El portal del infractor todavía no existe en este prototipo: por
 * eso la acción no genera comprobantes (no EM, no RC, no Cmte/Pref/Nro)
 * ni materializa proceso externo de pago. Sólo fija el monto del acta y
 * deja el pago voluntario habilitado para el futuro portal.
 */
public record RegistrarSolicitudPagoVoluntarioAccionRequest(BigDecimal monto) {
}
