package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaVersion;

import java.time.LocalDate;

public record DependenciaVersionResponse(
        Long idDep,
        int verDep,
        String nomDep,
        Long idDepPadre,
        Integer verDepPadre,
        TipoActa tipoActa,
        LocalDate fhVigDesde,
        LocalDate fhVigHasta,
        boolean siActiva
) {
    public static DependenciaVersionResponse de(FalDependenciaVersion v) {
        return new DependenciaVersionResponse(
                v.getIdDep(),
                v.getVerDep(),
                v.getNomDep(),
                v.getIdDepPadre(),
                v.getVerDepPadre(),
                v.getTipoActa(),
                v.getFhVigDesde(),
                v.getFhVigHasta(),
                v.isSiActiva()
        );
    }
}
