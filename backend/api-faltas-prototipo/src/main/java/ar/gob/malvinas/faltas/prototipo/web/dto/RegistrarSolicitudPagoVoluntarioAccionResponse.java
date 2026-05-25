package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.math.BigDecimal;

public record RegistrarSolicitudPagoVoluntarioAccionResponse(
        String status,
        String mensaje,
        String actaId,
        String bandejaActual,
        String estadoProcesoActual,
        String accionPendiente,
        BigDecimal montoPagoVoluntario) {
}
