package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.model.FalInspector;
import ar.gob.malvinas.faltas.core.domain.model.FalInspectorVersion;

import java.time.LocalDateTime;
import java.util.List;

public record InspectorResponse(
        Long idInsp,
        String idUser,
        int legajoInsp,
        String nomInsp,
        boolean siActivo,
        LocalDateTime fhAlta,
        String idUserAlta,
        List<InspectorVersionResponse> versiones
) {
    public static InspectorResponse de(FalInspector insp, List<FalInspectorVersion> versiones) {
        return new InspectorResponse(
                insp.getIdInsp(),
                insp.getIdUser(),
                insp.getLegajoInsp(),
                insp.getNomInsp(),
                insp.isSiActivo(),
                insp.getFhAlta(),
                insp.getIdUserAlta(),
                versiones.stream().map(InspectorVersionResponse::de).toList()
        );
    }
}
