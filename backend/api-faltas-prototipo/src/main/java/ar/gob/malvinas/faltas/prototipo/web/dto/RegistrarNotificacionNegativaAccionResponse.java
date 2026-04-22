package ar.gob.malvinas.faltas.prototipo.web.dto;

public record RegistrarNotificacionNegativaAccionResponse(
        String resultado,
        String mensaje,
        String actaId,
        String bandejaActual,
        String estadoProcesoActual,
        String accionPendiente) {
}
