package ar.gob.malvinas.faltas.core.web.dto;

/**
 * Bloque "links" de navegacion HATEOAS-lite para la respuesta de detalle de acta demo.
 *
 * Slice 8F-7.
 */
public record DemoActaDetalleLinksDto(
        String self,
        String dataset,
        String graph
) {}
