package ar.gob.malvinas.faltas.core.domain.exception;

public class ActaDocumentoNoEncontradaException extends RuntimeException {
    public ActaDocumentoNoEncontradaException(Long actaId, Long documentoId) {
        super("Relacion acta-documento no encontrada: acta=" + actaId + " documento=" + documentoId);
    }
}