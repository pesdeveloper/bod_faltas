package ar.gob.malvinas.faltas.core.domain.enums;

public enum EstadoObligacionPago {
    DETERMINADA((short) 1),
    PENDIENTE_FORMA_PAGO((short) 2),
    CON_FORMA_PAGO_VIGENTE((short) 3),
    CANCELADA_POR_PAGO((short) 4),
    REEMPLAZADA((short) 5),
    DEJADA_SIN_EFECTO((short) 6);

    private final short codigo;
    EstadoObligacionPago(short codigo) { this.codigo = codigo; }
    public short codigo() { return codigo; }
    public static EstadoObligacionPago fromCodigo(short codigo) {
        for (EstadoObligacionPago v : values()) if (v.codigo == codigo) return v;
        throw new IllegalArgumentException("EstadoObligacionPago desconocido: " + codigo);
    }
}
