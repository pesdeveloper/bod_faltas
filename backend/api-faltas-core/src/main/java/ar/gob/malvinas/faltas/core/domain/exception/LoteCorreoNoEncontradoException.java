package ar.gob.malvinas.faltas.core.domain.exception;

public class LoteCorreoNoEncontradoException extends RuntimeException {
    public LoteCorreoNoEncontradoException(Long id) {
        super("LoteCorreo no encontrado: id=" + id);
    }
    public LoteCorreoNoEncontradoException(String loteCodigo) {
        super("LoteCorreo no encontrado: codigo=" + loteCodigo);
    }
}
