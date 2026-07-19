package ar.gob.malvinas.faltas.core.application.result;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoRedaccionDocumento;

import java.util.Set;

/**
 * Respuesta de la operacion de creacion de redaccion documental.
 *
 * Slice 8F-1.
 */
public record DocumentoRedaccionResponse(
        Long id,
        Long idDocumento,
        Long plantillaContenidoId,
        EstadoRedaccionDocumento estadoRedaccion,
        String contenidoEditable,
        Set<String> variablesUsadas,
        Set<String> variablesFaltantes,
        Set<String> variablesDesconocidas,
        boolean completo
) {}
