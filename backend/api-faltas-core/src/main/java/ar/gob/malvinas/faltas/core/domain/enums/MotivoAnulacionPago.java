package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Motivo de anulacion de un movimiento de pago.
 * SMALLINT en fal_acta_pago_movimiento.motivo_anulacion_pago.
 */
public enum MotivoAnulacionPago {
    CONTRACARGO((short) 1),
    ANULACION_TESORERIA((short) 2),
    ERROR_OPERATIVO((short) 3),
    DUPLICADO((short) 4),
    REVERSION_MEDIO_PAGO((short) 5),
    OTRO((short) 6);

    private final short codigo;

    MotivoAnulacionPago(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static MotivoAnulacionPago fromCodigo(short codigo) {
        for (MotivoAnulacionPago v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("MotivoAnulacionPago desconocido: " + codigo);
    }
}
