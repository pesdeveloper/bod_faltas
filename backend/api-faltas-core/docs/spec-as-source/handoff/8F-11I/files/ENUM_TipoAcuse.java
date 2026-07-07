package ar.gob.malvinas.faltas.core.domain.enums;

public enum TipoAcuse {
    ACUSE_RECEPCION((short) 1),
    ACUSE_RECHAZO((short) 2),
    ACUSE_DOMICILIO_INEXISTENTE((short) 3),
    ACUSE_PERSONA_DESCONOCIDA((short) 4),
    ACUSE_AUSENTE((short) 5),
    ACUSE_OTRO((short) 6);

    private final short codigo;
    TipoAcuse(short codigo) { this.codigo = codigo; }
    public short codigo() { return codigo; }
    public static TipoAcuse fromCodigo(short codigo) {
        for (TipoAcuse t : values()) { if (t.codigo == codigo) return t; }
        throw new IllegalArgumentException("TipoAcuse no reconocido: " + codigo);
    }
    public boolean implicaResultadoNegativo() {
        return this == ACUSE_RECHAZO || this == ACUSE_DOMICILIO_INEXISTENTE || this == ACUSE_PERSONA_DESCONOCIDA;
    }
    public boolean implicaResultadoPositivo() { return this == ACUSE_RECEPCION; }
}