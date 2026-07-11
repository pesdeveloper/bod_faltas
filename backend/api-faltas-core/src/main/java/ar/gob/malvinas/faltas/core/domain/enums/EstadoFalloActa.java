package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado canonico del fallo en el circuito de firma y notificacion.
 *
 * PENDIENTE_FIRMA        -> fallo dictado; documento generado; pendiente de firma obligatoria.
 * PENDIENTE_NOTIFICACION -> ultima firma completada; fhFirma registrado; cola notificatoria preparada.
 * NOTIFICADO             -> resultado notificatorio positivo registrado; fhNotificacion registrado.
 * FIRME                  -> firmeza de condena declarada; fhFirmeza registrado.
 * REEMPLAZADO            -> fallo reemplazado por uno nuevo (estado lateral).
 * SIN_EFECTO             -> fallo anulado sin efecto (estado lateral).
 *
 * DICTADO y FIRMADO no son estados: son hechos persistidos en fhDictado y fhFirma.
 */
public enum EstadoFalloActa {
    PENDIENTE_FIRMA,
    PENDIENTE_NOTIFICACION,
    NOTIFICADO,
    FIRME,
    REEMPLAZADO,
    SIN_EFECTO
}
