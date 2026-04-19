package ar.gob.malvinas.faltas.prototipo.web.dto;

public record CerrarActaAccionResponse(
        String resultado,
        String mensaje,
        String actaId,
        String bandejaActual,
        String estadoProcesoActual) {
}
