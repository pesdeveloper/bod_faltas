package ar.gob.malvinas.faltas.prototipo.web.dto;

public record RegistrarMedidaPreventivaPosteriorAccionResponse(
        String resultado,
        String mensaje,
        String actaId,
        String documentoId,
        String tipoDocumento,
        String bandejaActual,
        String estadoProcesoActual) {
}
