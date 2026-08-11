package ar.gob.malvinas.faltas.core.geo.web.dto;

/**
 * Provincia del catálogo IGN/INDEC.
 * id: código INDEC (ej: 6 = Buenos Aires).
 */
public record ProvinciaGeoDto(
        long id,
        String nombre
) {}
