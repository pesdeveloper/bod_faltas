package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.model.FalFirmanteVersion;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmanteVersionHabilitacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FirmanteVersionResponse(
        Long idFirmante,
        int verFirmante,
        String idUser,
        String nomFirmante,
        String rolFirmante,
        String cargoFirmante,
        Long idDep,
        Integer verDep,
        LocalDate fhVigDesde,
        LocalDate fhVigHasta,
        boolean siActivo,
        LocalDateTime fhAlta,
        String idUserAlta,
        List<FirmanteHabilitacionResponse> habilitaciones
) {
    public static FirmanteVersionResponse de(FalFirmanteVersion v,
                                              List<FalFirmanteVersionHabilitacion> habs) {
        return new FirmanteVersionResponse(
                v.getIdFirmante(),
                v.getVerFirmante(),
                v.getIdUser(),
                v.getNomFirmante(),
                v.getRolFirmante(),
                v.getCargoFirmante(),
                v.getIdDep(),
                v.getVerDep(),
                v.getFhVigDesde(),
                v.getFhVigHasta(),
                v.isSiActivo(),
                v.getFhAlta(),
                v.getIdUserAlta(),
                habs.stream().map(FirmanteHabilitacionResponse::de).toList()
        );
    }
}
