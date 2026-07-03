package ar.gob.malvinas.faltas.core.application.command;

/**
 * Command para emitir formalmente un documento.
 * Slice 8C-6C-1.
 *
 * storageKey y hashDocu son obligatorios si plantilla.siGeneraPdf = true.
 * Si siGeneraPdf = false, pueden ser null.
 */
public record EmitirDocumentoCommand(
        Long documentoId,
        String idUserOperacion,
        String storageKey,
        String hashDocu
) {
    public EmitirDocumentoCommand {
        if (documentoId == null) {
            throw new IllegalArgumentException("documentoId es obligatorio");
        }
        if (idUserOperacion == null || idUserOperacion.isBlank()) {
            throw new IllegalArgumentException("idUserOperacion es obligatorio y no puede estar en blanco");
        }
    }
}