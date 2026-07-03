package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Bandejas operativas del sistema de faltas.
 * Las bandejas son vistas derivadas del snapshot, no fuente de verdad.
 */
public enum CodigoBandeja {

    ACTAS_EN_ENRIQUECIMIENTO("Actas en captura/enriquecimiento"),
    PENDIENTE_PREPARACION_DOCUMENTAL("Pendiente de preparacion documental"),
    PENDIENTE_FIRMA("Pendiente de firma"),
    PENDIENTE_NOTIFICACION("Pendiente de notificacion"),
    EN_NOTIFICACION("En notificacion - esperando resultado"),
    PENDIENTE_ANALISIS("Pendiente de analisis juridico"),
    PENDIENTES_RESOLUCION_REDACCION("Pendiente de redaccion resolutoria"),
    PENDIENTES_FALLO("Pendiente de dictado de fallo"),
    CON_APELACION("Con apelacion presentada"),
    GESTION_EXTERNA("En gestion externa"),
    PARALIZADAS("Paralizadas"),
    ARCHIVO("Archivo administrativo"),
    PENDIENTE_CONFIRMACION_PAGO("Pendiente de confirmacion de pago voluntario"),
    PENDIENTE_PAGO_CONDENA("Pendiente de pago de condena"),
    PENDIENTE_CONFIRMACION_PAGO_CONDENA("Pendiente de confirmacion de pago de condena"),
    CERRADAS("Cerradas definitivamente");

    private final String label;

    CodigoBandeja(String label) {
        this.label = label;
    }

    public String label() { return label; }
}
