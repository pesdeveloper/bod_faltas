package ar.gob.malvinas.faltas.core.domain.enums;

public enum EstadoAcuse {
    PENDIENTE((short) 1),
    RECIBIDO((short) 2),
    VALIDADO((short) 3),
    OBSERVADO((short) 4),
    ANULADO((short) 5);

    private final short codigo;
    EstadoAcuse(short codigo) { this.codigo = codigo; }
    public short codigo() { return codigo; }
    public static EstadoAcuse fromCodigo(short codigo) {
        for (EstadoAcuse e : values()) { if (e.codigo == codigo) return e; }
        throw new IllegalArgumentException("EstadoAcuse no reconocido: " + codigo);
    }
    public boolean producesEfecto() { return this == VALIDADO; }
    public boolean estaActivo() { return this != ANULADO; }
}
