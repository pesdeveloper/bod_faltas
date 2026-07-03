package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaFirmaReq;

import java.time.LocalDateTime;

public record DocumentoPlantillaFirmaReqResponse(
        Long id,
        Long plantillaId,
        short seqFirmaReq,
        short rolFirmaReq,
        Short mecanismoFirmaReq,
        boolean siObligatoria,
        boolean siActiva,
        LocalDateTime fhAlta,
        String idUserAlta
) {
    public static DocumentoPlantillaFirmaReqResponse from(FalDocumentoPlantillaFirmaReq r) {
        return new DocumentoPlantillaFirmaReqResponse(
                r.getId(),
                r.getPlantillaId(),
                r.getSeqFirmaReq(),
                r.getRolFirmaReq(),
                r.getMecanismoFirmaReq(),
                r.isSiObligatoria(),
                r.isSiActiva(),
                r.getFhAlta(),
                r.getIdUserAlta()
        );
    }
}