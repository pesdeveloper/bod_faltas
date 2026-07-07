package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record LabrarActaCommand(
        @NotNull TipoActa tipoActa,
        Long idDependencia,
        Long idInspector,
        LocalDate fechaActa,
        String domicilioHecho,
        String domicilioInfractor,
        String observaciones,
        Double latInfr,
        String infractorNombre,
        String infractorDocumento,
        Long idPersonaInfractor,
        @NotNull ResultadoFirmaInfractor resultadoFirmaInfractor,
        List<EvidenciaActaItem> evidenciasActa
) {
    /** Backward-compatible constructor for old String-based callers (controller, old tests). */
    public LabrarActaCommand(
            String tipoActa,
            String idDependencia,
            String idInspector,
            LocalDate fechaActa,
            String domicilioHecho,
            String domicilioInfractor,
            String observaciones,
            Double latInfr,
            Double lonInfr,
            String infractorNombre,
            String infractorDocumento,
            ResultadoFirmaInfractor resultadoFirmaInfractor,
            List<EvidenciaActaItem> evidenciasActa) {
        this(TipoActa.valueOf(tipoActa),
                parseLongId(idDependencia),
                parseLongId(idInspector),
                fechaActa, domicilioHecho, domicilioInfractor, observaciones, latInfr,
                infractorNombre, infractorDocumento, null,
                resultadoFirmaInfractor, evidenciasActa);
    }

    private static Long parseLongId(String s) {
        if (s == null) return 1L;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 1L; }
    }
}
