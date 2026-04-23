package ar.gob.malvinas.faltas.prototipo.web.dto;

public record RegistrarPagoInformadoAccionResponse(
        String status,
        String mensaje,
        String actaId,
        String situacionPago) {
}

