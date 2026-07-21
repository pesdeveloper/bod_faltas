package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import jakarta.validation.constraints.NotNull;

public record GenerarDocumentoCommand(
        @NotNull Long idActa,
        @NotNull TipoDocu tipoDocumento
) {}
