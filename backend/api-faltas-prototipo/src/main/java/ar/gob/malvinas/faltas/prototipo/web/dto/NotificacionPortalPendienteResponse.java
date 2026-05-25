package ar.gob.malvinas.faltas.prototipo.web.dto;

public record NotificacionPortalPendienteResponse(
        String id,
        String tipo,
        String canal,
        String estado,
        String resultado,
        String destinatario,
        String resumen,
        String mensajeVisible) {
}
