package ar.gob.malvinas.faltas.core.domain.enums;

public enum ClasificacionPago {
    NORMAL((short) 1),
    DUPLICADO_REAL((short) 2),
    EXCEDENTE((short) 3);

    private final short codigo;
    ClasificacionPago(short codigo) { this.codigo = codigo; }
    public short codigo() { return codigo; }
    public static ClasificacionPago fromCodigo(short codigo) {
        for (ClasificacionPago v : values()) if (v.codigo == codigo) return v;
        throw new IllegalArgumentException("ClasificacionPago desconocida: " + codigo);
    }
}