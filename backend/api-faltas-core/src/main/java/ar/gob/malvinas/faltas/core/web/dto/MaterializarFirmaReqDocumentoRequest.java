package ar.gob.malvinas.faltas.core.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request HTTP para materializar requisitos de firma de un documento.
 *
 * Slice 8C-4.
 */
public record MaterializarFirmaReqDocumentoRequest(
        @NotBlank String idUserAlta
) {}
