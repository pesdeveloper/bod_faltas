package ar.gob.malvinas.faltas.core.application.command;

import java.time.LocalDateTime;

/**
 * Cierre/rendicion de una asignacion de talonario manual fisico a inspector.
 * Valida que todos los numeros del rango tengan movimiento antes de cerrar.
 * Slice 8B-6.
 */
public record CerrarAsignacionTalonarioInspectorCommand(
        Long idAsignacion,
        LocalDateTime fhCierre,
        String idUserCierre,
        String observacion
) {}
