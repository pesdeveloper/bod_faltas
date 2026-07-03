package ar.gob.malvinas.faltas.core.web.dto;

import jakarta.validation.constraints.NotBlank;

public record FirmarDocumentoRequest(
        @NotBlank String firmante,
        String tipoFirma,
        String observaciones
) {}
