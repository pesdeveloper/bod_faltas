package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;

/**
 * Response para convalidacion de firma escaneada sin requisito (trazabilidad simple).
 * Slice 8C-6D-1.
 */
public record ConvalidacionFirmaEscaneadaResponse(
        Long documentoId,
        EstadoDocu estadoDocu,
        String mensaje
) {}
