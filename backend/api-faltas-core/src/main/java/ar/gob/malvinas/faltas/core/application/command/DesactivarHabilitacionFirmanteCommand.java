package ar.gob.malvinas.faltas.core.application.command;

public record DesactivarHabilitacionFirmanteCommand(
        Long idFirmante,
        int verFirmante,
        Short tipoDocu,
        Short rolFirmaReq,
        String idUserAlta
) {}
