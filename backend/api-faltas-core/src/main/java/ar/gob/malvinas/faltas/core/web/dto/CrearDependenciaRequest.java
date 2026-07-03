package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;

import java.time.LocalDate;

public record CrearDependenciaRequest(
        String codDep,
        String nomDep,
        Long idDepPadre,
        TipoActa tipoActa,
        LocalDate fhVigDesde,
        String idUserAlta
) {}
