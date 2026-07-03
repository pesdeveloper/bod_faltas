package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.TipoGestionExterna;

/**
 * DTO para la solicitud de derivacion a gestion externa.
 */
public record DerivarGestionExternaRequest(
        TipoGestionExterna tipoGestionExterna,
        String motivoDerivacion,
        String observaciones
) {}
