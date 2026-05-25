package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.time.LocalDateTime;

public record NotificadorMunicipalNotificacionResponse(
        String notificacionId,
        String actaId,
        String acta,
        String tipo,
        String canal,
        String estado,
        String resultado,
        String destinatario,
        String domicilio,
        String observacion,
        String qrNotificacion,
        LocalDateTime fechaPreparacion,
        LocalDateTime fechaEnvio) {
}
