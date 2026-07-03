package ar.gob.malvinas.faltas.core.web.dto;

import java.time.LocalDateTime;

/**
 * Request para asignar un talonario manual fisico a un inspector.
 * idTalonario proviene del path variable, no del body.
 */
public record AsignarTalonarioInspectorRequest(
        Long idInsp,
        Short verInsp,
        LocalDateTime fhEntrega,
        String idUserEntrega) {}
