package ar.gob.malvinas.faltas.core.application.command;

import java.time.LocalDate;

public record CrearFirmanteCommand(
        String idUser,
        String nomFirmante,
        String rolFirmante,
        String cargoFirmante,
        Long idDep,
        Integer verDep,
        LocalDate fhVigDesde,
        String idUserAlta
) {}
