package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado de un plan de pago de acta de faltas.
 * SMALLINT en fal_acta_plan_pago_ref.estado_plan.
 */
public enum EstadoPlanPago {
    ACTIVO((short) 1),
    EN_MORA((short) 2),
    VENCIDO((short) 3),
    CAIDO((short) 4),
    CANCELADO((short) 5),
    REFINANCIADO((short) 6);

    private final short codigo;

    EstadoPlanPago(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static EstadoPlanPago fromCodigo(short codigo) {
        for (EstadoPlanPago v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("EstadoPlanPago desconocido: " + codigo);
    }
}
