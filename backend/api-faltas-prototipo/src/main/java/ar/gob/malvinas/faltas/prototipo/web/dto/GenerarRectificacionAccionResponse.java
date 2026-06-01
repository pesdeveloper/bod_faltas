package ar.gob.malvinas.faltas.prototipo.web.dto;

public record GenerarRectificacionAccionResponse(
        String resultado,
        String mensaje,
        String actaId,
        String bandejaActual,
        String estadoProcesoActual) {
}
