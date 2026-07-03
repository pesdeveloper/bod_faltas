package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.TipoFirma;

/**
 * Comando para registrar una firma documental real.
 * Slice 8C-6B-1.
 */
public record RegistrarFirmaDocumentalCommand(
        Long documentoId,
        short seqFirmaReq,
        Long idFirmante,
        TipoFirma tipoFirma,
        String idUserFirma,
        String hashDocumento,
        String referenciaFirmaExt,
        String storageKey
) {}
