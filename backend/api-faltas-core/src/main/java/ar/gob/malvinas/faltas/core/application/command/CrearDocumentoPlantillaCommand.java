package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.MomentoNumeracionDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;

import java.time.LocalDate;

public record CrearDocumentoPlantillaCommand(
        String codigo,
        String nombre,
        String descripcion,
        TipoDocu tipoDocu,
        AccionDocumental accionDocumental,
        TipoActa tipoActa,
        TipoFirmaReq tipoFirmaReq,
        boolean siRequiereNumeracion,
        MomentoNumeracionDocu momentoNumeracionDocu,
        boolean siNotificable,
        boolean siGeneraPdf,
        boolean siSeleccionable,
        LocalDate fhVigDesde,
        LocalDate fhVigHasta,
        String idUserAlta
) {}