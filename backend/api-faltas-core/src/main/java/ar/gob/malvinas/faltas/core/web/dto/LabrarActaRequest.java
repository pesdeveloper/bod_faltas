package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.application.command.EvidenciaActaItem;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record LabrarActaRequest(
        @NotBlank String tipoActa,
        @NotBlank String idDependencia,
        @NotBlank String idInspector,
        LocalDate fechaActa,
        String domicilioHecho,
        String domicilioInfractor,
        String observaciones,
        Double latInfr,
        Double lonInfr,
        String infractorNombre,
        @NotBlank String infractorDocumento,
        @NotNull ResultadoFirmaInfractor resultadoFirmaInfractor,
        List<EvidenciaActaItem> evidenciasActa
) {}
