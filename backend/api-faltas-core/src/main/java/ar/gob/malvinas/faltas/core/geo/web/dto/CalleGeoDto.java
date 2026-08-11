package ar.gob.malvinas.faltas.core.geo.web.dto;

/**
 * Sugerencia de calle para autocomplete.
 * Siempre contextualizada con localidad (idLoc + localidad visibles).
 * nombreAnterior: alias histórico si difiere del nombre vigente.
 * texto: etiqueta de presentación para la UI (nombre + localidad [+ ex alias]).
 * Reutilizable por formulario web de acta y app móvil.
 */
public record CalleGeoDto(
        String idTca,
        String nombre,
        String idLoc,
        String localidad,
        String nombreAnterior,
        String texto,
        String fuente
) {}
