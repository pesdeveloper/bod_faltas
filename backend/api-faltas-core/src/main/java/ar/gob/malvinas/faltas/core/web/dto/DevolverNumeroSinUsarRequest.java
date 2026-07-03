package ar.gob.malvinas.faltas.core.web.dto;

import java.time.LocalDateTime;

public record DevolverNumeroSinUsarRequest(
        String observacion,
        Long idInsp,
        Short verInsp,
        LocalDateTime fhMovimiento,
        String idUserMovimiento
) {}
