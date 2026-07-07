package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado de una valorización de acta.
 * SMALLINT en fal_acta_valorizacion.estado_valorizacion.
 */
public enum EstadoValorizacion {
    PRELIMINAR((short) 1),
    CONFIRMADA((short) 2),
    REEMPLAZADA((short) 3),
    ANULADA((short) 4);

    private final short codigo;

    EstadoValorizacion(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static EstadoValorizacion fromCodigo(short codigo) {
        for (EstadoValorizacion v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("EstadoValorizacion desconocido: " + codigo);
    }
}
