package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.DocumentoVariableNamespace;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDatoVariableDocumento;

/**
 * Definicion de una variable documental disponible para combinacion.
 *
 * Convencion: {{namespace.nombreCampo}} - lowerCamelCase en namespace y campo.
 * No se persiste: vive como catalogo in-memory en DocumentoVariableRegistry.
 *
 * Slice 8F-1.
 */
public record DocumentoVariableDefinicion(
        String nombre,
        String descripcion,
        DocumentoVariableNamespace namespace,
        TipoDatoVariableDocumento tipoDato,
        boolean requerida,
        String ejemplo,
        String pathOrigen
) {
    private static final java.util.regex.Pattern NOMBRE_VALIDO =
            java.util.regex.Pattern.compile("[a-z][a-zA-Z0-9]*(?:\\.[a-z][a-zA-Z0-9]*)+");

    public DocumentoVariableDefinicion {
        if (nombre == null || nombre.isBlank())
            throw new IllegalArgumentException("nombre de variable requerido");
        if (namespace == null)
            throw new IllegalArgumentException("namespace requerido");
        if (tipoDato == null)
            throw new IllegalArgumentException("tipoDato requerido");
        if (!NOMBRE_VALIDO.matcher(nombre).matches())
            throw new IllegalArgumentException(
                    "nombre de variable no valido: '" + nombre + "'");
    }
}
