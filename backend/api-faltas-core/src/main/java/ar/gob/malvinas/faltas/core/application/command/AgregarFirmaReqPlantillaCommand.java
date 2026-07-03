package ar.gob.malvinas.faltas.core.application.command;

public record AgregarFirmaReqPlantillaCommand(
        Long plantillaId,
        short seqFirmaReq,
        short rolFirmaReq,
        Short mecanismoFirmaReq,
        boolean siObligatoria,
        boolean siActiva,
        String idUserAlta
) {}