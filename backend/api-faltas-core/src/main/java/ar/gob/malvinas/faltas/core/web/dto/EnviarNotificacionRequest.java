package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.CanalNotificacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnviarNotificacionRequest(
        @NotNull Long idDocumento,
        @NotNull CanalNotificacion canal,
        @Size(max = 120) String destinoDigital,
        @Size(max = 80) String referenciaExterna,
        String observaciones
) {}
