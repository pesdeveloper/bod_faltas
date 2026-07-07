package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Resultado del fallo dictado sobre un acta de faltas (resultado_fallo en fal_acta_fallo).
 */
public enum ResultadoFalloActa {

    ABSUELVE((short) 1),
    CONDENA((short) 2),
    DECLARA_NULIDAD((short) 3),
    OTRO((short) 9);

    private final short codigo;

    ResultadoFalloActa(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static ResultadoFalloActa fromCodigo(short cod) {
        for (ResultadoFalloActa v : values()) {
            if (v.codigo == cod) return v;
        }
        throw new IllegalArgumentException("ResultadoFalloActa desconocido: " + cod);
    }
}