package ar.gob.malvinas.faltas.core.domain.exception;

public class DocumentoNoEncontradoException extends RuntimeException {
    public DocumentoNoEncontradoException(String idDocumento) {
        super("Documento no encontrado: " + idDocumento);
    }
    public DocumentoNoEncontradoException(Long idDocumento) {
        super("Documento no encontrado: " + idDocumento);
    }
}
