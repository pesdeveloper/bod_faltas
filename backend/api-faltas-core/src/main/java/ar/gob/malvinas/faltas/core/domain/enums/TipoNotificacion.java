package ar.gob.malvinas.faltas.core.domain.enums;

public enum TipoNotificacion {
    ACTA_INFRACCION((short) 1),
    FALLO_ABSOLUTORIO((short) 2),
    FALLO_CONDENATORIO((short) 3);

    private final short codigo;
    TipoNotificacion(short codigo) { this.codigo = codigo; }
    public short codigo() { return codigo; }
    public static TipoNotificacion fromCodigo(short codigo) {
        for (TipoNotificacion t : values()) { if (t.codigo == codigo) return t; }
        throw new IllegalArgumentException("TipoNotificacion no reconocido: " + codigo);
    }
}
