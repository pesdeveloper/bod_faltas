package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipo/proposito de una valorización de acta.
 * SMALLINT en fal_acta_valorizacion.tipo_valorizacion_acta.
 */
public enum TipoValorizacionActa {
    INFRACCION_BASE((short) 1),
    PAGO_VOLUNTARIO((short) 2),
    CONDENA((short) 3),
    AJUSTE_ITEM((short) 4),
    AJUSTE_TOTAL((short) 5),
    MODIFICACION_APELACION((short) 6),
    GESTION_EXTERNA((short) 7);

    private final short codigo;

    TipoValorizacionActa(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static TipoValorizacionActa fromCodigo(short codigo) {
        for (TipoValorizacionActa v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("TipoValorizacionActa desconocido: " + codigo);
    }
}
