package ar.gob.malvinas.faltas.core.domain.enums;

public enum CanalNotificacion {
    CORREO_POSTAL((short) 1),
    NOTIFICADOR_MUNICIPAL((short) 2),
    PRESENCIAL((short) 3),
    PORTAL_INFRACTOR((short) 4),
    EMAIL((short) 5);

    private final short codigo;
    CanalNotificacion(short codigo) { this.codigo = codigo; }
    public short codigo() { return codigo; }
    public static CanalNotificacion fromCodigo(short codigo) {
        for (CanalNotificacion c : values()) { if (c.codigo == codigo) return c; }
        throw new IllegalArgumentException("CanalNotificacion no reconocido: " + codigo);
    }
    public boolean requiereDomicilioFisico() { return this == CORREO_POSTAL || this == NOTIFICADOR_MUNICIPAL; }
    public boolean esDigital() { return this == EMAIL; }
    public boolean esPortal() { return this == PORTAL_INFRACTOR; }
}