package ar.gob.malvinas.faltas.core.web.dto;

import java.time.LocalDateTime;

public record EmitirNumeroActaRequest(
        Long idDep,
        Short verDep,
        Short tipoActa,
        Long idInsp,
        Short verInsp,
        Long actaId,
        LocalDateTime fhMovimiento,
        String idUserMovimiento
) {}

