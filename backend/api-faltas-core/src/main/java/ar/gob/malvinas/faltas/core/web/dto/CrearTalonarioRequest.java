package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario;

public record CrearTalonarioRequest(
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
        String idUserAlta
) {}
