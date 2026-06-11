package ar.gob.malvinas.faltas.prototipo.web.dto;

public record AnularActaPorNulidadAccionResponse(
        String resultado,
        String mensaje,
        String actaId,
        String bandejaActual,
        String estadoProcesoActual,
        String motivoArchivo) {}
