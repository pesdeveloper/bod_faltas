package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.time.LocalDateTime;

public record ActaNotificacionResponse(
        String id,
        String actaId,
        String canal,
        String estadoNotificacion,
        String destinatarioResumen,
        String tipo,
        String canalTipificado,
        String estado,
        String resultado,
        String referencia,
        String eventoRelacionado,
        String loteId,
        String referenciaExterna,
        LocalDateTime fechaPreparacion,
        LocalDateTime fechaEnvio,
        LocalDateTime fechaResultado,
        String observacion,
        String destinatarioNombre,
        String destinatarioEmail,
        String domicilioTexto,
        Boolean domicilioElectronicoVerificado,
        Integer diasPlazoNotificacionElectronica) {
}
