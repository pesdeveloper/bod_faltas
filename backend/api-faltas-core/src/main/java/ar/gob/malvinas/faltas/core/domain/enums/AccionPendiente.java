package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Accion pendiente sobre el expediente, derivada en el snapshot.
 * Indica que debe hacer el operador en el siguiente paso.
 */
public enum AccionPendiente {
    COMPLETAR_CAPTURA,
    ENRIQUECER,
    GENERAR_DOCUMENTO,
    FIRMAR_DOCUMENTO,
    ENVIAR_NOTIFICACION,
    EVALUAR_NOTIFICACION,
    DECIDIR_REINTENTO_O_GESTION,
    DICTAR_FALLO,
    RESOLVER_APELACION,
    REGISTRAR_PAGO,
    DERIVAR_GESTION_EXTERNA,
    CONFIRMAR_PAGO,
    CORREGIR_PAGO,
    DECLARAR_CONDENA_FIRME,
    GESTIONAR_PAGO_CONDENA,
    CONFIRMAR_PAGO_CONDENA,
    CORREGIR_PAGO_CONDENA,
    NINGUNA
}