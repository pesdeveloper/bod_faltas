package ar.gob.malvinas.faltas.prototipo.web.mapper;

import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.domain.PrototipoReglasOperabilidad;
import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaDocumentoPortalResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaInfractorResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.NotificacionPortalPendienteResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mapper de {@link ActaMock} a {@link ActaInfractorResponse} (vista ciudadana).
 *
 * <p>Sin efecto lateral: no ejecuta comandos, no muta estado, no lanza
 * transiciones de dominio.
 *
 * <p>Computa el estado visible ciudadano y los flags de acciones disponibles
 * para el portal del infractor. Las actas PARALIZADAS se exponen como
 * {@code EN_TRAMITE} para no revelar textos internos al ciudadano.
 */
@Component
public final class ActaInfractorMapper {

    private final PrototipoStore store;

    public ActaInfractorMapper(PrototipoStore store) {
        this.store = store;
    }

    public ActaInfractorResponse map(ActaMock a) {
        String codigoQr = store.codigoQrDeActa(a.id());
        PrototipoStore.SituacionPagoMock situacion = store.getSituacionPago(a.id());
        BigDecimal monto = store.getMontoPagoVoluntario(a.id());
        PrototipoStore.CerrabilidadActaVista cv = store.getCerrabilidadActa(a.id());
        PrototipoStore.ResultadoFinalCierreMock resultadoFinalEnum =
                (cv != null && cv.resultadoFinal() != null)
                        ? cv.resultadoFinal()
                        : PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL;
        PrototipoStore.SituacionPagoCondena situacionPagoCondena = store.getSituacionPagoCondena(a.id());

        boolean enBandejaNoOperablePorPortal = estaEnBandejaNoOperablePorPortal(a);
        String estadoVisible = computarEstadoVisible(a);
        boolean enRevision = "EN_REVISION".equals(estadoVisible);

        boolean puedeConsultarEstado = true;
        boolean puedeSolicitarPagoVoluntario = !enRevision
                && monto == null
                && situacion == PrototipoStore.SituacionPagoMock.SIN_PAGO
                && resultadoFinalEnum == PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL
                && !enBandejaNoOperablePorPortal
                && !store.hayFalloCondenatorioDictado(a.id());
        boolean puedePagar = !enRevision
                && monto != null
                && (situacion == PrototipoStore.SituacionPagoMock.SOLICITADO
                        || situacion == PrototipoStore.SituacionPagoMock.OBSERVADO)
                && resultadoFinalEnum == PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL
                && !enBandejaNoOperablePorPortal;
        boolean puedePresentarApelacion = !enRevision && store.puedePresentarApelacion(a.id());
        ActaNotificacionMock notificacionPortalPendiente = store.findNotificacionPortalPendiente(a.id()).orElse(null);
        boolean puedeConfirmarVisualizacionNotificacion = !enRevision && notificacionPortalPendiente != null;
        Boolean domicilioElectronicoVerificado = store.listarNotificacionesPorActa(a.id()).stream()
                .filter(n -> n.canalTipificado().name().equals("DOMICILIO_ELECTRONICO"))
                .map(ActaNotificacionMock::domicilioElectronicoVerificado)
                .filter(Boolean.TRUE::equals)
                .findFirst()
                .orElse(Boolean.FALSE);

        // Nullable fields: null means absent from JSON for EN_REVISION
        String situacionPagoJson = enRevision ? null : situacion.name();
        PrototipoStore.TipoPago tipoPagoEnum = store.getTipoPago(a.id());
        String tipoPagoJson = enRevision ? null : tipoPagoEnum.name();
        String resultadoFinalJson = enRevision ? null : resultadoFinalEnum.name();
        String situacionPagoCondenaJson = enRevision ? null : situacionPagoCondena.name();
        BigDecimal montoPagoVoluntarioJson = enRevision ? null : monto;
        BigDecimal montoCondenaJson = enRevision ? null : store.getMontoCondena(a.id());

        Boolean puedeConsentirCondena = enRevision ? null
                : (resultadoFinalEnum == PrototipoStore.ResultadoFinalCierreMock.CONDENADO
                        && !store.hayApelacionPresentada(a.id()));
        Boolean puedePagarCondena = enRevision ? null
                : (resultadoFinalEnum == PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME
                        && (situacionPagoCondena == PrototipoStore.SituacionPagoCondena.PENDIENTE
                                || situacionPagoCondena == PrototipoStore.SituacionPagoCondena.OBSERVADO));

        List<ActaDocumentoPortalResponse> documentos = store.listarDocumentosVisiblesPortal(a.id()).stream()
                .map(this::mapDocumentoPortal)
                .toList();

        boolean hayDocumentoPendienteNotificacion = documentos.stream()
                .anyMatch(ActaDocumentoPortalResponse::notificable);

        String mensajeVisible = computarMensajeVisibleInfractor(
                estadoVisible, situacion, monto, puedePagar, puedeSolicitarPagoVoluntario,
                resultadoFinalEnum, situacionPagoCondena, hayDocumentoPendienteNotificacion, a);

        return new ActaInfractorResponse(
                a.numeroActa(),
                codigoQr,
                estadoVisible,
                situacionPagoJson,
                tipoPagoJson,
                resultadoFinalJson,
                situacionPagoCondenaJson,
                montoPagoVoluntarioJson,
                montoCondenaJson,
                puedeConsultarEstado,
                puedeSolicitarPagoVoluntario,
                puedePagar,
                puedePresentarApelacion,
                puedeConfirmarVisualizacionNotificacion,
                puedeConsentirCondena,
                puedePagarCondena,
                mapNotificacionPortalPendiente(notificacionPortalPendiente),
                domicilioElectronicoVerificado,
                documentos,
                mensajeVisible);
    }

    private ActaDocumentoPortalResponse mapDocumentoPortal(PrototipoStore.DocumentoPortalVista v) {
        boolean notificado = v.notificado();
        boolean pendienteNotificacion = v.pendienteNotificacion();
        String estadoNotificacion = pendienteNotificacion ? "PENDIENTE_NOTIFICACION"
                : notificado ? "NOTIFICADO" : "SIN_NOTIFICACION";
        return new ActaDocumentoPortalResponse(
                v.tipoDocumento(),
                v.titulo(),
                v.estadoDocumento(),
                estadoNotificacion,
                true,
                pendienteNotificacion,
                notificado,
                true);
    }

    private NotificacionPortalPendienteResponse mapNotificacionPortalPendiente(ActaNotificacionMock n) {
        if (n == null) {
            return null;
        }
        return new NotificacionPortalPendienteResponse(
                n.id(),
                n.tipo().name(),
                n.canalTipificado().name(),
                n.estado().name(),
                n.resultado().name(),
                primerNoVacio(n.destinatarioNombre(), n.destinatarioResumen()),
                n.destinatarioResumen(),
                "Al confirmar, se registrar\u00e1 la visualizaci\u00f3n de esta notificaci\u00f3n en el portal infractor.");
    }

    private static String primerNoVacio(String primero, String segundo) {
        if (primero != null && !primero.isBlank()) {
            return primero;
        }
        return segundo;
    }

    private boolean estaEnBandejaNoOperablePorPortal(ActaMock a) {
        if (a.estaCerrada()) {
            return true;
        }
        // PARALIZADAS se expone como EN_TRAMITE en portal; solo excluye terminales + externa.
        String b = a.bandejaActual();
        return PrototipoReglasOperabilidad.esBandejaTerminal(b)
                || PrototipoReglasOperabilidad.esBandejaGestionExterna(b);
    }

    private String computarEstadoVisible(ActaMock a) {
        if (a.estaCerrada() || PrototipoReglasOperabilidad.esBandejaCerrada(a.bandejaActual())) {
            return "CERRADA";
        }
        if (PrototipoReglasOperabilidad.esBandejaArchivo(a.bandejaActual())) {
            return "ARCHIVADA";
        }
        if (PrototipoReglasOperabilidad.esBandejaGestionExterna(a.bandejaActual())) {
            return "EN_GESTION_EXTERNA";
        }
        if ("ACTAS_EN_ENRIQUECIMIENTO".equals(a.bandejaActual())) {
            return "EN_REVISION";
        }
        return "EN_TRAMITE";
    }

    private String computarMensajeVisibleInfractor(
            String estadoVisible,
            PrototipoStore.SituacionPagoMock situacion,
            BigDecimal monto,
            boolean puedePagar,
            boolean puedeSolicitarPagoVoluntario,
            PrototipoStore.ResultadoFinalCierreMock resultadoFinal,
            PrototipoStore.SituacionPagoCondena situacionPagoCondena,
            boolean hayDocumentoPendienteNotificacion,
            ActaMock acta) {
        if ("CERRADA".equals(estadoVisible)) {
            return "El acta se encuentra cerrada; no requiere gestiones desde el portal.";
        }
        if ("ARCHIVADA".equals(estadoVisible)) {
            if (resultadoFinal == PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME) {
                if (situacionPagoCondena == PrototipoStore.SituacionPagoCondena.INFORMADO) {
                    return "La actuaci\u00f3n se encuentra archivada administrativamente."
                            + " El pago de condena se encuentra en proceso de verificaci\u00f3n"
                            + " por Direcci\u00f3n de Faltas.";
                }
                return "La actuaci\u00f3n se encuentra archivada administrativamente, pero registra una"
                        + " condena firme pendiente de pago. Puede regularizar el pago desde este portal.";
            }
            return "El acta se encuentra archivada; no admite gestiones desde el portal.";
        }
        if ("EN_GESTION_EXTERNA".equals(estadoVisible)) {
            return "El acta se encuentra en gesti\u00f3n externa; no admite gestiones desde el portal.";
        }
        if ("EN_REVISION".equals(estadoVisible)) {
            return "El acta se encuentra en revisi\u00f3n. "
                    + "Ser\u00e1 notificado cuando la documentaci\u00f3n est\u00e9 validada y disponible.";
        }
        // Condena firme — mensajes según situación de pago de condena
        if (resultadoFinal == PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME) {
            if (situacionPagoCondena == PrototipoStore.SituacionPagoCondena.CONFIRMADO) {
                return "Pago de condena confirmado.";
            }
            if (situacionPagoCondena == PrototipoStore.SituacionPagoCondena.OBSERVADO) {
                return "El pago de condena fue observado. Puede reintentar el pago.";
            }
            if (situacionPagoCondena == PrototipoStore.SituacionPagoCondena.INFORMADO) {
                return "Pago de condena en proceso de acreditaci\u00f3n. "
                        + "Direcci\u00f3n de Faltas verificar\u00e1 la acreditaci\u00f3n.";
            }
            if (situacionPagoCondena == PrototipoStore.SituacionPagoCondena.PENDIENTE) {
                return "La condena se encuentra firme y registra un monto pendiente de pago.";
            }
        }
        // Condenado (fallo notificado, condena aún no firme)
        if (resultadoFinal == PrototipoStore.ResultadoFinalCierreMock.CONDENADO) {
            return "El fallo fue notificado. Puede presentar apelaci\u00f3n dentro del plazo correspondiente"
                    + " o consentir la condena para avanzar al pago.";
        }
        // Documento con notificación pendiente
        if (hayDocumentoPendienteNotificacion) {
            return "El expediente registra documentaci\u00f3n pendiente de notificaci\u00f3n. "
                    + "Abra el documento para quedar notificado.";
        }
        // Fallo condenatorio dictado pero aún pendiente de firma
        if (store.hayFalloCondenatorioDictado(acta.id())
                && "PENDIENTE_FIRMA".equals(acta.bandejaActual())) {
            return "El expediente registra una resoluci\u00f3n en proceso de formalizaci\u00f3n. "
                    + "No puede realizar acciones hasta que sea firmada y notificada.";
        }
        // Pago voluntario
        if (situacion == PrototipoStore.SituacionPagoMock.CONFIRMADO) {
            return "El pago voluntario ya fue confirmado.";
        }
        if (situacion == PrototipoStore.SituacionPagoMock.PENDIENTE_CONFIRMACION) {
            return "Pago en proceso de acreditaci\u00f3n. Direcci\u00f3n de Faltas verificar\u00e1 la acreditaci\u00f3n.";
        }
        if (situacion == PrototipoStore.SituacionPagoMock.OBSERVADO && monto != null) {
            return "El pago fue observado. Puede intentar nuevamente.";
        }
        if (situacion == PrototipoStore.SituacionPagoMock.SOLICITADO && monto == null) {
            return "Solicitud de pago voluntario registrada. Direcci\u00f3n de Faltas evaluar\u00e1 el expediente.";
        }
        if (puedePagar) {
            return "Hay un monto disponible para pago voluntario.";
        }
        if (puedeSolicitarPagoVoluntario) {
            return "El acta se encuentra en tr\u00e1mite; puede solicitar pago voluntario.";
        }
        return "El acta se encuentra en tr\u00e1mite.";
    }
}
