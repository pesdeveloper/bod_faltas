package ar.gob.malvinas.faltas.prototipo.web;

public record PrototipoHealthResponse(
        String status,
        String modulo,
        String modo,
        int cantidadActas
) {}
