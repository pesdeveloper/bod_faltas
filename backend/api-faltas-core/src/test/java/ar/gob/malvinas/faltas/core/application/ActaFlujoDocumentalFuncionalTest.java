package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoCombinacionService;
import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoVariableRegistry;
import ar.gob.malvinas.faltas.core.application.command.*;
import ar.gob.malvinas.faltas.core.application.demo.*;
import ar.gob.malvinas.faltas.core.application.result.*;
import ar.gob.malvinas.faltas.core.application.service.*;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 8F-4C: Pruebas funcionales del circuito documental.
 *
 * Cubre:
 *   ACT-003-DOC-PENDIENTE-FIRMA      - Documento generado, pendiente firma
 *   ACT-023-REDACCION-BORRADOR       - Redaccion en BORRADOR
 *   ACT-024-PDF-MOCK-GENERADO        - PDF mock generado (redaccion CONFIRMADA)
 *   ACT-025-PRECONDICION-VIOLADA     - Guardrails: PrecondicionVioladaException
 *   ACT-027-DOC-ADJUNTO-CONVALIDADO  - Documento adjunto escaneado + convalidacion
 */
@DisplayName("8F-4C: ActaFlujoDocumentalFuncional")
class ActaFlujoDocumentalFuncionalTest {

    // =========================================================================
    // Contexto completo con redaccion + generacion mock
    // =========================================================================

    private ActaRepository actaRepo;
    private ActaEventoRepository eventoRepo;
    private ActaSnapshotRepository snapshotRepo;
    private DocumentoRepository docRepo;
    private DocumentoFirmaRepository firmaRepo;
    private NotificacionRepository notifRepo;
    private FalloActaRepository falloRepo;
    private DocumentoPlantillaRepository plantillaRepo;
    private DocumentoPlantillaContenidoRepository contenidoRepo;
    private DocumentoPlantillaDefaultRepository defaultRepo;
    private DocumentoRedaccionRepository redaccionRepo;

    private ActaService actaService;
    private DocumentoService docService;
    private NotificacionService notifService;
    private FalloActaService falloService;
    private DocumentoRedaccionService redaccionService;
    private DocumentoGeneracionMockService mockGenService;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        docRepo = new InMemoryDocumentoRepository();
        firmaRepo = new InMemoryDocumentoFirmaRepository();
        notifRepo = new InMemoryNotificacionRepository();
        falloRepo = new InMemoryFalloActaRepository();
        plantillaRepo = new InMemoryDocumentoPlantillaRepository();
        contenidoRepo = new InMemoryDocumentoPlantillaContenidoRepository();
        defaultRepo = new InMemoryDocumentoPlantillaDefaultRepository();
        redaccionRepo = new InMemoryDocumentoRedaccionRepository();

        PagoVoluntarioRepository pagoVolRepo = new InMemoryPagoVoluntarioRepository();
        PagoCondenaRepository pagoCondRepo = new InMemoryPagoCondenaRepository();
        ApelacionActaRepository apelRepo = new InMemoryApelacionActaRepository();

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoVolRepo, falloRepo, apelRepo, pagoCondRepo, FaltasClockTestSupport.FIXED);

        actaService = new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc,
                new InMemoryActaEvidenciaRepository(), FaltasClockTestSupport.FIXED);

        docService = new DocumentoService(
                actaRepo, docRepo, firmaRepo, eventoRepo, snapshotRepo, recalc, falloRepo,
                plantillaRepo,
                new TalonarioService(new InMemoryTalonarioRepository(),
                        new InMemoryDependenciaRepository(), new InMemoryInspectorRepository(), FaltasClockTestSupport.FIXED),
                new InMemoryDependenciaRepository(),
                new InMemoryDocumentoFirmaReqRepository(),
                new InMemoryFirmanteRepository(),
                new InMemoryNotificacionRepository(), FaltasClockTestSupport.FIXED);

        notifService = new NotificacionService(
                actaRepo, docRepo, notifRepo, eventoRepo, snapshotRepo, recalc,
                falloRepo, new NoOpBloqueantesMaterialesChecker(), FaltasClockTestSupport.FIXED, new ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionIntentoRepository(), new ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaDomicilioRepository());

        falloService = new FalloActaService(
                actaRepo, eventoRepo, snapshotRepo, docRepo, falloRepo, pagoVolRepo, recalc, FaltasClockTestSupport.FIXED);

        DocumentoCombinacionService combinacion =
                new DocumentoCombinacionService(new DocumentoVariableRegistry());
        DocumentoPlantillaDefaultService defaultSvc =
                new DocumentoPlantillaDefaultService(defaultRepo);

        redaccionService = new DocumentoRedaccionService(
                docRepo, defaultSvc, contenidoRepo, redaccionRepo, combinacion,
                actaRepo, falloRepo, pagoVolRepo, FaltasClockTestSupport.FIXED);

        mockGenService = new DocumentoGeneracionMockService(
                redaccionRepo, docRepo, new DocumentoPdfMockRenderer(FaltasClockTestSupport.FIXED), FaltasClockTestSupport.FIXED);

        // Seed plantillas mock
        PlantillasMockSeeder.seedar(plantillaRepo, contenidoRepo, defaultRepo);
    }

    private Long labrarCapturarEnriquecer() {
        Long id = actaService.labrar(new LabrarActaCommand(
                "TRANSITO", "DEP-01", "INS-001",
                LocalDate.of(2024, 3, 15),
                "Av. Pioneros 2345", "Belgrano 200",
                null, null, null,
                "Juan Carlos Perez", "12345678",
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null)).idActa();
        actaService.completarCaptura(new CompletarCapturaCommand(id, null));
        actaService.enriquecer(new EnriquecerActaCommand(id, null));
        return id;
    }

    private Long labrarYLlegarAAnalis() {
        Long id = labrarCapturarEnriquecer();
        Long docId = Long.parseLong(docService.generarDocumento(
                new GenerarDocumentoCommand(id, TipoDocu.ACTA_INFRACCION, null))
                .idEntidadAfectada());
        docService.firmarDocumento(new FirmarDocumentoCommand(docId, "Inspector", "DIGITAL", null));
        String notifId = notifService.enviarNotificacion(
                new EnviarNotificacionCommand(id, docId, CanalNotificacion.PRESENCIAL, null, null, null, "test-user")).idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(notifId, null));
        return id;
    }

    // =========================================================================
    // ACT-003
    // =========================================================================

    @Nested
    @DisplayName("ACT-003-DOC-PENDIENTE-FIRMA")
    class Act003 {

        @Test
        @DisplayName("Documento generado en ENRI: bandeja PENDIENTE_FIRMA, evento DOCGEN")
        void documento_generado_pendiente_firma() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-003-DOC-PENDIENTE-FIRMA");

            assertThat(res.ejecutado()).isTrue();

            FalActaSnapshot snap = runner.getSnapshotRepo().buscarPorActa(res.actaId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_FIRMA);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.DOCGEN);
            assertThat(tipos).doesNotContain(TipoEventoActa.DOCFIR);
        }
    }

    // =========================================================================
    // ACT-023
    // =========================================================================

    @Nested
    @DisplayName("ACT-023-REDACCION-BORRADOR")
    class Act023 {

        @Test
        @DisplayName("Redaccion creada en BORRADOR: storageKey y hashDocu son null")
        void redaccion_borrador_storage_null() {
            Long actaId = labrarYLlegarAAnalis();
            Long docId = Long.parseLong(docService.generarDocumento(
                    new GenerarDocumentoCommand(actaId, TipoDocu.ACTO_ADMINISTRATIVO, null))
                    .idEntidadAfectada());

            DocumentoRedaccionResponse resp = redaccionService.crearRedaccionConContextoActa(
                    actaId, docId, AccionDocumental.EMITIR_FALLO,
                    TipoActa.TRANSITO, 1L, (short) 1, "user-test");

            assertThat(resp).isNotNull();
            assertThat(resp.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.BORRADOR);
            assertThat(resp.contenidoEditable()).isNotNull().isNotBlank();

            FalDocumento doc = docRepo.buscarPorId(docId).orElseThrow();
            assertThat(doc.getStorageKey()).isNull();
            assertThat(doc.getHashDocu()).isNull();
        }

        @Test
        @DisplayName("Redaccion BORRADOR tiene variables combinadas del contexto del acta")
        void redaccion_borrador_variables_combinadas() {
            Long actaId = labrarYLlegarAAnalis();
            Long docId = Long.parseLong(docService.generarDocumento(
                    new GenerarDocumentoCommand(actaId, TipoDocu.ACTO_ADMINISTRATIVO, null))
                    .idEntidadAfectada());

            DocumentoRedaccionResponse resp = redaccionService.crearRedaccionConContextoActa(
                    actaId, docId, AccionDocumental.EMITIR_FALLO,
                    TipoActa.TRANSITO, 1L, (short) 1, "user-test");

            assertThat(resp.variablesUsadas()).isNotNull();
        }
    }

    // =========================================================================
    // ACT-024
    // =========================================================================

    @Nested
    @DisplayName("ACT-024-PDF-MOCK-GENERADO")
    class Act024 {

        @Test
        @DisplayName("Redaccion confirmada: storageKey contiene mock://, hashDocu contiene sha256-mock-")
        void pdf_mock_generado_storage_mock() {
            Long actaId = labrarYLlegarAAnalis();
            Long docId = Long.parseLong(docService.generarDocumento(
                    new GenerarDocumentoCommand(actaId, TipoDocu.ACTO_ADMINISTRATIVO, null))
                    .idEntidadAfectada());

            DocumentoRedaccionResponse redaccion = redaccionService.crearRedaccionConContextoActa(
                    actaId, docId, AccionDocumental.EMITIR_FALLO,
                    TipoActa.TRANSITO, 1L, (short) 1, "user-test");

            DocumentoGeneracionMockResponse mockResp = mockGenService.confirmarYGenerarMockPdf(
                    new ConfirmarRedaccionYGenerarDocumentoMockCommand(
                            redaccion.id(), "user-test", null));

            assertThat(mockResp).isNotNull();
            assertThat(mockResp.storageKey()).contains("mock://");
            assertThat(mockResp.hashDocu()).contains("sha256-mock-");

            FalDocumentoRedaccion redaccionFinal = redaccionRepo.buscarPorId(redaccion.id()).orElseThrow();
            assertThat(redaccionFinal.getEstadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.CONFIRMADA);
        }
    }

    // =========================================================================
    // ACT-025
    // =========================================================================

    @Nested
    @DisplayName("ACT-025-PRECONDICION-VIOLADA")
    class Act025 {

        @Test
        @DisplayName("DictarFallo en bloque CAPT lanza PrecondicionVioladaException")
        void fallo_en_capt_lanza_precondicion() {
            Long actaId = actaService.labrar(new LabrarActaCommand(
                    "TRANSITO", "DEP-01", "INS-001",
                    LocalDate.of(2024, 1, 1), "Dom1", "Dom2",
                    null, null, null, "Test", "99999999",
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null)).idActa();

            assertThatThrownBy(() ->
                    falloService.dictarAbsolutorio(new DictarFalloAbsolutorioCommand(actaId, "test", null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("Notificar sin documento firmado lanza PrecondicionVioladaException")
        void notificar_sin_firma_lanza_precondicion() {
            Long actaId = labrarCapturarEnriquecer();
            Long docId = Long.parseLong(docService.generarDocumento(
                    new GenerarDocumentoCommand(actaId, TipoDocu.ACTA_INFRACCION, null))
                    .idEntidadAfectada());

            assertThatThrownBy(() ->
                    notifService.enviarNotificacion(
                            new EnviarNotificacionCommand(actaId, docId, CanalNotificacion.PRESENCIAL, null, null, null, "test-user")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("Runner verifica guardrail: PrecondicionVioladaException al dictar fallo en CAPT")
        void runner_verifica_guardrail_fallo_en_capt() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-025-PRECONDICION-VIOLADA");

            assertThat(res.ejecutado()).isTrue();
            assertThat(res.pasosEjecutados())
                    .anyMatch(p -> p.contains("PrecondicionVioladaException") || p.contains("guardrail"));
        }
    }

    // =========================================================================
    // ACT-027
    // =========================================================================

    @Nested
    @DisplayName("ACT-027-DOC-ADJUNTO-CONVALIDADO")
    class Act027 {

        @Test
        @DisplayName("Documento adjunto escaneado: evento DOCADJ, bloque ENRI")
        void adjunto_escaneado_evento_docadj() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-027-DOC-ADJUNTO-CONVALIDADO");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ENRI);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.DOCADJ);
        }

        @Test
        @DisplayName("Adjunto directo con incorporarDocumentoEscaneado: storageKey y hashDocu presentes")
        void adjunto_con_storage_key_y_hash() {
            Long actaId = labrarCapturarEnriquecer();

            docService.incorporarDocumentoEscaneado(new IncorporarDocumentoEscaneadoCommand(
                    actaId, TipoDocu.ACTA_INFRACCION,
                    "storage://adjuntos/test-027.pdf",
                    "sha256-test-hash-027",
                    "inspector-001", null));

            List<FalDocumento> docs = docRepo.buscarPorActa(actaId);
            assertThat(docs).hasSize(1);
            FalDocumento doc = docs.get(0);
            assertThat(doc.getStorageKey()).isEqualTo("storage://adjuntos/test-027.pdf");
            assertThat(doc.getHashDocu()).isEqualTo("sha256-test-hash-027");
        }
    }
}
