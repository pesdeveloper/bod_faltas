package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Motivo de la carga manual o excepcional de nomenclatura en el acta de contravencion.
 * SMALLINT en fal_acta_contravencion.motivo_nomenclatura_manual.
 */
public enum MotivoNomenclaturaManual {
    SIN_DATOS_CATASTRO((short) 1),
    NO_RESUELVE_MAPA((short) 2),
    NO_RESUELVE_CUENTA((short) 3),
    INTEGRACION_NO_DISPONIBLE((short) 4),
    CONTINGENCIA_OPERATIVA((short) 5),
    OTRO((short) 6);

    private final short codigo;

    MotivoNomenclaturaManual(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static MotivoNomenclaturaManual fromCodigo(short codigo) {
        for (MotivoNomenclaturaManual v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("MotivoNomenclaturaManual desconocido: " + codigo);
    }
}
