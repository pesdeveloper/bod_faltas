package ar.gob.malvinas.faltas.core.application.command;

import jakarta.validation.constraints.NotBlank;

public record RegistrarNotificacionVencidaCommand(
        @NotBlank String idNotificacion,
        String observaciones
) {}
