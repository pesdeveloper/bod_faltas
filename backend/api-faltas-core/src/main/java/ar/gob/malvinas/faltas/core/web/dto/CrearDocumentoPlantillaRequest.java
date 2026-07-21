package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.MomentoNumeracionDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CrearDocumentoPlantillaRequest(
        @NotBlank String codigo,
        @NotBlank String nombre,
        @NotNull TipoDocu tipoDocu,
        @NotNull AccionDocumental accionDocumental,
        TipoActa tipoActa,
        @NotNull TipoFirmaReq tipoFirmaReq,
        boolean siRequiereNumeracion,
        @NotNull MomentoNumeracionDocu momentoNumeracionDocu,
        boolean siNotificable,
        boolean siGeneraPdf,
        boolean siSeleccionable,
        @NotNull LocalDate fhVigDesde,
        LocalDate fhVigHasta,
        @NotBlank String idUserAlta
) {}
