package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Modo del domicilio: origen territorial local o externo.
 * SMALLINT en fal_persona_domicilio.modo_domicilio.
 *
 * MALVINAS_LOCAL: domicilio dentro del municipio de Malvinas Argentinas.
 *   Requiere idProvincia=6, unidadTerritorialTipo=MUNICIPIO, idUnidadTerritorial=60515.
 * EXTERNO: domicilio fuera del municipio (otra ciudad, otra provincia).
 */
public enum ModoDomicilio {
    MALVINAS_LOCAL((short) 1),
    EXTERNO((short) 2);

    private final short codigo;

    ModoDomicilio(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static ModoDomicilio fromCodigo(short codigo) {
        for (ModoDomicilio v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("ModoDomicilio desconocido: " + codigo);
    }
}
