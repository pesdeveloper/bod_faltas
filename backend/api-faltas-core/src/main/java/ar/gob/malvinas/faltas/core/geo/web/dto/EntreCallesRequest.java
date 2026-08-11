package ar.gob.malvinas.faltas.core.geo.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request para geocodificar tramo entre calles.
 * Tres calles: objetivo + dos límites.
 * Las tres deben pertenecer a la misma localidad o a localidades contíguas documentadas.
 */
public record EntreCallesRequest(
        @NotBlank String idTcaCalleObjetivo,
        @NotBlank String idLocCalleObjetivo,
        @NotBlank String idTcaLimite1,
        @NotBlank String idLocLimite1,
        @NotBlank String idTcaLimite2,
        @NotBlank String idLocLimite2
) {}
