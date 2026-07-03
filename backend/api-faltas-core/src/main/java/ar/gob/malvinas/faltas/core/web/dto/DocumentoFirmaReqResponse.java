package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirmaReq;

import java.time.LocalDateTime;

/**
 * Response de requisito de firma de documento concreto.
 *
 * Slice 8C-4.
 */
public record DocumentoFirmaReqResponse(
        Long id,
        Long documentoId,
        short seqFirmaReq,
        short rolFirmaReq,
        Short mecanismoFirmaReq,
        Short ordenFirma,
        EstadoFirmaReq estadoFirmaReq,
        short estadoFirmaReqCodigo,
        boolean siObligatoria,
        boolean siActiva,
        LocalDateTime fhAlta,
        String idUserAlta
) {
    public static DocumentoFirmaReqResponse from(FalDocumentoFirmaReq req) {
        return new DocumentoFirmaReqResponse(
                req.getId(),
                req.getDocumentoId(),
                req.getSeqFirmaReq(),
                req.getRolFirmaReq(),
                req.getMecanismoFirmaReq(),
                req.getOrdenFirma(),
                req.getEstadoFirmaReq(),
                req.getEstadoFirmaReq() != null ? req.getEstadoFirmaReq().codigo() : 0,
                req.isSiObligatoria(),
                req.isSiActiva(),
                req.getFhAlta(),
                req.getIdUserAlta()
        );
    }
}
