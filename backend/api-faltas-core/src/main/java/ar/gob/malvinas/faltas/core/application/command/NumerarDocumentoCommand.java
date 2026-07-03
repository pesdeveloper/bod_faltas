package ar.gob.malvinas.faltas.core.application.command;

/**
 * Comando para numerar un documento usando el sistema de talonarios DOCUMENTO.
 *
 * El talonario se resuelve automaticamente por num_talonario_ambito con
 * clase_talonario = DOCUMENTO, usando el tipoDocu del documento y la
 * dependencia/version del acta asociada.
 *
 * El usuario no elige talonario ni numero.
 *
 * Slice 8C-5A: numeracion documental reusable.
 */
public record NumerarDocumentoCommand(
        Long documentoId,
        String idUserOperacion
) {}
