package ar.gob.malvinas.faltas.core.application.command;

import java.time.LocalDateTime;

/**
 * Comando interno para emitir un numero de talonario DOCUMENTO.
 *
 * Usado por TalonarioService.emitirNumeroDocumento() para resolver y emitir
 * el siguiente correlativo disponible de clase DOCUMENTO.
 *
 * Slice 8C-5A: numeracion documental reusable.
 */
public record EmitirNumeroDocumentoCommand(
        Long idDep,
        Short verDep,
        Short tipoDocu,
        Long documentoId,
        LocalDateTime fhMovimiento,
        String idUserMovimiento
) {}
