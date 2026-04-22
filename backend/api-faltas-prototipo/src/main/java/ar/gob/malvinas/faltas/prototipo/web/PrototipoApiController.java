package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.store.MockDataFactory;
import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaBandejaItemResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaDetalleResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaDocumentoResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaEventoResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaNotificacionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.BandejaResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ArchivarActaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.CerrarActaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.FirmarDocumentoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.GenerarMedidaPreventivaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.GenerarNotificacionActaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarNotificacionNegativaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarNotificacionPositivaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarNotificacionVencidaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ReintentarNotificacionAccionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/prototipo")
public class PrototipoApiController {

    private final PrototipoStore store;
    private final MockDataFactory mockDataFactory;

    public PrototipoApiController(PrototipoStore store, MockDataFactory mockDataFactory) {
        this.store = store;
        this.mockDataFactory = mockDataFactory;
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

    @GetMapping("/bandejas")
    public List<BandejaResponse> listarBandejas() {
        return store.listarBandejasConConteoOrdenadas().stream()
                .map(bc -> new BandejaResponse(bc.codigo(), BandejaNombres.nombre(bc.codigo()), bc.cantidadActas()))
                .toList();
    }

    @GetMapping("/bandejas/{codigo}/actas")
    public List<ActaBandejaItemResponse> listarActasPorBandeja(
            @PathVariable("codigo") String codigo,
            @RequestParam(value = "accionPendiente", required = false) String accionPendiente) {
        return store.listarActasPorBandeja(codigo, accionPendiente).stream()
                .map(this::mapActaBandejaItem)
                .toList();
    }

    @GetMapping("/actas/{id}")
    public ActaDetalleResponse detalleActa(@PathVariable("id") String id) {
        ActaMock acta = store.findActa(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return mapActaDetalle(acta);
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
                "Acta archivada desde análisis.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
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
        return new ActaBandejaItemResponse(
                a.id(),
                a.numeroActa(),
                a.infractorNombre(),
                a.bloqueActual(),
                a.estadoProcesoActual(),
                a.situacionAdministrativaActual(),
                a.bandejaActual(),
                store.getAccionPendiente(a.id()));
    }

    private ActaDetalleResponse mapActaDetalle(ActaMock a) {
        return new ActaDetalleResponse(
                a.id(),
                a.numeroActa(),
                a.bloqueActual(),
                a.estadoProcesoActual(),
                a.situacionAdministrativaActual(),
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
                store.getAccionPendiente(a.id()));
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
        return new ActaNotificacionResponse(
                n.id(),
                n.actaId(),
                n.canal(),
                n.estadoNotificacion(),
                n.destinatarioResumen());
    }
}
