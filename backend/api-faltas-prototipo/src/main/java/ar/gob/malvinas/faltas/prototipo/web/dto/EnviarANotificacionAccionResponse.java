package ar.gob.malvinas.faltas.prototipo.web.dto;

public record EnviarANotificacionAccionResponse(
        String resultado,
        String mensaje,
        String actaId,
        String bandejaActual,
        String estadoProcesoActual) {}
