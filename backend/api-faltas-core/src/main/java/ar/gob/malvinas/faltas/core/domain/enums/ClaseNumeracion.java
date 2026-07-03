package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo clase_numeracion / clase_talonario.
 *
 * Refleja el catalogo de num_politica.clase_numeracion y num_talonario.clase_talonario
 * y num_talonario_ambito.clase_talonario en MariaDB.
 *
 * Slice 8B-2: implementacion in-memory. Slice 9: columna SMALLINT en MariaDB.
 */
public enum ClaseNumeracion {

    ACTA((short) 1),
    DOCUMENTO((short) 2);

    private final short codigo;

    ClaseNumeracion(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static ClaseNumeracion desdeCodigo(short codigo) {
        for (ClaseNumeracion c : values()) {
            if (c.codigo == codigo) return c;
        }
        throw new IllegalArgumentException("ClaseNumeracion desconocida: " + codigo);
    }
}
