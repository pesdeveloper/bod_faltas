package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Origen del dia no computable registrado en el calendario administrativo local.
 *
 * Codigo numerico persistible (DECISION_DDL-ENUM-01): SMALLINT, prohibido ordinal().
 */
public enum OrigenDiaNoComputable {
    MANUAL((short) 1),
    SINCRONIZACION_EXTERNA((short) 2);

    private final short codigo;

    OrigenDiaNoComputable(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static OrigenDiaNoComputable desdeCodigo(short codigo) {
        for (OrigenDiaNoComputable v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("OrigenDiaNoComputable sin codigo: " + codigo);
    }
}
