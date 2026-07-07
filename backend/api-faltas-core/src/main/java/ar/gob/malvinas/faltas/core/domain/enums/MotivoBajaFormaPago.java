package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Motivo de baja de una forma de pago.
 * SMALLINT en fal_acta_forma_pago.motivo_baja.
 */
public enum MotivoBajaFormaPago {
    REFINANCIACION((short) 1),
    ANULACION_OBLIGACION((short) 2),
    ERROR_OPERATIVO((short) 3),
    OTRO((short) 4);

    private final short codigo;

    MotivoBajaFormaPago(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static MotivoBajaFormaPago fromCodigo(short codigo) {
        for (MotivoBajaFormaPago v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("MotivoBajaFormaPago desconocido: " + codigo);
    }
}
