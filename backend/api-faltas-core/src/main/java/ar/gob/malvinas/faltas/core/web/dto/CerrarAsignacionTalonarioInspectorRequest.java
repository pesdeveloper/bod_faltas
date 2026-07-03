package ar.gob.malvinas.faltas.core.web.dto;

import java.time.LocalDateTime;

public record CerrarAsignacionTalonarioInspectorRequest(
        LocalDateTime fhCierre,
        String idUserCierre,
        String observacion
) {}
