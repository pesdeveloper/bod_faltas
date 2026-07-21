package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Accion pendiente sobre el expediente, derivada en el snapshot.
 * Indica que debe hacer el operador en el siguiente paso.
 *
 * Codigo String estable para persistencia (DECISION_DDL-SNAP-01): VARCHAR(50), prohibido name().
 */
public enum AccionPendiente {
    COMPLETAR_CAPTURA("COMPLETAR_CAPTURA"),
    ENRIQUECER("ENRIQUECER"),
    GENERAR_DOCUMENTO("GENERAR_DOCUMENTO"),
    FIRMAR_DOCUMENTO("FIRMAR_DOCUMENTO"),
    ENVIAR_NOTIFICACION("ENVIAR_NOTIFICACION"),
    EVALUAR_NOTIFICACION("EVALUAR_NOTIFICACION"),
    DECIDIR_REINTENTO_O_GESTION("DECIDIR_REINTENTO_O_GESTION"),
    DICTAR_FALLO("DICTAR_FALLO"),
    RESOLVER_APELACION("RESOLVER_APELACION"),
    REGISTRAR_PAGO("REGISTRAR_PAGO"),
    DERIVAR_GESTION_EXTERNA("DERIVAR_GESTION_EXTERNA"),
    CONFIRMAR_PAGO("CONFIRMAR_PAGO"),
    CORREGIR_PAGO("CORREGIR_PAGO"),
    DECLARAR_CONDENA_FIRME("DECLARAR_CONDENA_FIRME"),
    GESTIONAR_PAGO_CONDENA("GESTIONAR_PAGO_CONDENA"),
    CONFIRMAR_PAGO_CONDENA("CONFIRMAR_PAGO_CONDENA"),
    CORREGIR_PAGO_CONDENA("CORREGIR_PAGO_CONDENA"),
    NINGUNA("NINGUNA");

    private final String codigo;

    AccionPendiente(String codigo) { this.codigo = codigo; }

    public String codigo() { return codigo; }

    public static AccionPendiente desdeCodigo(String codigo) {
        for (AccionPendiente v : values()) {
            if (v.codigo.equals(codigo)) return v;
        }
        throw new IllegalArgumentException("AccionPendiente sin codigo: " + codigo);
    }
}
