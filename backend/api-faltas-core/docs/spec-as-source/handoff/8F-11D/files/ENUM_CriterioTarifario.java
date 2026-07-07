package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Criterio de seleccion tarifaria para una valorización.
 * SMALLINT en fal_acta_valorizacion.criterio_tarifario.
 */
public enum CriterioTarifario {
    ULTIMO_VIGENTE((short) 1),
    MANTIENE_ANTERIOR((short) 2),
    MANUAL((short) 3);

    private final short codigo;

    CriterioTarifario(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static CriterioTarifario fromCodigo(short codigo) {
        for (CriterioTarifario v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("CriterioTarifario desconocido: " + codigo);
    }
}
