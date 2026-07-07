package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipo de prueba de alcoholemia.
 * SMALLINT en fal_acta_transito_alcoholemia.tipo_prueba.
 */
public enum TipoPruebaAlcoholemia {
    ALOMETRO((short) 1),
    ALCOHOLIMETRO((short) 2);

    private final short codigo;

    TipoPruebaAlcoholemia(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static TipoPruebaAlcoholemia fromCodigo(short codigo) {
        for (TipoPruebaAlcoholemia v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("TipoPruebaAlcoholemia desconocido: " + codigo);
    }
}
