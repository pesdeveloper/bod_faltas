package ar.gob.malvinas.faltas.prototipo.web.dto;

public record ArchivarPorVencimientoAccionResponse(
        String resultado,
        String mensaje,
        String actaId,
        String bandejaActual,
        String estadoProcesoActual,
        String motivoArchivo) {
}
