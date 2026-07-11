package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.TipoFirma;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request para registrar firma documental real via callback de la aplicacion de Firmas.
 * POST /api/faltas/documentos/{documentoId}/firmar-real
 *
 * El actor se extrae exclusivamente del token Bearer (JWT sub).
 * No se acepta idUserFirma en el body.
 * referenciaFirmaExt es obligatoria y actua como clave de idempotencia.
 */
public record RegistrarFirmaDocumentalRequest(
        @Positive short seqFirmaReq,
        @NotNull Long idFirmante,
        @NotNull TipoFirma tipoFirma,
        @NotBlank String referenciaFirmaExt,
        String hashDocumento,
        String storageKey
) {}
