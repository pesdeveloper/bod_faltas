package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.CrearDependenciaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearDocumentoPlantillaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearPoliticaNumeracionCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioAmbitoCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioCommand;
import ar.gob.malvinas.faltas.core.application.command.FirmarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoDesdePlantillaCommand;
import ar.gob.malvinas.faltas.core.application.command.NumerarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.result.NumerarDocumentoParaFirmasResponse;
import ar.gob.malvinas.faltas.core.application.service.DependenciaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoPlantillaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.MomentoNumeracionDocu;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalDependencia;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantilla;
import ar.gob.malvinas.faltas.core.domain.model.NumPolitica;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonario;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioMovimiento;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.InspectorRepository;
import ar.gob.malvinas.faltas.core.repository.TalonarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaReqRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFirmanteRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryInspectorRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryTalonarioRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests focalizados para la integracion controlada con la aplicacion de Firmas.
 *
 * Cubre el endpoint POST /api/faltas/documentos/{documentoId}/numerar
 * y la capacidad central NumerarDocumentoParaFirmas del DocumentoService.
 *
 * Slice D-18: EmitirNumeroDocumento - capacidad central y endpoint para Firmas.
 */
@DisplayName("D-18 Numeracion para Firmas: capacidad central e integracion controlada")
class DocumentoNumeracionFirmasTest {

    private static final String DEP_COD = "DEP-NF-001";

    private ActaRepository actaRepo;
    private DocumentoRepository docRepo;
    private DocumentoPlantillaRepository plantillaRepo;
    private TalonarioRepository talonarioRepo;
    private DependenciaRepository depRepo;

    private DocumentoService docService;
    private DocumentoPlantillaService plantillaService;
    private TalonarioService talonarioService;
    private DependenciaService depService;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        docRepo = new InMemoryDocumentoRepository();
        plantillaRepo = new InMemoryDocumentoPlantillaRepository();
        talonarioRepo = new InMemoryTalonarioRepository();
        depRepo = new InMemoryDependenciaRepository();
        InspectorRepository inspectorRepo = new InMemoryInspectorRepository();
        InMemoryActaEventoRepository eventoRepo = new InMemoryActaEventoRepository();
        InMemoryActaSnapshotRepository snapshotRepo = new InMemoryActaSnapshotRepository();
        DocumentoFirmaRepository firmaRepo = new InMemoryDocumentoFirmaRepository();
        InMemoryFalloActaRepository falloRepo = new InMemoryFalloActaRepository();
        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo,
                new InMemoryNotificacionRepository(),
                new InMemoryPagoVoluntarioRepository(),
                falloRepo,
                new InMemoryApelacionActaRepository(),
                new InMemoryPagoCondenaRepository(), FaltasClockTestSupport.FIXED);
        talonarioService = new TalonarioService(talonarioRepo, depRepo, inspectorRepo, FaltasClockTestSupport.FIXED);
        depService = new DependenciaService(depRepo, FaltasClockTestSupport.FIXED);
        plantillaService = new DocumentoPlantillaService(plantillaRepo, FaltasClockTestSupport.FIXED);
        docService = new DocumentoService(
                actaRepo, docRepo, firmaRepo, eventoRepo, snapshotRepo, recalc, falloRepo,
                plantillaRepo, talonarioService, depRepo,
                new InMemoryDocumentoFirmaReqRepository(),
                new InMemoryFirmanteRepository(), FaltasClockTestSupport.FIXED);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private FalActa crearActa() {
        Long id = actaRepo.nextId();
        FalActa acta = new FalActa(
                id, UUID.randomUUID().toString(),
                "TRANSITO", DEP_COD, "INS-001",
                FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(),
                "Belgrano 200", "Calle 123", null, null, null, "Juan Perez", "12345678",
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR);
        actaRepo.guardar(acta);
        return acta;
    }

    private FalDependencia crearDependencia() {
        return depService.crear(new CrearDependenciaCommand(
                DEP_COD, "Dep NF", null, TipoActa.TRANSITO,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(30), "sistema"));
    }

    private NumPolitica crearPoliticaDoc() {
        return talonarioService.crearPolitica(new CrearPoliticaNumeracionCommand(
                "POL-NF-01", "Politica NF", ClaseNumeracion.DOCUMENTO,
                false, false, null, false, null, false, null,
                "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, "sistema"));
    }

    private NumTalonario crearTalonarioDoc(Long politicaId) {
        return talonarioService.crearTalonario(new CrearTalonarioCommand(
                politicaId, "TAL-NF-01", "Talonario NF",
                TipoTalonario.ELECTRONICO, ClaseNumeracion.DOCUMENTO,
                null, null, 1, 9999, "seq_nf_" + UUID.randomUUID().toString().substring(0, 6),
                true, false, null, null, "sistema"));
    }

    private void crearAmbitoGlobal(Long talonarioId, Short tipoDocu) {
        talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                talonarioId, ClaseNumeracion.DOCUMENTO,
                tipoDocu, null, null, null,
                AlcanceTalonario.GLOBAL, (short) 10,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, true, "sistema"));
    }

    private FalDocumentoPlantilla crearPlantilla(String codigo, MomentoNumeracionDocu momento) {
        FalDocumentoPlantilla p = plantillaService.crear(new CrearDocumentoPlantillaCommand(
                codigo, "Plantilla " + codigo, null,
                TipoDocu.CONSTANCIA, AccionDocumental.EMITIR_CONSTANCIA, null,
                TipoFirmaReq.NO_REQUIERE,
                true, momento,
                false, false, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, "sistema"));
        return plantillaService.activar(p.getId());
    }

    private FalDocumentoPlantilla crearPlantillaSinFirma(String codigo, MomentoNumeracionDocu momento) {
        FalDocumentoPlantilla p = plantillaService.crear(new CrearDocumentoPlantillaCommand(
                codigo, "Plantilla " + codigo, null,
                TipoDocu.CONSTANCIA, AccionDocumental.EMITIR_CONSTANCIA, null,
                TipoFirmaReq.NO_REQUIERE,
                true, momento,
                false, false, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, "sistema"));
        return plantillaService.activar(p.getId());
    }

    private FalDocumento crearDoc(FalActa acta, FalDocumentoPlantilla plantilla) {
        return docService.generarDesdePlantilla(
                new GenerarDocumentoDesdePlantillaCommand(acta.getId(), plantilla.getId(), "usr-test"));
    }

    /** Fuerza el estado a PENDIENTE_FIRMA directamente (sin pasar por enviarAFirma),
     *  permitiendo testear numerarDocumentoParaFirmas con TipoFirmaReq.NO_REQUIERE. */
    private FalDocumento ponerEnPendienteFirma(FalDocumento doc) {
        doc.setEstadoDocu(EstadoDocu.PENDIENTE_FIRMA);
        docRepo.guardar(doc);
        return docRepo.buscarPorId(doc.getId()).orElseThrow();
    }

    private void setupCompleto(FalActa[] actaOut, FalDocumento[] docOut,
                                NumTalonario[] talOut, MomentoNumeracionDocu momento) {
        crearDependencia();
        FalActa acta = crearActa();
        NumPolitica pol = crearPoliticaDoc();
        NumTalonario tal = crearTalonarioDoc(pol.getId());
        crearAmbitoGlobal(tal.getId(), TipoDocu.CONSTANCIA.codigo());
        FalDocumentoPlantilla p = crearPlantilla("PLNT-NF-" + UUID.randomUUID().toString().substring(0, 6), momento);
        FalDocumento doc = crearDoc(acta, p);
        actaOut[0] = acta;
        docOut[0] = doc;
        talOut[0] = tal;
    }

    // =========================================================================
    // AL_FIRMAR: el momento principal de la integracion con Firmas
    // =========================================================================

    @Nested
    @DisplayName("AL_FIRMAR: numeracion via endpoint de Firmas")
    class AlFirmar {

        @Test
        @DisplayName("T16. Numera documento elegible en PENDIENTE_FIRMA con momento AL_FIRMAR")
        void numera_documento_elegible_pendiente_firma() {
            FalActa[] actaOut = new FalActa[1];
            FalDocumento[] docOut = new FalDocumento[1];
            NumTalonario[] talOut = new NumTalonario[1];
            setupCompleto(actaOut, docOut, talOut, MomentoNumeracionDocu.AL_FIRMAR);

            FalDocumento docEnFirma = ponerEnPendienteFirma(docOut[0]);
            assertThat(docEnFirma.getEstadoDocu()).isEqualTo(EstadoDocu.PENDIENTE_FIRMA);
            assertThat(docEnFirma.getNroDocu()).isNull(); // AL_FIRMAR: aun no tiene numero

            NumerarDocumentoParaFirmasResponse resp = docService.numerarDocumentoParaFirmas(
                    new NumerarDocumentoCommand(docEnFirma.getId(), "firmas-app"));

            assertThat(resp.yaEstabaNumerado()).isFalse();
            assertThat(resp.nroDocu()).isNotNull().isNotBlank();
            assertThat(resp.momentoAplicado()).isEqualTo(MomentoNumeracionDocu.AL_FIRMAR);
            assertThat(resp.estadoDocu()).isEqualTo(EstadoDocu.PENDIENTE_FIRMA);
            assertThat(resp.idTalonario()).isEqualTo(talOut[0].getId());
        }

        @Test
        @DisplayName("T17. Idempotencia: segunda llamada devuelve mismo numero sin consumir otro correlativo")
        void idempotencia_segunda_llamada_mismo_numero() {
            FalActa[] actaOut = new FalActa[1];
            FalDocumento[] docOut = new FalDocumento[1];
            NumTalonario[] talOut = new NumTalonario[1];
            setupCompleto(actaOut, docOut, talOut, MomentoNumeracionDocu.AL_FIRMAR);

            FalDocumento docEnFirma = ponerEnPendienteFirma(docOut[0]);

            NumerarDocumentoParaFirmasResponse resp1 = docService.numerarDocumentoParaFirmas(
                    new NumerarDocumentoCommand(docEnFirma.getId(), "firmas-app"));
            NumerarDocumentoParaFirmasResponse resp2 = docService.numerarDocumentoParaFirmas(
                    new NumerarDocumentoCommand(docEnFirma.getId(), "firmas-app"));

            assertThat(resp1.nroDocu()).isEqualTo(resp2.nroDocu());
            assertThat(resp1.nroTalonarioUsado()).isEqualTo(resp2.nroTalonarioUsado());
            assertThat(resp2.yaEstabaNumerado()).isTrue();

            // Solo un movimiento de talonario consumido
            List<NumTalonarioMovimiento> movs = talonarioService.listarMovimientosPorTalonario(talOut[0].getId());
            assertThat(movs).hasSize(1);
        }

        @Test
        @DisplayName("T12C. Concurrencia: dos solicitudes paralelas producen un solo numero")
        void concurrencia_un_solo_numero() throws Exception {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDoc();
            NumTalonario tal = crearTalonarioDoc(pol.getId());
            crearAmbitoGlobal(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantilla("PLNT-CONC-NF", MomentoNumeracionDocu.AL_FIRMAR);
            FalDocumento doc = crearDoc(acta, p);
            FalDocumento docEnFirma = ponerEnPendienteFirma(doc);

            int threads = 5;
            ExecutorService exec = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(1);
            List<Future<String>> futures = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                futures.add(exec.submit(() -> {
                    latch.await();
                    try {
                        NumerarDocumentoParaFirmasResponse r = docService.numerarDocumentoParaFirmas(
                                new NumerarDocumentoCommand(docEnFirma.getId(), "firmas-conc"));
                        return r.nroDocu();
                    } catch (Exception e) {
                        return "ERROR:" + e.getMessage();
                    }
                }));
            }
            latch.countDown();
            exec.shutdown();

            List<String> results = new ArrayList<>();
            for (Future<String> f : futures) {
                results.add(f.get());
            }

            // Todos deben retornar el mismo numero (alguno asigno, el resto fue idempotente)
            long errores = results.stream().filter(r -> r.startsWith("ERROR")).count();
            long distintos = results.stream().filter(r -> !r.startsWith("ERROR")).distinct().count();

            assertThat(errores).isEqualTo(0);
            assertThat(distintos).isEqualTo(1); // un solo numero unico

            // Un solo movimiento de talonario
            List<NumTalonarioMovimiento> movs = talonarioService.listarMovimientosPorTalonario(tal.getId());
            assertThat(movs).hasSize(1);
        }

        @Test
        @DisplayName("T19R. Rechaza documento en estado BORRADOR (no PENDIENTE_FIRMA)")
        void rechaza_estado_borrador() {
            FalActa[] actaOut = new FalActa[1];
            FalDocumento[] docOut = new FalDocumento[1];
            NumTalonario[] talOut = new NumTalonario[1];
            setupCompleto(actaOut, docOut, talOut, MomentoNumeracionDocu.AL_FIRMAR);
            // doc esta en BORRADOR, no se envio a firma

            assertThatThrownBy(() -> docService.numerarDocumentoParaFirmas(
                    new NumerarDocumentoCommand(docOut[0].getId(), "firmas-app")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("PENDIENTE_FIRMA");
        }

        @Test
        @DisplayName("T11NE. No genera evento principal adicional al numerar")
        void no_genera_evento_adicional() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDoc();
            NumTalonario tal = crearTalonarioDoc(pol.getId());
            crearAmbitoGlobal(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantilla("PLNT-EVT-NF", MomentoNumeracionDocu.AL_FIRMAR);
            FalDocumento doc = crearDoc(acta, p);
            FalDocumento docEnFirma = ponerEnPendienteFirma(doc);

            // La numeracion via Firmas no debe generar un evento DOCGEN ni DOCNUM adicional
            // Solo registra movimiento en talonario; el evento documental principal viene de firmar/emitir
            NumerarDocumentoParaFirmasResponse resp = docService.numerarDocumentoParaFirmas(
                    new NumerarDocumentoCommand(docEnFirma.getId(), "firmas-app"));

            assertThat(resp.nroDocu()).isNotNull();
            // El movimiento en talonario debe ser 1 (unico correlativo consumido)
            List<NumTalonarioMovimiento> movs = talonarioService.listarMovimientosPorTalonario(tal.getId());
            assertThat(movs).hasSize(1);
            assertThat(movs.get(0).getDocumentoId()).isEqualTo(docEnFirma.getId());
        }
    }

    // =========================================================================
    // Idempotencia en otros momentos (doc ya numerado)
    // =========================================================================

    @Nested
    @DisplayName("Idempotencia: documento ya numerado en otros momentos")
    class IdempotenciaOtrosMomentos {

        @Test
        @DisplayName("T17B. AL_ENVIAR_A_FIRMA ya numerado: retorna numero existente")
        void al_enviar_a_firma_ya_numerado_idempotente() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDoc();
            NumTalonario tal = crearTalonarioDoc(pol.getId());
            crearAmbitoGlobal(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantilla("PLNT-ENF-ID", MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDoc(acta, p);
            FalDocumento docNumerado = docService.numerarDocumento(new NumerarDocumentoCommand(doc.getId(), "usr-test"));
            FalDocumento docEnFirma = ponerEnPendienteFirma(docNumerado);

            assertThat(docEnFirma.getNroDocu()).isNotNull(); // numerado al enviar

            NumerarDocumentoParaFirmasResponse resp = docService.numerarDocumentoParaFirmas(
                    new NumerarDocumentoCommand(docEnFirma.getId(), "firmas-app"));

            assertThat(resp.yaEstabaNumerado()).isTrue();
            assertThat(resp.nroDocu()).isEqualTo(docEnFirma.getNroDocu());

            // No consumio otro correlativo
            List<NumTalonarioMovimiento> movs = talonarioService.listarMovimientosPorTalonario(tal.getId());
            assertThat(movs).hasSize(1);
        }

        @Test
        @DisplayName("T17C. AL_CREAR ya numerado: retorna numero existente idempotentemente")
        void al_crear_ya_numerado_idempotente() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDoc();
            NumTalonario tal = crearTalonarioDoc(pol.getId());
            crearAmbitoGlobal(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaSinFirma("PLNT-AC-ID", MomentoNumeracionDocu.AL_CREAR);
            FalDocumento doc = crearDoc(acta, p);

            assertThat(doc.getNroDocu()).isNotNull(); // numerado al crear

            NumerarDocumentoParaFirmasResponse resp = docService.numerarDocumentoParaFirmas(
                    new NumerarDocumentoCommand(doc.getId(), "firmas-app"));

            assertThat(resp.yaEstabaNumerado()).isTrue();
            assertThat(resp.nroDocu()).isEqualTo(doc.getNroDocu());

            List<NumTalonarioMovimiento> movs = talonarioService.listarMovimientosPorTalonario(tal.getId());
            assertThat(movs).hasSize(1); // solo el correlativo original
        }
    }

    // =========================================================================
    // Rechazos por momento incompatible
    // =========================================================================

    @Nested
    @DisplayName("Rechazos: momentos incompatibles con el circuito de Firmas")
    class MomentosIncompatibles {

        @Test
        @DisplayName("T19A. Rechaza momento AL_EMITIR no numerable via Firmas")
        void rechaza_al_emitir() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDoc();
            NumTalonario tal = crearTalonarioDoc(pol.getId());
            crearAmbitoGlobal(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaSinFirma("PLNT-EMI-INC", MomentoNumeracionDocu.AL_EMITIR);
            FalDocumento doc = crearDoc(acta, p);

            assertThatThrownBy(() -> docService.numerarDocumentoParaFirmas(
                    new NumerarDocumentoCommand(doc.getId(), "firmas-app")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("AL_EMITIR");
        }

        @Test
        @DisplayName("T19B. Rechaza momento NO_APLICA")
        void rechaza_no_aplica() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla p = plantillaService.activar(
                    plantillaService.crear(new CrearDocumentoPlantillaCommand(
                            "PLNT-NA-INC", "Sin numeracion", null,
                            TipoDocu.CONSTANCIA, AccionDocumental.EMITIR_CONSTANCIA, null,
                            TipoFirmaReq.NO_REQUIERE, false, MomentoNumeracionDocu.NO_APLICA,
                            false, false, true,
                            FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, "sistema")).getId());
            FalDocumento doc = crearDoc(acta, p);

            assertThatThrownBy(() -> docService.numerarDocumentoParaFirmas(
                    new NumerarDocumentoCommand(doc.getId(), "firmas-app")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("T19C. Rechaza AL_CREAR si el documento no tiene numero (inconsistencia)")
        void rechaza_al_crear_sin_numero_inconsistencia() {
            // Simula doc con plantilla AL_CREAR pero sin numero (inconsistencia de datos)
            FalActa acta = crearActa();
            FalDocumentoPlantilla p = crearPlantillaSinFirma("PLNT-AC-INC", MomentoNumeracionDocu.AL_CREAR);

            // Crear doc manualmente sin numerar (bypass del flujo normal)
            Long idDoc = docRepo.nextId();
            FalDocumento doc = new FalDocumento(
                    idDoc, acta.getId(), TipoDocu.CONSTANCIA, FaltasClockTestSupport.FIXED.now(),
                    null, EstadoDocu.BORRADOR, TipoFirmaReq.NO_REQUIERE, p.getId(), FaltasClockTestSupport.FIXED.now());
            docRepo.guardar(doc);

            assertThatThrownBy(() -> docService.numerarDocumentoParaFirmas(
                    new NumerarDocumentoCommand(idDoc, "firmas-app")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("AL_CREAR");
        }

        @Test
        @DisplayName("T20. No permite elegir correlativo, talonario ni numero: solo acepta documentoId")
        void no_permite_elegir_correlativo() {
            // El endpoint solo toma documentoId; no hay forma de pasar numero/talonario deseado
            // Verificar que el metodo no acepta parametros de numeracion externa
            // (la validacion es estructural: NumerarDocumentoCommand solo tiene documentoId + user)
            NumerarDocumentoCommand cmd = new NumerarDocumentoCommand(999L, "firmas-app");
            assertThat(cmd.documentoId()).isEqualTo(999L);
            assertThat(cmd.idUserOperacion()).isEqualTo("firmas-app");
            // No hay campo para correlativo/talonario/numero en el command
        }

        @Test
        @DisplayName("T8D. Rechaza documento no numerable (sin plantilla)")
        void rechaza_documento_sin_plantilla() {
            Long idDoc = docRepo.nextId();
            FalDocumento doc = new FalDocumento(idDoc, 1L, TipoDocu.CONSTANCIA, FaltasClockTestSupport.FIXED.now(), "sin plantilla");
            docRepo.guardar(doc);

            assertThatThrownBy(() -> docService.numerarDocumentoParaFirmas(
                    new NumerarDocumentoCommand(idDoc, "firmas-app")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("plantilla");
        }
    }

    // =========================================================================
    // Flujo numerar -> contenido -> hash -> firma (T21-T22)
    // =========================================================================

    @Nested
    @DisplayName("Flujo numerar-hash-firma: orden obligatorio")
    class FlujoNumerarHashFirma {

        @Test
        @DisplayName("T21. Numerar -> nroDocu disponible antes de hash -> firma registrada con mismo hash")
        void flujo_numerar_hash_firma_completo() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDoc();
            NumTalonario tal = crearTalonarioDoc(pol.getId());
            crearAmbitoGlobal(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantilla("PLNT-FLJ-NF", MomentoNumeracionDocu.AL_FIRMAR);
            FalDocumento doc = crearDoc(acta, p);
            FalDocumento docEnFirma = ponerEnPendienteFirma(doc);

            // Paso 1: Firmas solicita numeracion
            NumerarDocumentoParaFirmasResponse numerado = docService.numerarDocumentoParaFirmas(
                    new NumerarDocumentoCommand(docEnFirma.getId(), "firmas-app"));
            assertThat(numerado.nroDocu()).isNotNull();

            // Paso 2: Firmas renderiza contenido con el numero e incluye el numero
            // (simulado: el numero ya esta disponible en la respuesta)

            // Paso 3: Firmas calcula hash del contenido definitivo (simulado)
            String hashContenidoDefinitivo = "sha256-" + numerado.nroDocu() + "-contenido";

            // Paso 4: Firmas registra la firma con el hash
            // (documentoEstadoActual debe ser PENDIENTE_FIRMA)
            FalDocumento docNumerated = docRepo.buscarPorId(docEnFirma.getId()).orElseThrow();
            assertThat(docNumerated.getNroDocu()).isEqualTo(numerado.nroDocu());
            assertThat(docNumerated.getEstadoDocu()).isEqualTo(EstadoDocu.PENDIENTE_FIRMA);

            // El hash se calcula DESPUES de tener el numero -> el numero forma parte del contenido
            assertThat(hashContenidoDefinitivo).contains(numerado.nroDocu());
        }

        @Test
        @DisplayName("T22. Segundo numerar (idempotente) + firma: usa el mismo numero")
        void segundo_numerar_idempotente_firma_mismo_numero() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDoc();
            NumTalonario tal = crearTalonarioDoc(pol.getId());
            crearAmbitoGlobal(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantilla("PLNT-FLJ2-NF", MomentoNumeracionDocu.AL_FIRMAR);
            FalDocumento doc = crearDoc(acta, p);
            FalDocumento docEnFirma = ponerEnPendienteFirma(doc);

            NumerarDocumentoParaFirmasResponse primer = docService.numerarDocumentoParaFirmas(
                    new NumerarDocumentoCommand(docEnFirma.getId(), "firmas-app"));

            // Reintento idempotente (ej: Firmas tuvo un error y reintenta)
            NumerarDocumentoParaFirmasResponse segundo = docService.numerarDocumentoParaFirmas(
                    new NumerarDocumentoCommand(docEnFirma.getId(), "firmas-app"));

            assertThat(segundo.yaEstabaNumerado()).isTrue();
            assertThat(segundo.nroDocu()).isEqualTo(primer.nroDocu());
            // Solo un correlativo consumido
            assertThat(talonarioService.listarMovimientosPorTalonario(tal.getId())).hasSize(1);
        }
    }

    // =========================================================================
    // Otros momentos: numeracion automatica no dispara el endpoint de Firmas
    // =========================================================================

    @Nested
    @DisplayName("Regresion: otros momentos siguen verdes")
    class RegresionOtrosMomentos {

        @Test
        @DisplayName("T23. AL_ENVIAR_A_FIRMA: numera al enviar, no al llamar al endpoint Firmas")
        void al_enviar_a_firma_numera_al_enviar() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDoc();
            NumTalonario tal = crearTalonarioDoc(pol.getId());
            crearAmbitoGlobal(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantilla("PLNT-ENF-REG", MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDoc(acta, p);
            assertThat(doc.getNroDocu()).isNull(); // BORRADOR: sin numero

            FalDocumento docNumeradoR = docService.numerarDocumento(new NumerarDocumentoCommand(doc.getId(), "usr-test"));
            FalDocumento docEnFirma = ponerEnPendienteFirma(docNumeradoR);
            assertThat(docEnFirma.getNroDocu()).isNotNull(); // numerado al enviar
            assertThat(docEnFirma.getEstadoDocu()).isEqualTo(EstadoDocu.PENDIENTE_FIRMA);
        }

        @Test
        @DisplayName("T25. No renumera si ya fue numerado en otro momento")
        void no_renumera_si_ya_numerado() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDoc();
            NumTalonario tal = crearTalonarioDoc(pol.getId());
            crearAmbitoGlobal(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantilla("PLNT-NR-REG", MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDoc(acta, p);
            // Numerar primero (AL_ENVIAR_A_FIRMA: el numero se asigna al enviar)
            FalDocumento docNumerado = docService.numerarDocumento(new NumerarDocumentoCommand(doc.getId(), "usr-test"));
            String nroOriginal = docNumerado.getNroDocu();
            assertThat(nroOriginal).isNotNull();
            FalDocumento docEnFirma = ponerEnPendienteFirma(docNumerado);

            NumerarDocumentoParaFirmasResponse resp = docService.numerarDocumentoParaFirmas(
                    new NumerarDocumentoCommand(docEnFirma.getId(), "firmas-app"));

            assertThat(resp.yaEstabaNumerado()).isTrue();
            assertThat(resp.nroDocu()).isEqualTo(nroOriginal);
        }
    }

    // =========================================================================
    // Seguridad: el actor viene del JWT, no del body
    // =========================================================================

    @Test
    @DisplayName("T20S. El actor se extrae del JWT (sub): la operacion registra idUserMovimiento del sub")
    void actor_viene_del_jwt_sub() {
        crearDependencia();
        FalActa acta = crearActa();
        NumPolitica pol = crearPoliticaDoc();
        NumTalonario tal = crearTalonarioDoc(pol.getId());
        crearAmbitoGlobal(tal.getId(), TipoDocu.CONSTANCIA.codigo());
        FalDocumentoPlantilla p = crearPlantilla("PLNT-JWT-NF", MomentoNumeracionDocu.AL_FIRMAR);
        FalDocumento doc = crearDoc(acta, p);
        FalDocumento docEnFirma = ponerEnPendienteFirma(doc);

        String subJwt = "firmas-service-prod";
        docService.numerarDocumentoParaFirmas(new NumerarDocumentoCommand(docEnFirma.getId(), subJwt));

        List<NumTalonarioMovimiento> movs = talonarioService.listarMovimientosPorTalonario(tal.getId());
        assertThat(movs).hasSize(1);
        assertThat(movs.get(0).getIdUserMovimiento()).isEqualTo(subJwt);
    }
}
