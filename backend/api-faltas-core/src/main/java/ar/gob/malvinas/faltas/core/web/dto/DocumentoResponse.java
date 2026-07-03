package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;

import java.time.LocalDateTime;

/**
 * Slice 8C-6C-1: agrega storageKey, hashDocu, fhGeneracion.
 */
public record DocumentoResponse(
        Long id,
        Long idActa,
        TipoDocu tipoDocu,
        short tipoDocuCodigo,
        EstadoDocu estadoDocu,
        short estadoDocuCodigo,
        String nroDocu,
        Long idTalonario,
        Integer nroTalonarioUsado,
        TipoFirmaReq tipoFirmaReq,
        short tipoFirmaReqCodigo,
        Long plantillaId,
        String storageKey,
        String hashDocu,
        LocalDateTime fhGeneracion) {

    public static DocumentoResponse from(FalDocumento doc) {
        return new DocumentoResponse(
                doc.getId(),
                doc.getIdActa(),
                doc.getTipoDocu(),
                doc.getTipoDocu() != null ? doc.getTipoDocu().codigo() : 0,
                doc.getEstadoDocu(),
                doc.getEstadoDocu() != null ? doc.getEstadoDocu().codigo() : 0,
                doc.getNroDocu(),
                doc.getIdTalonario(),
                doc.getNroTalonarioUsado(),
                doc.getTipoFirmaReq(),
                doc.getTipoFirmaReq() != null ? doc.getTipoFirmaReq().codigo() : 0,
                doc.getPlantillaId(),
                doc.getStorageKey(),
                doc.getHashDocu(),
                doc.getFhGeneracion());
    }
}