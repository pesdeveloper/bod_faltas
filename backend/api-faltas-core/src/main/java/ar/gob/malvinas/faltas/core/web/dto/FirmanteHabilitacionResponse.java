package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.model.FalFirmanteVersionHabilitacion;

import java.time.LocalDateTime;

public record FirmanteHabilitacionResponse(
        Long idFirmante,
        int verFirmante,
        short tipoDocu,
        short rolFirmaReq,
        Short mecanismoFirmaReq,
        boolean siActivo,
        LocalDateTime fhAlta,
        String idUserAlta
) {
    public static FirmanteHabilitacionResponse de(FalFirmanteVersionHabilitacion h) {
        return new FirmanteHabilitacionResponse(
                h.getIdFirmante(),
                h.getVerFirmante(),
                h.getTipoDocu(),
                h.getRolFirmaReq(),
                h.getMecanismoFirmaReq(),
                h.isSiActivo(),
                h.getFhAlta(),
                h.getIdUserAlta()
        );
    }
}
