package ar.gob.malvinas.faltas.prototipo.web.dto;

public record NotificadorMunicipalAcuseResponse(
        String resultado,
        String mensaje,
        String actaId,
        String acta,
        String bandejaActual,
        String estadoProcesoActual,
        ActaNotificacionResponse notificacion,
        NotificadorMunicipalNotificacionResponse vista) {
}
