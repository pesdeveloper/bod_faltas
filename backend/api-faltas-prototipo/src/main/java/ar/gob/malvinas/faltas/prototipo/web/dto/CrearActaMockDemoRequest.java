package ar.gob.malvinas.faltas.prototipo.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Alta demo: dependencia y banderas mínimas. Valores nulos en booleanos se
 * interpretan como false en el backend.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CrearActaMockDemoRequest(
        String dependencia,
        String tipoActaDemo,
        Boolean ejeUrbano,
        Boolean rodadoRetenidoOSecuestrado,
        Boolean documentacionRetenida,
        Boolean medidaPreventivaClausura,
        Boolean medidaPreventivaParalizacionObra,
        Boolean decomisoSustanciasAlimenticias) {
}
