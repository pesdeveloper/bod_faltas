package ar.gob.malvinas.faltas.core.web.dto;

import java.util.List;

/**
 * Bloque "demo" con metadatos de la respuesta de detalle de acta demo.
 *
 * Slice 8F-7.
 */
public record DemoActaDetalleMetaDto(
        boolean mock,
        boolean materializada,
        String source,
        List<String> warnings
) {}
