package ar.gob.malvinas.faltas.core.web.dto;

public record DesactivarHabilitacionFirmanteRequest(
        Short tipoDocu,
        Short rolFirmaReq,
        String idUserAlta
) {}