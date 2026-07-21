package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Origen/canal del evento del acta.
 * SMALLINT en MariaDB (origen_evt en fal_acta_evento).
 *
 * Distingue el canal por el que se origino el evento:
 * accion de usuario, proceso automatico, integracion externa, portal infractor, etc.
 * Complementa actor_tipo para trazabilidad completa.
 */
public enum OrigenEvento {

    USUARIO_WEB((short) 1, "Accion de usuario en interfaz web"),
    DISPOSITIVO_MOBILE((short) 2, "Accion desde dispositivo movil"),
    PROCESO_AUTOMATICO((short) 3, "Proceso automatico del sistema"),
    INTEGRACION((short) 4, "Evento originado por integracion externa"),
    PORTAL_INFRACTOR((short) 5, "Accion del infractor en portal"),
    SERVICIO_NOTIFICACION((short) 6, "Servicio de notificacion"),
    LOTE_CORREO((short) 7, "Procesamiento de lote de correo"),
    SISTEMA_QR((short) 8, "Sistema de acceso QR");

    private final short codigo;
    private final String descripcion;

    OrigenEvento(short codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public short codigo() { return codigo; }
    public String descripcion() { return descripcion; }

    public static OrigenEvento fromCodigo(short codigo) {
        for (OrigenEvento v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("OrigenEvento desconocido: " + codigo);
    }
}
