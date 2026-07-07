package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipo de unidad territorial administrativa.
 * SMALLINT en fal_persona_domicilio.unidad_territorial_tipo.
 */
public enum UnidadTerritorialTipo {
    MUNICIPIO((short) 1),
    DEPARTAMENTO((short) 2),
    CIUDAD_AUTONOMA((short) 3);

    private final short codigo;

    UnidadTerritorialTipo(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static UnidadTerritorialTipo fromCodigo(short codigo) {
        for (UnidadTerritorialTipo v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("UnidadTerritorialTipo desconocido: " + codigo);
    }
}
