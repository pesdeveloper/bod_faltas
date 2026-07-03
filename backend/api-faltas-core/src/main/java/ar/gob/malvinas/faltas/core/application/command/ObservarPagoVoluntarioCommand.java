package ar.gob.malvinas.faltas.core.application.command;

public record ObservarPagoVoluntarioCommand(
        Long actaId,
        String motivoObservacion,
        String observaciones
) {}
