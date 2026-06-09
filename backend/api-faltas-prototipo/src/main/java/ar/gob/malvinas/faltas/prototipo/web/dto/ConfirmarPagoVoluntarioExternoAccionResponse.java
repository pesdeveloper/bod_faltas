package ar.gob.malvinas.faltas.prototipo.web.dto;

public record ConfirmarPagoVoluntarioExternoAccionResponse(
        String status,
        String mensaje,
        String actaId,
        String situacionPago) {
}
