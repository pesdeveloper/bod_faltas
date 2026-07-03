package ar.gob.malvinas.faltas.core.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnviarNotificacionRequest(
        @NotNull Long idDocumento,
        @NotBlank String canal,
        String observaciones
) {}