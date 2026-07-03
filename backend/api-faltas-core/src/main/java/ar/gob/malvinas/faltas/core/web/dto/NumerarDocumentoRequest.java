package ar.gob.malvinas.faltas.core.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request HTTP para numerar un documento manualmente.
 *
 * El talonario se resuelve automaticamente; el usuario solo informa quien opera.
 *
 * Slice 8C-5A: numeracion documental reusable.
 */
public record NumerarDocumentoRequest(
        @NotBlank String idUserOperacion
) {}
