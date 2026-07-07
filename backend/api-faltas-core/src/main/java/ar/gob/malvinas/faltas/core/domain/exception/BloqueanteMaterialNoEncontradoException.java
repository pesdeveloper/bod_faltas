package ar.gob.malvinas.faltas.core.domain.exception;

public class BloqueanteMaterialNoEncontradoException extends RuntimeException {
    public BloqueanteMaterialNoEncontradoException(String id) {
        super("Bloqueante material no encontrado: " + id);
    }
    public BloqueanteMaterialNoEncontradoException(Long id) {
        super("Bloqueante material no encontrado: " + id);
    }
}