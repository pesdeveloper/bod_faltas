package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado de una forma de pago.
 * SMALLINT en fal_acta_forma_pago.estado_forma_pago.
 *
 * GENERADA     -> forma generada, sin pago aun
 * PROCESADA    -> pago informado/procesado (no habilita cierre)
 * CONFIRMADA   -> pago confirmado por Tesoreria (puede habilitar cierre)
 * REEMPLAZADA  -> superada por refinanciacion
 * BAJA         -> dada de baja por cualquier otro motivo
 */
public enum EstadoFormaPago {
    GENERADA((short) 1),
    PROCESADA((short) 2),
    CONFIRMADA((short) 3),
    REEMPLAZADA((short) 4),
    BAJA((short) 5);

    private final short codigo;

    EstadoFormaPago(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static EstadoFormaPago fromCodigo(short codigo) {
        for (EstadoFormaPago v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("EstadoFormaPago desconocido: " + codigo);
    }
}
