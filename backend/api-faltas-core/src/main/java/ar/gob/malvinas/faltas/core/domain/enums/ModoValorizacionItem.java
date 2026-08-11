package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Modo de calculo de una dimension (ordinaria o pago voluntario) de un
 * {@code fal_acta_valorizacion_item}.
 *
 * SMALLINT en {@code fal_acta_valorizacion_item.modo_valorizacion_ordinaria} y
 * {@code modo_valorizacion_pago_voluntario}.
 *
 * PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7.5/S7.6.
 */
public enum ModoValorizacionItem {
    AUTOMATICA((short) 1),
    MANUAL_UNIDADES((short) 2),
    MANUAL_MONTO((short) 3);

    private final short codigo;

    ModoValorizacionItem(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public boolean esManual() {
        return this != AUTOMATICA;
    }

    public static ModoValorizacionItem fromCodigo(short codigo) {
        for (ModoValorizacionItem v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("ModoValorizacionItem desconocido: " + codigo);
    }
}
