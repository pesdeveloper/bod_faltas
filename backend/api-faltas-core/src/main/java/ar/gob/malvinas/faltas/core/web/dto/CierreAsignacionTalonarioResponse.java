package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoAsignacionTalonario;

import java.util.List;

public record CierreAsignacionTalonarioResponse(
        Long idAsignacion,
        Long idTalonario,
        EstadoAsignacionTalonario estadoAsignacion,
        boolean siActiva,
        List<Integer> numerosFaltantes,
        boolean cerrada,
        String observacion
) {}
