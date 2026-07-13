package ar.gob.malvinas.faltas.core.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * CMD-FALLO-004 (variante ordinaria): registrar resultado notificatorio positivo
 * sobre un intento concreto.
 *
 * El actor proviene exclusivamente del JWT sub; el adaptador HTTP lo inyecta.
 * No existe constructor legacy sin intentoId ni fallback de actor.
 */
public record RegistrarNotificacionPositivaCommand(
        @NotNull Long idNotificacion,
        @NotNull Long intentoId,
        String observaciones,
        @NotBlank @Size(max = 36) String actor
) {
}
