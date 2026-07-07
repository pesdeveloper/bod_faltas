package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipo/origen de una obligacion de pago de acta de faltas.
 * SMALLINT en fal_acta_obligacion_pago.tipo_obligacion.
 */
public enum TipoObligacionPago {
    PAGO_VOLUNTARIO((short) 1),
    CONDENA((short) 2);

    private final short codigo;

    TipoObligacionPago(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static TipoObligacionPago fromCodigo(short codigo) {
        for (TipoObligacionPago v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("TipoObligacionPago desconocido: " + codigo);
    }
}
