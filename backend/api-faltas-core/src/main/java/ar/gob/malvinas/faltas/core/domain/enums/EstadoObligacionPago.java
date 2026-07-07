package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado de una obligacion de pago de acta de faltas.
 * SMALLINT en fal_acta_obligacion_pago.estado_obligacion.
 *
 * Estados validos:
 *   DETERMINADA     -> recien creada, vigente
 *   DEUDA_EMITIDA   -> deuda generada en Ingresos (movimiento DEUDA_EMITIDA)
 *   CON_FORMA_PAGO  -> tiene forma de pago asignada (contado o plan)
 *   EN_PLAN         -> tiene plan de pago activo
 *   REFINANCIADA    -> plan refinanciado vigente
 *   CANCELADA       -> pago total confirmado
 *   ANULADA         -> anulada/reemplazada (p.ej. voluntario sustituido por condena)
 */
public enum EstadoObligacionPago {
    DETERMINADA((short) 1),
    DEUDA_EMITIDA((short) 2),
    CON_FORMA_PAGO((short) 3),
    EN_PLAN((short) 4),
    REFINANCIADA((short) 5),
    CANCELADA((short) 6),
    ANULADA((short) 7);

    private final short codigo;

    EstadoObligacionPago(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static EstadoObligacionPago fromCodigo(short codigo) {
        for (EstadoObligacionPago v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("EstadoObligacionPago desconocido: " + codigo);
    }
}
