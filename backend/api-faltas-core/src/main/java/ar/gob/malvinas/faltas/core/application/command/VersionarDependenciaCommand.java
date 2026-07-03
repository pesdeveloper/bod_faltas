package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;

import java.time.LocalDate;

public record VersionarDependenciaCommand(
        Long idDep,
        String nomDep,
        Long idDepPadre,
        TipoActa tipoActa,
        LocalDate fhVigDesde,
        String idUserAlta
) {}
