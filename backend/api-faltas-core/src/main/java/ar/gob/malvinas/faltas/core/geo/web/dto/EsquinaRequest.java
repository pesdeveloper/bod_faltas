package ar.gob.malvinas.faltas.core.geo.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request para geocodificar esquina (calle1 y calle2).
 * Ambas calles identificadas por idTca e idLoc.
 * Puede devolver múltiples candidatos si hay más de una intersección.
 */
public record EsquinaRequest(
        @NotBlank String idTcaCalle1,
        @NotBlank String idLocCalle1,
        @NotBlank String idTcaCalle2,
        @NotBlank String idLocCalle2
) {}
