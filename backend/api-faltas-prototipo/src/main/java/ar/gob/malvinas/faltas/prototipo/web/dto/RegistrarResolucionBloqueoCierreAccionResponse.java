package ar.gob.malvinas.faltas.prototipo.web.dto;

public record RegistrarResolucionBloqueoCierreAccionResponse(
        String status,
        String mensaje,
        String actaId,
        String documentoId,
        String tipoDocumento,
        String pendienteAtendido,
        CerrabilidadResponse cerrabilidad) {
}
