package ar.gob.malvinas.faltas.core.domain.exception;

public class DocumentoRedaccionNoEncontradaException extends RuntimeException {
    public DocumentoRedaccionNoEncontradaException(Long id) {
        super("Redaccion documental no encontrada: id=" + id);
    }
}