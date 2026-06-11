package ar.gob.malvinas.faltas.prototipo.web.dto;

/**
 * Vista ciudadana de un documento visible en el portal del infractor.
 * Solo expone los campos relevantes para el ciudadano; no expone
 * identificadores internos ni detalles administrativos.
 */
public record ActaDocumentoPortalResponse(
        String tipo,
        String titulo,
        String estadoDocumento,
        String estadoNotificacion,
        boolean visible,
        boolean notificable,
        boolean notificado,
        boolean puedeAbrir) {
}
