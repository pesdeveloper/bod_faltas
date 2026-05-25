package ar.gob.malvinas.faltas.prototipo.web.dto;

/**
 * Respuesta para la acción mock que materializa el vencimiento del plazo
 * de apelación (sin cálculo real de días). Si vence sin apelación
 * presentada, el {@code resultadoFinal} pasa a {@code CONDENA_FIRME} y el
 * portal/infractor deja de habilitar la presentación de apelación.
 */
public record RegistrarVencimientoPlazoApelacionAccionResponse(
        String estado,
        String mensaje,
        String actaId,
        String bandejaActual,
        String estadoProcesoActual,
        String resultadoFinal) {
}
