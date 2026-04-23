package ar.gob.malvinas.faltas.prototipo.web.dto;

public record RegistrarConstatacionMaterialTempranaAccionResponse(
        String codigo,
        String mensaje,
        String actaId,
        String documentoId,
        String tipoDocumento,
        String bandejaActual,
        String estadoProcesoActual) {
}
