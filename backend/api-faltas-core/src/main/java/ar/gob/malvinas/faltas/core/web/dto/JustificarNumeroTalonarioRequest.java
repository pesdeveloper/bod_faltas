package ar.gob.malvinas.faltas.core.web.dto;

import java.time.LocalDateTime;

public record JustificarNumeroTalonarioRequest(
        String observacion,
        Long idDep,
        Short verDep,
        Long idInsp,
        Short verInsp,
        LocalDateTime fhMovimiento,
        String idUserMovimiento
) {}
