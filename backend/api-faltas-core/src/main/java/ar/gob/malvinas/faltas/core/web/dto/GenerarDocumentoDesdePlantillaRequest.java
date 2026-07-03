package ar.gob.malvinas.faltas.core.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GenerarDocumentoDesdePlantillaRequest(
        @NotNull Long idActa,
        @NotNull Long plantillaId,
        @NotBlank String idUserAlta) {}
