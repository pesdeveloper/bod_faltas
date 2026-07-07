package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Origen de las coordenadas de ubicacion del domicilio.
 * SMALLINT en fal_persona_domicilio.origen_ubicacion.
 *
 * SIN_UBICACION(0) permite que origen sea null o este presente indicando ausencia.
 */
public enum OrigenUbicacion {
    SIN_UBICACION((short) 0),
    CALLE_ALTURA_GEOCODIFICADA((short) 1),
    AJUSTE_MANUAL_SOBRE_GEOCODIFICACION((short) 2),
    PUNTO_MANUAL((short) 3),
    PUNTO_MANUAL_REVERSE((short) 4),
    PARCELA_SELECCIONADA((short) 5),
    GPS_DISPOSITIVO((short) 6),
    MANUAL_SIN_MAPA((short) 7);

    private final short codigo;

    OrigenUbicacion(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    public static OrigenUbicacion fromCodigo(short codigo) {
        for (OrigenUbicacion v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("OrigenUbicacion desconocido: " + codigo);
    }
}
