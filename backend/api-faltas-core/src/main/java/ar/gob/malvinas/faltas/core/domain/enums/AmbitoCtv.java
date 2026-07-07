package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Ambito de la contravencion o actividad comercial registrada en el acta.
 * SMALLINT en fal_acta_contravencion.ambito_ctv y fal_acta_sustancias_alimenticias.ambito_ctv.
 * Compartido entre contravencion y sustancias alimenticias.
 */
public enum AmbitoCtv {
    BALDIO((short) 1),
    COMERCIO((short) 2),
    INDUSTRIA((short) 3),
    VIVIENDA((short) 4),
    LOCAL((short) 5),
    OTRO((short) 6);

    private final short codigo;

    AmbitoCtv(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static AmbitoCtv fromCodigo(short codigo) {
        for (AmbitoCtv v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("AmbitoCtv desconocido: " + codigo);
    }
}
