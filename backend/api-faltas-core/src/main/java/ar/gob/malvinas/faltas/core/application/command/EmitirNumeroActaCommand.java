package ar.gob.malvinas.faltas.core.application.command;

import java.time.LocalDateTime;

public record EmitirNumeroActaCommand(
        Long idDep,
        Short verDep,
        Short tipoActa,
        Long idInsp,
        Short verInsp,
        Long actaId,
        LocalDateTime fhMovimiento,
        String idUserMovimiento
) {}

