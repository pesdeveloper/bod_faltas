package ar.gob.malvinas.faltas.prototipo.web.dto;

public record PagoCondenaAccionResponse(
        String status,
        String mensaje,
        String actaId,
        String situacionPagoCondena) {
}
