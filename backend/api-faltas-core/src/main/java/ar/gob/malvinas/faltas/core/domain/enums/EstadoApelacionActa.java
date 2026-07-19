package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado del ciclo de vida de la apelacion del acta.
 *
 * PRESENTADA: apelacion presentada por el infractor (Slice 3B).
 * RECHAZADA: apelacion rechazada - condena queda firme (Slice futuro).
 * ACEPTADA_ABSUELVE: apelacion aceptada - absolucion en segunda instancia (Slice futuro).
 * SIN_EFECTO: apelacion anulada o sin efecto (Slice futuro).
 *
 * Codigo numerico persistible (DECISION_DDL-ENUM-01): SMALLINT, prohibido ordinal().
 */
public enum EstadoApelacionActa {
    PRESENTADA((short) 1),
    EN_ANALISIS((short) 2),
    RECHAZADA((short) 3),
    ACEPTADA_ABSUELVE((short) 4),
    RESUELTA((short) 5),
    SIN_EFECTO((short) 6);

    private final short codigo;

    EstadoApelacionActa(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static EstadoApelacionActa desdeCodigo(short codigo) {
        for (EstadoApelacionActa v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("EstadoApelacionActa sin codigo: " + codigo);
    }
}
