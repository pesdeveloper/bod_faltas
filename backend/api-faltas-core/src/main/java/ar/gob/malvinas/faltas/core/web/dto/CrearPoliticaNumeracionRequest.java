package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;

import java.time.LocalDate;

public record CrearPoliticaNumeracionRequest(
        String codigo,
        String descripcion,
        ClaseNumeracion claseNumeracion,
        boolean siReinicioAnual,
        boolean siIncluyePrefijo,
        String prefijo,
        boolean siIncluyeAnio,
        Short formatoAnio,
        boolean siIncluyeSerie,
        Short longitudNro,
        String formatoVisible,
        boolean siActiva,
        LocalDate fhVigDesde,
        LocalDate fhVigHasta,
        String idUserAlta
) {}
