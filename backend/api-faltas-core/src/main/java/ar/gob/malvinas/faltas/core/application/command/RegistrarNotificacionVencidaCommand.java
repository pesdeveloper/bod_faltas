package ar.gob.malvinas.faltas.core.application.command;

import jakarta.validation.constraints.NotNull;

public record RegistrarNotificacionVencidaCommand(
        @NotNull Long idNotificacion,
        String observaciones
) {
    public RegistrarNotificacionVencidaCommand(String idNotificacion, String observaciones) {
        this(idNotificacion != null ? Long.parseLong(idNotificacion) : null, observaciones);
    }
}
