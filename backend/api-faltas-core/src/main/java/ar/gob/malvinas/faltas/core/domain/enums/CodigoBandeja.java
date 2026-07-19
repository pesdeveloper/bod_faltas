package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Bandejas operativas del sistema de faltas.
 * Las bandejas son vistas derivadas del snapshot, no fuente de verdad.
 *
 * Codigo String estable para persistencia (DECISION_DDL-SNAP-01): VARCHAR(50), prohibido name().
 */
public enum CodigoBandeja {

    ACTAS_EN_ENRIQUECIMIENTO("ACTAS_EN_ENRIQUECIMIENTO", "Actas en captura/enriquecimiento"),
    PENDIENTE_PREPARACION_DOCUMENTAL("PENDIENTE_PREPARACION_DOCUMENTAL", "Pendiente de preparacion documental"),
    PENDIENTE_FIRMA("PENDIENTE_FIRMA", "Pendiente de firma"),
    PENDIENTE_NOTIFICACION("PENDIENTE_NOTIFICACION", "Pendiente de notificacion"),
    EN_NOTIFICACION("EN_NOTIFICACION", "En notificacion - esperando resultado"),
    PENDIENTE_ANALISIS("PENDIENTE_ANALISIS", "Pendiente de analisis juridico"),
    PENDIENTES_RESOLUCION_REDACCION("PENDIENTES_RESOLUCION_REDACCION", "Pendiente de redaccion resolutoria"),
    PENDIENTES_FALLO("PENDIENTES_FALLO", "Pendiente de dictado de fallo"),
    CON_APELACION("CON_APELACION", "Con apelacion presentada"),
    GESTION_EXTERNA("GESTION_EXTERNA", "En gestion externa"),
    PARALIZADAS("PARALIZADAS", "Paralizadas"),
    ARCHIVO("ARCHIVO", "Archivo administrativo"),
    PENDIENTE_CONFIRMACION_PAGO("PENDIENTE_CONFIRMACION_PAGO", "Pendiente de confirmacion de pago voluntario"),
    PENDIENTE_PAGO_CONDENA("PENDIENTE_PAGO_CONDENA", "Pendiente de pago de condena"),
    PENDIENTE_CONFIRMACION_PAGO_CONDENA("PENDIENTE_CONFIRMACION_PAGO_CONDENA", "Pendiente de confirmacion de pago de condena"),
    CERRADAS("CERRADAS", "Cerradas definitivamente");

    private final String codigo;
    private final String label;

    CodigoBandeja(String codigo, String label) {
        this.codigo = codigo;
        this.label = label;
    }

    public String codigo() { return codigo; }

    public String label() { return label; }

    public static CodigoBandeja desdeCodigo(String codigo) {
        for (CodigoBandeja v : values()) {
            if (v.codigo.equals(codigo)) return v;
        }
        throw new IllegalArgumentException("CodigoBandeja sin codigo: " + codigo);
    }
}
