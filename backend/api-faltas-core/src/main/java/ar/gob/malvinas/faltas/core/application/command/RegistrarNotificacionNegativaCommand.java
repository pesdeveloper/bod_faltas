package ar.gob.malvinas.faltas.core.application.command;

import jakarta.validation.constraints.NotNull;

public record RegistrarNotificacionNegativaCommand(
        @NotNull Long idNotificacion,
        String observaciones
) {
    public RegistrarNotificacionNegativaCommand(String idNotificacion, String observaciones) {
        this(idNotificacion != null ? Long.parseLong(idNotificacion) : null, observaciones);
    }
}