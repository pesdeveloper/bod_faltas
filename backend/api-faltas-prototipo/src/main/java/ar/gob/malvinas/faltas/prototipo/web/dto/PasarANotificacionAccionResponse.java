package ar.gob.malvinas.faltas.prototipo.web.dto;

public record PasarANotificacionAccionResponse(
        String resultado,
        String mensaje,
        String actaId,
        String bandejaActual,
        String estadoProcesoActual) {
}
