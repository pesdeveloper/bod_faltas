package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.model.FalInspectorVersion;

import java.time.LocalDate;

public record InspectorVersionResponse(
        Long idInsp,
        int verInsp,
        int legajoInsp,
        String nomInsp,
        Long idDep,
        int verDep,
        LocalDate fhVigDesde,
        LocalDate fhVigHasta,
        boolean siActivo
) {
    public static InspectorVersionResponse de(FalInspectorVersion v) {
        return new InspectorVersionResponse(
                v.getIdInsp(),
                v.getVerInsp(),
                v.getLegajoInsp(),
                v.getNomInsp(),
                v.getIdDep(),
                v.getVerDep(),
                v.getFhVigDesde(),
                v.getFhVigHasta(),
                v.isSiActivo()
        );
    }
}
