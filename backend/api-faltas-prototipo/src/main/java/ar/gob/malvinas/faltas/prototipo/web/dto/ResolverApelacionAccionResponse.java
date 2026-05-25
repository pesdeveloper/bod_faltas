package ar.gob.malvinas.faltas.prototipo.web.dto;

/**
 * Respuesta para la acción mock que resuelve una apelación/recurso ya
 * presentada. Ajusta {@code resultadoFinal} según el resultado de la
 * resolución; no cierra el acta automáticamente.
 */
public record ResolverApelacionAccionResponse(
        String estado,
        String mensaje,
        String actaId,
        String bandejaActual,
        String estadoProcesoActual,
        String resultadoFinal,
        String resultadoResolucion) {
}
