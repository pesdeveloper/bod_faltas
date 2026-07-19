package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.MomentoNumeracionDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantilla;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DocumentoPlantillaResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        TipoDocu tipoDocu,
        short tipoDocuCodigo,
        AccionDocumental accionDocumental,
        short accionDocumentalCodigo,
        TipoActa tipoActa,
        TipoFirmaReq tipoFirmaReq,
        short tipoFirmaReqCodigo,
        boolean siRequiereNumeracion,
        MomentoNumeracionDocu momentoNumeracionDocu,
        short momentoNumeracionDocuCodigo,
        boolean siNotificable,
        boolean siGeneraPdf,
        boolean siSeleccionable,
        boolean siActiva,
        LocalDate fhVigDesde,
        LocalDate fhVigHasta,
        LocalDateTime fhAlta,
        String idUserAlta
) {
    public static DocumentoPlantillaResponse from(FalDocumentoPlantilla p) {
        return new DocumentoPlantillaResponse(
                p.getId(),
                p.getCodigo(),
                p.getNombre(),
                p.getDescripcion(),
                p.getTipoDocu(),
                p.getTipoDocu().codigo(),
                p.getAccionDocumental(),
                p.getAccionDocumental().codigo(),
                p.getTipoActa(),
                p.getTipoFirmaReq(),
                p.getTipoFirmaReq().codigo(),
                p.isSiRequiereNumeracion(),
                p.getMomentoNumeracionDocu(),
                p.getMomentoNumeracionDocu().codigo(),
                p.isSiNotificable(),
                p.isSiGeneraPdf(),
                p.isSiSeleccionable(),
                p.isSiActiva(),
                p.getFhVigDesde(),
                p.getFhVigHasta(),
                p.getFhAlta(),
                p.getIdUserAlta()
        );
    }
}
