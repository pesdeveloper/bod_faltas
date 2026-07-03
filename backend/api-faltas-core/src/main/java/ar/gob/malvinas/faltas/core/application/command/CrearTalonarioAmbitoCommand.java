package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;

import java.time.LocalDate;

public record CrearTalonarioAmbitoCommand(
        Long talonarioId,
        ClaseNumeracion claseTalonario,
        Short tipoDocu,
        Short tipoActa,
        Long idDep,
        Short verDep,
        AlcanceTalonario alcance,
        short prioridad,
        LocalDate fhDesde,
        LocalDate fhHasta,
        boolean siActivo,
        String idUserAlta
) {}
