package ar.gob.malvinas.faltas.core.domain.enums;

public enum OrigenUltimaActualizacion {
    TIEMPO_REAL((short) 1),
    SINCRONIZACION_NOCTURNA((short) 2),
    REBUILD((short) 3),
    CORRECCION_CONTROLADA((short) 4);

    private final short codigo;
    OrigenUltimaActualizacion(short codigo) { this.codigo = codigo; }
    public short codigo() { return codigo; }
    public static OrigenUltimaActualizacion fromCodigo(short codigo) {
        for (OrigenUltimaActualizacion v : values()) if (v.codigo == codigo) return v;
        throw new IllegalArgumentException("OrigenUltimaActualizacion desconocido: " + codigo);
    }
}