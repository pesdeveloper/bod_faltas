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
 *
 * Codigo numerico persistible (DECISION_DDL-ENUM-01): SMALLINT, prohibido ordinal().
 */
public enum EstadoPagoCondena {

    NO_APLICA((short) 1),

    PENDIENTE((short) 2),

    INFORMADO((short) 3),

    CONFIRMADO((short) 4),

    OBSERVADO((short) 5);

    private final short codigo;

    EstadoPagoCondena(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static EstadoPagoCondena desdeCodigo(short codigo) {
        for (EstadoPagoCondena v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("EstadoPagoCondena sin codigo: " + codigo);
    }
}
