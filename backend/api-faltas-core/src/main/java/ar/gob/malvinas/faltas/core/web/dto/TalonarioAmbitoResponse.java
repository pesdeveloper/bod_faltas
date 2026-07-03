package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioAmbito;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TalonarioAmbitoResponse(
        Long id,
        Long talonarioId,
        ClaseNumeracion claseTalonario,
        Short tipoDocu,
        Short tipoActa,
        Long idDep,
        Short verDep,
        AlcanceTalonario alcance,
        short prioridad,
        LocalDate fhDesde,
        LocalDate fhHasta,
        boolean siActivo,
        LocalDateTime fhAlta,
        String idUserAlta
) {
    public static TalonarioAmbitoResponse de(NumTalonarioAmbito a) {
        return new TalonarioAmbitoResponse(
                a.getId(), a.getTalonarioId(), a.getClaseTalonario(),
                a.getTipoDocu(), a.getTipoActa(),
                a.getIdDep(), a.getVerDep(), a.getAlcance(), a.getPrioridad(),
                a.getFhDesde(), a.getFhHasta(), a.isSiActivo(),
                a.getFhAlta(), a.getIdUserAlta());
    }
}
