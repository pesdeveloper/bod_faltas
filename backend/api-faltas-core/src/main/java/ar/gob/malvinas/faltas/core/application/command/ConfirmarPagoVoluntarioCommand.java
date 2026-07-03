package ar.gob.malvinas.faltas.core.application.command;

public record ConfirmarPagoVoluntarioCommand(
        Long actaId,
        String observaciones
) {}
