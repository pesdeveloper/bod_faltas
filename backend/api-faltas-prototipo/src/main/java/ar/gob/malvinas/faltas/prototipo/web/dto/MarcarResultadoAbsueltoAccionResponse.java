package ar.gob.malvinas.faltas.prototipo.web.dto;

public record MarcarResultadoAbsueltoAccionResponse(
        String status,
        String mensaje,
        String actaId,
        String bandejaActual,
        CerrabilidadResponse cerrabilidad) {
}
