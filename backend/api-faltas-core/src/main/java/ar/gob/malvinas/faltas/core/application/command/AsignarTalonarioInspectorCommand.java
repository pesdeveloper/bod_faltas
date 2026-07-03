package ar.gob.malvinas.faltas.core.application.command;

import java.time.LocalDateTime;

/**
 * Comando para asignar un talonario manual fisico a un inspector.
 *
 * estadoAsignacion no se recibe: el service lo fija en ENTREGADO.
 * siActiva no se recibe: el service lo fija en true.
 */
public record AsignarTalonarioInspectorCommand(
        Long idTalonario,
        Long idInsp,
        short verInsp,
        LocalDateTime fhEntrega,
        String idUserEntrega) {}
