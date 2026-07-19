package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;

import java.util.Collections;
import java.util.Map;

/**
 * Comando para crear una redaccion documental en estado BORRADOR.
 *
 * variablesContexto: contexto para la combinacion del template.
 *   - En slice 8F-1 el llamador provee el contexto directamente.
 *   - Slice 8F-2 agrega DocumentoVariableContextBuilder para construirlo desde acta.
 *
 * Slice 8F-1.
 */
public record CrearRedaccionDocumentoCommand(
        Long idDocumento,
        Long idActa,
        AccionDocumental accionDocumental,
        TipoActa tipoActa,
        Long idDependencia,
        Short verDependencia,
        String idUserOperacion,
        Map<String, Object> variablesContexto
) {
    public CrearRedaccionDocumentoCommand {
        if (variablesContexto == null) variablesContexto = Collections.emptyMap();
    }
}
