package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.bandeja.SubBandejaCodigo;
import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import ar.gob.malvinas.faltas.prototipo.web.mapper.ActaDetalleMapper;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaBandejaItemResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaDetalleResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaDocumentoResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaEventoResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaNotificacionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.BandejaResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.SubBandejaResumenResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.PrototipoActaBusquedaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/prototipo")
public class PrototipoConsultaActasController {

    private final PrototipoStore store;
    private final ActaBusquedaHelper actaBusquedaHelper;
    private final ActaDetalleMapper actaDetalleMapper;

    public PrototipoConsultaActasController(
            PrototipoStore store,
            ActaBusquedaHelper actaBusquedaHelper,
            ActaDetalleMapper actaDetalleMapper) {
        this.store = store;
        this.actaBusquedaHelper = actaBusquedaHelper;
        this.actaDetalleMapper = actaDetalleMapper;
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
        return actaDetalleMapper.map(acta);
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
                .map(ActaDetalleMapper::mapActaNotificacion)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Helpers privados de mapeo (exclusivos de consulta)
    // -------------------------------------------------------------------------

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
                ActaDetalleMapper.mapCerrabilidad(store.getCerrabilidadActa(a.id())),
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

    // -------------------------------------------------------------------------
    // Helpers privados de parseo de filtros (exclusivos de listarActasPorBandeja)
    // -------------------------------------------------------------------------

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
}
