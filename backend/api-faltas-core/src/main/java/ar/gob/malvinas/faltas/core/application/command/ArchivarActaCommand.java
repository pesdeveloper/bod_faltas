package ar.gob.malvinas.faltas.core.application.command;

/**
 * Comando para archivar un acta.
 *
 * idMotivoArchivo: ID del motivo de archivo activo.
 * observacionTexto: texto para fal_observacion (obligatorio si el motivo lo requiere).
 * documentoId: documento adjunto opcional.
 * idUserOperacion: usuario que ejecuta la operacion.
 */
public record ArchivarActaCommand(
        Long actaId,
        Long idMotivoArchivo,
        String observacionTexto,
        Long documentoId,
        String idUserOperacion
) {
    public ArchivarActaCommand(Long actaId, Long idMotivoArchivo, String observacionTexto) {
        this(actaId, idMotivoArchivo, observacionTexto, null, "SYS");
    }
}
