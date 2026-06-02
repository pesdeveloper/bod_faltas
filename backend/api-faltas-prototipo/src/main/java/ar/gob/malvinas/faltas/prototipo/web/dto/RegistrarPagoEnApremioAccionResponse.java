package ar.gob.malvinas.faltas.prototipo.web.dto;

public record RegistrarPagoEnApremioAccionResponse(
        String resultado,
        String mensaje,
        String actaId,
        String bandejaActual,
        String estadoProcesoActual) {
}
