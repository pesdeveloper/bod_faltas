package ar.gob.malvinas.faltas.core.application.command;

/**
 * Comando para materializar requisitos de firma de un documento desde su plantilla.
 *
 * Slice 8C-4.
 */
public record MaterializarFirmaReqDocumentoCommand(
        Long documentoId,
        String idUserAlta
) {}
