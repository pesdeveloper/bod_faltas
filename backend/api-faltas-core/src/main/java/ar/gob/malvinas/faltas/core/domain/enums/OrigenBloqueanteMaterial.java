package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Origen del bloqueante material registrado sobre un acta.
 * SMALLINT en {@code fal_acta_bloqueante_cierre_material.origen} y en
 * {@code fal_medida_preventiva.tipo_bloqueante_default}.
 *
 * <p>{@code codigo()}/{@code fromCodigo()} agregados en Wave 4 (alta integral JDBC): el enum no
 * tenia mapeo fisico explicito porque hasta esa wave solo se usaba en memoria. El orden de
 * declaracion ya vigente se preserva exactamente como codigo (1..4).
 */
public enum OrigenBloqueanteMaterial {
    MEDIDA_PREVENTIVA((short) 1),
    RODADO((short) 2),
    DOCUMENTACION_RETENIDA((short) 3),
    OTRO((short) 4);

    private final short codigo;

    OrigenBloqueanteMaterial(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static OrigenBloqueanteMaterial fromCodigo(short codigo) {
        for (OrigenBloqueanteMaterial v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("OrigenBloqueanteMaterial desconocido: " + codigo);
    }
}
