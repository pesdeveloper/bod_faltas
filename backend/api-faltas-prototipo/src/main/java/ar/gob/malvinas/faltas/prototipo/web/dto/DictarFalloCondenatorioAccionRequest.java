package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.math.BigDecimal;

/**
 * Body de la acción {@code dictar-fallo-condenatorio}. El monto de condena
 * es obligatorio y debe ser estrictamente mayor a cero; la validación de
 * rango la aplica el controller antes de invocar el store.
 *
 * <p>Conceptualmente distinto de {@code montoPagoVoluntario} (pago voluntario
 * previo al fallo). No genera comprobantes (sin EM, sin RC, sin
 * Cmte/Pref/Nro) ni habilita pago post condena en este slice.
 */
public record DictarFalloCondenatorioAccionRequest(BigDecimal montoCondena) {
}
