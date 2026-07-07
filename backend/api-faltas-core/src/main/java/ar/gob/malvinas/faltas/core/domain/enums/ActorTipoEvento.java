package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipo de actor que origina un evento del acta.
 * SMALLINT en MariaDB (actor_tipo en fal_acta_evento).
 *
 * Clasifica la naturaleza funcional del actor para auditoria y trazabilidad.
 * actor_id guarda la identidad tecnica cuando existe subject IDP o equivalente.
 * actor_ref permite trazar actores externos sin forzar FK artificial.
 */
public enum ActorTipoEvento {

    USUARIO_INTERNO((short) 1, "Usuario municipal autenticado"),
    INSPECTOR((short) 2, "Inspector del organismo"),
    INFRACTOR((short) 3, "Persona infractora (portal/presencial)"),
    SISTEMA((short) 4, "Proceso automatico del sistema"),
    INTEGRACION((short) 5, "Integracion con sistema externo"),
    NOTIFICADOR((short) 6, "Servicio de notificacion");

    private final short codigo;
    private final String descripcion;

    ActorTipoEvento(short codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public short codigo() { return codigo; }
    public String descripcion() { return descripcion; }

    public static ActorTipoEvento fromCodigo(short codigo) {
        for (ActorTipoEvento v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("ActorTipoEvento desconocido: " + codigo);
    }
}