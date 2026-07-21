package ar.gob.malvinas.faltas.core.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request para convalidar la firma escaneada/olografa de un documento adjunto.
 * Slice 8C-6D-1.
 *
 * seqFirmaReq: nullable. Si se informa, cumple el FalDocumentoFirmaReq correspondiente.
 */
public record ConvalidarFirmaEscaneadaRequest(
        Short seqFirmaReq,
        @NotNull Long idFirmante,
        @NotBlank String idUserFirma,
        String referenciaFirmaExt
) {}
