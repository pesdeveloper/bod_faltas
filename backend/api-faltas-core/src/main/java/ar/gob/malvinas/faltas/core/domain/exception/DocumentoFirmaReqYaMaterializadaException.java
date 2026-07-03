package ar.gob.malvinas.faltas.core.domain.exception;

/**
 * Se lanza cuando se intenta materializar requisitos de firma para un documento
 * que ya tiene requisitos materializados.
 */
public class DocumentoFirmaReqYaMaterializadaException extends RuntimeException {
    public DocumentoFirmaReqYaMaterializadaException(Long documentoId) {
        super("El documento ya tiene requisitos de firma materializados: " + documentoId);
    }
}
