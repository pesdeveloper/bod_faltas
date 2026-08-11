package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipo de infraccion que clasifica la conducta de un articulo normativo.
 * SMALLINT en {@code fal_articulo_normativa_faltas.tipo_infraccion}.
 *
 * Espeja el dominio de {@link TipoActa}: cada articulo normativo tarda su
 * conducta a un ambito equivalente al tipo de acta que puede imputarlo.
 * Agregado en Wave 4 PARTE-2 al implementar el primer JDBC de este catalogo
 * (columna preexistente en el DDL sin campo Java correspondiente hasta ahora).
 */
public enum TipoInfraccion {
    TRANSITO((short) 1),
    CONTRAVENCION((short) 2),
    SUSTANCIAS_ALIMENTICIAS((short) 3),
    COMERCIO((short) 4);

    private final short codigo;

    TipoInfraccion(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static TipoInfraccion fromCodigo(short codigo) {
        for (TipoInfraccion v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("TipoInfraccion desconocido: " + codigo);
    }
}
