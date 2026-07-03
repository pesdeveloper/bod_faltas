package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import jakarta.validation.constraints.NotNull;

public record GenerarDocumentoRequest(
        @NotNull TipoDocu tipoDocumento,
        String descripcion
) {}
