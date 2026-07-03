package ar.gob.malvinas.faltas.core.application.command;

import java.time.LocalDateTime;

/**
 * Devolucion de un numero de talonario manual fisico sin usar.
 * Slice 8B-6.
 */
public record DevolverNumeroSinUsarCommand(
        Long idTalonario,
        int nroTalonario,
        String observacion,
        Long idInsp,
        Short verInsp,
        LocalDateTime fhMovimiento,
        String idUserMovimiento
) {}
