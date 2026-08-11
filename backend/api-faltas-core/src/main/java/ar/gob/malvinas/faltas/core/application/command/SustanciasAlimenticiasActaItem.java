package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.AmbitoCtv;

/**
 * Bloque tipado de acta de sustancias alimenticias/bromatología, agregado en Wave 4
 * Parte 3A. 1:1 con {@code fal_acta_sustancias_alimenticias}. Solo aplicable cuando
 * {@code tipoActa=SUSTANCIAS_ALIMENTICIAS}.
 */
public record SustanciasAlimenticiasActaItem(
        Long rubroId,
        Integer idRub,
        AmbitoCtv ambitoCtv,
        String ambitoCtvTxt,
        String descripcionSustancias
) {}
