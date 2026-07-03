package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonario;

import java.time.LocalDateTime;

public record TalonarioResponse(
        Long id,
        int versionRow,
        Long politicaId,
        String codigo,
        String descripcion,
        TipoTalonario tipoTalonario,
        ClaseNumeracion claseTalonario,
        Short anio,
        String serie,
        int nroDesde,
        Integer nroHasta,
        String nombreSecuencia,
        boolean siActivo,
        boolean siBloqueado,
        String codDesbloqueo,
        String obsTalonario,
        LocalDateTime fhAlta,
        String idUserAlta
) {
    public static TalonarioResponse de(NumTalonario t) {
        return new TalonarioResponse(
                t.getId(), t.getVersionRow(), t.getPoliticaId(),
                t.getCodigo(), t.getDescripcion(), t.getTipoTalonario(), t.getClaseTalonario(),
                t.getAnio(), t.getSerie(), t.getNroDesde(), t.getNroHasta(),
                t.getNombreSecuencia(), t.isSiActivo(), t.isSiBloqueado(),
                t.getCodDesbloqueo(), t.getObsTalonario(), t.getFhAlta(), t.getIdUserAlta());
    }
}
