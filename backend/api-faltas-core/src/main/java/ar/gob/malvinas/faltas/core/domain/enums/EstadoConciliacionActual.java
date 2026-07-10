package ar.gob.malvinas.faltas.core.domain.enums;

public enum EstadoConciliacionActual {
    NO_APLICA((short) 1),
    PENDIENTE_TESORERIA((short) 2),
    CONCILIADO_TESORERIA((short) 3),
    OBSERVADO_TESORERIA((short) 4);

    private final short codigo;
    EstadoConciliacionActual(short codigo) { this.codigo = codigo; }
    public short codigo() { return codigo; }
    public static EstadoConciliacionActual fromCodigo(short codigo) {
        for (EstadoConciliacionActual v : values()) if (v.codigo == codigo) return v;
        throw new IllegalArgumentException("EstadoConciliacionActual desconocido: " + codigo);
    }
}