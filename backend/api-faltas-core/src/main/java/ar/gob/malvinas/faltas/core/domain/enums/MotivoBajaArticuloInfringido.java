package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Motivo de baja lógica de un artículo imputado al acta.
 * SMALLINT en fal_acta_articulo_infringido.motivo_baja.
 */
public enum MotivoBajaArticuloInfringido {
    CORRECCION_IMPUTACION((short) 1),
    ANULACION_IMPUTACION((short) 2),
    REEMPLAZO_ARTICULO((short) 3),
    ERROR_CARGA((short) 4);

    private final short codigo;

    MotivoBajaArticuloInfringido(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static MotivoBajaArticuloInfringido fromCodigo(short codigo) {
        for (MotivoBajaArticuloInfringido v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("MotivoBajaArticuloInfringido desconocido: " + codigo);
    }
}
