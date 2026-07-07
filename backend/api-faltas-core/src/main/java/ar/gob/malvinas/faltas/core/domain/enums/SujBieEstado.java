package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado del vinculo de la persona con el sistema de Ingresos (SujBie).
 * SMALLINT en fal_persona.SujBieEstado.
 */
public enum SujBieEstado {
    SIN_CUENTA((short) 1),
    PENDIENTE_CREACION((short) 2),
    ACTIVA((short) 3),
    ERROR_CREACION((short) 4),
    INACTIVA((short) 5);

    private final short codigo;

    SujBieEstado(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static SujBieEstado fromCodigo(short codigo) {
        for (SujBieEstado v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("SujBieEstado desconocido: " + codigo);
    }
}
