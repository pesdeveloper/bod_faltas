package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;

import java.time.LocalDate;

public record CrearTalonarioAmbitoRequest(
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
