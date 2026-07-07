package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Resultado cualitativo de prueba de alcoholemia.
 * SMALLINT en fal_acta_transito_alcoholemia.resultado_cualitativo.
 */
public enum ResultadoCualitativoAlcoholemia {
    NEGATIVO((short) 1),
    POSITIVO((short) 2),
    INVALIDO((short) 3),
    NO_REALIZADO((short) 4);

    private final short codigo;

    ResultadoCualitativoAlcoholemia(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static ResultadoCualitativoAlcoholemia fromCodigo(short codigo) {
        for (ResultadoCualitativoAlcoholemia v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("ResultadoCualitativoAlcoholemia desconocido: " + codigo);
    }
}
