package ar.gob.malvinas.faltas.core.application.demo;

import ar.gob.malvinas.faltas.core.application.result.CasoUsoFuncionalEjecucionResultado;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.web.dto.DemoActaDetalleActaDto;
import ar.gob.malvinas.faltas.core.web.dto.DemoActaDetalleDatasetDto;
import ar.gob.malvinas.faltas.core.web.dto.DemoActaDetalleLinksDto;
import ar.gob.malvinas.faltas.core.web.dto.DemoActaDetalleMetaDto;
import ar.gob.malvinas.faltas.core.web.dto.DemoActaDetalleResponse;
import ar.gob.malvinas.faltas.core.web.dto.DemoDocumentoDetalleDto;
import ar.gob.malvinas.faltas.core.web.dto.DemoTimelineEventoDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de materializacion de actas demo individuales.
 *
 * Para cada codigo del DatasetFuncionalDominioCatalog:
 * - Valida que el codigo exista (404 si no).
 * - Crea un CasoUsoFuncionalRunner aislado y ejecuta el flujo real.
 * - Construye la respuesta frontend-ready a partir de la instancia real resultante.
 *
 * Idempotencia: cada llamada crea un runner fresco con repos propios.
 * El resultado es determinista: mismos pasos = mismo estado final.
 *
 * Guardrails:
 * - No JDBC, no SQL, no JPA, no storage real.
 * - storageKey y hashDocu se exponen tal como los genera el flujo real (mock://, sha256-mock-).
 * - No genera PDF real ni escribe archivos.
 *
 * Slice 8F-7.
 */
@Service
public class DemoActaMaterializadorService {

    /**
     * Materializa una acta demo y devuelve la respuesta frontend-ready.
     *
     * @param codigo codigo del dataset funcional (ej: "ACT-001-LABRADA")
     * @return respuesta con instancia real, timeline y documentos
     * @throws ResponseStatusException 404 si el codigo no existe en el catalogo
     */
    public DemoActaDetalleResponse materializar(String codigo) {
        ActaMockFuncionalDefinicion def = resolverDefinicion(codigo);

        CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
        CasoUsoFuncionalEjecucionResultado resultado = runner.ejecutar(codigo);

        Long actaId = resultado.actaId();

        FalActa acta = null;
        List<FalActaEvento> eventos = List.of();
        List<FalDocumento> docs = List.of();
        Optional<FalActaSnapshot> snapshotOpt = Optional.empty();

        if (actaId != null) {
            acta = runner.getActaRepo().buscarPorId(actaId).orElse(null);
            eventos = runner.getEventoRepo().buscarPorActa(actaId);
            docs = runner.getDocRepo().buscarPorActa(actaId);
            snapshotOpt = runner.getSnapshotRepo().buscarPorActa(actaId);
        }

        DemoActaDetalleDatasetDto datasetDto = buildDatasetDto(def);
        DemoActaDetalleActaDto actaDto = buildActaDto(actaId, acta, resultado, snapshotOpt);
        List<DemoTimelineEventoDto> timeline = buildTimeline(eventos);
        List<DemoDocumentoDetalleDto> documentos = buildDocumentos(docs);
        DemoActaDetalleMetaDto meta = buildMeta(resultado);
        DemoActaDetalleLinksDto links = buildLinks(codigo);

        return new DemoActaDetalleResponse(
                def.codigo(),
                def.titulo(),
                def.descripcion(),
                def.casoUsoPrincipal(),
                datasetDto,
                actaDto,
                timeline,
                documentos,
                meta,
                links
        );
    }

    // =========================================================================
    // Resolucion del catalogo
    // =========================================================================

    private ActaMockFuncionalDefinicion resolverDefinicion(String codigo) {
        try {
            return DatasetFuncionalDominioCatalog.buscarPorCodigo(codigo);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Acta demo no encontrada: " + codigo);
        }
    }

    // =========================================================================
    // Builders de DTO
    // =========================================================================

    private DemoActaDetalleDatasetDto buildDatasetDto(ActaMockFuncionalDefinicion def) {
        return new DemoActaDetalleDatasetDto(
                def.bloqueEsperado() != null ? def.bloqueEsperado().name() : null,
                def.situacionEsperada() != null ? def.situacionEsperada().name() : null,
                def.bandejaEsperada() != null ? def.bandejaEsperada().name() : null,
                def.cerrableEsperado()
        );
    }

    private DemoActaDetalleActaDto buildActaDto(
            Long actaId,
            FalActa acta,
            CasoUsoFuncionalEjecucionResultado resultado,
            Optional<FalActaSnapshot> snapshotOpt) {

        if (actaId == null || acta == null) {
            return new DemoActaDetalleActaDto(null, null, null, null, null, null, null, null, false);
        }

        String bloqueActual = resultado.bloqueFinal() != null
                ? resultado.bloqueFinal()
                : (acta.getBloqueActual() != null ? acta.getBloqueActual().name() : null);

        String estadoProcesal = resultado.estadoFinal() != null
                ? resultado.estadoFinal()
                : (acta.getEstadoProcesal() != null ? acta.getEstadoProcesal().name() : null);

        String situacion = acta.getSituacionAdministrativa() != null
                ? acta.getSituacionAdministrativa().name() : null;

        String resultadoFinal = resultado.resultadoFinal() != null
                ? resultado.resultadoFinal()
                : (acta.getResultadoFinal() != null ? acta.getResultadoFinal().name() : null);

        String bandeja = resultado.bandejaFinal() != null ? resultado.bandejaFinal() : null;

        boolean cerrable = resultado.cerrableFinal();

        return new DemoActaDetalleActaDto(
                actaId,
                acta.getNroActa(),
                acta.getUuidTecnico(),
                bloqueActual,
                estadoProcesal,
                situacion,
                resultadoFinal,
                bandeja,
                cerrable
        );
    }

    private List<DemoTimelineEventoDto> buildTimeline(List<FalActaEvento> eventos) {
        var sortedEventos = eventos.stream()
                .sorted(java.util.Comparator.comparing(FalActaEvento::fhEvt)
                        .thenComparingLong(e -> e.getId() != null ? e.getId() : 0L))
                .toList();
        var result = new java.util.ArrayList<DemoTimelineEventoDto>();
        for (int i = 0; i < sortedEventos.size(); i++) {
            var e = sortedEventos.get(i);
            result.add(new DemoTimelineEventoDto(
                    i + 1,
                    e.getId() != null ? String.valueOf(e.getId()) : null,
                    e.tipoEvt() != null ? e.tipoEvt().name() : null,
                    e.descripcionLegible(),
                    e.fhEvt() != null ? e.fhEvt().toString() : null
            ));
        }
        return result;
    }

    private List<DemoDocumentoDetalleDto> buildDocumentos(List<FalDocumento> docs) {
        return docs.stream()
                .map(d -> new DemoDocumentoDetalleDto(
                        d.getId(),
                        d.getTipoDocu() != null ? d.getTipoDocu().name() : null,
                        d.getEstadoDocu() != null ? d.getEstadoDocu().name() : null,
                        d.getStorageKey(),
                        d.getHashDocu(),
                        true,
                        d.getFhGeneracion() != null ? d.getFhGeneracion().toString() : null,
                        d.getPlantillaId(),
                        d.getDescripcion()
                ))
                .toList();
    }

    private DemoActaDetalleMetaDto buildMeta(CasoUsoFuncionalEjecucionResultado resultado) {
        boolean materializada = resultado.ejecutado() && resultado.actaId() != null;
        return new DemoActaDetalleMetaDto(
                true,
                materializada,
                "DATASET_FUNCIONAL",
                resultado.advertencias() != null ? resultado.advertencias() : List.of()
        );
    }

    private DemoActaDetalleLinksDto buildLinks(String codigo) {
        return new DemoActaDetalleLinksDto(
                "/demo/actas/" + codigo,
                "/demo/actas/dataset-funcional",
                "/demo/documentos/graph"
        );
    }
}
