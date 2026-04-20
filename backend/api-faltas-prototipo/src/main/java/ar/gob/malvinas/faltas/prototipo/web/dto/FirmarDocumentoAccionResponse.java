package ar.gob.malvinas.faltas.prototipo.web.dto;

public record FirmarDocumentoAccionResponse(
        String resultado,
        String mensaje,
        String actaId,
        String documentoId,
        String bandejaActual,
        String estadoProcesoActual) {
}
