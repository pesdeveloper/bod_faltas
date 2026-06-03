package ar.gob.malvinas.faltas.prototipo.web.dto;

/**
 * Detalle de un campo que produjo coincidencia en una búsqueda global de actas.
 *
 * @param tipo      tipo semántico del campo, p. ej. {@code NUMERO_ACTA}, {@code DOCUMENTO_INFRACTOR}.
 * @param label     etiqueta visible para la UI, p. ej. {@code "Número de acta"}, {@code "DOC"}.
 * @param valor     valor completo del campo que produjo la coincidencia.
 * @param fragmento porción del valor que coincidió con la query; puede ser {@code null}.
 * @param score     puntaje parcial de esta coincidencia; puede ser {@code null}.
 */
public record PrototipoActaBusquedaMatchResponse(
        String tipo,
        String label,
        String valor,
        String fragmento,
        Integer score) {
}
