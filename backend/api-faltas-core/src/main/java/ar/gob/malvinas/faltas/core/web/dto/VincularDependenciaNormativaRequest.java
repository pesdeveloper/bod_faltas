package ar.gob.malvinas.faltas.core.web.dto;
public record VincularDependenciaNormativaRequest(
    Long normativaId,
    String idUserAlta
) {}
