package ar.gob.malvinas.faltas.core.domain.exception;

public class LoteCodigoDuplicadoException extends RuntimeException {
    public LoteCodigoDuplicadoException(String loteCodigo) {
        super("Codigo de lote ya existe: " + loteCodigo);
    }
}
