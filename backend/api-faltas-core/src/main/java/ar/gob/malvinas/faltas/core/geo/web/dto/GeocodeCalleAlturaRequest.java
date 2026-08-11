package ar.gob.malvinas.faltas.core.geo.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request para geocodificar calle + altura.
 * idTca e idLoc identifican la calle de forma inequívoca.
 * No usar solo texto: puede haber calles homónimas en distintas localidades.
 */
public record GeocodeCalleAlturaRequest(
        @NotBlank String idTca,
        @NotBlank String idLoc,
        @NotNull @Min(1) @Max(99999) Integer altura
) {}
