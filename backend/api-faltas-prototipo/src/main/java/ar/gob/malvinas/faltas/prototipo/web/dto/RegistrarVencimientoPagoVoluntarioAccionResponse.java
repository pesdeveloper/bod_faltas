package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.math.BigDecimal;

/**
 * Respuesta de POST /api/prototipo/actas/{id}/acciones/registrar-vencimiento-pago-voluntario.
 *
 * <p>Expone el nuevo estado de pago ({@code VENCIDO}) y el monto histórico
 * conservado si ya estaba fijado. El trámite puede continuar a fallo de fondo.
 */
public record RegistrarVencimientoPagoVoluntarioAccionResponse(
        String resultado,
        String mensaje,
        String actaId,
        String bandejaActual,
        String estadoProcesoActual,
        String situacionPago,
        BigDecimal montoPagoVoluntario) {}
