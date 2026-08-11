package ar.gob.malvinas.faltas.core.geo.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Request para reverse geocoding a partir de un punto geográfico.
 * lat/lng en WGS84 (EPSG:4326).
 */
public record ReversePointRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double lng
) {}
