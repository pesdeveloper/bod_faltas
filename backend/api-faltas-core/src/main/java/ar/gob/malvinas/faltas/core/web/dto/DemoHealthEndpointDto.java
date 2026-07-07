package ar.gob.malvinas.faltas.core.web.dto;

/**
 * Entrada de un endpoint demo conocido en el health response.
 * Slice 8F-8.
 */
public record DemoHealthEndpointDto(
        String method,
        String path,
        boolean ready,
        String descripcion
) {}