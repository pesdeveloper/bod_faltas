package ar.gob.malvinas.faltas.core.geo.web.dto;

/**
 * Resumen de comercio asociado a un inmueble de la parcela.
 * cuentaDisplay: "{idSuj}-{idBie}" para presentación en UI.
 */
public record ComercioResumenDto(
        int idSuj,
        int idBie,
        String nombreFantasia,
        String cuentaDisplay
) {}
