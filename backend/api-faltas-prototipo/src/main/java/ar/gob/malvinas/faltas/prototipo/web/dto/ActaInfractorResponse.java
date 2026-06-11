package ar.gob.malvinas.faltas.prototipo.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vista ciudadana mínima del acta para el portal del infractor.
 *
 * <p>El portal consulta acá usando el código ciudadano/QR (no el
 * {@code actaId} interno). El DTO no expone detalles administrativos
 * innecesarios y NO materializa comprobantes.
 *
 * <p>{@code estadoVisible} es una etiqueta coarse-grained para el infractor
 * ({@code EN_TRAMITE}, {@code EN_REVISION}, {@code CERRADA}, {@code ARCHIVADA},
 * {@code EN_GESTION_EXTERNA}). Los flags de acción reflejan qué puede ofrecer
 * el portal en cada estado. Los campos nullable se omiten del JSON cuando son
 * null (p. ej. en estado {@code EN_REVISION}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ActaInfractorResponse(
        String acta,
        String codigoQr,
        String estadoVisible,
        String situacionPago,
        String tipoPago,
        String resultadoFinal,
        String situacionPagoCondena,
        BigDecimal montoPagoVoluntario,
        BigDecimal montoCondena,
        boolean puedeConsultarEstado,
        boolean puedeSolicitarPagoVoluntario,
        boolean puedePagar,
        boolean puedePresentarApelacion,
        boolean puedeConfirmarVisualizacionNotificacion,
        Boolean puedeConsentirCondena,
        Boolean puedePagarCondena,
        NotificacionPortalPendienteResponse notificacionPortalPendiente,
        Boolean domicilioElectronicoVerificado,
        List<ActaDocumentoPortalResponse> documentos,
        String mensajeVisible) {
}
