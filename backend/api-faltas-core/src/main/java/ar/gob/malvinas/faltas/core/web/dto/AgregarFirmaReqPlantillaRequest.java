package ar.gob.malvinas.faltas.core.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AgregarFirmaReqPlantillaRequest(
        @Positive short seqFirmaReq,
        @Positive short rolFirmaReq,
        Short mecanismoFirmaReq,
        boolean siObligatoria,
        boolean siActiva,
        @NotBlank String idUserAlta
) {}