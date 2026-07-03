package ar.gob.malvinas.faltas.core.application.command;

import java.time.LocalDateTime;

/**
 * Justificacion de un numero de talonario manual fisico sin uso.
 * Slice 8B-6.
 */
public record JustificarNumeroTalonarioCommand(
        Long idTalonario,
        int nroTalonario,
        String observacion,
        Long idDep,
        Short verDep,
        Long idInsp,
        Short verInsp,
        LocalDateTime fhMovimiento,
        String idUserMovimiento
) {}
