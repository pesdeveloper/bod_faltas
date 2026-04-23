package ar.gob.malvinas.faltas.prototipo.web.dto;

public record AdjuntarComprobantePagoInformadoAccionResponse(
        String status,
        String mensaje,
        String actaId,
        String situacionPago,
        String accionPendiente,
        PagoInformadoResponse pagoInformado) {
}

