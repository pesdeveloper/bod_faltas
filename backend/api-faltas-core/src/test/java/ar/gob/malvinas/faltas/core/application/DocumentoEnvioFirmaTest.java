package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.AgregarFirmaReqPlantillaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearDependenciaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearDocumentoPlantillaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearPoliticaNumeracionCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioAmbitoCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioCommand;
import ar.gob.malvinas.faltas.core.application.command.EnviarAFirmaCommand;
import ar.gob.malvinas.faltas.core.application.service.DependenciaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoPlantillaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.MomentoNumeracionDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalDependencia;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantilla;
import ar.gob.malvinas.faltas.core.domain.model.NumPolitica;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonario;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaReqRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del Micro-slice 8C-5B: Enviar documento a firma con numeracion automatica cuando corresponda.
 *
 * Verifica:
 * - Transicion BORRADOR -> PENDIENTE_FIRMA.
 * - Numeracion automatica si momentoNumeracionDocu = AL_ENVIAR_A_FIRMA.
 * - Sin numeracion si AL_FIRMAR, NO_APLICA o AL_EMITIR.
 * - Error si AL_CREAR y documento no tiene nroDocu (inconsistencia).
 * - Materializacion de firma_req automatica si no existia.
 * - Sin re-materializacion si ya existia.
 * - Validaciones de estado, plantilla, tipoFirmaReq.
 */
@DisplayName("Micro-slice 8C-5B: Enviar documento a firma")
class DocumentoEnvioFirmaTest {

    private static final String DEP_COD = "DEP-001";
    private static final String USER = "tester";

    private InMemoryActaRepository actaRepo;
    private DocumentoRepository docRepo;
    private DocumentoPlantillaRepository plantillaRepo;
    private TalonarioRepository talonarioRepo;
    private DependenciaRepository depRepo;
    private InspectorRepository inspectorRepo;
    private DocumentoFirmaReqRepository firmaReqRepo;

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
        inspectorRepo = new InMemoryInspectorRepository();
        firmaReqRepo = new InMemoryDocumentoFirmaReqRepository();

        InMemoryActaEventoRepository eventoRepo = new InMemoryActaEventoRepository();
        InMemoryActaSnapshotRepository snapshotRepo = new InMemoryActaSnapshotRepository();

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo,
                new InMemoryNotificacionRepository(),
                new InMemoryPagoVoluntarioRepository(),
                new InMemoryFalloActaRepository(),
                new InMemoryApelacionActaRepository(),
                new InMemoryPagoCondenaRepository(), FaltasClockTestSupport.FIXED, snapshotRepo);

        talonarioService = new TalonarioService(talonarioRepo, depRepo, inspectorRepo, FaltasClockTestSupport.FIXED);
        depService = new DependenciaService(depRepo, FaltasClockTestSupport.FIXED);
        plantillaService = new DocumentoPlantillaService(plantillaRepo, FaltasClockTestSupport.FIXED);

        docService = new DocumentoService(
                actaRepo, docRepo, new InMemoryDocumentoFirmaRepository(),
                eventoRepo, snapshotRepo, recalc, new InMemoryFalloActaRepository(),
                plantillaRepo, talonarioService, depRepo, firmaReqRepo,
                new ar.gob.malvinas.faltas.core.repository.memory.InMemoryFirmanteRepository(),
                new InMemoryNotificacionRepository(), FaltasClockTestSupport.FIXED);
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
                DEP_COD, "Dep Uno", null, TipoActa.TRANSITO,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(30), "sistema"));
    }

    private NumPolitica crearPoliticaDoc() {
        return talonarioService.crearPolitica(new CrearPoliticaNumeracionCommand(
                "POL-DOC-5B", "Politica doc 8C-5B", ClaseNumeracion.DOCUMENTO,
                false, false, null, false, null, false, null,
                "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, "sistema"));
    }

    private NumTalonario crearTalonarioDocGlobal(Long politicaId) {
        return talonarioService.crearTalonario(new CrearTalonarioCommand(
                politicaId, "TAL-DOC-5B", "Talonario doc 8C-5B",
                TipoTalonario.ELECTRONICO, ClaseNumeracion.DOCUMENTO,
                null, null, 1, 9999, "seq_doc_5b",
                true, false, null, null, "sistema"));
    }

    private void crearAmbitoGlobalDocumento(Long talonarioId, Short tipoDocu) {
        talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                talonarioId, ClaseNumeracion.DOCUMENTO,
                tipoDocu, null, null, null,
                AlcanceTalonario.GLOBAL,
                (short) 10,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, true, "sistema"));
    }

    /**
     * Crea plantilla con tipoFirmaReq = FIRMA_AUTORIDAD y un firma_req obligatorio en plantilla.
     */
    private FalDocumentoPlantilla crearPlantillaConFirmaReq(String codigo, MomentoNumeracionDocu momento,
            @SuppressWarnings("unused") boolean siNumeracionIgnorado) {
        boolean siNumeracion = momento != MomentoNumeracionDocu.NO_APLICA;
        FalDocumentoPlantilla p = plantillaService.crear(new CrearDocumentoPlantillaCommand(
                codigo, "Plantilla " + codigo,
                TipoDocu.ACTO_ADMINISTRATIVO, AccionDocumental.EMITIR_FALLO, null,
                TipoFirmaReq.FIRMA_AUTORIDAD,
                siNumeracion, momento,
                false, false, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, "sistema"));
        plantillaService.agregarFirmaReq(new AgregarFirmaReqPlantillaCommand(
                p.getId(), (short) 1, (short) 1, null, true, true, "sistema"));
        return plantillaService.activar(p.getId());
    }

    /**
     * Crea plantilla con tipoFirmaReq = NO_REQUIERE (para pruebas de rechazo).
     */
    private FalDocumentoPlantilla crearPlantillaNoRequiereFirma(String codigo) {
        FalDocumentoPlantilla p = plantillaService.crear(new CrearDocumentoPlantillaCommand(
                codigo, "Plantilla sin firma " + codigo,
                TipoDocu.CONSTANCIA, AccionDocumental.EMITIR_CONSTANCIA, null,
                TipoFirmaReq.NO_REQUIERE,
                false, MomentoNumeracionDocu.NO_APLICA,
                false, false, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, "sistema"));
        return plantillaService.activar(p.getId());
    }

    /**
     * Crea un documento en BORRADOR con la plantilla dada, directamente en el repositorio.
     */
    private FalDocumento crearDocumentoBorrador(FalActa acta, FalDocumentoPlantilla plantilla) {
        Long id = docRepo.nextId();
        FalDocumento doc = new FalDocumento(
                id, acta.getId(), plantilla.getTipoDocu(), FaltasClockTestSupport.FIXED.now(),
                EstadoDocu.BORRADOR, plantilla.getTipoFirmaReq(), plantilla.getId(), FaltasClockTestSupport.FIXED.now());
        docRepo.guardar(doc);
        return doc;
    }

    /**
     * Crea un documento en BORRADOR ya numerado (simula AL_CREAR satisfecho).
     */
    private FalDocumento crearDocumentoBorradorNumerado(FalActa acta, FalDocumentoPlantilla plantilla,
            String nroDocu) {
        Long id = docRepo.nextId();
        FalDocumento doc = new FalDocumento(
                id, acta.getId(), plantilla.getTipoDocu(), FaltasClockTestSupport.FIXED.now(),
                EstadoDocu.BORRADOR, plantilla.getTipoFirmaReq(), plantilla.getId(), FaltasClockTestSupport.FIXED.now());
        doc.setNroDocu(nroDocu);
        docRepo.guardar(doc);
        return doc;
    }

    // =========================================================================
    // Casos validos
    // =========================================================================

    @Nested
    @DisplayName("Casos validos - transicion BORRADOR -> PENDIENTE_FIRMA")
    class CasosValidos {

        @Test
        @DisplayName("AL_FIRMAR: envia sin numerar, estado queda PENDIENTE_FIRMA")
        void alFirmar_enviaSinNumerar() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaConFirmaReq(
                    "PL-FIRMA-1", MomentoNumeracionDocu.AL_FIRMAR, false);
            FalDocumento doc = crearDocumentoBorrador(acta, plantilla);

            FalDocumento result = docService.enviarAFirma(
                    new EnviarAFirmaCommand(doc.getId(), USER));

            assertThat(result.getEstadoDocu()).isEqualTo(EstadoDocu.PENDIENTE_FIRMA);
            assertThat(result.getNroDocu()).isNull();
            assertThat(result.getIdTalonario()).isNull();
        }

        @Test
        @DisplayName("NO_APLICA: envia sin numerar, estado queda PENDIENTE_FIRMA")
        void noAplica_enviaSinNumerar() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaConFirmaReq(
                    "PL-FIRMA-2", MomentoNumeracionDocu.NO_APLICA, false);
            FalDocumento doc = crearDocumentoBorrador(acta, plantilla);

            FalDocumento result = docService.enviarAFirma(
                    new EnviarAFirmaCommand(doc.getId(), USER));

            assertThat(result.getEstadoDocu()).isEqualTo(EstadoDocu.PENDIENTE_FIRMA);
            assertThat(result.getNroDocu()).isNull();
        }

        @Test
        @DisplayName("AL_EMITIR: envia sin numerar, estado queda PENDIENTE_FIRMA")
        void alEmitir_enviaSinNumerar() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaConFirmaReq(
                    "PL-FIRMA-3", MomentoNumeracionDocu.AL_EMITIR, false);
            FalDocumento doc = crearDocumentoBorrador(acta, plantilla);

            FalDocumento result = docService.enviarAFirma(
                    new EnviarAFirmaCommand(doc.getId(), USER));

            assertThat(result.getEstadoDocu()).isEqualTo(EstadoDocu.PENDIENTE_FIRMA);
            assertThat(result.getNroDocu()).isNull();
        }

        @Test
        @DisplayName("AL_ENVIAR_A_FIRMA: numera automaticamente y cambia a PENDIENTE_FIRMA")
        void alEnviarAFirma_numeraYCambiaEstado() {
            crearDependencia();
            NumPolitica pol = crearPoliticaDoc();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.ACTO_ADMINISTRATIVO.codigo());

            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaConFirmaReq(
                    "PL-FIRMA-4", MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA, true);
            FalDocumento doc = crearDocumentoBorrador(acta, plantilla);

            FalDocumento result = docService.enviarAFirma(
                    new EnviarAFirmaCommand(doc.getId(), USER));

            assertThat(result.getEstadoDocu()).isEqualTo(EstadoDocu.PENDIENTE_FIRMA);
            assertThat(result.getNroDocu()).isNotNull().isNotBlank();
            assertThat(result.getIdTalonario()).isNotNull();
            assertThat(result.getNroTalonarioUsado()).isGreaterThan(0);
        }

        @Test
        @DisplayName("AL_CREAR ya numerado: envia sin re-numerar, estado PENDIENTE_FIRMA")
        void alCrearYaNumerado_enviaSinReNumerar() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaConFirmaReq(
                    "PL-FIRMA-5", MomentoNumeracionDocu.AL_CREAR, true);
            FalDocumento doc = crearDocumentoBorradorNumerado(acta, plantilla, "DOC-001");

            FalDocumento result = docService.enviarAFirma(
                    new EnviarAFirmaCommand(doc.getId(), USER));

            assertThat(result.getEstadoDocu()).isEqualTo(EstadoDocu.PENDIENTE_FIRMA);
            assertThat(result.getNroDocu()).isEqualTo("DOC-001");
        }

        @Test
        @DisplayName("firma_req se materializa automaticamente al enviar a firma")
        void firmaReq_seMaterializaAutomaticamente() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaConFirmaReq(
                    "PL-FIRMA-6", MomentoNumeracionDocu.AL_FIRMAR, false);
            FalDocumento doc = crearDocumentoBorrador(acta, plantilla);

            assertThat(firmaReqRepo.existePorDocumento(doc.getId())).isFalse();

            docService.enviarAFirma(new EnviarAFirmaCommand(doc.getId(), USER));

            assertThat(firmaReqRepo.existePorDocumento(doc.getId())).isTrue();
            assertThat(firmaReqRepo.listarPorDocumento(doc.getId())).hasSize(1);
        }

        @Test
        @DisplayName("firma_req no se duplica si ya estaba materializada")
        void firmaReq_noSeDuplicaSiYaExiste() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaConFirmaReq(
                    "PL-FIRMA-7", MomentoNumeracionDocu.AL_FIRMAR, false);
            FalDocumento doc = crearDocumentoBorrador(acta, plantilla);

            // Pre-materialize manually
            ar.gob.malvinas.faltas.core.application.service.DocumentoFirmaReqService firmaReqSvc =
                    new ar.gob.malvinas.faltas.core.application.service.DocumentoFirmaReqService(
                            docRepo, plantillaRepo, firmaReqRepo, FaltasClockTestSupport.FIXED);
            firmaReqSvc.materializarDesdePlantilla(
                    new ar.gob.malvinas.faltas.core.application.command.MaterializarFirmaReqDocumentoCommand(
                            doc.getId(), USER));

            int countBefore = firmaReqRepo.listarPorDocumento(doc.getId()).size();

            docService.enviarAFirma(new EnviarAFirmaCommand(doc.getId(), USER));

            assertThat(firmaReqRepo.listarPorDocumento(doc.getId())).hasSize(countBefore);
        }

        @Test
        @DisplayName("documento queda en PENDIENTE_FIRMA persistido en repositorio")
        void documentoPersisteEnPendienteFirma() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaConFirmaReq(
                    "PL-FIRMA-8", MomentoNumeracionDocu.AL_FIRMAR, false);
            FalDocumento doc = crearDocumentoBorrador(acta, plantilla);

            docService.enviarAFirma(new EnviarAFirmaCommand(doc.getId(), USER));

            FalDocumento persisted = docRepo.buscarPorId(doc.getId()).orElseThrow();
            assertThat(persisted.getEstadoDocu()).isEqualTo(EstadoDocu.PENDIENTE_FIRMA);
        }
    }

    // =========================================================================
    // Validaciones de entrada
    // =========================================================================

    @Nested
    @DisplayName("Validaciones de entrada")
    class ValidacionesEntrada {

        @Test
        @DisplayName("documentoId null falla con PrecondicionVioladaException")
        void documentoIdNull_falla() {
            assertThatThrownBy(() -> docService.enviarAFirma(new EnviarAFirmaCommand(null, USER)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("documentoId");
        }

        @Test
        @DisplayName("idUserOperacion null falla con PrecondicionVioladaException")
        void idUserOperacionNull_falla() {
            assertThatThrownBy(() -> docService.enviarAFirma(new EnviarAFirmaCommand(1L, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idUserOperacion");
        }

        @Test
        @DisplayName("idUserOperacion blank falla con PrecondicionVioladaException")
        void idUserOperacionBlank_falla() {
            assertThatThrownBy(() -> docService.enviarAFirma(new EnviarAFirmaCommand(1L, "   ")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idUserOperacion");
        }

        @Test
        @DisplayName("documento no encontrado falla con DocumentoNoEncontradoException")
        void documentoNoEncontrado_falla() {
            assertThatThrownBy(() -> docService.enviarAFirma(new EnviarAFirmaCommand(9999L, USER)))
                    .isInstanceOf(DocumentoNoEncontradoException.class);
        }
    }

    // =========================================================================
    // Validaciones de estado y precondiciones
    // =========================================================================

    @Nested
    @DisplayName("Validaciones de estado y precondiciones")
    class ValidacionesPrecondicion {

        @Test
        @DisplayName("documento no BORRADOR (PENDIENTE_FIRMA) falla")
        void docNoBorrador_pendienteFirma_falla() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaConFirmaReq(
                    "PL-ERR-1", MomentoNumeracionDocu.AL_FIRMAR, false);
            Long id = docRepo.nextId();
            FalDocumento doc = new FalDocumento(
                    id, acta.getId(), plantilla.getTipoDocu(), FaltasClockTestSupport.FIXED.now(),
                EstadoDocu.PENDIENTE_FIRMA, plantilla.getTipoFirmaReq(), plantilla.getId(), FaltasClockTestSupport.FIXED.now());
            docRepo.guardar(doc);

            assertThatThrownBy(() -> docService.enviarAFirma(new EnviarAFirmaCommand(id, USER)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("BORRADOR");
        }

        @Test
        @DisplayName("documento FIRMADO falla al intentar enviar a firma")
        void docFirmado_falla() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaConFirmaReq(
                    "PL-ERR-2", MomentoNumeracionDocu.AL_FIRMAR, false);
            Long id = docRepo.nextId();
            FalDocumento doc = new FalDocumento(
                    id, acta.getId(), plantilla.getTipoDocu(), FaltasClockTestSupport.FIXED.now(),
                EstadoDocu.FIRMADO, plantilla.getTipoFirmaReq(), plantilla.getId(), FaltasClockTestSupport.FIXED.now());
            docRepo.guardar(doc);

            assertThatThrownBy(() -> docService.enviarAFirma(new EnviarAFirmaCommand(id, USER)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("BORRADOR");
        }

        @Test
        @DisplayName("documento sin plantillaId falla")
        void docSinPlantilla_falla() {
            FalActa acta = crearActa();
            Long id = docRepo.nextId();
            FalDocumento doc = new FalDocumento(
                    id, acta.getId(), TipoDocu.CONSTANCIA, FaltasClockTestSupport.FIXED.now(),
                EstadoDocu.BORRADOR, TipoFirmaReq.FIRMA_AUTORIDAD, null, FaltasClockTestSupport.FIXED.now());
            docRepo.guardar(doc);

            assertThatThrownBy(() -> docService.enviarAFirma(new EnviarAFirmaCommand(id, USER)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("plantilla");
        }

        @Test
        @DisplayName("tipoFirmaReq=NO_REQUIERE falla: no puede ir a PENDIENTE_FIRMA")
        void noRequiereFirma_falla() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaNoRequiereFirma("PL-ERR-3");
            FalDocumento doc = crearDocumentoBorrador(acta, plantilla);

            assertThatThrownBy(() -> docService.enviarAFirma(new EnviarAFirmaCommand(doc.getId(), USER)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("NO_REQUIERE");
        }

        @Test
        @DisplayName("AL_CREAR sin nroDocu falla por inconsistencia")
        void alCrearSinNroDocu_falla() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaConFirmaReq(
                    "PL-ERR-4", MomentoNumeracionDocu.AL_CREAR, true);
            FalDocumento doc = crearDocumentoBorrador(acta, plantilla);

            assertThat(doc.getNroDocu()).isNull();

            assertThatThrownBy(() -> docService.enviarAFirma(new EnviarAFirmaCommand(doc.getId(), USER)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("AL_CREAR")
                    .hasMessageContaining("nroDocu");
        }
    }

    // =========================================================================
    // Guardrails
    // =========================================================================

    @Nested
    @DisplayName("Guardrails")
    class Guardrails {

        @Test
        @DisplayName("estado del documento permanece BORRADOR si numeracion falla (sin talonario)")
        void estadoPermaneceBorradorSiNumeracionFalla() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaConFirmaReq(
                    "PL-GRD-1", MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA, true);
            FalDocumento doc = crearDocumentoBorrador(acta, plantilla);

            // Sin dependencia ni talonario -> numeracion falla
            assertThatThrownBy(() -> docService.enviarAFirma(new EnviarAFirmaCommand(doc.getId(), USER)))
                    .isInstanceOf(PrecondicionVioladaException.class);

            FalDocumento persisted = docRepo.buscarPorId(doc.getId()).orElseThrow();
            assertThat(persisted.getEstadoDocu()).isEqualTo(EstadoDocu.BORRADOR);
        }

        @Test
        @DisplayName("nroDocu del documento no cambia si no es AL_ENVIAR_A_FIRMA")
        void nroDocuNoModificadoSiNoEsAlEnviarAFirma() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaConFirmaReq(
                    "PL-GRD-2", MomentoNumeracionDocu.AL_FIRMAR, false);
            FalDocumento doc = crearDocumentoBorrador(acta, plantilla);

            assertThat(doc.getNroDocu()).isNull();

            FalDocumento result = docService.enviarAFirma(new EnviarAFirmaCommand(doc.getId(), USER));

            assertThat(result.getNroDocu()).isNull();
            assertThat(result.getIdTalonario()).isNull();
        }

        @Test
        @DisplayName("AL_ENVIAR_A_FIRMA: nroDocu asignado y movimiento de talonario registrado")
        void alEnviarAFirma_movimientoTalonarioRegistrado() {
            crearDependencia();
            NumPolitica pol = crearPoliticaDoc();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.ACTO_ADMINISTRATIVO.codigo());

            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaConFirmaReq(
                    "PL-GRD-3", MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA, true);
            FalDocumento doc = crearDocumentoBorrador(acta, plantilla);

            FalDocumento result = docService.enviarAFirma(new EnviarAFirmaCommand(doc.getId(), USER));

            assertThat(result.getIdTalonario()).isEqualTo(tal.getId());
            assertThat(result.getNroTalonarioUsado()).isEqualTo(1);
        }
    }
}
