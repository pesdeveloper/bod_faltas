package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.TipoFirma;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request para registrar firma documental real.
 * POST /api/faltas/documentos/{documentoId}/firmar-real
 * Slice 8C-6B-1.
 */
public record RegistrarFirmaDocumentalRequest(
        @Positive short seqFirmaReq,
        @NotNull Long idFirmante,
        @NotNull TipoFirma tipoFirma,
        @NotBlank String idUserFirma,
        String hashDocumento,
        String referenciaFirmaExt,
        String storageKey
) {}
