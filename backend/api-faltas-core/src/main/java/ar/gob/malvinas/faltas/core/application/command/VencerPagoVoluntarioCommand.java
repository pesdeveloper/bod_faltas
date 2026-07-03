package ar.gob.malvinas.faltas.core.application.command;

public record VencerPagoVoluntarioCommand(
        Long actaId,
        String observaciones
) {}
