package ar.gob.malvinas.faltas.prototipo.web.dto;

public record ReconocerOrigenBloqueanteMaterialAccionResponse(
        String codigo,
        String mensaje,
        String actaId,
        String origenBloqueante,
        CerrabilidadResponse cerrabilidad) {
}
