package ar.gob.malvinas.faltas.core.application.command;

public record ConfirmarPagoCondenaCommand(
        Long actaId,
        String observaciones
) {}

