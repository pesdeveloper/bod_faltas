package ar.gob.malvinas.faltas.core.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request HTTP para emitir formalmente un documento.
 * Slice 8C-6C-1.
 *
 * storageKey y hashDocu se validan en DocumentoService segun plantilla.siGeneraPdf.
 */
public record EmitirDocumentoRequest(
        @NotBlank String idUserOperacion,
        String storageKey,
        String hashDocu
) {}