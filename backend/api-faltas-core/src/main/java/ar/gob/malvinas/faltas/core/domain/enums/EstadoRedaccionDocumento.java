package ar.gob.malvinas.faltas.core.domain.enums;

public enum EstadoRedaccionDocumento {
    BORRADOR((short) 1),
    CONFIRMADA((short) 2),
    REABIERTA((short) 3),
    ANULADA((short) 4);

    private final short codigo;

    EstadoRedaccionDocumento(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() { return codigo; }

    public static EstadoRedaccionDocumento desdeCodigo(short codigo) {
        for (EstadoRedaccionDocumento e : values()) {
            if (e.codigo == codigo) return e;
        }
        throw new IllegalArgumentException("EstadoRedaccionDocumento desconocido: " + codigo);
    }
}
