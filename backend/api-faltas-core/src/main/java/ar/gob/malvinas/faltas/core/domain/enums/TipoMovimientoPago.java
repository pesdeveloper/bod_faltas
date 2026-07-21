package ar.gob.malvinas.faltas.core.domain.enums;

public enum TipoMovimientoPago {
    DEUDA_EMITIDA((short) 1),
    PAGO_PROCESADO((short) 2),
    PAGO_CONFIRMADO((short) 3),
    PAGO_REVERTIDO((short) 4),
    EMISION_ANULADA((short) 5);

    private final short codigo;
    TipoMovimientoPago(short codigo) { this.codigo = codigo; }
    public short codigo() { return codigo; }
    public static TipoMovimientoPago fromCodigo(short codigo) {
        for (TipoMovimientoPago v : values()) if (v.codigo == codigo) return v;
        throw new IllegalArgumentException("TipoMovimientoPago desconocido: " + codigo);
    }
}
