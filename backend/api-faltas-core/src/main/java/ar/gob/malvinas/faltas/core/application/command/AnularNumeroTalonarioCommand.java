package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.MotivoAnulacionTalonario;

import java.time.LocalDateTime;

/**
 * Anulacion administrativa de un numero de talonario manual fisico.
 * Slice 8B-6.
 */
public record AnularNumeroTalonarioCommand(
        Long idTalonario,
        int nroTalonario,
        MotivoAnulacionTalonario motivoAnulacion,
        String observacion,
        Long idDep,
        Short verDep,
        Long idInsp,
        Short verInsp,
        LocalDateTime fhMovimiento,
        String idUserMovimiento
) {}
