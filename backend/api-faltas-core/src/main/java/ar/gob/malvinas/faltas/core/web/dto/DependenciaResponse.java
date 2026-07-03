package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalDependencia;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaVersion;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DependenciaResponse(
        Long idDep,
        String codDep,
        String nomDep,
        Long idDepPadre,
        boolean siActiva,
        LocalDateTime fhAlta,
        String idUserAlta,
        Integer verDepVigente,
        TipoActa tipoActaVigente,
        LocalDate fhVigDesde
) {
    public static DependenciaResponse de(FalDependencia dep, FalDependenciaVersion versionVigente) {
        return new DependenciaResponse(
                dep.getIdDep(),
                dep.getCodDep(),
                dep.getNomDep(),
                dep.getIdDepPadre(),
                dep.isSiActiva(),
                dep.getFhAlta(),
                dep.getIdUserAlta(),
                versionVigente != null ? versionVigente.getVerDep() : null,
                versionVigente != null ? versionVigente.getTipoActa() : null,
                versionVigente != null ? versionVigente.getFhVigDesde() : null
        );
    }
}
