package ar.gob.malvinas.faltas.core.application.command;

public record AgregarHabilitacionFirmanteCommand(
        Long idFirmante,
        int verFirmante,
        Short tipoDocu,
        Short rolFirmaReq,
        Short mecanismoFirmaReq,
        String idUserAlta
) {}