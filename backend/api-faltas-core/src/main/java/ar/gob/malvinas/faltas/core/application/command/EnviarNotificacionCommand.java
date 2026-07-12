package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.CanalNotificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnviarNotificacionCommand(
        Long idActa,
        @NotNull Long idDocumento,
        @NotNull CanalNotificacion canal,
        @Size(max = 120) String destinoDigital,
        @Size(max = 80) String referenciaExterna,
        String observaciones,
        @NotBlank @Size(max = 36) String actor
) {}
