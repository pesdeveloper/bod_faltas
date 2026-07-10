package ar.gob.malvinas.faltas.core.domain.enums;

public enum OrigenMovimiento {
    INGRESOS((short) 1),
    TESORERIA((short) 2),
    CAJA((short) 3),
    ENTIDAD_RECAUDADORA((short) 4),
    APREMIO((short) 5),
    USUARIO_FALTAS((short) 6),
    INTEGRACION_EXTERNA((short) 7),
    PROCESO_PROGRAMADO((short) 8);

    private final short codigo;
    OrigenMovimiento(short codigo) { this.codigo = codigo; }
    public short codigo() { return codigo; }
    public static OrigenMovimiento fromCodigo(short codigo) {
        for (OrigenMovimiento v : values()) if (v.codigo == codigo) return v;
        throw new IllegalArgumentException("OrigenMovimiento desconocido: " + codigo);
    }
}