package ar.gob.malvinas.faltas.core.application.command;

import jakarta.validation.constraints.NotNull;

public record FirmarDocumentoCommand(
        @NotNull Long idDocumento,
        String firmante,
        String tipoFirma,
        String observaciones
) {}
