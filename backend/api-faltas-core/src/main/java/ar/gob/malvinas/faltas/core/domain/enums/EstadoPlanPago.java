package ar.gob.malvinas.faltas.core.domain.enums;

public enum EstadoPlanPago {
    ACTIVO((short) 1),
    FINALIZADO_POR_PAGO((short) 2),
    ANULADO((short) 3),
    REFINANCIADO((short) 4);

    private final short codigo;
    EstadoPlanPago(short codigo) { this.codigo = codigo; }
    public short codigo() { return codigo; }
    public static EstadoPlanPago fromCodigo(short codigo) {
        for (EstadoPlanPago v : values()) if (v.codigo == codigo) return v;
        throw new IllegalArgumentException("EstadoPlanPago desconocido: " + codigo);
    }
}
