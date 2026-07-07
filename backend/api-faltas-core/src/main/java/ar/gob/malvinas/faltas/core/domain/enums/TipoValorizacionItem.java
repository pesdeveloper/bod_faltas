package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipo de ítem dentro de una valorización de acta.
 * SMALLINT en fal_acta_valorizacion_item.tipo_valorizacion_item.
 */
public enum TipoValorizacionItem {
    AUTOMATICA((short) 1),
    PAGO_VOLUNTARIO((short) 2),
    MANUAL((short) 3),
    FALLO((short) 4);

    private final short codigo;

    TipoValorizacionItem(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static TipoValorizacionItem fromCodigo(short codigo) {
        for (TipoValorizacionItem v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("TipoValorizacionItem desconocido: " + codigo);
    }
}
