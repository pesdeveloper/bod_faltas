package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo tipo_talonario.
 *
 * Refleja el catalogo de num_talonario.tipo_talonario en MariaDB.
 *
 * Slice 8B-2: implementacion in-memory. Slice 9: columna SMALLINT en MariaDB.
 */
public enum TipoTalonario {

    ELECTRONICO((short) 1),
    MANUAL_FISICO((short) 2);

    private final short codigo;

    TipoTalonario(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static TipoTalonario desdeCodigo(short codigo) {
        for (TipoTalonario t : values()) {
            if (t.codigo == codigo) return t;
        }
        throw new IllegalArgumentException("TipoTalonario desconocido: " + codigo);
    }
}
