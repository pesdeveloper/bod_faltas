package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;
import ar.gob.malvinas.faltas.core.domain.model.NumPolitica;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PoliticaNumeracionResponse(
        Long id,
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
        LocalDateTime fhAlta,
        String idUserAlta
) {
    public static PoliticaNumeracionResponse de(NumPolitica p) {
        return new PoliticaNumeracionResponse(
                p.getId(), p.getCodigo(), p.getDescripcion(), p.getClaseNumeracion(),
                p.isSiReinicioAnual(), p.isSiIncluyePrefijo(), p.getPrefijo(),
                p.isSiIncluyeAnio(), p.getFormatoAnio(), p.isSiIncluyeSerie(),
                p.getLongitudNro(), p.getFormatoVisible(), p.isSiActiva(),
                p.getFhVigDesde(), p.getFhVigHasta(), p.getFhAlta(), p.getIdUserAlta());
    }
}
