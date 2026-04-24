package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.util.List;

/**
 * Plano de hechos materiales del acta, separado de la nómina documental.
 * {@code lecturaOperativa} agrega contexto de demo cuando subsisten
 * condiciones o cumplimientos material pendientes frente a un resultado final
 * potencialmente compatible con cierre.
 */
public record HechosMaterialesActaResponse(
        List<HechosMaterialesEjeResponse> ejes, String lecturaOperativa) {
}
