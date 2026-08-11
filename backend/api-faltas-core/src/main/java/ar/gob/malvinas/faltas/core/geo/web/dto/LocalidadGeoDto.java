package ar.gob.malvinas.faltas.core.geo.web.dto;

/**
 * Localidad del catálogo Malvinas Argentinas.
 * idLoc: código de 2 caracteres (LP, GB, etc.)
 * Reutilizable por formulario web de acta y app móvil.
 */
public record LocalidadGeoDto(
        String idLoc,
        String nombre,
        String fuente
) {}
