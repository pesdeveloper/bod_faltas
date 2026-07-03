package ar.gob.malvinas.faltas.core.application.command;

import jakarta.validation.constraints.NotNull;

public record EnriquecerActaCommand(
        @NotNull Long idActa,
        String observaciones
) {}

