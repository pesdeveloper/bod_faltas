package ar.gob.malvinas.faltas.core.domain.enums;

public enum EstadoLote {
    GENERADO((short) 1),
    EMITIDO((short) 2),
    PROCESADO((short) 3),
    ANULADO((short) 4),
    CON_ERROR((short) 5);

    private final short codigo;
    EstadoLote(short codigo) { this.codigo = codigo; }
    public short codigo() { return codigo; }
    public static EstadoLote fromCodigo(short codigo) {
        for (EstadoLote e : values()) { if (e.codigo == codigo) return e; }
        throw new IllegalArgumentException("EstadoLote no reconocido: " + codigo);
    }
    public boolean esAnulable() { return this == GENERADO || this == EMITIDO; }
    public boolean esEmitible() { return this == GENERADO; }
    public boolean esProcesable() { return this == EMITIDO; }
    public boolean esFinal() { return this == PROCESADO || this == ANULADO; }
}