package ar.gob.malvinas.faltas.core.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnviarNotificacionCommand(
        Long idActa,
        @NotNull Long idDocumento,
        @NotBlank String canal,
        String observaciones
) {}