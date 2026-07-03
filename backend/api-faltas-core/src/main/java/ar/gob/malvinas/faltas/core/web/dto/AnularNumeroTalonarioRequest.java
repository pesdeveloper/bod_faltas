package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.MotivoAnulacionTalonario;

import java.time.LocalDateTime;

public record AnularNumeroTalonarioRequest(
        MotivoAnulacionTalonario motivoAnulacion,
        String observacion,
        Long idDep,
        Short verDep,
        Long idInsp,
        Short verInsp,
        LocalDateTime fhMovimiento,
        String idUserMovimiento
) {}
