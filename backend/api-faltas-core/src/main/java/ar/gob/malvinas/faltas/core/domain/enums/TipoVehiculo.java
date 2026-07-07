package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Tipo de vehiculo registrado en el acta.
 * SMALLINT en fal_acta_vehiculo.tipo_vehiculo.
 */
public enum TipoVehiculo {
    AUTO((short) 1),
    MOTO((short) 2),
    CAMIONETA((short) 3),
    CAMION((short) 4),
    COLECTIVO((short) 5),
    UTILITARIO((short) 6),
    ACOPLADO((short) 7),
    BICICLETA((short) 8),
    MAQUINARIA((short) 9),
    OTRO((short) 10);

    private final short codigo;

    TipoVehiculo(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static TipoVehiculo fromCodigo(short codigo) {
        for (TipoVehiculo v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("TipoVehiculo desconocido: " + codigo);
    }
}
