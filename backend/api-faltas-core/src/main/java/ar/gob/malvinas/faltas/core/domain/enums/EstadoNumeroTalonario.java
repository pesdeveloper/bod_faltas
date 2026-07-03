package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo estado_numero_talonario.
 *
 * Refleja el catalogo de num_talonario_movimiento.estado_numero en MariaDB.
 * El campo productivo es SMALLINT NOT NULL; se mapea via codigo().
 *
 * Slice 8B-4: implementacion in-memory.
 * Slice 8B-6: flujos completos de ANULADO, DEVUELTO_SIN_USAR.
 * Slice 9: columna SMALLINT en MariaDB.
 */
public enum EstadoNumeroTalonario {

    USADO((short) 1),
    ANULADO((short) 2),
    DEVUELTO_SIN_USAR((short) 3),
    RENDIDO((short) 4),
    JUSTIFICADO((short) 5);

    private final short codigo;

    EstadoNumeroTalonario(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static EstadoNumeroTalonario desdeCodigo(short codigo) {
        for (EstadoNumeroTalonario e : values()) {
            if (e.codigo == codigo) return e;
        }
        throw new IllegalArgumentException("EstadoNumeroTalonario desconocido: " + codigo);
    }
}
