package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Sub-clasificacion opcional de gravedad de un articulo normativo.
 * SMALLINT nullable en {@code fal_articulo_normativa_faltas.categoria_infraccion}.
 *
 * Agregado en Wave 4 PARTE-2 al implementar el primer JDBC de este catalogo
 * (columna preexistente en el DDL sin campo Java correspondiente hasta ahora).
 */
public enum CategoriaInfraccion {
    LEVE((short) 1),
    GRAVE((short) 2),
    GRAVISIMA((short) 3);

    private final short codigo;

    CategoriaInfraccion(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static CategoriaInfraccion fromCodigo(short codigo) {
        for (CategoriaInfraccion v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("CategoriaInfraccion desconocida: " + codigo);
    }
}
