package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.util.List;

/**
 * Plano de hechos materiales del acta, separado de la nómina documental.
 */
public record HechosMaterialesActaResponse(List<HechosMaterialesEjeResponse> ejes) {
}
