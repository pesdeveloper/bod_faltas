package ar.gob.malvinas.faltas.core.domain.model;

/**
 * Identidad compuesta del pivot acta-documento.
 * La pertenencia funcional de un documento a un expediente se identifica
 * por (actaId, documentoId). Inmutable.
 */
public record ActaDocumentoId(Long actaId, Long documentoId) {

    public ActaDocumentoId {
        if (actaId == null || actaId <= 0)
            throw new IllegalArgumentException("actaId debe ser positivo");
        if (documentoId == null || documentoId <= 0)
            throw new IllegalArgumentException("documentoId debe ser positivo");
    }
}
