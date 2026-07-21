package ar.gob.malvinas.faltas.core.web.dto;

public record AgregarHabilitacionFirmanteRequest(
        Short tipoDocu,
        Short rolFirmaReq,
        Short mecanismoFirmaReq,
        String idUserAlta
) {}
