package ar.gob.malvinas.faltas.prototipo.web.dto;

public record ParalizarActaAccionResponse(
        String resultado,
        String mensaje,
        String actaId,
        String bandeja,
        String estadoProceso,
        String situacionAdministrativa,
        String accionPendiente) {
}
