package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipo de persona fisica o juridica.
 * SMALLINT en fal_persona.tipo_persona.
 */
public enum TipoPersona {
    FISICA((short) 1),
    JURIDICA((short) 2);

    private final short codigo;

    TipoPersona(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static TipoPersona fromCodigo(short codigo) {
        for (TipoPersona v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("TipoPersona desconocido: " + codigo);
    }
}
