package ar.gob.malvinas.faltas.prototipo.domain;

public record ActaNotificacionMock(
        String id,
        String actaId,
        String canal,
        String estadoNotificacion,
        String destinatarioResumen) {
}
