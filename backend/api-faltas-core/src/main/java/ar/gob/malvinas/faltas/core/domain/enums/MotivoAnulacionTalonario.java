package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Catalogo productivo motivo_anulacion_talonario.
 *
 * Refleja el catalogo de num_talonario_movimiento.motivo_anulacion en MariaDB.
 * El campo productivo es SMALLINT NULL; solo aplica cuando estadoNumero = ANULADO.
 *
 * Slice 8B-4: implementacion in-memory.
 * Slice 8B-6: flujo completo de anulacion administrativa.
 * Slice 9: columna SMALLINT en MariaDB.
 */
public enum MotivoAnulacionTalonario {

    ERROR_LABRADO((short) 1),
    ROTURA_FORMULARIO((short) 2),
    DUPLICADO((short) 3),
    EXTRAVIO((short) 4),
    OTRO((short) 5);

    private final short codigo;

    MotivoAnulacionTalonario(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static MotivoAnulacionTalonario desdeCodigo(short codigo) {
        for (MotivoAnulacionTalonario m : values()) {
            if (m.codigo == codigo) return m;
        }
        throw new IllegalArgumentException("MotivoAnulacionTalonario desconocido: " + codigo);
    }
}
