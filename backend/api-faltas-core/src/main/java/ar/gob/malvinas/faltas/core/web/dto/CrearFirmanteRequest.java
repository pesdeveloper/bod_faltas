package ar.gob.malvinas.faltas.core.web.dto;

import java.time.LocalDate;

public record CrearFirmanteRequest(
        String idUser,
        String nomFirmante,
        String rolFirmante,
        String cargoFirmante,
        Long idDep,
        Integer verDep,
        LocalDate fhVigDesde,
        String idUserAlta
) {}
