package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.model.FalFirmante;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmanteVersion;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmanteVersionHabilitacion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record FirmanteResponse(
        Long idFirmante,
        String idUser,
        String nomFirmante,
        boolean siActivo,
        LocalDateTime fhAlta,
        String idUserAlta,
        List<FirmanteVersionResponse> versiones
) {
    public static FirmanteResponse de(FalFirmante f,
                                       List<FalFirmanteVersion> versiones,
                                       Map<Integer, List<FalFirmanteVersionHabilitacion>> habsPorVer) {
        return new FirmanteResponse(
                f.getIdFirmante(),
                f.getIdUser(),
                f.getNomFirmante(),
                f.isSiActivo(),
                f.getFhAlta(),
                f.getIdUserAlta(),
                versiones.stream()
                        .map(v -> FirmanteVersionResponse.de(
                                v,
                                habsPorVer.getOrDefault(v.getVerFirmante(), List.of())))
                        .toList()
        );
    }
}
