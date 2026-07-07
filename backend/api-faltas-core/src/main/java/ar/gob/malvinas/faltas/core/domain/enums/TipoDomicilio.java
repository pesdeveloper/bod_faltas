package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipo de domicilio de la persona.
 * SMALLINT en fal_persona_domicilio.tipo_domicilio.
 */
public enum TipoDomicilio {
    REAL((short) 1),
    LEGAL((short) 2),
    FISCAL((short) 3),
    CONSTITUIDO((short) 4),
    HALLADO((short) 5),
    OTRO((short) 6);

    private final short codigo;

    TipoDomicilio(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static TipoDomicilio fromCodigo(short codigo) {
        for (TipoDomicilio v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("TipoDomicilio desconocido: " + codigo);
    }
}
