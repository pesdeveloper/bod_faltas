package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Motivo por el que una obligacion o plan queda apto para intimacion.
 * SMALLINT en fal_acta_obligacion_pago.motivo_apta_intimacion y
 * fal_acta_plan_pago_ref.motivo_apta_intimacion.
 */
public enum MotivoAptitudIntimacion {
    MORA_DIAS((short) 1),
    MORA_CUOTAS((short) 2),
    MORA_CUOTAS_CONSECUTIVAS((short) 3),
    PLAN_CAIDO((short) 4),
    OTRO((short) 5);

    private final short codigo;

    MotivoAptitudIntimacion(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static MotivoAptitudIntimacion fromCodigo(short codigo) {
        for (MotivoAptitudIntimacion v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("MotivoAptitudIntimacion desconocido: " + codigo);
    }
}
