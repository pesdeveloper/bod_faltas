package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request para incorporar un documento escaneado/adjunto externo.
 * Slice 8C-6D-1.
 */
public record IncorporarDocumentoEscaneadoRequest(
        @NotNull TipoDocu tipoDocu,
        @NotBlank String storageKey,
        @NotBlank String hashDocu,
        @NotBlank String idUserAlta,
        Long plantillaId
) {}