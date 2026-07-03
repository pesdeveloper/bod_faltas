package ar.gob.malvinas.faltas.core.web.dto;

public record RegistrarApelacionRequest(
        String presentante,
        String fundamentos,
        String observaciones
) {}