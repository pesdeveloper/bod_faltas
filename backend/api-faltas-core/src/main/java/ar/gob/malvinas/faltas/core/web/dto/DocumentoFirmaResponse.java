package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoFirma;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirma;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirma;

import java.time.LocalDateTime;

/**
 * Response de firma documental registrada.
 * Slice 8C-6B-1.
 */
public record DocumentoFirmaResponse(
        Long id,
        Long idDocumento,
        short seqFirmaReq,
        Long idFirmante,
        short verFirmante,
        String idUserFirma,
        short rolFirmante,
        String nombreFirmante,
        TipoFirma tipoFirma,
        short tipoFirmaCodigo,
        EstadoFirma estadoFirma,
        short estadoFirmaCodigo,
        String hashDocumento,
        String referenciaFirmaExt,
        String storageKey,
        String mensajeError,
        LocalDateTime fhFirma,
        LocalDateTime fhAlta,
        String idUserAlta
) {
    public static DocumentoFirmaResponse from(FalDocumentoFirma f) {
        return new DocumentoFirmaResponse(
                f.getId(),
                f.getIdDocumento(),
                f.getSeqFirmaReq(),
                f.getIdFirmante(),
                f.getVerFirmante(),
                f.getIdUserFirma(),
                f.getRolFirmante(),
                f.getNombreFirmante(),
                f.getTipoFirma(),
                f.getTipoFirma() != null ? f.getTipoFirma().codigo() : 0,
                f.getEstadoFirma(),
                f.getEstadoFirma() != null ? f.getEstadoFirma().codigo() : 0,
                f.getHashDocumento(),
                f.getReferenciaFirmaExt(),
                f.getStorageKey(),
                f.getMensajeError(),
                f.getFhFirma(),
                f.getFhAlta(),
                f.getIdUserAlta()
        );
    }
}
