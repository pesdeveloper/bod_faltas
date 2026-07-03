package ar.gob.malvinas.faltas.core.application.command;

import java.time.LocalDate;

public record VersionarInspectorCommand(
        Long idInsp,
        Integer legajoInsp,
        String nomInsp,
        Long idDep,
        Integer verDep,
        LocalDate fhVigDesde,
        String idUserAlta
) {}
