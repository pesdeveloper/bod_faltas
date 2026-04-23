package ar.gob.malvinas.faltas.prototipo.web.dto;

public record ConfirmarPagoInformadoAccionResponse(
        String status,
        String mensaje,
        String actaId,
        String situacionPago) {
}

