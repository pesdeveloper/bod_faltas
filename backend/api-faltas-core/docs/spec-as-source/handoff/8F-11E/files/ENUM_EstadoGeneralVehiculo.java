package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado general visible del vehiculo al momento del acta.
 * SMALLINT en fal_acta_vehiculo.estado_general_vehiculo.
 */
public enum EstadoGeneralVehiculo {
    BUENO((short) 1),
    REGULAR((short) 2),
    MALO((short) 3),
    SIN_VERIFICAR((short) 4),
    NO_APLICA((short) 5);

    private final short codigo;

    EstadoGeneralVehiculo(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static EstadoGeneralVehiculo fromCodigo(short codigo) {
        for (EstadoGeneralVehiculo v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("EstadoGeneralVehiculo desconocido: " + codigo);
    }
}
