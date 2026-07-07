package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Motivo de manualización de una valorización o ítem.
 * SMALLINT en fal_acta_valorizacion_item.motivo_manual.
 * OTRO_FUNDADO exige documentoId o, en 8F-11G, observación formal.
 */
public enum MotivoManualizacionValorizacion {
    CRITERIO_AUTORIDAD((short) 1),
    MANTIENE_VALOR_ANTERIOR((short) 2),
    CORRECCION_CALCULO((short) 3),
    AJUSTE_EXCEPCIONAL((short) 4),
    OTRO_FUNDADO((short) 5);

    private final short codigo;

    MotivoManualizacionValorizacion(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static MotivoManualizacionValorizacion fromCodigo(short codigo) {
        for (MotivoManualizacionValorizacion v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("MotivoManualizacionValorizacion desconocido: " + codigo);
    }
}
