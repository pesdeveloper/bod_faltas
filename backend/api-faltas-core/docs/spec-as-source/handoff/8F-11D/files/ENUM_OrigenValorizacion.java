package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Actor que origina una valorización de acta.
 * SMALLINT en fal_acta_valorizacion.origen_valorizacion.
 * No confundir con el tipo de valorización.
 */
public enum OrigenValorizacion {
    SISTEMA((short) 1),
    DIRECCION_FALTAS((short) 2),
    JUEZ_ADMINISTRATIVO((short) 3),
    JUEZ_PAZ((short) 4),
    APREMIO((short) 5);

    private final short codigo;

    OrigenValorizacion(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static OrigenValorizacion fromCodigo(short codigo) {
        for (OrigenValorizacion v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("OrigenValorizacion desconocido: " + codigo);
    }
}
