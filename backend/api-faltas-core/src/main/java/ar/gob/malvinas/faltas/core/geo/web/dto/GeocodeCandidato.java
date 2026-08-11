package ar.gob.malvinas.faltas.core.geo.web.dto;

import java.util.List;

/**
 * Candidato de geocodificación o reverse geocoding.
 * Un request puede devolver múltiples candidatos cuando hay ambigüedad.
 * El cliente debe mostrarlos todos y permitir elegir explícitamente.
 * Reutilizable por formulario web de acta y app móvil.
 */
public record GeocodeCandidato(
        double lat,
        double lng,
        String calle,
        String idTca,
        Integer alturaAprox,
        Integer alturaRangoDesde,
        Integer alturaRangoHasta,
        String ladoCuadra,
        String idLoc,
        String localidad,
        String barrio,
        String manzana,
        String nomenclatura,
        String calidad,
        String evidencia,
        List<String> advertencias,
        String descripcion
) {}
