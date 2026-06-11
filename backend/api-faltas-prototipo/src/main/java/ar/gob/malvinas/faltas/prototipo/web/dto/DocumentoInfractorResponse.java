package ar.gob.malvinas.faltas.prototipo.web.dto;

/**
 * Documento del expediente visible para el infractor desde el portal.
 *
 * <p>Solo se listan documentos formalmente visibles: firmados y en etapa de
 * notificación o ya notificados. Documentos internos, borradores o piezas
 * sin firma no se exponen. {@code estadoNotificacion} es
 * {@code PENDIENTE_NOTIFICACION} o {@code NOTIFICADO}; abrir un documento
 * pendiente registra notificación positiva por canal
 * {@code PORTAL_INFRACTOR} (acción idempotente: si ya estaba notificado,
 * abrirlo de nuevo solo permite consulta).
 */
public record DocumentoInfractorResponse(
        String tipo,
        String titulo,
        String estadoDocumento,
        String estadoNotificacion,
        boolean visible,
        boolean notificable,
        boolean notificado,
        boolean puedeAbrir) {
}
