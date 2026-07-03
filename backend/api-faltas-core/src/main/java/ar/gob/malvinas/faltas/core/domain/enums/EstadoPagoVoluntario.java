package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado del flujo de pago voluntario de un acta.
 *
 * Pago voluntario es un flujo con estados, no una accion unica.
 * No existe un estado PAGO_INFORMADO ni un evento PAGVOL.
 *
 * Transiciones:
 *   SIN_PAGO -> SOLICITADO  (via PAGVSO)
 *   SOLICITADO -> MONTO_FIJADO  (via PAGVMF)
 *   SOLICITADO/MONTO_FIJADO -> PENDIENTE_CONFIRMACION  (via PAGINF)
 *   PENDIENTE_CONFIRMACION -> CONFIRMADO  (via PAGCNF) -> cierre
 *   PENDIENTE_CONFIRMACION -> OBSERVADO  (via PAGOBS)
 *   OBSERVADO -> PENDIENTE_CONFIRMACION  (nuevo intento del infractor via PAGINF)
 *   SOLICITADO/MONTO_FIJADO/OBSERVADO -> VENCIDO  (via PAGVVN) -> habilita analisis/fallo
 */
public enum EstadoPagoVoluntario {

    SIN_PAGO,

    SOLICITADO,

    MONTO_FIJADO,

    PENDIENTE_CONFIRMACION,

    CONFIRMADO,

    OBSERVADO,

    VENCIDO
}