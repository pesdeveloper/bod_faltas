package ar.gob.malvinas.faltas.core.application.command;

import java.time.LocalDateTime;

/**
 * Comando para devolver una asignacion activa de talonario manual fisico.
 *
 * estadoAsignacion no se recibe: el service lo fija en DEVUELTO.
 */
public record DevolverTalonarioInspectorCommand(
        Long idAsignacion,
        LocalDateTime fhDevolucion,
        String idUserDevolucion) {}
