package ar.gob.malvinas.faltas.core.web.dto;

import java.time.LocalDate;

public record VersionarInspectorRequest(
        Integer legajoInsp,
        String nomInsp,
        Long idDep,
        Integer verDep,
        LocalDate fhVigDesde,
        String idUserAlta
) {}
