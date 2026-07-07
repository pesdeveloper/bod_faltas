package ar.gob.malvinas.faltas.core.domain.exception;

public class ActaDocumentoYaExisteException extends RuntimeException {
    public ActaDocumentoYaExisteException(Long actaId, Long documentoId) {
        super("El documento " + documentoId + " ya esta asociado al acta " + actaId);
    }
}