package ar.gob.malvinas.faltas.prototipo.web.mapper;

import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.domain.CanalNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.EstadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.PrototipoReglasOperabilidad;
import static ar.gob.malvinas.faltas.prototipo.domain.PrototipoResultadoFinalHelper.resultadoFinalVigente;
import ar.gob.malvinas.faltas.prototipo.domain.ResultadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.TipoNotificacion;
import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import ar.gob.malvinas.faltas.prototipo.web.dto.AccionesUiResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaDetalleResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaNotificacionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.BromatologiaDatoResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.CerrabilidadResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ComprobanteMockResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.HechosMaterialesActaResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.HechosMaterialesEjeResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.PagoInformadoResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.TransitoDatoResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mapper de {@link ActaMock} a {@link ActaDetalleResponse}.
 *
 * <p>Sin efecto lateral: no ejecuta comandos, no muta estado, no lanza
 * transiciones de dominio.
 *
 * <p>Los tres mappers estáticos ({@link #mapActaNotificacion},
 * {@link #mapCerrabilidad}, {@link #mapPagoInformado}) son accesibles también
 * desde los controllers especializados del prototipo para los endpoints que los
 * necesitan individualmente, evitando duplicación.
 */
@Component
public final class ActaDetalleMapper {

    private final PrototipoStore store;

    public ActaDetalleMapper(PrototipoStore store) {
        this.store = store;
    }

    public ActaDetalleResponse map(ActaMock a) {
        PrototipoStore.PagoInformadoMock pagoInformado = store.getPagoInformado(a.id());
        return new ActaDetalleResponse(
                a.id(),
                a.numeroActa(),
                a.bloqueActual(),
                a.estadoProcesoActual(),
                a.situacionAdministrativaActual(),
                store.getSituacionPago(a.id()).name(),
                store.getTipoPago(a.id()).name(),
                mapPagoInformado(pagoInformado),
                a.estaCerrada(),
                a.permiteReingreso(),
                a.fechaCreacion(),
                a.infractorNombre(),
                a.infractorDocumento(),
                a.inspectorNombre(),
                a.resumenHecho(),
                a.bandejaActual(),
                a.tieneDocumentos(),
                a.tieneNotificaciones(),
                store.listarPiezasRequeridas(a.id()),
                store.listarPiezasGeneradas(a.id()),
                store.getAccionPendiente(a.id()),
                store.getMotivoArchivo(a.id()),
                store.getTipoGestionExterna(a.id()),
                mapCerrabilidad(store.getCerrabilidadActa(a.id())),
                store.getAccionesPagoVoluntarioDisponibles(a.id()),
                store.getSituacionPagoCondena(a.id()).name(),
                store.getAccionesPagoCondenaDisponibles(a.id()),
                store.getAccionesGestionExternaDisponibles(a.id()),
                store.getResultadoExternoPostGestion(a.id()) != null
                        ? store.getResultadoExternoPostGestion(a.id()).name()
                        : null,
                store.getMontoCondenaSugeridoPostGestionExterna(a.id()),
                mapHechosMateriales(store.getHechosMaterialesActa(a.id())),
                store.getDependenciaDemo(a.id()).orElse(null),
                store.getTipoActaAltaDemo(a.id()).orElse(null),
                store.getActaTransitoMock(a.id())
                        .map(
                                t ->
                                        new TransitoDatoResponse(
                                                t.ejeUrbano(),
                                                t.rodadoRetenidoOSecuestrado(),
                                                t.documentacionRetenida(),
                                                t.medidaPreventivaAplicable()))
                        .orElse(null),
                store.getActaBromatologiaMock(a.id())
                        .map(b -> new BromatologiaDatoResponse(b.decomisoSustanciasAlimenticias()))
                        .orElse(null),
                store.getMontoPagoVoluntario(a.id()),
                store.getMontoCondena(a.id()),
                store.listarNotificacionesPorActa(a.id()).stream()
                        .map(ActaDetalleMapper::mapActaNotificacion)
                        .toList(),
                computarAccionesUi(a),
                derivarMotivoParalizacion(store.getAccionPendiente(a.id())),
                store.getObservacionParalizacion(a.id()));
    }

    // -------------------------------------------------------------------------
    // Mappers estáticos compartidos por controllers del prototipo
    // -------------------------------------------------------------------------

    public static ActaNotificacionResponse mapActaNotificacion(ActaNotificacionMock n) {
        if (n == null) {
            return null;
        }
        TipoNotificacion tipo = n.tipo() != null ? n.tipo() : TipoNotificacion.ACTA_INFRACCION;
        CanalNotificacion canal = n.canalTipificado() != null
                ? n.canalTipificado()
                : CanalNotificacion.CORREO_POSTAL;
        EstadoNotificacion estado = n.estado() != null
                ? n.estado()
                : EstadoNotificacion.PENDIENTE_PREPARACION;
        ResultadoNotificacion resultado = n.resultado() != null
                ? n.resultado()
                : ResultadoNotificacion.SIN_RESULTADO;
        return new ActaNotificacionResponse(
                n.id(),
                n.actaId(),
                n.canal(),
                n.estadoNotificacion(),
                n.destinatarioResumen(),
                tipo.name(),
                canal.name(),
                estado.name(),
                resultado.name(),
                n.referencia(),
                n.eventoRelacionado(),
                n.loteId(),
                n.referenciaExterna(),
                n.fechaPreparacion(),
                n.fechaEnvio(),
                n.fechaResultado(),
                n.observacion(),
                n.destinatarioNombre(),
                n.destinatarioEmail(),
                n.domicilioTexto(),
                n.domicilioElectronicoVerificado(),
                n.diasPlazoNotificacionElectronica());
    }

    public static CerrabilidadResponse mapCerrabilidad(PrototipoStore.CerrabilidadActaVista v) {
        if (v == null) {
            return new CerrabilidadResponse("SIN_RESULTADO_FINAL", false, List.of(), "Sin acta.");
        }
        return new CerrabilidadResponse(
                v.resultadoFinal().name(),
                v.cerrable(),
                v.pendientesBloqueantes().stream().map(Enum::name).toList(),
                v.motivoNoCerrable());
    }

    public static PagoInformadoResponse mapPagoInformado(PrototipoStore.PagoInformadoMock p) {
        if (p == null) {
            return null;
        }
        ComprobanteMockResponse comprobante = null;
        if (p.comprobanteId() != null || p.comprobanteNombreArchivo() != null) {
            comprobante = new ComprobanteMockResponse(p.comprobanteId(), p.comprobanteNombreArchivo());
        }
        return new PagoInformadoResponse(p.fechaInformado(), comprobante);
    }

    // -------------------------------------------------------------------------
    // Helpers privados exclusivos del mapper de detalle
    // -------------------------------------------------------------------------

    private AccionesUiResponse computarAccionesUi(ActaMock a) {
        boolean archivoReingreso = a.permiteReingreso() && PrototipoReglasOperabilidad.esBandejaArchivo(a.bandejaActual());
        boolean paralizarActa = store.puedeParalizarActa(a.id());

        String bandeja = a.bandejaActual();
        boolean bandejaOperativaInterna = !PrototipoReglasOperabilidad.esBandejaSinAccionesInternas(bandeja);

        boolean consentirCondenaYRegistrarPago = false;
        boolean pagoVoluntario = false;
        boolean vencimientoPagoVoluntario = false;
        boolean falloFondo = false;
        boolean cumplimientoMaterial = false;
        boolean resolucionBloqueante = false;
        boolean cierre = false;
        boolean enviarANotificacion = false;
        boolean anularActa = false;
        boolean firmaPendiente = false;
        boolean notificacion = false;
        boolean reintentarNotificacion = false;
        boolean pagoCondena = false;
        boolean confirmarPagoCondena = false;
        boolean observarPagoCondena = false;
        boolean gestionExterna = false;
        boolean apelacionPresencial = false;
        boolean vencimientoPlazoApelacion = false;

        if (!a.estaCerrada()
                && bandejaOperativaInterna
                && "ACTIVA".equals(a.situacionAdministrativaActual())) {
            PrototipoStore.ResultadoFinalCierreMock rf = resultadoFinalVigente(store, a.id());
            PrototipoStore.SituacionPagoMock sp = store.getSituacionPago(a.id());

            // consentirCondenaYRegistrarPago
            // Camino A: fallo ya notificado (CONDENADO)
            // Camino B: fallo condenatorio firmado aún sin notificar (infractor se presenta presencialmente)
            boolean candidato = rf == PrototipoStore.ResultadoFinalCierreMock.CONDENADO
                    || (rf == PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL
                            && store.hayFalloCondenatorioPendienteDeNotificacion(a.id()));
            if (candidato && !store.hayApelacionPresentada(a.id())) {
                java.math.BigDecimal monto = store.getMontoCondena(a.id());
                PrototipoStore.SituacionPagoCondena spc = store.getSituacionPagoCondena(a.id());
                consentirCondenaYRegistrarPago =
                        monto != null
                                && monto.signum() > 0
                                && spc == PrototipoStore.SituacionPagoCondena.NO_APLICA
                                && sp == PrototipoStore.SituacionPagoMock.SIN_PAGO;
            }
            apelacionPresencial = rf == PrototipoStore.ResultadoFinalCierreMock.CONDENADO
                    && store.puedePresentarApelacion(a.id());
            vencimientoPlazoApelacion = store.puedePresentarApelacion(a.id());

            boolean sinResultadoFinal = rf == PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL;
            boolean sinFalloDictado = !store.hayFalloDictado(a.id());
            boolean analisisOperativo = "PENDIENTE_ANALISIS".equals(bandeja)
                    && "PENDIENTE_REVISION".equals(a.estadoProcesoActual());
            String accionPendiente = store.getAccionPendiente(a.id());
            boolean falloPostGestionExterna = PrototipoStore.ACCION_DICTAR_FALLO_POST_GESTION_EXTERNA.equals(
                    accionPendiente)
                    && store.hayResultadoExternoPostGestionPendiente(a.id());
            boolean notificacionPendienteDeDecision =
                    PrototipoStore.ACCION_REINTENTAR_NOTIFICACION.equals(accionPendiente)
                            || PrototipoStore.ACCION_EVALUAR_NOTIFICACION_VENCIDA.equals(accionPendiente);
            reintentarNotificacion = analisisOperativo && notificacionPendienteDeDecision;

            // pagoVoluntario: Dirección puede iniciar o gestionar una solicitud activa.
            boolean sinPago = sp == PrototipoStore.SituacionPagoMock.SIN_PAGO;
            boolean solicitudActiva = sp == PrototipoStore.SituacionPagoMock.SOLICITADO;
            pagoVoluntario = !falloPostGestionExterna
                    && sinResultadoFinal
                    && sinFalloDictado
                    && (sinPago || solicitudActiva);

            // vencimientoPagoVoluntario: hay solicitud activa y monto vigente sin pago.
            BigDecimal montoPagoVoluntario = store.getMontoPagoVoluntario(a.id());
            vencimientoPagoVoluntario = sinResultadoFinal
                    && sinFalloDictado
                    && sp == PrototipoStore.SituacionPagoMock.SOLICITADO
                    && montoPagoVoluntario != null
                    && montoPagoVoluntario.signum() > 0;

            // falloFondo: solo en análisis operativo; notificación y firma lo bloquean.
            boolean pagoCompatibleConFallo = sp == PrototipoStore.SituacionPagoMock.SIN_PAGO
                    || sp == PrototipoStore.SituacionPagoMock.VENCIDO;
            falloFondo = analisisOperativo
                    && !notificacionPendienteDeDecision
                    && ((sinResultadoFinal && sinFalloDictado && pagoCompatibleConFallo) || falloPostGestionExterna);

            // cumplimientoMaterial / resolucionBloqueante:
            // Separados según fase del resolutorio (PENDIENTE_FIRMA / FIRMADO / sin doc).
            // resolucionBloqueante: bloqueante sin resolutorio → operador debe generarlo.
            // cumplimientoMaterial: resolutorio FIRMADO → operador puede registrar hecho.
            PrototipoStore.CerrabilidadActaVista cv = store.getCerrabilidadActa(a.id());
            boolean tieneHabilitante = hayHabilitanteMaterialParaUi(a.id(), rf, cv);
            boolean tienePendientesMateriales = cv != null && !cv.pendientesBloqueantes().isEmpty();
            if (tieneHabilitante && tienePendientesMateriales) {
                if (store.hayPendientesSinResolutorio(a.id())) {
                    resolucionBloqueante = true;
                }
                if (store.hayPendientesConResolutorioFirmado(a.id())) {
                    cumplimientoMaterial = true;
                }
            }

            // cierre: puede ejecutarse cerrar-acta ahora
            cierre = store.puedeCerrarDesdeAnalisis(a.id());

            // enviarANotificacion / anularActa: solo en ACTAS_EN_ENRIQUECIMIENTO activa
            if ("ACTAS_EN_ENRIQUECIMIENTO".equals(bandeja)) {
                enviarANotificacion = true;
                anularActa = true;
            }
        }

        firmaPendiente = "PENDIENTE_FIRMA".equals(bandeja);
        notificacion = "PENDIENTE_NOTIFICACION".equals(bandeja) || "EN_NOTIFICACION".equals(bandeja);
        List<String> accionesPagoCondena = store.getAccionesPagoCondenaDisponibles(a.id());
        pagoCondena = accionesPagoCondena.contains("INFORMAR");
        confirmarPagoCondena = accionesPagoCondena.contains("CONFIRMAR");
        observarPagoCondena = accionesPagoCondena.contains("OBSERVAR");
        gestionExterna = !store.getAccionesGestionExternaDisponibles(a.id()).isEmpty();

        return new AccionesUiResponse(
                archivoReingreso,
                paralizarActa,
                consentirCondenaYRegistrarPago,
                pagoVoluntario,
                vencimientoPagoVoluntario,
                falloFondo,
                cumplimientoMaterial,
                resolucionBloqueante,
                cierre,
                enviarANotificacion,
                anularActa,
                firmaPendiente,
                notificacion,
                reintentarNotificacion,
                pagoCondena,
                confirmarPagoCondena,
                observarPagoCondena,
                gestionExterna,
                apelacionPresencial,
                vencimientoPlazoApelacion);
    }

    /**
     * Habilitante material para UI: ABSUELTO, PAGO_CONFIRMADO o CONDENA_FIRME
     * con situacionPagoCondena=CONFIRMADO. Duplica intencionalmente la lógica
     * de {@code CerrabilidadSupport#tieneResultadoHabilitanteMaterial} para
     * evitar acoplamiento controller-support.
     */
    private boolean hayHabilitanteMaterialParaUi(
            String actaId,
            PrototipoStore.ResultadoFinalCierreMock rf,
            PrototipoStore.CerrabilidadActaVista cv) {
        if (rf == PrototipoStore.ResultadoFinalCierreMock.ABSUELTO
                || rf == PrototipoStore.ResultadoFinalCierreMock.PAGO_CONFIRMADO) {
            return true;
        }
        if (rf == PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME) {
            PrototipoStore.SituacionPagoCondena spc = store.getSituacionPagoCondena(actaId);
            return spc == PrototipoStore.SituacionPagoCondena.CONFIRMADO;
        }
        return false;
    }

    private HechosMaterialesActaResponse mapHechosMateriales(PrototipoStore.HechosMaterialesActaVista v) {
        if (v == null || v.ejes() == null) {
            return new HechosMaterialesActaResponse(List.of(), null);
        }
        return new HechosMaterialesActaResponse(
                v.ejes().stream()
                        .map(
                                e -> new HechosMaterialesEjeResponse(
                                        e.clave(),
                                        e.etiqueta(),
                                        e.fase().name(),
                                        e.bloqueaCierre(),
                                        e.descripcion(),
                                        e.ejeBloqueanteCierre()))
                        .toList(),
                v.lecturaOperativa());
    }

    /**
     * Deriva el c&oacute;digo de motivo de paralizaci&oacute;n desde la acci&oacute;n pendiente.
     * Solo aplica cuando la acci&oacute;n pendiente es una de las PARALIZACION_* reconocidas.
     * Retorna {@code null} en cualquier otro caso.
     */
    private String derivarMotivoParalizacion(String accionPendiente) {
        if (accionPendiente == null) {
            return null;
        }
        return switch (accionPendiente) {
            case PrototipoStore.ACCION_PARALIZACION_ESPERA_DOCUMENTAL -> "ESPERA_DOCUMENTAL";
            case PrototipoStore.ACCION_PARALIZACION_ESPERA_INFORME_EXTERNO -> "ESPERA_INFORME_EXTERNO";
            case PrototipoStore.ACCION_PARALIZACION_ESPERA_OTRA_DEPENDENCIA -> "ESPERA_OTRA_DEPENDENCIA";
            case PrototipoStore.ACCION_PARALIZACION_ESPERA_RESOLUCION_RELACIONADA -> "ESPERA_RESOLUCION_RELACIONADA";
            case PrototipoStore.ACCION_PARALIZACION_OTRO -> "OTRO";
            default -> null;
        };
    }

}
