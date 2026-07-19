package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Origen del documento presentado dentro de una apelacion (origen_presentacion en fal_acta_apelacion_documento).
 */
public enum OrigenPresentacion {

    INFRACTOR((short) 1),
    ABOGADO((short) 2),
    OPERADOR_INTERNO((short) 3),
    PORTAL_INFRACTOR((short) 4),
    MESA_ENTRADA((short) 5),
    INTEGRACION_EXTERNA((short) 6);

    private final short codigo;

    OrigenPresentacion(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static OrigenPresentacion fromCodigo(short cod) {
        for (OrigenPresentacion v : values()) {
            if (v.codigo == cod) return v;
        }
        throw new IllegalArgumentException("OrigenPresentacion desconocido: " + cod);
    }
}
