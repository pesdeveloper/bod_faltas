package ar.gob.malvinas.faltas.core.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request para enviar un documento a firma.
 *
 * Slice 8C-5B.
 */
public record EnviarAFirmaRequest(
        @NotBlank String idUserOperacion
) {}