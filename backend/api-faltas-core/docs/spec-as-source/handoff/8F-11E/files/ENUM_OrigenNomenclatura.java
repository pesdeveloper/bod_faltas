package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Origen de la nomenclatura catastral instanciada en el acta de contravencion.
 * SMALLINT en fal_acta_contravencion.origen_nomencl.
 */
public enum OrigenNomenclatura {
    CATASTRO((short) 1),
    MAPA_INTERACTIVO((short) 2),
    CUENTA_INMUEBLE((short) 3),
    CUENTA_COMERCIO((short) 4),
    INTEGRACION_EXTERNA((short) 5),
    MANUAL_EXCEPCIONAL((short) 6);

    private final short codigo;

    OrigenNomenclatura(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static OrigenNomenclatura fromCodigo(short codigo) {
        for (OrigenNomenclatura v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("OrigenNomenclatura desconocido: " + codigo);
    }
}
