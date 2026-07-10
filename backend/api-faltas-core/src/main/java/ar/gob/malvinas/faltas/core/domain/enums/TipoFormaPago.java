package ar.gob.malvinas.faltas.core.domain.enums;

public enum TipoFormaPago {
    RECIBO_AL_COBRO((short) 1),
    PLAN_PAGO((short) 2),
    REFINANCIACION((short) 3);

    private final short codigo;
    TipoFormaPago(short codigo) { this.codigo = codigo; }
    public short codigo() { return codigo; }
    public static TipoFormaPago fromCodigo(short codigo) {
        for (TipoFormaPago v : values()) if (v.codigo == codigo) return v;
        throw new IllegalArgumentException("TipoFormaPago desconocido: " + codigo);
    }
}