package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Unidad de medida del resultado numerico de alcoholemia.
 * SMALLINT en fal_acta_transito_alcoholemia.unidad_medida.
 */
public enum UnidadMedidaAlcoholemia {
    G_L((short) 1),
    MG_L_AIRE((short) 2);

    private final short codigo;

    UnidadMedidaAlcoholemia(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static UnidadMedidaAlcoholemia fromCodigo(short codigo) {
        for (UnidadMedidaAlcoholemia v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("UnidadMedidaAlcoholemia desconocido: " + codigo);
    }
}
