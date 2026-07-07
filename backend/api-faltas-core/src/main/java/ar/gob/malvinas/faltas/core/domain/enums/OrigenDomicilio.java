package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Origen del domicilio: por que via fue obtenido.
 * SMALLINT en fal_persona_domicilio.origen_domicilio.
 *
 * INVESTIGACION usa nombre ASCII puro (sin tilde).
 */
public enum OrigenDomicilio {
    LABRADO((short) 1),
    INVESTIGACION((short) 2),
    DDJJ((short) 3),
    REINTENTO((short) 4),
    PORTAL((short) 5),
    EXTERNO((short) 6),
    OPERADOR((short) 7);

    private final short codigo;

    OrigenDomicilio(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static OrigenDomicilio fromCodigo(short codigo) {
        for (OrigenDomicilio v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("OrigenDomicilio desconocido: " + codigo);
    }
}
