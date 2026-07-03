package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo alcance de ambito de talonario.
 *
 * Refleja el catalogo de num_talonario_ambito.alcance en MariaDB.
 *
 * Slice 8B-2: implementacion in-memory. Slice 9: columna SMALLINT en MariaDB.
 */
public enum AlcanceTalonario {

    GLOBAL((short) 1),
    DEPENDENCIA((short) 2),
    TRANSVERSAL_DOCUMENTO((short) 3);

    private final short codigo;

    AlcanceTalonario(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static AlcanceTalonario desdeCodigo(short codigo) {
        for (AlcanceTalonario a : values()) {
            if (a.codigo == codigo) return a;
        }
        throw new IllegalArgumentException("AlcanceTalonario desconocido: " + codigo);
    }
}
