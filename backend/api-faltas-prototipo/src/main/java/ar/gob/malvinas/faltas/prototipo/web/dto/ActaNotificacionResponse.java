package ar.gob.malvinas.faltas.prototipo.web.dto;

public record ActaNotificacionResponse(
        String id,
        String actaId,
        String canal,
        String estadoNotificacion,
        String destinatarioResumen) {
}
