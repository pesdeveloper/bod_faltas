package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado del proceso notificatorio de un documento del expediente.
 */
public enum EstadoNotificacion {
    PENDIENTE_ENVIO,
    EN_PROCESO,
    CON_ACUSE_POSITIVO,
    CON_ACUSE_NEGATIVO,
    VENCIDA,
    REQUIERE_DECISION
}
