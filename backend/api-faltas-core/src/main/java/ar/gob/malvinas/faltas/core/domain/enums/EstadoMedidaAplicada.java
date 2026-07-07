package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado de la medida preventiva aplicada al acta.
 * SMALLINT en fal_acta_medida_preventiva.estado_medida.
 */
public enum EstadoMedidaAplicada {
    APLICADA((short) 1),
    LEVANTADA((short) 2),
    ANULADA((short) 3),
    CUMPLIDA((short) 4);

    private final short codigo;

    EstadoMedidaAplicada(short codigo) { this.codigo = codigo; }

    public short codigo() { return codigo; }

    public static EstadoMedidaAplicada fromCodigo(short codigo) {
        for (EstadoMedidaAplicada v : values()) {
            if (v.codigo == codigo) return v;
        }
        throw new IllegalArgumentException("EstadoMedidaAplicada desconocido: " + codigo);
    }
}
