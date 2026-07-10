package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.CrearDependenciaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearDocumentoPlantillaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearPoliticaNumeracionCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioAmbitoCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoDesdePlantillaCommand;
import ar.gob.malvinas.faltas.core.application.command.NumerarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.service.DependenciaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoPlantillaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoNumeroTalonario;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del Micro-slice 8C-5A: Numeracion documental reusable con talonarios DOCUMENTO.
 *
 * Verifica:
 * - Emision de numero documental usando clase_talonario = DOCUMENTO.
 * - Uso de tipoDocu del documento para filtrar ambitos.
 * - Uso de dependencia/version del acta para resolver talonario.
 * - Asignacion de nroDocu, idTalonario, nroTalonarioUsado en FalDocumento.
 * - Registro de NumTalonarioMovimiento con documentoId y actaId=null.
 * - Validaciones de precondicion.
 * - Guardrails: no consume talonario ACTA, no firma, no PDF.
 */
@DisplayName("Micro-slice 8C-5A: Numeracion documental reusable")
class DocumentoNumeracionTest {

    private static final String DEP_COD = "DEP-001";

    private ActaRepository actaRepo;
    private DocumentoRepository docRepo;
    private DocumentoPlantillaRepository plantillaRepo;
    private TalonarioRepository talonarioRepo;
    private DependenciaRepository depRepo;
    private InspectorRepository inspectorRepo;

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
                new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaReqRepository(),
                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryFirmanteRepository(), FaltasClockTestSupport.FIXED);
    }

    // =========================================================================
    // Helpers de setup
    // =========================================================================

    /**
     * Crea y registra un acta con idDependencia = DEP_COD = "DEP-001".
     * Esto permite que DocumentoService resuelva la dependencia via findByCodDep.
     */
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

    /**
     * Crea y registra la dependencia "DEP-001" en el repositorio.
     * DependenciaService.crear auto-genera version 1 con fhVigDesde = hoy - 30 dias.
     * Esto permite que findByCodDep("DEP-001") y findVersionVigente funcionen.
     */
    private FalDependencia crearDependencia() {
        return depService.crear(new CrearDependenciaCommand(
                DEP_COD, "Dep Uno", null, TipoActa.TRANSITO,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(30), "sistema"));
    }

    private NumPolitica crearPoliticaDocSimple() {
        return talonarioService.crearPolitica(new CrearPoliticaNumeracionCommand(
                "POL-DOC-01", "Politica documentos", ClaseNumeracion.DOCUMENTO,
                false, false, null, false, null, false, null,
                "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, "sistema"));
    }

    private NumPolitica crearPoliticaDocConPrefijo() {
        return talonarioService.crearPolitica(new CrearPoliticaNumeracionCommand(
                "POL-DOC-02", "Politica documentos con prefijo y longitud", ClaseNumeracion.DOCUMENTO,
                false, true, "NOTIF", false, null, false, (short) 5,
                "{PREFIJO}-{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, "sistema"));
    }

    private NumTalonario crearTalonarioDocGlobal(Long politicaId) {
        return talonarioService.crearTalonario(new CrearTalonarioCommand(
                politicaId, "TAL-DOC-01", "Talonario documental global",
                TipoTalonario.ELECTRONICO, ClaseNumeracion.DOCUMENTO,
                null, null, 1, 9999, "seq_doc_global",
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

    private void crearAmbitoDependencia(Long talonarioId, Short tipoDocu, Long idDep, Short verDep) {
        talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                talonarioId, ClaseNumeracion.DOCUMENTO,
                tipoDocu, null, idDep, verDep,
                AlcanceTalonario.DEPENDENCIA,
                (short) 5,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, true, "sistema"));
    }

    private FalDocumentoPlantilla crearPlantillaConNumeracion(String codigo, TipoDocu tipoDocu,
            MomentoNumeracionDocu momento) {
        FalDocumentoPlantilla p = plantillaService.crear(new CrearDocumentoPlantillaCommand(
                codigo, "Plantilla " + codigo, null,
                tipoDocu, AccionDocumental.EMITIR_CONSTANCIA, null,
                TipoFirmaReq.NO_REQUIERE,
                true, momento,
                false, false, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, "sistema"));
        return plantillaService.activar(p.getId());
    }

    private FalDocumentoPlantilla crearPlantillaSinNumeracion(String codigo) {
        FalDocumentoPlantilla p = plantillaService.crear(new CrearDocumentoPlantillaCommand(
                codigo, "Plantilla sin numeracion " + codigo, null,
                TipoDocu.CONSTANCIA, AccionDocumental.EMITIR_CONSTANCIA, null,
                TipoFirmaReq.NO_REQUIERE,
                false, MomentoNumeracionDocu.NO_APLICA,
                false, false, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, "sistema"));
        return plantillaService.activar(p.getId());
    }

    private FalDocumento crearDocumentoConPlantilla(FalActa acta, FalDocumentoPlantilla plantilla) {
        return docService.generarDesdePlantilla(
                new GenerarDocumentoDesdePlantillaCommand(acta.getId(), plantilla.getId(), "usr-test"));
    }

    // =========================================================================
    // Numeracion valida
    // =========================================================================

    @Nested
    @DisplayName("Numeracion valida")
    class NumeracionValida {

        @Test
        @DisplayName("1. Numera documento no numerado con plantilla numerada")
        void numera_documento_no_numerado() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-001", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            FalDocumento result = docService.numerarDocumento(
                    new NumerarDocumentoCommand(doc.getId(), "usr-test"));

            assertThat(result.getNroDocu()).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("2. Usa clase talonario DOCUMENTO (idTalonario apunta al talonario DOCUMENTO)")
        void usa_clase_documento() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-002", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            FalDocumento result = docService.numerarDocumento(
                    new NumerarDocumentoCommand(doc.getId(), "usr-test"));

            assertThat(result.getIdTalonario()).isEqualTo(tal.getId());
        }

        @Test
        @DisplayName("3. Guarda nroDocu en documento")
        void guarda_nro_docu() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-003", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            FalDocumento result = docService.numerarDocumento(
                    new NumerarDocumentoCommand(doc.getId(), "usr-test"));

            assertThat(result.getNroDocu()).isNotNull();
        }

        @Test
        @DisplayName("4. Guarda idTalonario en documento")
        void guarda_id_talonario() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-004", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            FalDocumento result = docService.numerarDocumento(
                    new NumerarDocumentoCommand(doc.getId(), "usr-test"));

            assertThat(result.getIdTalonario()).isEqualTo(tal.getId());
        }

        @Test
        @DisplayName("5. Guarda nroTalonarioUsado = 1 (primer numero del talonario)")
        void guarda_nro_talonario_usado() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-005", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            FalDocumento result = docService.numerarDocumento(
                    new NumerarDocumentoCommand(doc.getId(), "usr-test"));

            assertThat(result.getNroTalonarioUsado()).isEqualTo(1);
        }

        @Test
        @DisplayName("6. Registra movimiento con documentoId correcto")
        void registra_movimiento_con_documento_id() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-006", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            FalDocumento result = docService.numerarDocumento(
                    new NumerarDocumentoCommand(doc.getId(), "usr-test"));

            List<NumTalonarioMovimiento> movs = talonarioService.listarMovimientosPorTalonario(tal.getId());
            assertThat(movs).hasSize(1);
            assertThat(movs.get(0).getDocumentoId()).isEqualTo(result.getId());
        }

        @Test
        @DisplayName("7. Movimiento documental tiene actaId null")
        void movimiento_documental_acta_id_null() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-007", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            docService.numerarDocumento(new NumerarDocumentoCommand(doc.getId(), "usr-test"));

            List<NumTalonarioMovimiento> movs = talonarioService.listarMovimientosPorTalonario(tal.getId());
            assertThat(movs.get(0).getActaId()).isNull();
        }

        @Test
        @DisplayName("8. Movimiento documental tiene estadoNumero USADO")
        void movimiento_estado_usado() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-008", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            docService.numerarDocumento(new NumerarDocumentoCommand(doc.getId(), "usr-test"));

            List<NumTalonarioMovimiento> movs = talonarioService.listarMovimientosPorTalonario(tal.getId());
            assertThat(movs.get(0).getEstadoNumero()).isEqualTo(EstadoNumeroTalonario.USADO);
        }

        @Test
        @DisplayName("9. Formato visible respeta politica con prefijo y longitud")
        void formato_visible_respeta_politica() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocConPrefijo();
            NumTalonario tal = talonarioService.crearTalonario(new CrearTalonarioCommand(
                    pol.getId(), "TAL-DOC-NOTIF", "Talonario notif",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.DOCUMENTO,
                    null, null, 1, 9999, "seq_doc_notif",
                    true, false, null, null, "sistema"));
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.NOTIFICACION_ACTA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-009", TipoDocu.NOTIFICACION_ACTA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            FalDocumento result = docService.numerarDocumento(
                    new NumerarDocumentoCommand(doc.getId(), "usr-test"));

            // POL-DOC-02: prefijo="NOTIF", longitud=5 -> "NOTIF-00001"
            assertThat(result.getNroDocu()).isEqualTo("NOTIF-00001");
        }

        @Test
        @DisplayName("10. No cambia estadoDocu al numerar (permanece BORRADOR)")
        void no_cambia_estado_docu_al_numerar() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-010", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            FalDocumento result = docService.numerarDocumento(
                    new NumerarDocumentoCommand(doc.getId(), "usr-test"));

            assertThat(result.getEstadoDocu()).isEqualTo(EstadoDocu.BORRADOR);
        }

        @Test
        @DisplayName("11. Talonario DEPENDENCIA resuelve correctamente por idDep y verDep")
        void talonario_dependencia_resuelve_por_dep() {
            FalDependencia dep = crearDependencia(); // verDep=1 auto-creado
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoDependencia(tal.getId(), TipoDocu.CONSTANCIA.codigo(), dep.getIdDep(), (short) 1);
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-011", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            FalDocumento result = docService.numerarDocumento(
                    new NumerarDocumentoCommand(doc.getId(), "usr-test"));

            assertThat(result.getNroDocu()).isNotNull();
            assertThat(result.getIdTalonario()).isEqualTo(tal.getId());
        }

        @Test
        @DisplayName("12. Correlativo incremental: dos documentos numerados consecutivamente")
        void correlativo_incremental() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-012", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);

            FalDocumento doc1 = crearDocumentoConPlantilla(acta, p);
            FalDocumento doc2 = crearDocumentoConPlantilla(acta, p);

            FalDocumento r1 = docService.numerarDocumento(new NumerarDocumentoCommand(doc1.getId(), "usr"));
            FalDocumento r2 = docService.numerarDocumento(new NumerarDocumentoCommand(doc2.getId(), "usr"));

            assertThat(r1.getNroTalonarioUsado()).isEqualTo(1);
            assertThat(r2.getNroTalonarioUsado()).isEqualTo(2);
        }
    }

    // =========================================================================
    // Validaciones
    // =========================================================================

    @Nested
    @DisplayName("Validaciones de precondicion")
    class Validaciones {

        @Test
        @DisplayName("13. Falla si documento no existe")
        void falla_si_documento_no_existe() {
            assertThatThrownBy(() ->
                docService.numerarDocumento(new NumerarDocumentoCommand(9999L, "usr")))
                .isInstanceOf(DocumentoNoEncontradoException.class);
        }

        @Test
        @DisplayName("14. Falla si documento ya esta numerado")
        void falla_si_ya_numerado() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-014", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);
            docService.numerarDocumento(new NumerarDocumentoCommand(doc.getId(), "usr"));

            assertThatThrownBy(() ->
                docService.numerarDocumento(new NumerarDocumentoCommand(doc.getId(), "usr")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("ya tiene numero");
        }

        @Test
        @DisplayName("15. Falla si documento no tiene plantilla (plantillaId null)")
        void falla_si_sin_plantilla() {
            Long id = docRepo.nextId();
            FalDocumento doc = new FalDocumento(id, 1L,
                    TipoDocu.CONSTANCIA, FaltasClockTestSupport.FIXED.now(), "sin plantilla");
            docRepo.guardar(doc);

            assertThatThrownBy(() ->
                docService.numerarDocumento(new NumerarDocumentoCommand(id, "usr")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("plantilla");
        }

        @Test
        @DisplayName("16. Falla si plantilla no requiere numeracion (siRequiereNumeracion=false)")
        void falla_si_plantilla_sin_numeracion() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla p = crearPlantillaSinNumeracion("PLNT-5A-016");
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            assertThatThrownBy(() ->
                docService.numerarDocumento(new NumerarDocumentoCommand(doc.getId(), "usr")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("siRequiereNumeracion");
        }

        @Test
        @DisplayName("17. Falla si momentoNumeracionDocu = NO_APLICA")
        void falla_si_momento_no_aplica() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla p = crearPlantillaSinNumeracion("PLNT-5A-017");
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            assertThatThrownBy(() ->
                docService.numerarDocumento(new NumerarDocumentoCommand(doc.getId(), "usr")))
                .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("18. Falla si dependencia del acta no esta registrada (codDep no encontrado)")
        void falla_si_dependencia_no_encontrada() {
            // Acta con codDep="DEP-001" pero no hay dependencia registrada en repo
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-018", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            assertThatThrownBy(() ->
                docService.numerarDocumento(new NumerarDocumentoCommand(doc.getId(), "usr")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("dependencia");
        }

        @Test
        @DisplayName("19. Falla si no hay ambito DOCUMENTO aplicable")
        void falla_si_no_hay_ambito_documento() {
            crearDependencia();
            FalActa acta = crearActa();
            // Sin talonario DOCUMENTO configurado para CONSTANCIA
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-019", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            assertThatThrownBy(() ->
                docService.numerarDocumento(new NumerarDocumentoCommand(doc.getId(), "usr")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("No hay talonario DOCUMENTO");
        }

        @Test
        @DisplayName("20. Falla por ambiguedad: dos talonarios con misma prioridad para el mismo contexto")
        void falla_ambiguedad_prioridad() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();

            NumTalonario tal1 = talonarioService.crearTalonario(new CrearTalonarioCommand(
                    pol.getId(), "TAL-DOC-AMB1", "Talonario 1",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.DOCUMENTO,
                    null, null, 1, 9999, "seq_doc_amb1", true, false, null, null, "sistema"));
            talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    tal1.getId(), ClaseNumeracion.DOCUMENTO,
                    TipoDocu.CONSTANCIA.codigo(), null, null, null,
                    AlcanceTalonario.GLOBAL, (short) 10,
                    FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, true, "sistema"));

            NumTalonario tal2 = talonarioService.crearTalonario(new CrearTalonarioCommand(
                    pol.getId(), "TAL-DOC-AMB2", "Talonario 2",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.DOCUMENTO,
                    null, null, 1, 9999, "seq_doc_amb2", true, false, null, null, "sistema"));
            talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    tal2.getId(), ClaseNumeracion.DOCUMENTO,
                    TipoDocu.CONSTANCIA.codigo(), null, null, null,
                    AlcanceTalonario.GLOBAL, (short) 10,
                    FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, true, "sistema"));

            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-020", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            assertThatThrownBy(() ->
                docService.numerarDocumento(new NumerarDocumentoCommand(doc.getId(), "usr")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("Ambiguedad");
        }

        @Test
        @DisplayName("21. Falla si talonario bloqueado")
        void falla_talonario_bloqueado() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = talonarioService.crearTalonario(new CrearTalonarioCommand(
                    pol.getId(), "TAL-DOC-BLOQ", "Bloqueado",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.DOCUMENTO,
                    null, null, 1, 9999, "seq_doc_bloq",
                    true, true, "SECRET", null, "sistema")); // siBloqueado=true
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-021", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            assertThatThrownBy(() ->
                docService.numerarDocumento(new NumerarDocumentoCommand(doc.getId(), "usr")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("No hay talonario DOCUMENTO");
        }

        @Test
        @DisplayName("22. Falla si se excede nroHasta del talonario")
        void falla_si_excede_nro_hasta() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            // Talonario con solo 1 numero (nroDesde=1, nroHasta=1)
            NumTalonario tal = talonarioService.crearTalonario(new CrearTalonarioCommand(
                    pol.getId(), "TAL-DOC-RANGO1", "Un numero",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.DOCUMENTO,
                    null, null, 1, 1, "seq_doc_rango1",
                    true, false, null, null, "sistema"));
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-022", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);

            FalDocumento doc1 = crearDocumentoConPlantilla(acta, p);
            docService.numerarDocumento(new NumerarDocumentoCommand(doc1.getId(), "usr")); // usa el 1

            FalDocumento doc2 = crearDocumentoConPlantilla(acta, p);
            assertThatThrownBy(() ->
                docService.numerarDocumento(new NumerarDocumentoCommand(doc2.getId(), "usr")))
                .isInstanceOf(PrecondicionVioladaException.class);
        }
    }

    // =========================================================================
    // Guardrails
    // =========================================================================

    @Nested
    @DisplayName("Guardrails de la numeracion documental")
    class Guardrails {

        @Test
        @DisplayName("23. No consume talonario ACTA al numerar documento")
        void no_consume_talonario_acta() {
            crearDependencia();
            FalActa acta = crearActa();

            // Crear talonario ACTA (no debe ser usado para documento)
            NumPolitica polActa = talonarioService.crearPolitica(new CrearPoliticaNumeracionCommand(
                    "POL-ACTA-GUARD", "Politica actas guardrail", ClaseNumeracion.ACTA,
                    false, false, null, false, null, false, null,
                    "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, "sistema"));
            NumTalonario talActa = talonarioService.crearTalonario(new CrearTalonarioCommand(
                    polActa.getId(), "TAL-ACTA-GUARD", "Talonario acta guardrail",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                    null, null, 1, 9999, "seq_acta_guard", true, false, null, null, "sistema"));
            talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    talActa.getId(), ClaseNumeracion.ACTA,
                    null, null, null, null,
                    AlcanceTalonario.GLOBAL, (short) 1,
                    FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, true, "sistema"));

            // Crear talonario DOCUMENTO
            NumPolitica polDoc = crearPoliticaDocSimple();
            NumTalonario talDoc = crearTalonarioDocGlobal(polDoc.getId());
            crearAmbitoGlobalDocumento(talDoc.getId(), TipoDocu.CONSTANCIA.codigo());

            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-023", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            docService.numerarDocumento(new NumerarDocumentoCommand(doc.getId(), "usr"));

            // Movimientos del talonario ACTA: deben ser 0
            List<NumTalonarioMovimiento> movsActa = talonarioService.listarMovimientosPorTalonario(talActa.getId());
            assertThat(movsActa).isEmpty();

            // Movimientos del talonario DOCUMENTO: deben ser 1
            List<NumTalonarioMovimiento> movsDoc = talonarioService.listarMovimientosPorTalonario(talDoc.getId());
            assertThat(movsDoc).hasSize(1);
        }

        @Test
        @DisplayName("24. No duplica numeracion: segunda llamada falla")
        void no_duplica_numeracion() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-024", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            docService.numerarDocumento(new NumerarDocumentoCommand(doc.getId(), "usr"));

            assertThatThrownBy(() ->
                docService.numerarDocumento(new NumerarDocumentoCommand(doc.getId(), "usr")))
                .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("25. documentoId obligatorio en NumerarDocumentoCommand")
        void documento_id_obligatorio() {
            assertThatThrownBy(() ->
                docService.numerarDocumento(new NumerarDocumentoCommand(null, "usr")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("documentoId");
        }

        @Test
        @DisplayName("26. idUserOperacion obligatorio en NumerarDocumentoCommand")
        void usuario_obligatorio() {
            assertThatThrownBy(() ->
                docService.numerarDocumento(new NumerarDocumentoCommand(1L, null)))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("idUserOperacion");
        }

        @Test
        @DisplayName("27. idUserOperacion no puede ser blank")
        void usuario_no_blank() {
            assertThatThrownBy(() ->
                docService.numerarDocumento(new NumerarDocumentoCommand(1L, "   ")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("idUserOperacion");
        }
    }

    // =========================================================================
    // AL_CREAR
    // =========================================================================

    @Nested
    @DisplayName("AL_CREAR: numeracion automatica al generar desde plantilla")
    class AlCrear {

        @Test
        @DisplayName("28. AL_CREAR: genera documento numerado automaticamente")
        void al_crear_genera_numerado() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-AL-CREAR", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_CREAR);

            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            assertThat(doc.getNroDocu()).isNotNull();
            assertThat(doc.getIdTalonario()).isEqualTo(tal.getId());
            assertThat(doc.getNroTalonarioUsado()).isEqualTo(1);
        }

        @Test
        @DisplayName("29. AL_CREAR: documento sigue BORRADOR despues de numerar")
        void al_crear_sigue_borrador() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-AL-CREAR-2", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_CREAR);

            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            assertThat(doc.getEstadoDocu()).isEqualTo(EstadoDocu.BORRADOR);
        }

        @Test
        @DisplayName("30. AL_CREAR: registra movimiento documental con documentoId")
        void al_crear_registra_movimiento() {
            crearDependencia();
            FalActa acta = crearActa();
            NumPolitica pol = crearPoliticaDocSimple();
            NumTalonario tal = crearTalonarioDocGlobal(pol.getId());
            crearAmbitoGlobalDocumento(tal.getId(), TipoDocu.CONSTANCIA.codigo());
            FalDocumentoPlantilla p = crearPlantillaConNumeracion(
                    "PLNT-5A-AL-CREAR-3", TipoDocu.CONSTANCIA, MomentoNumeracionDocu.AL_CREAR);

            FalDocumento doc = crearDocumentoConPlantilla(acta, p);

            List<NumTalonarioMovimiento> movs = talonarioService.listarMovimientosPorTalonario(tal.getId());
            assertThat(movs).hasSize(1);
            assertThat(movs.get(0).getDocumentoId()).isEqualTo(doc.getId());
            assertThat(movs.get(0).getActaId()).isNull();
        }
    }

    // =========================================================================
    // FIRMA_MIXTA guardrail
    // =========================================================================

    @Test
    @DisplayName("31. FIRMA_MIXTA no existe en TipoFirmaReq")
    void firma_mixta_no_existe() {
        for (TipoFirmaReq v : TipoFirmaReq.values()) {
            assertThat(v.name()).doesNotContain("MIXTA");
        }
    }
}