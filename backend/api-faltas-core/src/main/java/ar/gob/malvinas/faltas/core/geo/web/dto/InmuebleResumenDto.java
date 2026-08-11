package ar.gob.malvinas.faltas.core.geo.web.dto;

/**
 * Resumen de inmueble asociado a una parcela.
 * cuentaDisplay: "{idSuj}-{idBie}" para presentación en UI.
 */
public record InmuebleResumenDto(
        int idSuj,
        int idBie,
        String cuentaDisplay
) {}
