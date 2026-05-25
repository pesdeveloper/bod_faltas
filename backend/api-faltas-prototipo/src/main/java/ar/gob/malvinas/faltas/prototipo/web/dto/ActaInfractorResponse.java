package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.math.BigDecimal;

/**
 * Vista ciudadana mínima del acta para el futuro portal del infractor.
 *
 * <p>El portal consulta acá usando el código ciudadano/QR (no el
 * {@code actaId} interno). El DTO no expone detalles administrativos
 * innecesarios y NO materializa comprobantes (sin EM, sin RC, sin
 * Cmte/Pref/Nro, sin emisión de deuda ni recibo): esos artefactos sólo se
 * modelarán al implementar el pago real en un slice posterior.
 *
 * <p>El campo {@code acta} expone el número visible del acta (p. ej.
 * {@code A-2026-0003}) y no el identificador interno. {@code estadoVisible}
 * es una etiqueta coarse-grained para el infractor ({@code EN_TRAMITE},
 * {@code CERRADA}, {@code ARCHIVADA}, {@code EN_GESTION_EXTERNA}). Los
 * flags de acción reflejan, sin ambigüedad, qué puede ofrecer el portal en
 * cada estado.
 */
public record ActaInfractorResponse(
        String acta,
        String codigoQr,
        String estadoVisible,
        String situacionPago,
        String resultadoFinal,
        BigDecimal montoPagoVoluntario,
        BigDecimal montoCondena,
        boolean puedeConsultarEstado,
        boolean puedeSolicitarPagoVoluntario,
        boolean puedePagar,
        boolean puedePresentarApelacion,
        String mensajeVisible) {
}
