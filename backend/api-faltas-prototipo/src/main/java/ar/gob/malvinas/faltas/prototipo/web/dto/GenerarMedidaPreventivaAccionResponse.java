package ar.gob.malvinas.faltas.prototipo.web.dto;

public record GenerarMedidaPreventivaAccionResponse(
        String resultado,
        String mensaje,
        String actaId,
        String bandejaActual,
        String estadoProcesoActual) {
}
