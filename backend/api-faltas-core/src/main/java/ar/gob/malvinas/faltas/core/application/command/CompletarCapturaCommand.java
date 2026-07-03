package ar.gob.malvinas.faltas.core.application.command;

import jakarta.validation.constraints.NotNull;

public record CompletarCapturaCommand(
        @NotNull Long idActa,
        String observaciones
) {}

