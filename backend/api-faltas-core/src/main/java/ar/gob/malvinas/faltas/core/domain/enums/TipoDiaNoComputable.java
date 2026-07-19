package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Clasifica la naturaleza del dia no computable registrado en el calendario administrativo local.
 *
 * Codigo numerico persistible (DECISION_DDL-ENUM-01): SMALLINT, prohibido ordinal().
 */
public enum TipoDiaNoComputable {
    FERIADO((short) 1),
    ASUETO_ADMINISTRATIVO((short) 2),
    OTRO((short) 3);

    private final short codigo;

    TipoDiaNoComputable(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static TipoDiaNoComputable desdeCodigo(short codigo) {
        for (TipoDiaNoComputable v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("TipoDiaNoComputable sin codigo: " + codigo);
    }
}
