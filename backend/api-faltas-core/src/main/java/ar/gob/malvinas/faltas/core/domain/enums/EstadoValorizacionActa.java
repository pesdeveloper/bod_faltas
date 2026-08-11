package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado de la cabecera unica de valorizacion de un acta.
 * SMALLINT en {@code fal_acta_valorizacion.estado_valorizacion}.
 *
 * Rediseño PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7.8: cabecera unica y mutable por
 * acta (no multiples filas por acta+tipo). El alta calcula PRELIMINAR y nunca
 * confirma: la confirmacion es una operacion posterior (fuera del alcance del
 * alta, prevista para ENRI en una wave futura).
 */
public enum EstadoValorizacionActa {
    PRELIMINAR((short) 1),
    CONFIRMADA((short) 2);

    private final short codigo;

    EstadoValorizacionActa(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static EstadoValorizacionActa fromCodigo(short codigo) {
        for (EstadoValorizacionActa v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("EstadoValorizacionActa desconocido: " + codigo);
    }
}
