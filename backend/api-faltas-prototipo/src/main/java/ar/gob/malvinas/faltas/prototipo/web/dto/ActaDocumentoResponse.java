package ar.gob.malvinas.faltas.prototipo.web.dto;

public record ActaDocumentoResponse(
        String id,
        String actaId,
        String tipoDocumento,
        String estadoDocumento,
        String nombreArchivo) {
}
