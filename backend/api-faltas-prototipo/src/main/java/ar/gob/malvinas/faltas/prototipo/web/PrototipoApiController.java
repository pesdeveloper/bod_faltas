package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.domain.CanalNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.EstadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.ResultadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.TipoNotificacion;
import ar.gob.malvinas.faltas.prototipo.bandeja.SubBandejaCodigo;
import ar.gob.malvinas.faltas.prototipo.store.MockDataFactory;
import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import ar.gob.malvinas.faltas.prototipo.web.dto.AccionesUiResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.AnularActaPorNulidadAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.EnviarANotificacionAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaBandejaItemResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaDetalleResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaDocumentoPortalResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaDocumentoResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaEventoResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaInfractorResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaNotificacionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.AdjuntarComprobantePagoInformadoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.BandejaResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.SubBandejaResumenResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ArchivarActaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ArchivarPorVencimientoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.CerrarActaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ConsentirCondenaYRegistrarPagoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.CerrabilidadResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.HechosMaterialesActaResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.HechosMaterialesEjeResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ComprobanteMockResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ConfirmarPagoInformadoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ConfirmarPagoVoluntarioExternoAccionRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.ConfirmarPagoVoluntarioExternoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.DerivarAGestionExternaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.DictarFalloAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.DictarFalloCondenatorioAccionRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.FirmarDocumentoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.GenerarLoteCorreoPostalRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.GenerarMedidaPreventivaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.GenerarNotificacionActaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.GenerarNulidadAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.GenerarRectificacionAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.GenerarResolucionAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.MarcarResultadoAbsueltoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.NotificadorMunicipalAcuseRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.NotificadorMunicipalAcuseResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.NotificadorMunicipalNotificacionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.NotificacionPortalPendienteResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ObservarPagoInformadoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.PagoInformadoResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.PagoCondenaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarNotificacionNegativaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarNotificacionPositivaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarSolicitudPagoVoluntarioAccionRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarSolicitudPagoVoluntarioAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarApelacionAccionRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarApelacionAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarVencimientoPagoVoluntarioAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarVencimientoPlazoApelacionAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ResolverApelacionAccionRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.ResolverApelacionAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarNotificacionVencidaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarPagoInformadoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarConstatacionMaterialTempranaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarMedidaPreventivaPosteriorAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarCumplimientoMaterialBloqueoCierreAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ReconocerOrigenBloqueanteMaterialAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarResolucionBloqueoCierreAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ReactivarActaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.JuzgadoMontoModificadoRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.ReingresarActaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ReingresarDesdeApremioSinPagoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ReingresarDesdeGestionExternaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarPagoEnApremioAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarResolucionJuzgadoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ReintentarNotificacionAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ReintentarNotificacionVencidaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.BromatologiaDatoResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.CrearActaMockDemoRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.PrototipoActaBusquedaResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.TransitoDatoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/prototipo")
public class PrototipoApiController {

    private final PrototipoStore store;
    private final MockDataFactory mockDataFactory;
    private final ActaBusquedaHelper actaBusquedaHelper;

    public PrototipoApiController(
            PrototipoStore store,
            MockDataFactory mockDataFactory,
            ActaBusquedaHelper actaBusquedaHelper) {
        this.store = store;
        this.mockDataFactory = mockDataFactory;
        this.actaBusquedaHelper = actaBusquedaHelper;
    }

    @GetMapping("/health")
    public PrototipoHealthResponse health() {
        return new PrototipoHealthResponse(
                "UP",
                "api-faltas-prototipo",
                "mock-en-memoria",
                store.getActas().size());
    }

    @PostMapping("/reset")
    public PrototipoResetResponse reset() {
        mockDataFactory.loadInitialData(store);
        return new PrototipoResetResponse(
                "OK",
                "Escenario mock reinicializado",
                store.getActas().size());
    }

    /**
     * Alta mínima de acta mock en vivo (demo), numeración {@code ACTA-DEMO-nnnn}.
     * Consistente con anclas y proyección material del prototipo in-memory.
     */
    @PostMapping("/actas/mock")
    @ResponseStatus(HttpStatus.CREATED)
    public ActaDetalleResponse crearActaMockDemo(@RequestBody CrearActaMockDemoRequest request) {
        PrototipoStore.CrearActaMockDemoResultado r = store.crearActaMockDemo(request);
        if (r.estado() == PrototipoStore.CrearActaMockDemoEstado.BAD_REQUEST) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, r.mensaje() != null ? r.mensaje() : "Solicitud inválida (demo).");
        }
        return mapActaDetalle(r.acta());
    }

    @GetMapping("/bandejas")
    public List<BandejaResponse> listarBandejas() {
        return store.listarBandejasConResumenOperativo().stream()
                .map(this::mapBandejaResponse)
                .toList();
    }

    @GetMapping("/bandejas/{codigo}/actas")
    public List<ActaBandejaItemResponse> listarActasPorBandeja(
            @PathVariable("codigo") String codigo,
            @RequestParam(value = "accionPendiente", required = false) String accionPendiente,
            @RequestParam(value = "situacionPago", required = false) String situacionPago,
            @RequestParam(value = "resultadoFinal", required = false) String resultadoFinal,
            @RequestParam(value = "cerrable", required = false) Boolean cerrable,
            @RequestParam(value = "pendienteBloqueante", required = false) String pendienteBloqueante,
            @RequestParam(value = "subBandeja", required = false) String subBandeja) {
        PrototipoStore.SituacionPagoMock filtroSituacionPago = parseSituacionPago(situacionPago);
        PrototipoStore.ResultadoFinalCierreMock filtroRes = parseResultadoFinalCierre(resultadoFinal);
        PrototipoStore.PendienteBloqueanteCierreMock filtroPend = parsePendienteBloqueante(pendienteBloqueante);
        return store.listarActasPorBandeja(
                codigo,
                accionPendiente,
                filtroSituacionPago,
                filtroRes,
                cerrable,
                filtroPend,
                subBandeja).stream()
                .map(this::mapActaBandejaItem)
                .toList();
    }

    @GetMapping("/actas/{id}")
    public ActaDetalleResponse detalleActa(@PathVariable("id") String id) {
        ActaMock acta = store.findActa(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return mapActaDetalle(acta);
    }

    /**
     * Búsqueda global liviana de actas por número, actaId o fragmento numérico.
     *
     * <p>Busca en todo el dataset demo sin mutar estado. Devuelve lista vacía
     * para {@code q} nulo o en blanco. El resultado está limitado a
     * {@link ActaBusquedaHelper#MAX_RESULTADOS} entradas, ordenadas por
     * relevancia y luego por {@code numeroActa} ascendente.
     */
    @GetMapping("/actas/buscar")
    public List<PrototipoActaBusquedaResponse> buscarActas(
            @RequestParam(value = "q", required = false) String q) {
        return actaBusquedaHelper.buscar(q);
    }

    /**
     * Acceso ciudadano por código QR/código ciudadano: el futuro portal del
     * infractor consulta acá usando un código opaco estable (ver
     * {@link PrototipoStore#codigoQrDeActa(String)}), sin conocer ni exponer
     * el {@code actaId} interno. Devuelve una vista ciudadana mínima
     * ({@link ActaInfractorResponse}); no expone detalles administrativos
     * innecesarios y no materializa EM/RC/Cmte/Pref/Nro ni recibos: el
     * pago real y los comprobantes se modelarán en un slice posterior.
     */
    @GetMapping("/infractor/actas/{codigoQr}")
    public ActaInfractorResponse obtenerActaInfractorPorCodigoQr(
            @PathVariable("codigoQr") String codigoQr) {
        ActaMock acta = store.findActaPorCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return mapActaInfractor(acta);
    }

    /**
     * Presentación de apelación desde el portal del infractor por código QR.
     * El canal se fija internamente como {@code PORTAL_INFRACTOR}; el body es
     * opcional y, si incluye {@code canal}, se ignora (el cliente ciudadano no
     * puede forzar otro canal). Devuelve la vista ciudadana actualizada sin
     * exponer el {@code actaId} interno.
     */
    @PostMapping("/infractor/actas/{codigoQr}/acciones/registrar-apelacion")
    public ActaInfractorResponse registrarApelacionInfractorPorCodigoQr(
            @PathVariable("codigoQr") String codigoQr,
            @RequestBody(required = false) RegistrarApelacionAccionRequest request) {
        ActaMock acta = store.findActaPorCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PrototipoStore.RegistrarApelacionResultado r = store.registrarApelacion(
                acta.id(), PrototipoStore.CanalPresentacionApelacionMock.PORTAL_INFRACTOR);
        if (r.estado() == PrototipoStore.RegistrarApelacionEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarApelacionEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        ActaMock actualizada = store.findActaPorCodigoQr(codigoQr).orElse(acta);
        return mapActaInfractor(actualizada);
    }

    @PostMapping("/infractor/actas/{codigoQr}/acciones/confirmar-visualizacion-notificacion")
    public ActaInfractorResponse confirmarVisualizacionNotificacionInfractorPorCodigoQr(
            @PathVariable("codigoQr") String codigoQr) {
        ActaMock acta = store.findActaPorCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PrototipoStore.ConfirmarVisualizacionNotificacionPortalResultado r =
                store.confirmarVisualizacionNotificacionPortal(acta.id());
        if (r.estado() == PrototipoStore.ConfirmarVisualizacionNotificacionPortalEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ConfirmarVisualizacionNotificacionPortalEstado.SIN_NOTIFICACION_PENDIENTE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No hay notificación pendiente por DOMICILIO_ELECTRONICO para confirmar desde el portal.");
        }
        if (r.estado() == PrototipoStore.ConfirmarVisualizacionNotificacionPortalEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        ActaMock actualizada = store.findActaPorCodigoQr(codigoQr).orElse(acta);
        return mapActaInfractor(actualizada);
    }
    /**
     * Solicitud de pago voluntario iniciada por el infractor desde el portal.
     * No requiere monto: Direccion de Faltas evaluara y, si corresponde,
     * fijara el monto. Devuelve la vista ciudadana actualizada.
     */
    @PostMapping("/infractor/actas/{codigoQr}/acciones/solicitar-pago-voluntario")
    public ActaInfractorResponse solicitarPagoVoluntarioInfractorPorCodigoQr(
            @PathVariable("codigoQr") String codigoQr) {
        ActaMock acta = store.findActaPorCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado r =
                store.solicitarPagoVoluntarioDesdePortal(acta.id());
        if (r.estado() == PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.CONFLICT_EN_REVISION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede solicitar pago voluntario porque el acta se encuentra en revisión.");
        }
        if (r.estado() == PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.CONFLICT_FALLO_DICTADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede solicitar pago voluntario porque existe un fallo condenatorio en proceso.");
        }
        if (r.estado() == PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No es posible solicitar pago voluntario en el estado actual del acta.");
        }
        ActaMock actualizada = store.findActaPorCodigoQr(codigoQr).orElse(acta);
        return mapActaInfractor(actualizada);
    }

    /**
     * El infractor informa el pago voluntario desde el portal.
     * Precondición: monto fijado por Dirección (situacionPago=SOLICITADO u OBSERVADO).
     * Efecto: situacionPago pasa a PENDIENTE_CONFIRMACION; Dirección puede confirmar u observar.
     */
    @PostMapping("/infractor/actas/{codigoQr}/acciones/pagar-voluntario")
    public ActaInfractorResponse pagarVoluntarioInfractorPorCodigoQr(
            @PathVariable("codigoQr") String codigoQr) {
        ActaMock acta = store.findActaPorCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PrototipoStore.InformarPagoVoluntarioDesdePortalResultado r =
                store.informarPagoVoluntarioDesdePortal(acta.id());
        if (r.estado() == PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.CONFLICT_SIN_MONTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede pagar porque Dirección de Faltas aún no fijó el monto.");
        }
        if (r.estado() == PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.CONFLICT_YA_INFORMADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El pago voluntario ya se encuentra en proceso de acreditación.");
        }
        if (r.estado() == PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.CONFLICT_YA_CONFIRMADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El pago voluntario ya fue confirmado.");
        }
        if (r.estado() == PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.CONFLICT_FALLO_DICTADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede informar pago voluntario porque existe un fallo condenatorio dictado.");
        }
        if (r.estado() == PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No es posible informar el pago en el estado actual del acta.");
        }
        ActaMock actualizada = store.findActaPorCodigoQr(codigoQr).orElse(acta);
        return mapActaInfractor(actualizada);
    }

    /**
     * El infractor abre un documento notificable desde el portal, confirmando
     * su visualización. Si el documento ya fue notificado, es idempotente.
     * Devuelve 404 si el documento no existe o no es visible para el infractor.
     */
    @PostMapping("/infractor/actas/{codigoQr}/documentos/{tipoDocumento}/ver")
    public ActaInfractorResponse verDocumentoInfractorPorCodigoQr(
            @PathVariable("codigoQr") String codigoQr,
            @PathVariable("tipoDocumento") String tipoDocumento) {
        ActaMock acta = store.findActaPorCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PrototipoStore.VerDocumentoPortalResultado r = store.verDocumentoPortal(acta.id(), tipoDocumento);
        if (r.estado() == PrototipoStore.VerDocumentoPortalEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.VerDocumentoPortalEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        ActaMock actualizada = store.findActaPorCodigoQr(codigoQr).orElse(acta);
        return mapActaInfractor(actualizada);
    }

    /**
     * El infractor informa el pago de condena desde el portal.
     * Precondición: resultadoFinal=CONDENA_FIRME y situacionPagoCondena=PENDIENTE u OBSERVADO.
     * Devuelve 409 si no se puede pagar en el estado actual.
     */
    @PostMapping("/infractor/actas/{codigoQr}/acciones/pagar-condena")
    public ActaInfractorResponse pagarCondenaInfractorPorCodigoQr(
            @PathVariable("codigoQr") String codigoQr) {
        ActaMock acta = store.findActaPorCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PrototipoStore.PagoCondenaResultado r = store.informarPagoCondenaDesdePortal(acta.id());
        if (r.estado() == PrototipoStore.PagoCondenaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.PagoCondenaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        ActaMock actualizada = store.findActaPorCodigoQr(codigoQr).orElse(acta);
        return mapActaInfractor(actualizada);
    }

    /**
     * El infractor consiente la condena desde el portal, renunciando a apelar.
     * Precondición: resultadoFinal=CONDENADO, sin apelacion presentada.
     * Efecto: resultadoFinal=CONDENA_FIRME, situacionPagoCondena=PENDIENTE.
     */
    @PostMapping("/infractor/actas/{codigoQr}/acciones/consentir-condena")
    public ActaInfractorResponse consentirCondenaInfractorPorCodigoQr(
            @PathVariable("codigoQr") String codigoQr) {
        ActaMock acta = store.findActaPorCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PrototipoStore.ConsentirCondenaResultado r = store.consentirCondena(acta.id());
        if (r.estado() == PrototipoStore.ConsentirCondenaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ConsentirCondenaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede consentir la condena en el estado actual del acta.");
        }
        ActaMock actualizada = store.findActaPorCodigoQr(codigoQr).orElse(acta);
        return mapActaInfractor(actualizada);
    }

    @GetMapping("/actas/{id}/eventos")
    public List<ActaEventoResponse> listarEventosActa(@PathVariable("id") String id) {
        if (!store.existeActa(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return store.listarEventosActaOrdenados(id).stream()
                .map(this::mapActaEvento)
                .toList();
    }

    @GetMapping("/actas/{id}/documentos")
    public List<ActaDocumentoResponse> listarDocumentosActa(@PathVariable("id") String id) {
        if (!store.existeActa(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return store.listarDocumentosPorActa(id).stream()
                .map(this::mapActaDocumento)
                .toList();
    }

    @GetMapping("/actas/{id}/notificaciones")
    public List<ActaNotificacionResponse> listarNotificacionesActa(@PathVariable("id") String id) {
        if (!store.existeActa(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return store.listarNotificacionesPorActa(id).stream()
                .map(this::mapActaNotificacion)
                .toList();
    }

    @GetMapping("/notificaciones/notificador-municipal")
    public List<NotificadorMunicipalNotificacionResponse> listarNotificacionesNotificadorMunicipal() {
        return store.listarNotificacionesNotificadorMunicipal().stream()
                .map(PrototipoApiController::mapNotificadorMunicipalVista)
                .toList();
    }

    @PostMapping("/notificaciones/notificador-municipal/{notificacionId}/acuse")
    public NotificadorMunicipalAcuseResponse registrarAcuseNotificadorMunicipal(
            @PathVariable("notificacionId") String notificacionId,
            @RequestBody(required = false) NotificadorMunicipalAcuseRequest request) {
        if (request == null || request.resultado() == null || request.resultado().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resultado requerido");
        }
        ResultadoNotificacion resultado;
        try {
            resultado = ResultadoNotificacion.valueOf(request.resultado().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "resultado inválido: " + request.resultado());
        }
        if (resultado == ResultadoNotificacion.SIN_RESULTADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resultado sin efecto");
        }
        PrototipoStore.RegistrarAcuseNotificadorMunicipalResultado r =
                store.registrarAcuseNotificadorMunicipal(notificacionId, resultado, request.observacion());
        if (r.estado() == PrototipoStore.RegistrarAcuseNotificadorMunicipalEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "notificacionId no existe: " + notificacionId);
        }
        if (r.estado() == PrototipoStore.RegistrarAcuseNotificadorMunicipalEstado.WRONG_CHANNEL) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La notificación no pertenece al canal NOTIFICADOR_MUNICIPAL.");
        }
        if (r.estado() == PrototipoStore.RegistrarAcuseNotificadorMunicipalEstado.BAD_REQUEST) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resultado inválido para acuse municipal");
        }
        if (r.estado() == PrototipoStore.RegistrarAcuseNotificadorMunicipalEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new NotificadorMunicipalAcuseResponse(
                "OK",
                "Acuse municipal registrado.",
                r.actaId(),
                r.acta(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                mapActaNotificacion(r.notificacion()),
                mapNotificadorMunicipalVista(r.vista()));
    }

    @GetMapping("/notificaciones/correo/listas-para-lote")
    public List<PrototipoStore.CorreoPostalNotificacionListaItem> listarNotificacionesCorreoListasParaLote() {
        return store.listarNotificacionesCorreoListasParaLote();
    }

    @GetMapping("/notificaciones/correo/lotes")
    public List<PrototipoStore.CorreoLoteResumen> listarLotesCorreoGenerados() {
        return store.listarLotesCorreoGenerados();
    }

    @PostMapping("/notificaciones/correo/lotes/{loteId}/anular")
    public PrototipoStore.AnularLoteCorreoResultado anularLoteCorreoPostalDemo(
            @PathVariable("loteId") String loteId) {
        PrototipoStore.AnularLoteCorreoResultado resultado = store.anularLoteCorreoPostalDemo(loteId);
        if ("NOT_FOUND".equals(resultado.estado())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, resultado.mensaje());
        }
        if ("CONFLICT".equals(resultado.estado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, resultado.mensaje());
        }
        return resultado;
    }

    @PostMapping("/notificaciones/correo/{notificacionId}/enviar-individual")
    public PrototipoStore.EnviarIndividualCorreoResultado enviarIndividualCorreoPostalDemo(
            @PathVariable("notificacionId") String notificacionId) {
        PrototipoStore.EnviarIndividualCorreoResultado resultado =
                store.enviarIndividualCorreoPostalDemo(notificacionId);
        if ("NOT_FOUND".equals(resultado.estado())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, resultado.mensaje());
        }
        if ("CONFLICT".equals(resultado.estado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, resultado.mensaje());
        }
        return resultado;
    }

    @PostMapping("/notificaciones/correo/lotes/generar")
    public PrototipoStore.GenerarLoteCorreoResultado generarLoteCorreoPostalDemo(
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestBody(required = false) GenerarLoteCorreoPostalRequest body) {
        try {
            String tipoParam = body != null && body.tipo() != null && !body.tipo().isBlank() ? body.tipo() : tipo;
            List<String> notificacionIds = body != null ? body.notificacionIds() : null;
            return store.generarLoteCorreoPostalDemo(tipoParam, notificacionIds);
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo generar el lote CSV de correo postal demo.",
                    ex);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/notificaciones/correo/trazabilidad")
    public List<PrototipoStore.CorreoPostalTrazabilidadItem> buscarTrazabilidadCorreoPostal(
            @RequestParam("acta") String acta) {
        return store.buscarTrazabilidadCorreoPorActa(acta);
    }

    @PostMapping("/notificaciones/correo/respuestas/procesar-demo")
    public PrototipoStore.ProcesarRespuestaCorreoResultado procesarRespuestaCorreoPostalDemo(
            @RequestParam(name = "loteId", required = false) String loteId) {
        try {
            return store.procesarRespuestaCorreoPostalDemo(loteId);
        } catch (NoSuchFileException ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe el CSV de respuesta demo: " + ex.getFile(),
                    ex);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo procesar el CSV de respuesta de correo postal demo.",
                    ex);
        }
    }

    @PostMapping("/actas/{id}/acciones/registrar-notificacion-positiva")
    public RegistrarNotificacionPositivaAccionResponse registrarNotificacionPositiva(@PathVariable("id") String id) {
        PrototipoStore.RegistrarNotificacionPositivaResultado r = store.registrarNotificacionPositiva(id);
        if (r.estado() == PrototipoStore.RegistrarNotificacionPositivaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarNotificacionPositivaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarNotificacionPositivaAccionResponse(
                "OK",
                "Notificación positiva registrada; acta pasa a análisis.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    @PostMapping("/actas/{id}/acciones/registrar-notificacion-negativa")
    public RegistrarNotificacionNegativaAccionResponse registrarNotificacionNegativa(@PathVariable("id") String id) {
        PrototipoStore.RegistrarNotificacionNegativaResultado r = store.registrarNotificacionNegativa(id);
        if (r.estado() == PrototipoStore.RegistrarNotificacionNegativaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarNotificacionNegativaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarNotificacionNegativaAccionResponse(
                "OK",
                "Notificación negativa registrada; acta retorna a análisis con acción pendiente.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente());
    }

    /**
     * Acción administrativa "Pago voluntario": Dirección de Faltas fija el
     * monto del acta y deja el pago voluntario habilitado. El portal del
     * infractor todavía no existe; cuando exista, deberá mostrar "Pagar"
     * en lugar de "Solicitar pago voluntario" en cuanto detecte monto
     * fijado. Esta acción NO genera comprobantes (sin EM, sin RC, sin
     * Cmte/Pref/Nro): los comprobantes sólo se materializan en el
     * proceso externo de pago.
     */
    @PostMapping("/actas/{id}/acciones/registrar-solicitud-pago-voluntario")
    public RegistrarSolicitudPagoVoluntarioAccionResponse registrarSolicitudPagoVoluntario(
            @PathVariable("id") String id,
            @RequestBody(required = false) RegistrarSolicitudPagoVoluntarioAccionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body requerido con monto");
        }
        BigDecimal monto = request.monto();
        if (monto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "monto requerido");
        }
        if (monto.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "monto debe ser mayor a cero");
        }
        PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado r =
                store.registrarSolicitudPagoVoluntario(id, monto);
        if (r.estado() == PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarSolicitudPagoVoluntarioAccionResponse(
                "OK",
                "Pago voluntario habilitado por Dirección de Faltas con monto fijado.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente(),
                r.montoPagoVoluntario());
    }

    /**
     * Dirección de Faltas fija el monto de pago voluntario cuando el infractor
     * solicitó el pago desde el portal pero no hay monto asignado aún.
     * Precondición: {@code situacionPago=SOLICITADO} y
     * {@code montoPagoVoluntario=null}. Tras la acción, el infractor puede
     * informar el pago desde el portal.
     */
    @PostMapping("/actas/{id}/acciones/fijar-monto-pago-voluntario")
    public RegistrarSolicitudPagoVoluntarioAccionResponse fijarMontoPagoVoluntario(
            @PathVariable("id") String id,
            @RequestBody(required = false) RegistrarSolicitudPagoVoluntarioAccionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body requerido con monto");
        }
        BigDecimal monto = request.monto();
        if (monto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "monto requerido");
        }
        if (monto.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "monto debe ser mayor a cero");
        }
        PrototipoStore.FijarMontoPagoVoluntarioResultado r =
                store.fijarMontoPagoVoluntario(id, monto);
        if (r.estado() == PrototipoStore.FijarMontoPagoVoluntarioEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.FijarMontoPagoVoluntarioEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No es posible fijar monto: el acta no está en estado SOLICITADO o se encuentra en bandeja no operable.");
        }
        return new RegistrarSolicitudPagoVoluntarioAccionResponse(
                "OK",
                "Monto de pago voluntario fijado por Dirección de Faltas. El infractor puede ahora informar el pago.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                store.getAccionPendiente(r.actaId()),
                r.montoPagoVoluntario());
    }

    /**
     * Dirección de Faltas registra que el plazo/oportunidad de pago voluntario
     * venció sin que el infractor pagara. Precondición: {@code situacionPago=SOLICITADO}
     * y {@code resultadoFinal=SIN_RESULTADO_FINAL}. Tras la acción, el acta queda con
     * {@code situacionPago=VENCIDO} y el trámite puede continuar a fallo de fondo.
     * El {@code montoPagoVoluntario} se conserva como dato histórico.
     */
    @PostMapping("/actas/{id}/acciones/registrar-vencimiento-pago-voluntario")
    public RegistrarVencimientoPagoVoluntarioAccionResponse registrarVencimientoPagoVoluntario(
            @PathVariable("id") String id) {
        PrototipoStore.RegistrarVencimientoPagoVoluntarioResultado r =
                store.registrarVencimientoPagoVoluntario(id);
        if (r.estado() == PrototipoStore.RegistrarVencimientoPagoVoluntarioEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarVencimientoPagoVoluntarioEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No es posible registrar vencimiento: el acta no esta en situacion SOLICITADO o se encuentra en bandeja no operable.");
        }
        return new RegistrarVencimientoPagoVoluntarioAccionResponse(
                "OK",
                "Vencimiento de pago voluntario registrado. El tramite puede continuar a fallo.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.situacionPago() != null ? r.situacionPago().name() : null,
                r.montoPagoVoluntario());
    }

    @PostMapping("/actas/{id}/acciones/registrar-pago-informado")
    public RegistrarPagoInformadoAccionResponse registrarPagoInformado(@PathVariable("id") String id) {
        PrototipoStore.RegistrarPagoInformadoResultado r = store.registrarPagoInformado(id);
        if (r.estado() == PrototipoStore.RegistrarPagoInformadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarPagoInformadoEstado.CONFLICT_SIN_MONTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede informar pago voluntario porque Dirección de Faltas aún no fijó el monto.");
        }
        if (r.estado() == PrototipoStore.RegistrarPagoInformadoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarPagoInformadoAccionResponse(
                "OK",
                "Pago informado registrado (mock). Aún sin confirmación interna.",
                r.actaId(),
                r.situacionPago().name());
    }

    @PostMapping("/actas/{id}/acciones/adjuntar-comprobante-pago-informado")
    public AdjuntarComprobantePagoInformadoAccionResponse adjuntarComprobantePagoInformado(
            @PathVariable("id") String id,
            @RequestParam(value = "nombreArchivo", required = false) String nombreArchivo) {
        PrototipoStore.AdjuntarComprobantePagoInformadoResultado r =
                store.adjuntarComprobantePagoInformado(id, nombreArchivo);
        if (r.estado() == PrototipoStore.AdjuntarComprobantePagoInformadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.AdjuntarComprobantePagoInformadoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        PagoInformadoResponse pago = mapPagoInformado(r.pagoInformado());
        return new AdjuntarComprobantePagoInformadoAccionResponse(
                "OK",
                "Comprobante adjuntado (mock); queda pendiente de confirmación interna.",
                r.actaId(),
                r.situacionPago().name(),
                r.accionPendiente(),
                pago);
    }

    @PostMapping("/actas/{id}/acciones/confirmar-pago-informado")
    public ConfirmarPagoInformadoAccionResponse confirmarPagoInformado(@PathVariable("id") String id) {
        PrototipoStore.ConfirmarPagoInformadoResultado r = store.confirmarPagoInformado(id);
        if (r.estado() == PrototipoStore.ConfirmarPagoInformadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ConfirmarPagoInformadoEstado.CONFLICT) {
            throw conflictConfirmarPagoInformado(id);
        }
        return new ConfirmarPagoInformadoAccionResponse(
                "OK",
                "Pago confirmado internamente (mock).",
                r.actaId(),
                r.situacionPago().name());
    }

    /**
     * Confirma pago voluntario desde sistema externo de cobro (gateway/caja).
     * El monto recibido debe coincidir con el monto fijado por Dirección.
     * Precondición: situacionPago == PAGO_INFORMADO o PENDIENTE_CONFIRMACION.
     */
    @PostMapping("/actas/{id}/acciones/confirmar-pago-voluntario-externo")
    public ConfirmarPagoVoluntarioExternoAccionResponse confirmarPagoVoluntarioExterno(
            @PathVariable("id") String id,
            @RequestBody(required = false) ConfirmarPagoVoluntarioExternoAccionRequest request) {
        java.math.BigDecimal monto = request != null ? request.monto() : null;
        String origen = request != null && request.origen() != null ? request.origen().trim() : "SISTEMA_EXTERNO";
        PrototipoStore.ConfirmarPagoVoluntarioExternoResultado r =
                store.confirmarPagoVoluntarioExterno(id, monto, origen);
        if (r.estado() == PrototipoStore.ConfirmarPagoVoluntarioExternoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ConfirmarPagoVoluntarioExternoEstado.CONFLICT_YA_CONFIRMADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El pago voluntario ya fue confirmado.");
        }
        if (r.estado() == PrototipoStore.ConfirmarPagoVoluntarioExternoEstado.CONFLICT_SIN_PAGO_PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No existe pago voluntario informado previo. El infractor debe iniciar el pago desde el portal.");
        }
        if (r.estado() == PrototipoStore.ConfirmarPagoVoluntarioExternoEstado.CONFLICT_MONTO_DISTINTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El monto informado no coincide con el monto fijado para este expediente.");
        }
        if (r.estado() == PrototipoStore.ConfirmarPagoVoluntarioExternoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No es posible confirmar el pago en el estado actual del acta.");
        }
        return new ConfirmarPagoVoluntarioExternoAccionResponse(
                "OK",
                "Pago voluntario confirmado por sistema externo de cobro.",
                r.actaId(),
                r.situacionPago().name());
    }

    /**
     * Resultado final mock: ABSUELTO. Solo desde {@code PENDIENTE_ANALISIS};
     * incompat con {@code PAGO_CONFIRMADO}. No implica cierre.
     */
    @PostMapping("/actas/{id}/acciones/marcar-resultado-final-absuelto")
    public MarcarResultadoAbsueltoAccionResponse marcarResultadoFinalAbsuelto(@PathVariable("id") String id) {
        PrototipoStore.MarcarResultadoAbsueltoResultado r = store.marcarResultadoAbsuelto(id);
        if (r.estado() == PrototipoStore.MarcarResultadoAbsueltoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.MarcarResultadoAbsueltoEstado.CONFLICT) {
            throw conflictMarcarResultadoAbsuelto(id);
        }
        CerrabilidadResponse c = mapCerrabilidad(store.getCerrabilidadActa(r.actaId()));
        return new MarcarResultadoAbsueltoAccionResponse(
                "OK",
                "Resultado final mock: ABSUELTO (no implica cierre automático).",
                r.actaId(),
                r.bandejaActual(),
                c);
    }

    /**
     * Registra el cumplimiento material efectivo (alias de
     * {@code POST .../registrar-cumplimiento-material-bloqueo-cierre}).
     */
    @PostMapping("/actas/{id}/acciones/resolver-pendiente-bloqueante-cierre")
    public RegistrarCumplimientoMaterialBloqueoCierreAccionResponse resolverPendienteBloqueanteCierre(
            @PathVariable("id") String id,
            @RequestParam("tipo") String tipo) {
        return ejecutarRegistrarCumplimientoMaterialBloqueoCierre(id, tipo);
    }

    /**
     * Constatación mínima en D1/D2: incorpora al expediente el ancla
     * documental alineada con
     * {@code ACTA_RETENCION}, {@code CONSTATACION_RETENCION_DOCUMENTACION} o
     * {@code MEDIDA_PREVENTIVA}. Parámetro {@code tipo}:
     * {@code SECUESTRO_RODADO}, {@code RETENCION_DOCUMENTAL},
     * {@code MEDIDA_PREVENTIVA_APLICABLE}. Requiere
     * {@code ACTAS_EN_ENRIQUECIMIENTO} y bloque {@code D1_CAPTURA} o
     * {@code D2_ENRIQUECIMIENTO}, y al menos un evento de trazabilidad previo
     * en el expediente (p. ej. {@code ALTA} en D1).
     */
    @PostMapping("/actas/{id}/acciones/registrar-constatacion-material-temprana")
    public RegistrarConstatacionMaterialTempranaAccionResponse registrarConstatacionMaterialTemprana(
            @PathVariable("id") String id, @RequestParam("tipo") String tipo) {
        PrototipoStore.TipoConstatacionMaterialTemprana t;
        try {
            t = PrototipoStore.TipoConstatacionMaterialTemprana.valueOf(tipo);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo inválido: " + tipo);
        }
        PrototipoStore.RegistrarConstatacionMaterialTempranaResultado r =
                store.registrarConstatacionMaterialTemprana(id, t);
        if (r.estado() == PrototipoStore.RegistrarConstatacionMaterialTempranaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarConstatacionMaterialTempranaEstado.CONFLICT) {
            if (r.motivoConflicto()
                    == PrototipoStore.MotivoConflictoConstatacionMaterialTemprana.TIPO_YA_EN_EXPEDIENTE) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Ya existe una constatación material temprana activa de este tipo para el acta (ancla"
                                + " documental ya en expediente).");
            }
            if (r.motivoConflicto()
                    == PrototipoStore.MotivoConflictoConstatacionMaterialTemprana.FUERA_ETAPA_LABRADO_ENRIQUECIMIENTO) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La constatación material temprana solo puede registrarse en etapa de labrado o"
                                + " enriquecimiento (D1/D2, bandeja de actas en enriquecimiento).");
            }
            if (r.motivoConflicto()
                    == PrototipoStore.MotivoConflictoConstatacionMaterialTemprana.SIN_TRAZA_PREVIA) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Se requiere al menos un evento de trazabilidad en el expediente para registrar"
                                + " constatación material temprana.");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarConstatacionMaterialTempranaAccionResponse(
                "OK",
                "Constatación de material en expediente (D1/D2; mock; misma ancla que el circuito de cierre).",
                r.actaId(),
                r.documentoId(),
                r.tipoDocumento(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    /**
     * Incorpora ancla de medida preventiva nacida durante el trámite
     * administrativo (p. ej. inspección posterior a labrado en
     * contravención), en {@code PENDIENTE_ANALISIS}. No es constatación
     * temprana D1/D2. Reutiliza el mismo criterio documental
     * ({@code MEDIDA_PREVENTIVA}) que cierre y reconocimiento, sin datos de
     * tránsito.
     */
    @PostMapping("/actas/{id}/acciones/registrar-medida-preventiva-posterior")
    public RegistrarMedidaPreventivaPosteriorAccionResponse registrarMedidaPreventivaPosterior(
            @PathVariable("id") String id) {
        PrototipoStore.RegistrarMedidaPreventivaPosteriorResultado r = store.registrarMedidaPreventivaPosterior(id);
        if (r.estado() == PrototipoStore.RegistrarMedidaPreventivaPosteriorEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarMedidaPreventivaPosteriorEstado.CONFLICT) {
            if (r.motivoConflicto()
                    == PrototipoStore.MotivoConflictoRegistroMedidaPreventivaPosterior
                            .MEDIDA_YA_EN_EXPEDIENTE) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El expediente ya contiene ancla de medida preventiva (MEDIDA_PREVENTIVA).");
            }
            if (r.motivoConflicto()
                    == PrototipoStore.MotivoConflictoRegistroMedidaPreventivaPosterior
                            .FUERA_PENDIENTE_ANALISIS) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Registrar medida preventiva posterior solo aplica con acta en PENDIENTE_ANALISIS"
                                + " (mock).");
            }
            if (r.motivoConflicto()
                    == PrototipoStore.MotivoConflictoRegistroMedidaPreventivaPosterior
                            .ACTA_EN_ARCHIVO) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No aplica a acta en bandeja ARCHIVO (mock).");
            }
            if (r.motivoConflicto()
                    == PrototipoStore.MotivoConflictoRegistroMedidaPreventivaPosterior.ACTA_CERRADA) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No aplica a acta cerrada (mock).");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarMedidaPreventivaPosteriorAccionResponse(
                "OK",
                "Medida preventiva registrada desde trámite posterior al labrado (ancla al circuito de cierre;"
                        + " mock).",
                r.actaId(),
                r.documentoId(),
                r.tipoDocumento(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    /**
     * Reconoce el hecho material que origina un bloqueante de cierre, anclado
     * a documentación del expediente. Parámetro {@code tipo}:
     * {@code MEDIDA_ACTIVA} requiere documento de tipo
     * {@code MEDIDA_PREVENTIVA}; {@code SECUESTRO_RODADO} requiere
     * {@code ACTA_RETENCION}; {@code RETENCION_DOCUMENTAL} requiere
     * {@code CONSTATACION_RETENCION_DOCUMENTACION}. Idempotente si el origen
     * ya constaba.
     */
    @PostMapping("/actas/{id}/acciones/reconocer-origen-bloqueo-cierre-material")
    public ReconocerOrigenBloqueanteMaterialAccionResponse reconocerOrigenBloqueoCierreMaterial(
            @PathVariable("id") String id, @RequestParam("tipo") String tipo) {
        TipoReconocimientoOrigenBloqueo t = TipoReconocimientoOrigenBloqueo.parse(tipo);
        PrototipoStore.ReconocerOrigenBloqueanteMaterialResultado r;
        if (t == TipoReconocimientoOrigenBloqueo.MEDIDA_ACTIVA) {
            r = store.reconocerOrigenBloqueanteMedidaPreventiva(id);
        } else if (t == TipoReconocimientoOrigenBloqueo.SECUESTRO_RODADO) {
            r = store.reconocerOrigenBloqueanteSecuestroRodado(id);
        } else {
            r = store.reconocerOrigenBloqueanteRetencionDocumental(id);
        }
        if (r.estado() == PrototipoStore.ReconocerOrigenBloqueanteMaterialEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReconocerOrigenBloqueanteMaterialEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        CerrabilidadResponse c = mapCerrabilidad(r.cerrabilidad());
        return new ReconocerOrigenBloqueanteMaterialAccionResponse(
                "OK",
                "Origen material de bloqueo de cierre reconocido (mock).",
                r.actaId(),
                r.origenBloqueante().name(),
                c);
    }

    /**
     * Registra en el expediente el documento mock resolutorio (no sustituye el
     * cumplimiento material; usar
     * {@link #registrarCumplimientoMaterialBloqueoCierre(String, String)}).
     * Parámetro {@code tipo}: bloque de cierre material
     * ({@code LEVANTAMIENTO_MEDIDA_PREVENTIVA}, {@code LIBERACION_RODADO},
     * {@code ENTREGA_DOCUMENTACION}). Opcional {@code documentoConCircuitoFirmaNotif}
     * (por defecto {@code false}): si es {@code true} y el tipo es
     * levantamiento de medida, el documento se emite con circuito
     * firma+notif mock ({@code PENDIENTE_FIRMA} / tipo
     * {@code DOC_LEVANTAMIENTO_MEDIDA_CIRCUITO_FIRMA_NOTIF}) sin sumar otro
     * bloqueante material. Requiere origen material; la acta no debe estar
     * cerrada ni en {@code GESTION_EXTERNA}, {@code ARCHIVO} ni
     * {@code CERRADAS} (tampoco limitado a {@code PENDIENTE_ANALISIS}).
     */
    @PostMapping("/actas/{id}/acciones/registrar-resolucion-bloqueo-cierre")
    public RegistrarResolucionBloqueoCierreAccionResponse registrarResolucionBloqueoCierreDocumental(
            @PathVariable("id") String id,
            @RequestParam("tipo") String tipo,
            @RequestParam(value = "documentoConCircuitoFirmaNotif", defaultValue = "false")
                    boolean documentoConCircuitoFirmaNotif) {
        PrototipoStore.PendienteBloqueanteCierreMock t = parsePendienteBloqueanteRequired(tipo);
        PrototipoStore.RegistrarResolucionBloqueoCierreResultado r =
                store.registrarResolucionBloqueoCierreDocumental(id, t, documentoConCircuitoFirmaNotif);
        if (r.estado() == PrototipoStore.RegistrarResolucionBloqueoCierreEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarResolucionBloqueoCierreEstado.CONFLICT) {
            String msg = r.motivoNoCerrable();
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    msg != null && !msg.isBlank() ? msg : "Conflicto al registrar resolución documental.");
        }
        CerrabilidadResponse c = new CerrabilidadResponse(
                r.resultadoFinal().name(),
                r.cerrable(),
                r.pendientesBloqueantesCierreRestantes(),
                r.motivoNoCerrable());
        return new RegistrarResolucionBloqueoCierreAccionResponse(
                "OK",
                "Documento resolutorio mock incorporado; el bloqueo persiste hasta registrar cumplimiento material efectivo.",
                r.actaId(),
                r.documentoId(),
                r.tipoDocumento(),
                r.pendienteAsociado(),
                c);
    }

    /**
     * Registro mock de cumplimiento material (medida levantada, rodado
     * liberado, documentación entregada). Requiere documento resolutorio y origen
     * coherentes. Parámetro {@code tipo} igual a
     * {@link #registrarResolucionBloqueoCierreDocumental(String, String)}.
     */
    @PostMapping("/actas/{id}/acciones/registrar-cumplimiento-material-bloqueo-cierre")
    public RegistrarCumplimientoMaterialBloqueoCierreAccionResponse registrarCumplimientoMaterialBloqueoCierre(
            @PathVariable("id") String id,
            @RequestParam("tipo") String tipo) {
        return ejecutarRegistrarCumplimientoMaterialBloqueoCierre(id, tipo);
    }

    private RegistrarCumplimientoMaterialBloqueoCierreAccionResponse ejecutarRegistrarCumplimientoMaterialBloqueoCierre(
            String id, String tipo) {
        PrototipoStore.PendienteBloqueanteCierreMock t = parsePendienteBloqueanteRequired(tipo);
        PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreResultado r =
                store.registrarCumplimientoMaterialBloqueoCierre(id, t);
        if (r.estado() == PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarCumplimientoMaterialBloqueoCierreEstado.CONFLICT) {
            String msg = r.motivoNoCerrable();
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    msg != null && !msg.isBlank() ? msg : "Conflicto al registrar cumplimiento material.");
        }
        CerrabilidadResponse c = new CerrabilidadResponse(
                r.resultadoFinal().name(),
                r.cerrable(),
                r.pendientesBloqueantesCierreRestantes(),
                r.motivoNoCerrable());
        return new RegistrarCumplimientoMaterialBloqueoCierreAccionResponse(
                "OK",
                "Cumplimiento material efectivo registrado (mock).",
                r.actaId(),
                r.pendienteCumplido(),
                c);
    }

    @PostMapping("/actas/{id}/acciones/observar-pago-informado")
    public ObservarPagoInformadoAccionResponse observarPagoInformado(@PathVariable("id") String id) {
        PrototipoStore.ObservarPagoInformadoResultado r = store.observarPagoInformado(id);
        if (r.estado() == PrototipoStore.ObservarPagoInformadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ObservarPagoInformadoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new ObservarPagoInformadoAccionResponse(
                "OK",
                "Pago observado/no confirmado internamente (mock).",
                r.actaId(),
                r.situacionPago().name());
    }

    /**
     * Dirección registra en un único acto el consentimiento presencial de la
     * condena y el pago correspondiente. Precondición:
     * {@code resultadoFinal=CONDENADO}, sin apelación presentada,
     * {@code situacionPagoCondena=NO_APLICA}, {@code situacionPago=SIN_PAGO},
     * {@code montoCondena > 0}. Efecto: {@code resultadoFinal=CONDENA_FIRME},
     * {@code situacionPagoCondena=INFORMADO},
     * {@code situacionPago=PENDIENTE_CONFIRMACION}, {@code tipoPago=CONDENA}.
     * No confirma la acreditación; luego quedan disponibles confirmar y
     * observar acreditación.
     */
    @PostMapping("/actas/{id}/acciones/consentir-condena-y-registrar-pago")
    public ConsentirCondenaYRegistrarPagoAccionResponse consentirCondenaYRegistrarPago(
            @PathVariable("id") String id) {
        if (!store.existeActa(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        PrototipoStore.ConsentirCondenaYRegistrarPagoResultado r =
                store.consentirCondenaYRegistrarPago(id);
        if (r.estado() == PrototipoStore.ConsentirCondenaYRegistrarPagoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ConsentirCondenaYRegistrarPagoEstado.CONFLICT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede consentir la condena y registrar el pago en el estado actual del acta.");
        }
        return new ConsentirCondenaYRegistrarPagoAccionResponse(
                "OK",
                "Condena consentida presencialmente y pago registrado."
                        + " La acreditacion queda pendiente de confirmacion.",
                id,
                r.resultadoFinal() != null ? r.resultadoFinal().name() : null,
                r.situacionPagoCondena() != null ? r.situacionPagoCondena().name() : null);
    }

    @PostMapping("/actas/{id}/acciones/informar-pago-condena")
    public PagoCondenaAccionResponse informarPagoCondena(@PathVariable("id") String id) {
        PrototipoStore.PagoCondenaResultado r = store.informarPagoCondena(id);
        return mapPagoCondena(r, "Pago de condena informado correctamente.");
    }

    @PostMapping("/actas/{id}/acciones/confirmar-pago-condena")
    public PagoCondenaAccionResponse confirmarPagoCondena(@PathVariable("id") String id) {
        PrototipoStore.PagoCondenaResultado r = store.confirmarPagoCondena(id);
        return mapPagoCondena(r, "Pago de condena confirmado correctamente.");
    }

    @PostMapping("/actas/{id}/acciones/observar-pago-condena")
    public PagoCondenaAccionResponse observarPagoCondena(@PathVariable("id") String id) {
        PrototipoStore.PagoCondenaResultado r = store.observarPagoCondena(id);
        return mapPagoCondena(r, "Pago de condena observado.");
    }

    @PostMapping("/actas/{id}/acciones/registrar-notificacion-vencida")
    public RegistrarNotificacionVencidaAccionResponse registrarNotificacionVencida(@PathVariable("id") String id) {
        PrototipoStore.RegistrarNotificacionVencidaResultado r = store.registrarNotificacionVencida(id);
        if (r.estado() == PrototipoStore.RegistrarNotificacionVencidaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarNotificacionVencidaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarNotificacionVencidaAccionResponse(
                "OK",
                "Notificación vencida registrada; acta retorna a análisis con acción pendiente.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente());
    }

    @PostMapping("/actas/{id}/acciones/reintentar-notificacion")
    public ReintentarNotificacionAccionResponse reintentarNotificacion(@PathVariable("id") String id) {
        PrototipoStore.ReintentarNotificacionResultado r = store.reintentarNotificacion(id);
        if (r.estado() == PrototipoStore.ReintentarNotificacionEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReintentarNotificacionEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new ReintentarNotificacionAccionResponse(
                "OK",
                "Reintento de notificación solicitado; acta vuelve a PENDIENTE_NOTIFICACION.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    /**
     * Decisión posterior mínima sobre casos que volvieron a análisis por
     * vencimiento de notificación (proceso del sistema que detecta plazos
     * vencidos; en este prototipo se materializa manualmente vía el endpoint
     * de notificación vencida). Solo aplica a actas en PENDIENTE_ANALISIS con
     * accionPendiente = EVALUAR_NOTIFICACION_VENCIDA. Devuelve el caso a
     * PENDIENTE_NOTIFICACION reutilizando la notificación existente.
     */
    @PostMapping("/actas/{id}/acciones/reintentar-notificacion-vencida")
    public ReintentarNotificacionVencidaAccionResponse reintentarNotificacionVencida(@PathVariable("id") String id) {
        PrototipoStore.ReintentarNotificacionVencidaResultado r = store.reintentarNotificacionVencida(id);
        if (r.estado() == PrototipoStore.ReintentarNotificacionVencidaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReintentarNotificacionVencidaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new ReintentarNotificacionVencidaAccionResponse(
                "OK",
                "Decisión posterior al vencimiento: se reintenta la notificación; acta vuelve a PENDIENTE_NOTIFICACION.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    @PostMapping("/actas/{id}/acciones/cerrar-acta")
    public CerrarActaAccionResponse cerrarActa(@PathVariable("id") String id) {
        PrototipoStore.CerrarActaResultado r = store.cerrarActaDesdeAnalisis(id);
        if (r.estado() == PrototipoStore.CerrarActaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.CerrarActaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new CerrarActaAccionResponse(
                "OK",
                "Acta cerrada desde análisis.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    /**
     * Derivación efectiva a gestión externa con tipo APREMIO. Solo aplica a
     * actas en PENDIENTE_ANALISIS con
     * accionPendiente = DERIVAR_GESTION_EXTERNA (casos que ya atravesaron
     * fallo + notificación de fallo + ventana de espera posterior de 5 días
     * sin novedad). Materializa la salida del circuito interno: el acta sale
     * de análisis, pasa a la macro-bandeja GESTION_EXTERNA y queda con
     * tipoGestionExterna = APREMIO, reingresable. El retorno efectivo se
     * modelará en un slice posterior.
     */
    @PostMapping("/actas/{id}/acciones/derivar-a-apremio")
    public DerivarAGestionExternaAccionResponse derivarAApremio(@PathVariable("id") String id) {
        PrototipoStore.DerivarAGestionExternaResultado r = store.derivarAApremio(id);
        if (r.estado() == PrototipoStore.DerivarAGestionExternaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.DerivarAGestionExternaEstado.CONFLICT) {
            String motivo = r.motivo();
            throw motivo != null && !motivo.isBlank()
                    ? new ResponseStatusException(HttpStatus.CONFLICT, motivo)
                    : new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new DerivarAGestionExternaAccionResponse(
                "OK",
                "Acta derivada efectivamente a gestión externa; tipo " + r.tipoGestionExterna() + ".",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.tipoGestionExterna());
    }

    /**
     * Derivación efectiva a gestión externa con tipo JUZGADO_DE_PAZ.
     * Alternativa tipada a {@code derivar-a-apremio}: misma precondición,
     * mismo efecto sobre bandeja/bloque/estado/situación/reingreso, sólo
     * cambia el {@code tipoGestionExterna} asignado.
     */
    @PostMapping("/actas/{id}/acciones/derivar-a-juzgado-de-paz")
    public DerivarAGestionExternaAccionResponse derivarAJuzgadoDePaz(@PathVariable("id") String id) {
        PrototipoStore.DerivarAGestionExternaResultado r = store.derivarAJuzgadoDePaz(id);
        if (r.estado() == PrototipoStore.DerivarAGestionExternaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.DerivarAGestionExternaEstado.CONFLICT) {
            String motivo = r.motivo();
            throw motivo != null && !motivo.isBlank()
                    ? new ResponseStatusException(HttpStatus.CONFLICT, motivo)
                    : new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new DerivarAGestionExternaAccionResponse(
                "OK",
                "Acta derivada efectivamente a gestión externa; tipo " + r.tipoGestionExterna() + ".",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.tipoGestionExterna());
    }

    /**
     * Retorno efectivo desde la macro-bandeja GESTION_EXTERNA al circuito
     * operativo. Solo aplica a actas en GESTION_EXTERNA que preserven
     * {@code permiteReingreso = true}. Devuelve el caso a PENDIENTE_ANALISIS
     * con marca operativa {@code REVISION_POST_GESTION_EXTERNA} y preserva el
     * {@code tipoGestionExterna} original como trazabilidad sintética de la
     * gestión externa de la que provino. En este slice el reingreso queda
     * consumido (no se modelan todavía políticas diferenciadas por tipo).
     */
    @PostMapping("/actas/{id}/acciones/reingresar-desde-gestion-externa")
    public ReingresarDesdeGestionExternaAccionResponse reingresarDesdeGestionExterna(@PathVariable("id") String id) {
        PrototipoStore.ReingresarDesdeGestionExternaResultado r = store.reingresarActaDesdeGestionExterna(id);
        if (r.estado() == PrototipoStore.ReingresarDesdeGestionExternaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReingresarDesdeGestionExternaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        String mensaje = r.tipoGestionExternaPrevia() == null
                ? "Retorno desde gestión externa; acta vuelve a PENDIENTE_ANALISIS."
                : "Retorno desde gestión externa (tipo previo " + r.tipoGestionExternaPrevia()
                        + "); acta vuelve a PENDIENTE_ANALISIS.";
        return new ReingresarDesdeGestionExternaAccionResponse(
                "OK",
                mensaje,
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente(),
                r.tipoGestionExternaPrevia());
    }

    /**
     * APREMIO: retorno administrativo sin pago. Solo aplica a actas en
     * GESTION_EXTERNA con tipoGestionExterna = APREMIO y permiteReingreso.
     * Deja el acta en PENDIENTE_ANALISIS con condena firme pendiente de pago.
     * tipoGestionExterna queda null (limpiado).
     */
    @PostMapping("/actas/{id}/acciones/apremio-reingresar-sin-pago")
    public ReingresarDesdeApremioSinPagoAccionResponse apremioReingresarSinPago(
            @PathVariable("id") String id) {
        PrototipoStore.ReingresarDesdeApremioSinPagoResultado r =
                store.reingresarDesdeApremioSinPago(id);
        if (r.estado() == PrototipoStore.ReingresarDesdeApremioSinPagoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReingresarDesdeApremioSinPagoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new ReingresarDesdeApremioSinPagoAccionResponse(
                "OK",
                "Retorno desde Apremio sin pago; condena firme pendiente. "
                        + "Acta vuelve a análisis.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente());
    }

    /**
     * APREMIO: registra pago efectuado en el proceso de apremio. Solo aplica
     * a actas en GESTION_EXTERNA con tipoGestionExterna = APREMIO y
     * permiteReingreso. Confirma la situación de pago y deja el acta en
     * condición de cierre. tipoGestionExterna queda null (limpiado).
     */
    @PostMapping("/actas/{id}/acciones/apremio-registrar-pago")
    public RegistrarPagoEnApremioAccionResponse apremioRegistrarPago(
            @PathVariable("id") String id) {
        PrototipoStore.RegistrarPagoEnApremioResultado r = store.registrarPagoEnApremio(id);
        if (r.estado() == PrototipoStore.RegistrarPagoEnApremioEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarPagoEnApremioEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarPagoEnApremioAccionResponse(
                "OK",
                "Pago informado desde Apremio; queda pendiente de confirmación interna.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    @PostMapping("/actas/{id}/acciones/apremio-reingresar-monto-modificado")
    public RegistrarResolucionJuzgadoAccionResponse apremioReingresarMontoModificado(
            @PathVariable("id") String id,
            @RequestBody JuzgadoMontoModificadoRequest request) {
        PrototipoStore.ReingresarDesdeJuzgadoResultado r =
                store.reingresarDesdeApremioMontoModificado(id, request.nuevoMonto());
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarResolucionJuzgadoAccionResponse(
                "OK",
                "Resultado externo de Apremio registrado: propone modificar monto de condena a "
                        + request.nuevoMonto() + ".",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente(),
                r.resolucion());
    }

    @PostMapping("/actas/{id}/acciones/apremio-reingresar-absuelto")
    public RegistrarResolucionJuzgadoAccionResponse apremioReingresarAbsuelto(
            @PathVariable("id") String id) {
        PrototipoStore.ReingresarDesdeJuzgadoResultado r =
                store.reingresarDesdeApremioAbsuelto(id);
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarResolucionJuzgadoAccionResponse(
                "OK",
                "Resultado externo de Apremio registrado: propone absolución.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente(),
                r.resolucion());
    }

    /**
     * JUZGADO_DE_PAZ: registra resolución judicial absolutoria. Solo aplica a
     * actas en GESTION_EXTERNA con tipoGestionExterna = JUZGADO_DE_PAZ y
     * permiteReingreso. Establece resultadoFinal = ABSUELTO y deja el acta
     * cerrable. tipoGestionExterna queda null (limpiado).
     */
    @PostMapping("/actas/{id}/acciones/juzgado-reingresar-absuelto")
    public RegistrarResolucionJuzgadoAccionResponse juzgadoReingresarAbsuelto(
            @PathVariable("id") String id) {
        PrototipoStore.ReingresarDesdeJuzgadoResultado r =
                store.reingresarDesdeJuzgadoAbsuelto(id);
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarResolucionJuzgadoAccionResponse(
                "OK",
                "Resultado externo de Juzgado de Paz registrado: propone absolución.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente(),
                r.resolucion());
    }

    /**
     * JUZGADO_DE_PAZ: registra resolución judicial que confirma la condena
     * original. Solo aplica a actas en GESTION_EXTERNA con
     * tipoGestionExterna = JUZGADO_DE_PAZ y permiteReingreso. Mantiene
     * CONDENA_FIRME y deja el acta con pago de condena pendiente.
     * tipoGestionExterna queda null (limpiado).
     */
    @PostMapping("/actas/{id}/acciones/juzgado-reingresar-condena-confirmada")
    public RegistrarResolucionJuzgadoAccionResponse juzgadoReingresarCondenaConfirmada(
            @PathVariable("id") String id) {
        PrototipoStore.ReingresarDesdeJuzgadoResultado r =
                store.reingresarDesdeJuzgadoCondenaConfirmada(id);
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarResolucionJuzgadoAccionResponse(
                "OK",
                "Resolución de Juzgado de Paz registrada: confirma condena.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente(),
                r.resolucion());
    }

    /**
     * JUZGADO_DE_PAZ: registra resolución judicial que modifica el monto de
     * condena. Solo aplica a actas en GESTION_EXTERNA con
     * tipoGestionExterna = JUZGADO_DE_PAZ, permiteReingreso y nuevoMonto > 0.
     * Actualiza monto, mantiene CONDENA_FIRME, deja el acta con pago pendiente.
     * tipoGestionExterna queda null (limpiado).
     */
    @PostMapping("/actas/{id}/acciones/juzgado-reingresar-monto-modificado")
    public RegistrarResolucionJuzgadoAccionResponse juzgadoReingresarMontoModificado(
            @PathVariable("id") String id,
            @RequestBody JuzgadoMontoModificadoRequest request) {
        PrototipoStore.ReingresarDesdeJuzgadoResultado r =
                store.reingresarDesdeJuzgadoMontoModificado(id, request.nuevoMonto());
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarResolucionJuzgadoAccionResponse(
                "OK",
                "Resultado externo de Juzgado de Paz registrado: propone modificar monto de condena a "
                        + request.nuevoMonto() + ".",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente(),
                r.resolucion());
    }

    @PostMapping("/actas/{id}/acciones/archivar-acta")
    public ArchivarActaAccionResponse archivarActa(@PathVariable("id") String id) {
        PrototipoStore.ArchivarActaResultado r = store.archivarActaDesdeAnalisis(id);
        if (r.estado() == PrototipoStore.ArchivarActaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ArchivarActaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new ArchivarActaAccionResponse(
                "OK",
                "Acta archivada directamente desde análisis; motivo " + r.motivoArchivo() + ".",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.motivoArchivo());
    }

    /**
     * Archivo específico de casos que volvieron a análisis por evaluación de
     * notificación vencida. No reutiliza la acción genérica para no diluir la
     * semántica: el motivo de archivo resultante queda distinguido del
     * archivo directo. Solo aplica a actas en PENDIENTE_ANALISIS con
     * accionPendiente = EVALUAR_NOTIFICACION_VENCIDA.
     */
    @PostMapping("/actas/{id}/acciones/archivar-por-vencimiento")
    public ArchivarPorVencimientoAccionResponse archivarPorVencimiento(@PathVariable("id") String id) {
        PrototipoStore.ArchivarPorVencimientoResultado r = store.archivarPorVencimiento(id);
        if (r.estado() == PrototipoStore.ArchivarPorVencimientoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ArchivarPorVencimientoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new ArchivarPorVencimientoAccionResponse(
                "OK",
                "Decisión posterior al vencimiento: acta archivada; motivo " + r.motivoArchivo() + ".",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.motivoArchivo());
    }

    /**
     * Reingreso explícito desde la macro-bandeja ARCHIVO. Solo aplica a
     * actas archivadas que preserven {@code permiteReingreso = true}. Devuelve
     * el caso a PENDIENTE_ANALISIS con marca operativa
     * {@code REVISION_POST_REINGRESO} para dejarlo distinguible dentro de
     * análisis. No modifica {@code motivoArchivo}: la trazabilidad del motivo
     * de archivo original se preserva explícitamente.
     */
    @PostMapping("/actas/{id}/acciones/reingresar-acta")
    public ReingresarActaAccionResponse reingresarActa(@PathVariable("id") String id) {
        PrototipoStore.ReingresarActaResultado r = store.reingresarActaDesdeArchivo(id);
        if (r.estado() == PrototipoStore.ReingresarActaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReingresarActaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        String destino = r.bandejaActual() != null ? r.bandejaActual() : "PENDIENTE_ANALISIS";
        String mensaje = r.motivoArchivoPrevio() == null
                ? "Reingreso desde archivo; acta vuelve a " + destino + "."
                : "Reingreso desde archivo (motivo previo " + r.motivoArchivoPrevio()
                        + "); acta vuelve a " + destino + ".";
        return new ReingresarActaAccionResponse(
                "OK",
                mensaje,
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente(),
                r.motivoArchivoPrevio());
    }

    /**
     * Envía el acta desde enriquecimiento (D2) a la bandeja de notificación
     * pendiente (D4/PENDIENTE_NOTIFICACION). Solo aplica a actas en
     * {@code ACTAS_EN_ENRIQUECIMIENTO} con situaciónAdministrativa ACTIVA.
     * Los bloqueantes materiales pendientes no impiden esta transición.
     */
    @PostMapping("/actas/{id}/acciones/enviar-a-notificacion")
    public EnviarANotificacionAccionResponse enviarANotificacion(@PathVariable("id") String id) {
        PrototipoStore.EnviarActaANotificacionResultado r = store.enviarActaANotificacion(id);
        if (r.estado() == PrototipoStore.EnviarActaANotificacionEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.EnviarActaANotificacionEstado.CONFLICT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El acta no está en enriquecimiento activo; no se puede enviar a notificación.");
        }
        return new EnviarANotificacionAccionResponse(
                "OK",
                "Acta enviada a notificación.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    /**
     * Anula el acta y la archiva por nulidad desde la etapa de enriquecimiento.
     * Solo aplica a actas en {@code ACTAS_EN_ENRIQUECIMIENTO} con
     * situaciónAdministrativa ACTIVA. El acta queda en ARCHIVO con
     * {@code motivoArchivo=NULIDAD} y {@code permiteReingreso=true}.
     */
    @PostMapping("/actas/{id}/acciones/anular-acta")
    public AnularActaPorNulidadAccionResponse anularActa(@PathVariable("id") String id) {
        PrototipoStore.AnularActaPorNulidadResultado r = store.anularActaPorNulidad(id);
        if (r.estado() == PrototipoStore.AnularActaPorNulidadEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.AnularActaPorNulidadEstado.CONFLICT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El acta no está en enriquecimiento activo; no se puede anular.");
        }
        return new AnularActaPorNulidadAccionResponse(
                "OK",
                "Acta anulada y archivada por nulidad.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.motivoArchivo());
    }

    /**
     * Reactivación desde la macro-bandeja PARALIZADAS. Solo aplica a actas
     * cuya {@code bandejaActual} sea {@code PARALIZADAS}. Devuelve el caso a
     * {@code PENDIENTE_ANALISIS} con marca operativa
     * {@code REVISION_POST_REACTIVACION}; la información histórica de la
     * paralización queda en el log de eventos.
     */
    @PostMapping("/actas/{id}/acciones/reactivar-acta")
    public ReactivarActaAccionResponse reactivarActa(@PathVariable("id") String id) {
        PrototipoStore.ReactivarActaResultado r = store.reactivarActa(id);
        if (r.estado() == PrototipoStore.ReactivarActaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReactivarActaEstado.CONFLICT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El acta no se encuentra en bandeja PARALIZADAS.");
        }
        return new ReactivarActaAccionResponse(
                "OK",
                "Acta reactivada correctamente.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente());
    }

    @PostMapping("/actas/{id}/acciones/generar-medida-preventiva")
    public GenerarMedidaPreventivaAccionResponse generarMedidaPreventiva(@PathVariable("id") String id) {
        PrototipoStore.GenerarMedidaPreventivaResultado r = store.generarMedidaPreventiva(id);
        if (r.estado() == PrototipoStore.GenerarMedidaPreventivaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.GenerarMedidaPreventivaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new GenerarMedidaPreventivaAccionResponse(
                "OK",
                "Medida preventiva generada.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    @PostMapping("/actas/{id}/acciones/generar-notificacion-acta")
    public GenerarNotificacionActaAccionResponse generarNotificacionActa(@PathVariable("id") String id) {
        PrototipoStore.GenerarNotificacionActaResultado r = store.generarNotificacionActa(id);
        if (r.estado() == PrototipoStore.GenerarNotificacionActaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.GenerarNotificacionActaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new GenerarNotificacionActaAccionResponse(
                "OK",
                "Notificación del acta generada.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    /**
     * Producción de la pieza NULIDAD como pieza no-fallo dentro del
     * circuito documental/resolutivo. Solo aplica a actas en
     * PENDIENTES_RESOLUCION_REDACCION que declaran NULIDAD como pieza
     * requerida (caso demo alineado con spec: ACTA-0012 con
     * estadoProcesoActual = PENDIENTE_NULIDAD). Semánticamente: produce la
     * pieza nulidad, genera el documento asociado pendiente de firma y
     * emite el evento NULIDAD_GENERADA; la transición de bandeja sigue la
     * misma regla agregadora de piezas que MEDIDA_PREVENTIVA y
     * NOTIFICACION_ACTA (no se declara nulidad como bandeja terminal).
     */
    @PostMapping("/actas/{id}/acciones/generar-nulidad")
    public GenerarNulidadAccionResponse generarNulidad(@PathVariable("id") String id) {
        PrototipoStore.GenerarNulidadResultado r = store.generarNulidad(id);
        if (r.estado() == PrototipoStore.GenerarNulidadEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.GenerarNulidadEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new GenerarNulidadAccionResponse(
                "OK",
                "Nulidad generada.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    /**
     * Produccion de la pieza RESOLUCION como pieza no-fallo dentro del
     * circuito documental/resolutivo. Solo aplica a actas en
     * PENDIENTES_RESOLUCION_REDACCION que declaran RESOLUCION como pieza
     * requerida (caso demo: ACTA-0011 con estadoProcesoActual =
     * PENDIENTE_RESOLUCION). Al firmarse el documento generado, la acta
     * pasa a PENDIENTE_NOTIFICACION.
     */
    @PostMapping("/actas/{id}/acciones/generar-resolucion")
    public GenerarResolucionAccionResponse generarResolucion(@PathVariable("id") String id) {
        PrototipoStore.GenerarResolucionResultado r = store.generarResolucion(id);
        if (r.estado() == PrototipoStore.GenerarResolucionEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.GenerarResolucionEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new GenerarResolucionAccionResponse(
                "OK",
                "Resolucion generada.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    /**
     * Produccion de la pieza RECTIFICACION como pieza no-fallo dentro del
     * circuito documental/resolutivo. Solo aplica a actas en
     * PENDIENTES_RESOLUCION_REDACCION que declaran RECTIFICACION como pieza
     * requerida (caso demo: ACTA-0014 con estadoProcesoActual =
     * PENDIENTE_RECTIFICACION). Al firmarse el documento generado, la acta
     * pasa a PENDIENTE_NOTIFICACION.
     */
    @PostMapping("/actas/{id}/acciones/generar-rectificacion")
    public GenerarRectificacionAccionResponse generarRectificacion(@PathVariable("id") String id) {
        PrototipoStore.GenerarRectificacionResultado r = store.generarRectificacion(id);
        if (r.estado() == PrototipoStore.GenerarRectificacionEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.GenerarRectificacionEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new GenerarRectificacionAccionResponse(
                "OK",
                "Rectificacion generada.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    /**
     * Dicta fallo absolutorio desde {@code PENDIENTE_ANALISIS}: produce el
     * documento mock {@code FALLO_ABSOLUTORIO} en {@code PENDIENTE_FIRMA} y
     * mueve la acta a la bandeja {@code PENDIENTE_FIRMA}. No cambia
     * {@code resultadoFinal} todavía; eso ocurre al notificarse
     * positivamente el fallo (vía endpoint existente
     * {@code registrar-notificacion-positiva}). La firma del documento
     * reutiliza {@code firmar-documento/{documentoId}}.
     */
    @PostMapping("/actas/{id}/acciones/dictar-fallo-absolutorio")
    public DictarFalloAccionResponse dictarFalloAbsolutorio(@PathVariable("id") String id) {
        return mapDictarFallo(
                store.dictarFalloAbsolutorio(id),
                "Fallo absolutorio dictado; documento pendiente de firma.",
                null);
    }

    /**
     * Dicta fallo condenatorio desde {@code PENDIENTE_ANALISIS}: produce el
     * documento mock {@code FALLO_CONDENATORIO} en {@code PENDIENTE_FIRMA}
     * y mueve la acta a la bandeja {@code PENDIENTE_FIRMA}. No cambia
     * {@code resultadoFinal} todavía; al notificarse positivamente el fallo
     * se abre el plazo de apelación y el portal del infractor podrá ofrecer
     * la acción correspondiente.
     */
    @PostMapping("/actas/{id}/acciones/dictar-fallo-condenatorio")
    public DictarFalloAccionResponse dictarFalloCondenatorio(
            @PathVariable("id") String id,
            @RequestBody(required = false) DictarFalloCondenatorioAccionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body requerido con montoCondena");
        }
        BigDecimal montoCondena = request.montoCondena();
        if (montoCondena == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "montoCondena requerido");
        }
        if (montoCondena.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "montoCondena debe ser mayor a cero");
        }
        return mapDictarFallo(
                store.dictarFalloCondenatorio(id, montoCondena),
                "Fallo condenatorio dictado; documento pendiente de firma.",
                montoCondena);
    }

    /**
     * Registra el vencimiento mock del plazo de apelación (sin cálculo
     * real de días). Solo aplica si {@code resultadoFinal} es
     * {@code CONDENADO} y el plazo está abierto. Marca el resultado como
     * {@code CONDENA_FIRME}; el portal/infractor deja de habilitar la
     * presentación de apelación.
     */
    @PostMapping("/actas/{id}/acciones/registrar-vencimiento-plazo-apelacion")
    public RegistrarVencimientoPlazoApelacionAccionResponse registrarVencimientoPlazoApelacion(
            @PathVariable("id") String id) {
        PrototipoStore.RegistrarVencimientoPlazoApelacionResultado r =
                store.registrarVencimientoPlazoApelacion(id);
        if (r.estado() == PrototipoStore.RegistrarVencimientoPlazoApelacionEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarVencimientoPlazoApelacionEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarVencimientoPlazoApelacionAccionResponse(
                "OK",
                "Plazo de apelación vencido sin apelación; resultadoFinal CONDENA_FIRME.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.resultadoFinal());
    }

    /**
     * Registra la presentación mock de apelación/recurso mientras el plazo
     * está abierto. Cierra el plazo, conserva {@code resultadoFinal}
     * {@code CONDENADO} y no resuelve ni eleva el recurso.
     */
    @PostMapping("/actas/{id}/acciones/registrar-apelacion")
    public RegistrarApelacionAccionResponse registrarApelacion(
            @PathVariable("id") String id,
            @RequestBody(required = false) RegistrarApelacionAccionRequest request) {
        if (request == null || request.canal() == null || request.canal().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "canal requerido");
        }
        PrototipoStore.CanalPresentacionApelacionMock canal;
        try {
            canal = PrototipoStore.CanalPresentacionApelacionMock.valueOf(request.canal().trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "canal inválido: " + request.canal());
        }
        PrototipoStore.RegistrarApelacionResultado r = store.registrarApelacion(id, canal);
        if (r.estado() == PrototipoStore.RegistrarApelacionEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarApelacionEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarApelacionAccionResponse(
                "OK",
                "Apelación/recurso presentado; plazo de apelación cerrado; resultadoFinal CONDENADO.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.resultadoFinal(),
                r.canal());
    }

    /**
     * Resuelve mock de apelación/recurso ya presentado con plazo cerrado.
     * {@code RECHAZADA} confirma la condena ({@code CONDENA_FIRME});
     * {@code ACEPTADA_ABSUELVE} absuelve ({@code ABSUELTO}). No cierra el
     * acta ni deriva a gestión externa.
     */
    @PostMapping("/actas/{id}/acciones/resolver-apelacion")
    public ResolverApelacionAccionResponse resolverApelacion(
            @PathVariable("id") String id,
            @RequestBody(required = false) ResolverApelacionAccionRequest request) {
        if (request == null || request.resultado() == null || request.resultado().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resultado requerido");
        }
        PrototipoStore.ResultadoResolucionApelacionMock resultado;
        try {
            resultado =
                    PrototipoStore.ResultadoResolucionApelacionMock.valueOf(request.resultado().trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "resultado inválido: " + request.resultado());
        }
        PrototipoStore.ResolverApelacionResultado r = store.resolverApelacion(id, resultado);
        if (r.estado() == PrototipoStore.ResolverApelacionEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ResolverApelacionEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        String mensaje =
                resultado == PrototipoStore.ResultadoResolucionApelacionMock.RECHAZADA
                        ? "Apelación rechazada; condena confirmada; resultadoFinal CONDENA_FIRME."
                        : "Apelación acogida — absolución; resultadoFinal ABSUELTO.";
        return new ResolverApelacionAccionResponse(
                "OK",
                mensaje,
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.resultadoFinal(),
                r.resultadoResolucion());
    }

    private DictarFalloAccionResponse mapDictarFallo(
            PrototipoStore.DictarFalloResultado r, String mensajeOk, BigDecimal montoCondena) {
        if (r.estado() == PrototipoStore.DictarFalloEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.DictarFalloEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new DictarFalloAccionResponse(
                "OK",
                mensajeOk,
                r.actaId(),
                r.documentoId(),
                r.tipoDocumento(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                montoCondena);
    }

    @PostMapping("/actas/{id}/acciones/firmar-documento/{documentoId}")
    public FirmarDocumentoAccionResponse firmarDocumento(
            @PathVariable("id") String id,
            @PathVariable("documentoId") String documentoId) {
        PrototipoStore.FirmarDocumentoResultado r = store.firmarDocumento(id, documentoId);
        if (r.estado() == PrototipoStore.FirmarDocumentoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.FirmarDocumentoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new FirmarDocumentoAccionResponse(
                "OK",
                "Documento firmado.",
                r.actaId(),
                r.documentoId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    private ActaBandejaItemResponse mapActaBandejaItem(ActaMock a) {
        var clasificacion = store.clasificarSubBandeja(a.id());
        return new ActaBandejaItemResponse(
                a.id(),
                a.numeroActa(),
                a.infractorNombre(),
                a.bloqueActual(),
                a.estadoProcesoActual(),
                a.situacionAdministrativaActual(),
                a.bandejaActual(),
                store.getSituacionPago(a.id()).name(),
                store.getAccionPendiente(a.id()),
                store.getMotivoArchivo(a.id()),
                store.getTipoGestionExterna(a.id()),
                mapCerrabilidad(store.getCerrabilidadActa(a.id())),
                clasificacion.subBandeja(),
                clasificacion.subBandejaLabel(),
                clasificacion.chip(),
                clasificacion.accionPrincipal(),
                clasificacion.prioridadSubBandeja(),
                clasificacion.chipsSecundarios(),
                store.getDependenciaDemo(a.id()).orElse(null));
    }

    private BandejaResponse mapBandejaResponse(PrototipoStore.BandejaResumenOperativo resumen) {
        List<SubBandejaResumenResponse> subBandejas = resumen.subBandejas().stream()
                .map(sb -> new SubBandejaResumenResponse(
                        sb.codigo(),
                        SubBandejaCodigo.porCodigo(sb.codigo())
                                .map(SubBandejaCodigo::label)
                                .orElse(sb.codigo()),
                        sb.cantidad()))
                .toList();
        return new BandejaResponse(
                resumen.codigo(),
                BandejaNombres.nombre(resumen.codigo()),
                resumen.cantidadActas(),
                subBandejas);
    }

    private ActaDetalleResponse mapActaDetalle(ActaMock a) {
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
                        .map(this::mapActaNotificacion)
                        .toList(),
                computarAccionesUi(a));
    }

    private AccionesUiResponse computarAccionesUi(ActaMock a) {
        boolean archivoReingreso = a.permiteReingreso() && "ARCHIVO".equals(a.bandejaActual());

        String bandeja = a.bandejaActual();
        boolean bandejaOperativaInterna = !"ARCHIVO".equals(bandeja)
                && !"CERRADAS".equals(bandeja)
                && !"GESTION_EXTERNA".equals(bandeja);

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
        boolean pagoCondena = false;
        boolean confirmarPagoCondena = false;
        boolean observarPagoCondena = false;
        boolean gestionExterna = false;
        boolean apelacionPresencial = false;
        boolean vencimientoPlazoApelacion = false;

        if (!a.estaCerrada()
                && bandejaOperativaInterna
                && "ACTIVA".equals(a.situacionAdministrativaActual())) {
            PrototipoStore.ResultadoFinalCierreMock rf = resultadoFinalVigente(a.id());
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
            boolean falloPostGestionExterna = PrototipoStore.ACCION_DICTAR_FALLO_POST_GESTION_EXTERNA.equals(
                    store.getAccionPendiente(a.id()))
                    && store.hayResultadoExternoPostGestionPendiente(a.id());

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
                    && ((sinResultadoFinal && sinFalloDictado && pagoCompatibleConFallo) || falloPostGestionExterna);

            // cumplimientoMaterial / resolucionBloqueante:
            // solo ejecutables cuando existe un habilitante material
            PrototipoStore.CerrabilidadActaVista cv = store.getCerrabilidadActa(a.id());
            boolean tieneHabilitante = hayHabilitanteMaterialParaUi(a.id(), rf, cv);
            boolean tienePendientesMateriales = cv != null && !cv.pendientesBloqueantes().isEmpty();
            if (tieneHabilitante && tienePendientesMateriales) {
                cumplimientoMaterial = true;
                resolucionBloqueante = true;
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

    private ActaInfractorResponse mapActaInfractor(ActaMock a) {
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
                "Al confirmar, se registrará la visualización de esta notificación en el portal infractor.");
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
        String b = a.bandejaActual();
        return "CERRADAS".equals(b) || "ARCHIVO".equals(b) || "GESTION_EXTERNA".equals(b);
    }

    private String computarEstadoVisible(ActaMock a) {
        if (a.estaCerrada() || "CERRADAS".equals(a.bandejaActual())) {
            return "CERRADA";
        }
        if ("ARCHIVO".equals(a.bandejaActual())) {
            return "ARCHIVADA";
        }
        if ("GESTION_EXTERNA".equals(a.bandejaActual())) {
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
                    return "La actuación se encuentra archivada administrativamente."
                            + " El pago de condena se encuentra en proceso de verificación"
                            + " por Dirección de Faltas.";
                }
                return "La actuación se encuentra archivada administrativamente, pero registra una"
                        + " condena firme pendiente de pago. Puede regularizar el pago desde este portal.";
            }
            return "El acta se encuentra archivada; no admite gestiones desde el portal.";
        }
        if ("EN_GESTION_EXTERNA".equals(estadoVisible)) {
            return "El acta se encuentra en gestión externa; no admite gestiones desde el portal.";
        }
        if ("EN_REVISION".equals(estadoVisible)) {
            return "El acta se encuentra en revisión. "
                    + "Será notificado cuando la documentación esté validada y disponible.";
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
                return "Pago de condena en proceso de acreditación. "
                        + "Dirección de Faltas verificará la acreditación.";
            }
            if (situacionPagoCondena == PrototipoStore.SituacionPagoCondena.PENDIENTE) {
                return "La condena se encuentra firme y registra un monto pendiente de pago.";
            }
        }
        // Condenado (fallo notificado, condena aún no firme)
        if (resultadoFinal == PrototipoStore.ResultadoFinalCierreMock.CONDENADO) {
            return "El fallo fue notificado. Puede presentar apelación dentro del plazo correspondiente"
                    + " o consentir la condena para avanzar al pago.";
        }
        // Documento con notificación pendiente
        if (hayDocumentoPendienteNotificacion) {
            return "El expediente registra documentación pendiente de notificación. "
                    + "Abra el documento para quedar notificado.";
        }
        // Fallo condenatorio dictado pero aún pendiente de firma
        if (store.hayFalloCondenatorioDictado(acta.id())
                && "PENDIENTE_FIRMA".equals(acta.bandejaActual())) {
            return "El expediente registra una resolución en proceso de formalización. "
                    + "No puede realizar acciones hasta que sea firmada y notificada.";
        }
        // Pago voluntario
        if (situacion == PrototipoStore.SituacionPagoMock.CONFIRMADO) {
            return "El pago voluntario ya fue confirmado.";
        }
        if (situacion == PrototipoStore.SituacionPagoMock.PAGO_INFORMADO
                || situacion == PrototipoStore.SituacionPagoMock.PENDIENTE_CONFIRMACION) {
            return "Pago en proceso de acreditación. Dirección de Faltas verificará la acreditación.";
        }
        if (situacion == PrototipoStore.SituacionPagoMock.OBSERVADO && monto != null) {
            return "El pago fue observado. Puede intentar nuevamente.";
        }
        if (situacion == PrototipoStore.SituacionPagoMock.SOLICITADO && monto == null) {
            return "Solicitud de pago voluntario registrada. Dirección de Faltas evaluará el expediente.";
        }
        if (puedePagar) {
            return "Hay un monto disponible para pago voluntario.";
        }
        if (puedeSolicitarPagoVoluntario) {
            return "El acta se encuentra en trámite; puede solicitar pago voluntario.";
        }
        return "El acta se encuentra en trámite.";
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

    private PrototipoStore.SituacionPagoMock parseSituacionPago(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return PrototipoStore.SituacionPagoMock.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "situacionPago inválida: " + raw);
        }
    }

    private PrototipoStore.ResultadoFinalCierreMock parseResultadoFinalCierre(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return PrototipoStore.ResultadoFinalCierreMock.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resultadoFinal inválido: " + raw);
        }
    }

    private PrototipoStore.PendienteBloqueanteCierreMock parsePendienteBloqueante(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return PrototipoStore.PendienteBloqueanteCierreMock.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pendienteBloqueante inválido: " + raw);
        }
    }

    private PrototipoStore.PendienteBloqueanteCierreMock parsePendienteBloqueanteRequired(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo de pendiente requerido");
        }
        try {
            return PrototipoStore.PendienteBloqueanteCierreMock.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo de pendiente inválido: " + raw);
        }
    }

    private CerrabilidadResponse mapCerrabilidad(PrototipoStore.CerrabilidadActaVista v) {
        if (v == null) {
            return new CerrabilidadResponse("SIN_RESULTADO_FINAL", false, List.of(), "Sin acta.");
        }
        return new CerrabilidadResponse(
                v.resultadoFinal().name(),
                v.cerrable(),
                v.pendientesBloqueantes().stream().map(Enum::name).toList(),
                v.motivoNoCerrable());
    }

    private ResponseStatusException conflictConfirmarPagoInformado(String actaId) {
        PrototipoStore.ResultadoFinalCierreMock rf = resultadoFinalVigente(actaId);
        if (rf == PrototipoStore.ResultadoFinalCierreMock.ABSUELTO
                || rf == PrototipoStore.ResultadoFinalCierreMock.CONDENADO
                || rf == PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME) {
            return new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede confirmar pago informado (flujo viejo): resultado jurídico vigente "
                            + rf.name()
                            + ".");
        }
        return new ResponseStatusException(HttpStatus.CONFLICT);
    }

    private ResponseStatusException conflictMarcarResultadoAbsuelto(String actaId) {
        PrototipoStore.ResultadoFinalCierreMock rf = resultadoFinalVigente(actaId);
        if (rf == PrototipoStore.ResultadoFinalCierreMock.PAGO_CONFIRMADO
                || rf == PrototipoStore.ResultadoFinalCierreMock.CONDENADO
                || rf == PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME) {
            return new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede marcar absolución directa (flujo viejo): resultado final vigente "
                            + rf.name()
                            + ".");
        }
        return new ResponseStatusException(HttpStatus.CONFLICT);
    }

    private PrototipoStore.ResultadoFinalCierreMock resultadoFinalVigente(String actaId) {
        PrototipoStore.CerrabilidadActaVista v = store.getCerrabilidadActa(actaId);
        return v != null ? v.resultadoFinal() : PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL;
    }

    private PagoInformadoResponse mapPagoInformado(PrototipoStore.PagoInformadoMock p) {
        if (p == null) {
            return null;
        }
        ComprobanteMockResponse comprobante = null;
        if (p.comprobanteId() != null || p.comprobanteNombreArchivo() != null) {
            comprobante = new ComprobanteMockResponse(p.comprobanteId(), p.comprobanteNombreArchivo());
        }
        return new PagoInformadoResponse(p.fechaInformado(), comprobante);
    }

    private PagoCondenaAccionResponse mapPagoCondena(
            PrototipoStore.PagoCondenaResultado r,
            String mensajeOk) {
        if (r.estado() == PrototipoStore.PagoCondenaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.PagoCondenaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new PagoCondenaAccionResponse(
                "OK",
                mensajeOk,
                r.actaId(),
                r.situacionPagoCondena().name());
    }

    private ActaEventoResponse mapActaEvento(ActaEventoMock e) {
        return new ActaEventoResponse(
                e.id(),
                e.fechaHora(),
                e.tipoEvento(),
                e.bloqueOrigen(),
                e.bloqueDestino(),
                e.descripcion());
    }

    private ActaDocumentoResponse mapActaDocumento(ActaDocumentoMock d) {
        return new ActaDocumentoResponse(
                d.id(),
                d.actaId(),
                d.tipoDocumento(),
                d.estadoDocumento(),
                d.nombreArchivo());
    }

    private ActaNotificacionResponse mapActaNotificacion(ActaNotificacionMock n) {
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

    private static NotificadorMunicipalNotificacionResponse mapNotificadorMunicipalVista(
            PrototipoStore.NotificacionMunicipalVista v) {
        return new NotificadorMunicipalNotificacionResponse(
                v.notificacionId(),
                v.actaId(),
                v.acta(),
                v.tipo(),
                v.canal(),
                v.estado(),
                v.resultado(),
                v.destinatario(),
                v.domicilio(),
                v.observacion(),
                v.qrNotificacion(),
                v.fechaPreparacion(),
                v.fechaEnvio());
    }

    private enum TipoReconocimientoOrigenBloqueo {
        /**
         * Ancla: documento de tipo {@code MEDIDA_PREVENTIVA} en el expediente
         * (origen {@code MEDIDA_PREVENTIVA_ACTIVA}).
         */
        MEDIDA_ACTIVA,
        SECUESTRO_RODADO,
        RETENCION_DOCUMENTAL;

        static TipoReconocimientoOrigenBloqueo parse(String raw) {
            if (raw == null || raw.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo requerido");
            }
            try {
                return TipoReconocimientoOrigenBloqueo.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo inválido: " + raw);
            }
        }
    }
}
