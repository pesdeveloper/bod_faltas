package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Origen de una observacion en fal_observacion.
 * Codigos enteros unicos. Sin ordinal.
 */
public enum OrigenObservacion {

    USUARIO(1),
    SISTEMA(2),
    INTEGRACION(3);

    private final int codigo;

    OrigenObservacion(int codigo) {
        this.codigo = codigo;
    }

    public int codigo() {
        return codigo;
    }

    public static OrigenObservacion fromCodigo(int codigo) {
        for (OrigenObservacion o : values()) {
            if (o.codigo == codigo) return o;
        }
        throw new IllegalArgumentException("OrigenObservacion no reconocido para codigo: " + codigo);
    }
}
