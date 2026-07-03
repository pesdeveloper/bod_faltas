package ar.gob.malvinas.faltas.core.web.dto;

import java.time.LocalDateTime;

/**
 * Request para devolver una asignacion de talonario manual fisico.
 * idAsignacion proviene del path variable, no del body.
 */
public record DevolverTalonarioInspectorRequest(
        LocalDateTime fhDevolucion,
        String idUserDevolucion) {}
