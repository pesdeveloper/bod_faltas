package ar.gob.malvinas.faltas.prototipo.web.dto;

/**
 * Respuesta para la acción mock que registra la presentación de
 * apelación/recurso. Cierra el plazo de apelación y mantiene
 * {@code resultadoFinal} en {@code CONDENADO}; no resuelve ni eleva el
 * recurso.
 */
public record RegistrarApelacionAccionResponse(
        String estado,
        String mensaje,
        String actaId,
        String bandejaActual,
        String estadoProcesoActual,
        String resultadoFinal,
        String canal) {
}
