package ar.gob.malvinas.faltas.prototipo.web.dto;

public record ObservarPagoInformadoAccionResponse(
        String status,
        String mensaje,
        String actaId,
        String situacionPago) {
}

