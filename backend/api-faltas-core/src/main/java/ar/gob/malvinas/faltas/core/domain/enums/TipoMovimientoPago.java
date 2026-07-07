package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipo de movimiento de pago de acta de faltas.
 * SMALLINT en fal_acta_pago_movimiento.tipo_movimiento.
 * 27 tipos exactos del modelo productivo.
 */
public enum TipoMovimientoPago {
    DEUDA_EMITIDA((short) 1),
    PAGO_CONTADO_GENERADO((short) 2),
    PAGO_PROCESADO((short) 3),
    PAGO_CONFIRMADO_TESORERIA((short) 4),
    PAGO_PROCESADO_ANULADO((short) 5),
    PAGO_CONFIRMADO_TESORERIA_ANULADO((short) 6),
    PLAN_GENERADO((short) 7),
    CUOTA_PAGO_PROCESADO((short) 8),
    CUOTA_PAGO_CONFIRMADO_TESORERIA((short) 9),
    CUOTA_PAGO_PROCESADO_ANULADO((short) 10),
    CUOTA_PAGO_CONFIRMADO_TESORERIA_ANULADO((short) 11),
    PLAN_EN_MORA((short) 12),
    PLAN_VENCIDO((short) 13),
    PLAN_CAIDO((short) 14),
    OBLIGACION_APTA_INTIMACION((short) 15),
    PLAN_APTO_INTIMACION((short) 16),
    INTIMACION_PAGO_GENERADA((short) 17),
    INTIMACION_PLAN_GENERADA((short) 18),
    PLAN_REFINANCIADO((short) 19),
    PLAN_CANCELADO((short) 20),
    OBLIGACION_CANCELADA((short) 21),
    PAGO_OBSERVADO((short) 22),
    PAGO_ANULADO((short) 23),
    OPERACION_TESORERIA_ANULADA((short) 24),
    CONTRACARGO_REGISTRADO((short) 25),
    MOVIMIENTO_REPROCESADO((short) 26),
    OTRO((short) 27);

    private final short codigo;

    TipoMovimientoPago(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static TipoMovimientoPago fromCodigo(short codigo) {
        for (TipoMovimientoPago v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("TipoMovimientoPago desconocido: " + codigo);
    }
}
