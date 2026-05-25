package ar.gob.malvinas.faltas.prototipo.web.dto;

/**
 * Respuesta para acciones de dictado de fallo en análisis jurídico
 * ({@code dictar-fallo-absolutorio} y {@code dictar-fallo-condenatorio}).
 *
 * <p>El acta queda con bandeja {@code PENDIENTE_FIRMA} y el documento mock
 * del fallo en estado {@code PENDIENTE_FIRMA}; la firma se completa usando
 * el endpoint existente {@code firmar-documento/{documentoId}}. El
 * {@code resultadoFinal} todavía no cambia: se materializa cuando se
 * registra positivamente la notificación del fallo.
 */
public record DictarFalloAccionResponse(
        String estado,
        String mensaje,
        String actaId,
        String documentoId,
        String tipoDocumento,
        String bandejaActual,
        String estadoProcesoActual,
        java.math.BigDecimal montoCondena) {
}
