package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipo de unidad usada en el tarifario de faltas.
 * SMALLINT en fal_tarifario_unidad_faltas.tipo_unidad y fal_acta_valorizacion.
 */
public enum TipoUnidadFaltas {
    SALARIO((short) 1),
    UNIDAD_FIJA((short) 2),
    MONTO((short) 3);

    private final short codigo;

    TipoUnidadFaltas(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static TipoUnidadFaltas fromCodigo(short codigo) {
        for (TipoUnidadFaltas v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("TipoUnidadFaltas desconocido: " + codigo);
    }
}
