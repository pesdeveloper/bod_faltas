package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo estado_asignacion_talonario.
 *
 * Refleja el catalogo de num_talonario_inspector.estado_asignacion en MariaDB.
 * El campo productivo es SMALLINT NOT NULL; se mapea via codigo().
 *
 * Slice 8B-3: implementacion in-memory.
 * Slice 8B-6: CERRADO y OBSERVADO reciben flujos completos.
 * Slice 9: columna SMALLINT en MariaDB.
 */
public enum EstadoAsignacionTalonario {

    ENTREGADO((short) 1),
    DEVUELTO((short) 2),
    CERRADO((short) 3),
    OBSERVADO((short) 4);

    private final short codigo;

    EstadoAsignacionTalonario(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static EstadoAsignacionTalonario desdeCodigo(short codigo) {
        for (EstadoAsignacionTalonario e : values()) {
            if (e.codigo == codigo) return e;
        }
        throw new IllegalArgumentException("EstadoAsignacionTalonario desconocido: " + codigo);
    }
}
