package ar.gob.malvinas.faltas.core.web.dto;

/**
 * Bloque de estado del endpoint de reset dev/test en el health demo.
 * Slice 8F-8.
 */
public record DemoHealthResetDto(
        String endpoint,
        boolean enabled,
        boolean defaultSeguro
) {}