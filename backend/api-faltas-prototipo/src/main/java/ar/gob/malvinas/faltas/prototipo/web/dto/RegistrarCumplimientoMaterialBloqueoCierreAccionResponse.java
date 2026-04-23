package ar.gob.malvinas.faltas.prototipo.web.dto;

public record RegistrarCumplimientoMaterialBloqueoCierreAccionResponse(
        String status,
        String mensaje,
        String actaId,
        String pendienteCumplido,
        CerrabilidadResponse cerrabilidad) {
}
