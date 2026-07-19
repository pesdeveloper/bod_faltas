package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo de tipos de acta.
 * Cada dependencia versionada define un unico tipo_acta que puede labrar.
 */
public enum TipoActa {

    TRANSITO((short) 1),
    CONTRAVENCION((short) 2),
    SUSTANCIAS_ALIMENTICIAS((short) 3),
    COMERCIO((short) 4);

    private final short codigo;

    TipoActa(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static TipoActa fromCodigo(short codigo) {
        for (TipoActa v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("TipoActa sin codigo: " + codigo);
    }
}
