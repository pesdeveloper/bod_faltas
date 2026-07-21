package ar.gob.malvinas.faltas.core.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request del endpoint POST /api/faltas/notificaciones/{id}/positiva.
 *
 * No incluye actor, idUser, sub ni idNotificacion: el actor proviene
 * exclusivamente del JWT sub y la notificacion del path.
 */
public record RegistrarNotificacionPositivaRequest(
        @NotNull Long intentoId,
        String observaciones
) {}
