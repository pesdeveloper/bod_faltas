package ar.gob.malvinas.faltas.core.domain.enums;

public enum MotivoPlanCaidoCalculado {
    CUOTAS_EN_MORA((short) 1),
    MORA_CONSECUTIVA((short) 2),
    ANTIGUEDAD_MORA((short) 3),
    REGLA_INGRESOS((short) 4),
    COMBINADA((short) 5);

    private final short codigo;
    MotivoPlanCaidoCalculado(short codigo) { this.codigo = codigo; }
    public short codigo() { return codigo; }
    public static MotivoPlanCaidoCalculado fromCodigo(short codigo) {
        for (MotivoPlanCaidoCalculado v : values()) if (v.codigo == codigo) return v;
        throw new IllegalArgumentException("MotivoPlanCaidoCalculado desconocido: " + codigo);
    }
}