package ar.gob.malvinas.faltas.core.domain.enums;

public enum EstadoFormaPago {
    GENERADA((short) 1),
    VIGENTE((short) 2),
    VENCIDA((short) 3),
    PAGADA((short) 4),
    ANULADA((short) 5),
    REEMPLAZADA((short) 6);

    private final short codigo;
    EstadoFormaPago(short codigo) { this.codigo = codigo; }
    public short codigo() { return codigo; }
    public static EstadoFormaPago fromCodigo(short codigo) {
        for (EstadoFormaPago v : values()) if (v.codigo == codigo) return v;
        throw new IllegalArgumentException("EstadoFormaPago desconocido: " + codigo);
    }
}