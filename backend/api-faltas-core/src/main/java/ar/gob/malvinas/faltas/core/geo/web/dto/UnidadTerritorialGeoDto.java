package ar.gob.malvinas.faltas.core.geo.web.dto;

/**
 * Municipio o Departamento del catálogo IGN/INDEC.
 * tipo: MUNICIPIO | DEPARTAMENTO | CIUDAD_AUTONOMA
 * provinciaId: código INDEC de provincia.
 * Reutilizable por formulario web de acta y app móvil.
 */
public record UnidadTerritorialGeoDto(
        long id,
        String nombre,
        String tipo,
        long provinciaId
) {}
