package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado del flujo de pago de condena de un acta.
 *
 * Pago de condena es un flujo con estados, no una accion unica.
 * No existe un evento PAGCON ni un estado generico de pago condena.
 *
 * Transiciones:
 *   NO_APLICA  -> inicial antes de que haya condena firme
 *   PENDIENTE  -> condena firme declarada, pago aun no informado
 *   INFORMADO  -> infractor informo pago (via PCOINF)
 *   CONFIRMADO -> pago confirmado (via PCOCNF) -> cierre del acta
 *   OBSERVADO  -> pago informado fue observado/rechazado (via PCOOBS) -> corregir
 */
public enum EstadoPagoCondena {

    NO_APLICA,

    PENDIENTE,

    INFORMADO,

    CONFIRMADO,

    OBSERVADO
}
