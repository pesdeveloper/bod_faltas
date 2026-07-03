package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado del fallo en el circuito de firma y notificacion.
 *
 * DICTADO             -> fallo dictado, documento generado, pendiente de firma.
 * PENDIENTE_FIRMA     -> sinonimo operativo de DICTADO (doc PENDIENTE_FIRMA).
 * FIRMADO             -> documento de fallo firmado, pendiente de notificacion.
 * PENDIENTE_NOTIFICACION -> equivale a FIRMADO en transicion a notificacion.
 * NOTIFICADO          -> notificacion positiva registrada.
 * SIN_EFECTO          -> fallo anulado o sin efecto (reservado para slices futuros).
 */
public enum EstadoFalloActa {
    DICTADO,
    PENDIENTE_FIRMA,
    FIRMADO,
    PENDIENTE_NOTIFICACION,
    NOTIFICADO,
    SIN_EFECTO
}
