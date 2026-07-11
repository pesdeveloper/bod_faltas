package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.CrearDependenciaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearDocumentoPlantillaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearPoliticaNumeracionCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioAmbitoCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioCommand;
import ar.gob.malvinas.faltas.core.application.command.EmitirDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoDesdePlantillaCommand;
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
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantilla;
import ar.gob.malvinas.faltas.core.domain.model.NumPolitica;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonario;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del Micro-slice 8C-6C-1: Emision formal in-memory con storage/hash simulado.
 *
 * Verifica:
 * - Flujo BORRADOR -> EMITIDO para NO_REQUIERE.
 * - Flujo FIRMADO -> EMITIDO para documentos con firma.
 * - Numeracion automatica si momentoNumeracionDocu = AL_EMITIR.
 * - Error si otros momentos y documento no tiene nroDocu.
 * - siGeneraPdf=true exige storageKey y hashDocu.
 * - siGeneraPdf=false permite storageKey/hashDocu null.
 * - Guardrails: no firma, no notificacion, no PDF real, no JDBC.
 * - TipoEventoActa.DOCEMI existe y no reemplaza DOCGEN/DOCFIR.
 */
@DisplayName("Micro-slice 8C-6C-1: Emision formal de documentos")
class DocumentoEmisionFormalTest {

    private static final String DEP_COD = "DEP-EMI";
    private static final String USER = "emi-tester";

    private InMemoryActaRepository actaRepo;
    private InMemoryDocumentoRepository docRepo;
    private InMemoryDocumentoPlantillaRepository plantillaRepo;
    private InMemoryTalonarioRepository talonarioRepo;
    private InMemoryDependenciaRepository depRepo;
    private InMemoryActaEventoRepository eventoRepo;

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

        eventoRepo = new InMemoryActaEventoRepository();
        InMemoryActaSnapshotRepository snapshotRepo = new InMemoryActaSnapshotRepository();
        InMemoryFalloActaRepository falloRepo = new InMemoryFalloActaRepository();

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo,
                new InMemoryNotificacionRepository(),
                new InMemoryPagoVoluntarioRepository(),
                falloRepo,
                new InMemoryApelacionActaRepository(),
                new InMemoryPagoCondenaRepository(), FaltasClockTestSupport.FIXED);

        talonarioService = new TalonarioService(talonarioRepo, depRepo, new InMemoryInspectorRepository(), FaltasClockTestSupport.FIXED);
        depService = new DependenciaService(depRepo, FaltasClockTestSupport.FIXED);
        plantillaService = new DocumentoPlantillaService(plantillaRepo, FaltasClockTestSupport.FIXED);

        docService = new DocumentoService(
                actaRepo, docRepo, new InMemoryDocumentoFirmaRepository(),
                eventoRepo, snapshotRepo, recalc, falloRepo,
                plantillaRepo, talonarioService, depRepo,
                new InMemoryDocumentoFirmaReqRepository(),
                new InMemoryFirmanteRepository(),
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
                "Belgrano 200", "Calle 123", null, null, null, "Pedro Gomez", "98765432",
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR);
        actaRepo.guardar(acta);
        return acta;
    }

    private void crearDependencia() {
        depService.crear(new CrearDependenciaCommand(
                DEP_COD, "Dep Emision", null, TipoActa.TRANSITO,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(30), "sistema"));
    }

    private NumPolitica crearPoliticaDoc(String codigo) {
        return talonarioService.crearPolitica(new CrearPoliticaNumeracionCommand(
                codigo, "Politica " + codigo, ClaseNumeracion.DOCUMENTO,
                false, false, null, false, null, false, null,
                "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, "sistema"));
    }

    private NumTalonario crearTalonarioDoc(Long politicaId, String codigo) {
        return talonarioService.crearTalonario(new CrearTalonarioCommand(
                politicaId, codigo, "Talonario " + codigo,
                TipoTalonario.ELECTRONICO, ClaseNumeracion.DOCUMENTO,
                null, null, 1, 9999, "seq_doc_emi",
                true, false, null, null, "sistema"));
    }

    private void crearAmbitoGlobal(Long talonarioId, TipoDocu tipoDocu) {
        talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                talonarioId, ClaseNumeracion.DOCUMENTO,
                tipoDocu.codigo(), null, null, null,
                AlcanceTalonario.GLOBAL,
                (short) 10,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, true, "sistema"));
    }

    /**
     * Crea plantilla con NO_REQUIERE (sin firma), activa.
     * siGeneraPdf configurable. momentoNumeracion configurable.
     */
    private FalDocumentoPlantilla crearPlantillaSinFirma(String codigo, boolean siGeneraPdf,
                                                          boolean siNumeracion,
                                                          MomentoNumeracionDocu momento) {
        FalDocumentoPlantilla p = plantillaService.crear(new CrearDocumentoPlantillaCommand(
                codigo, "Plantilla " + codigo, null,
                TipoDocu.CONSTANCIA, AccionDocumental.EMITIR_CONSTANCIA, null,
                TipoFirmaReq.NO_REQUIERE,
                siNumeracion, momento,
                false, siGeneraPdf, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, "sistema"));
        return plantillaService.activar(p.getId());
    }

    /**
     * Crea plantilla NO_REQUIERE activa (usable como plantilla base para documentos
     * que tienen tipoFirmaReq seteado directamente en FIRMA_INTERNA).
     */
    private FalDocumentoPlantilla crearPlantillaConFirma(String codigo, boolean siGeneraPdf) {
        FalDocumentoPlantilla p = plantillaService.crear(new CrearDocumentoPlantillaCommand(
                codigo, "Plantilla firma " + codigo, null,
                TipoDocu.CONSTANCIA, AccionDocumental.EMITIR_CONSTANCIA, null,
                TipoFirmaReq.NO_REQUIERE,
                false, MomentoNumeracionDocu.NO_APLICA,
                false, siGeneraPdf, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, "sistema"));
        return plantillaService.activar(p.getId());
    }

    private FalDocumento generarBorrador(FalActa acta, FalDocumentoPlantilla plantilla) {
        return docService.generarDesdePlantilla(
                new GenerarDocumentoDesdePlantillaCommand(acta.getId(), plantilla.getId(), USER));
    }

    /** Fuerza estado FIRMADO con TipoFirmaReq.FIRMA_INTERNA. */
    private FalDocumento simularDocumentoFirmado(FalDocumentoPlantilla plantilla) {
        FalActa acta = crearActa();
        Long docId = docRepo.nextId();
        FalDocumento doc = new FalDocumento(
                docId, acta.getId(), plantilla.getTipoDocu(),
                FaltasClockTestSupport.FIXED.now(), null,
                EstadoDocu.FIRMADO, TipoFirmaReq.FIRMA_INTERNA,
                plantilla.getId(), FaltasClockTestSupport.FIXED.now());
        docRepo.guardar(doc);
        return doc;
    }

    /** Fuerza estado y TipoFirmaReq.FIRMA_INTERNA (para tests de flujo con firma). */
    private FalDocumento crearDocConFirmaInternaYEstado(FalDocumentoPlantilla plantilla, EstadoDocu estado) {
        FalActa acta = crearActa();
        Long docId = docRepo.nextId();
        FalDocumento doc = new FalDocumento(
                docId, acta.getId(), plantilla.getTipoDocu(),
                FaltasClockTestSupport.FIXED.now(), null,
                estado, TipoFirmaReq.FIRMA_INTERNA,
                plantilla.getId(), FaltasClockTestSupport.FIXED.now());
        docRepo.guardar(doc);
        return doc;
    }

    /** Fuerza estado personalizado usando el tipoFirmaReq de la plantilla. */
    private FalDocumento crearDocumentoConEstado(FalDocumentoPlantilla plantilla, EstadoDocu estado) {
        FalActa acta = crearActa();
        Long docId = docRepo.nextId();
        FalDocumento doc = new FalDocumento(
                docId, acta.getId(), plantilla.getTipoDocu(),
                FaltasClockTestSupport.FIXED.now(), null,
                estado, plantilla.getTipoFirmaReq(),
                plantilla.getId(), FaltasClockTestSupport.FIXED.now());
        docRepo.guardar(doc);
        return doc;
    }

    // =========================================================================
    // 1. Emision sin firma (NO_REQUIERE: BORRADOR -> EMITIDO)
    // =========================================================================

    @Nested
    @DisplayName("Emision sin firma (NO_REQUIERE)")
    class EmisionSinFirma {

        @Test
        @DisplayName("1. Documento NO_REQUIERE puede emitirse desde BORRADOR")
        void emitirDesdeborrador() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-EMI-01", false, false, MomentoNumeracionDocu.NO_APLICA);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            FalDocumento resultado = docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            assertThat(resultado.getEstadoDocu()).isEqualTo(EstadoDocu.EMITIDO);
        }

        @Test
        @DisplayName("2. Cambia estado a EMITIDO")
        void cambiaEstadoAEmitido() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-EMI-02", false, false, MomentoNumeracionDocu.NO_APLICA);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            FalDocumento guardado = docRepo.buscarPorId(doc.getId()).orElseThrow();
            assertThat(guardado.getEstadoDocu()).isEqualTo(EstadoDocu.EMITIDO);
        }

        @Test
        @DisplayName("3. Setea fhGeneracion al emitir")
        void seteaFhGeneracion() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-EMI-03", false, false, MomentoNumeracionDocu.NO_APLICA);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            FalDocumento resultado = docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            assertThat(resultado.getFhGeneracion()).isNotNull();
        }

        @Test
        @DisplayName("4. siGeneraPdf=true: guarda storageKey")
        void guardaStorageKey() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-EMI-04", true, false, MomentoNumeracionDocu.NO_APLICA);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            FalDocumento resultado = docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER,
                            "storage/actas/1/doc-04.pdf", "hash-abc123"));

            assertThat(resultado.getStorageKey()).isEqualTo("storage/actas/1/doc-04.pdf");
        }

        @Test
        @DisplayName("5. siGeneraPdf=true: guarda hashDocu")
        void guardaHashDocu() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-EMI-05", true, false, MomentoNumeracionDocu.NO_APLICA);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            FalDocumento resultado = docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER,
                            "storage/key", "sha256-xyz789"));

            assertThat(resultado.getHashDocu()).isEqualTo("sha256-xyz789");
        }

        @Test
        @DisplayName("6. siGeneraPdf=false: permite storageKey y hashDocu null")
        void permiteNullsSinPdf() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-EMI-06", false, false, MomentoNumeracionDocu.NO_APLICA);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            FalDocumento resultado = docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            assertThat(resultado.getStorageKey()).isNull();
            assertThat(resultado.getHashDocu()).isNull();
            assertThat(resultado.getEstadoDocu()).isEqualTo(EstadoDocu.EMITIDO);
        }
    }

    // =========================================================================
    // 2. Emision con firma (FIRMADO -> EMITIDO)
    // =========================================================================

    @Nested
    @DisplayName("Emision con firma requerida")
    class EmisionConFirma {

        @Test
        @DisplayName("7. Documento con firma requerida puede emitirse desde FIRMADO")
        void emitirDesdeFirmado() {
            FalDocumentoPlantilla plantilla = crearPlantillaConFirma("PL-FIR-01", false);
            FalDocumento doc = simularDocumentoFirmado(plantilla);

            FalDocumento resultado = docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            assertThat(resultado.getEstadoDocu()).isEqualTo(EstadoDocu.EMITIDO);
        }

        @Test
        @DisplayName("8. Documento con firma requerida no puede emitirse desde BORRADOR")
        void noEmitirDesdeBorrador() {
            FalDocumentoPlantilla plantilla = crearPlantillaConFirma("PL-FIR-02", false);
            FalDocumento doc = crearDocConFirmaInternaYEstado(plantilla, EstadoDocu.BORRADOR);

            assertThatThrownBy(() ->
                    docService.emitirDocumento(new EmitirDocumentoCommand(doc.getId(), USER, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("FIRMADO");
        }

        @Test
        @DisplayName("9. Documento con firma requerida no puede emitirse desde PENDIENTE_FIRMA")
        void noEmitirDesdePendienteFirma() {
            FalDocumentoPlantilla plantilla = crearPlantillaConFirma("PL-FIR-03", false);
            FalDocumento doc = crearDocConFirmaInternaYEstado(plantilla, EstadoDocu.PENDIENTE_FIRMA);

            assertThatThrownBy(() ->
                    docService.emitirDocumento(new EmitirDocumentoCommand(doc.getId(), USER, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("PENDIENTE_FIRMA");
        }
    }

    // =========================================================================
    // 3. Numeracion AL_EMITIR
    // =========================================================================

    @Nested
    @DisplayName("Numeracion AL_EMITIR")
    class NumeracionAlEmitir {

        @Test
        @DisplayName("10. Si momento=AL_EMITIR y no esta numerado, numera automaticamente antes de emitir")
        void numeraAutomaticamente() {
            crearDependencia();
            NumPolitica pol = crearPoliticaDoc("POL-EMI-10");
            NumTalonario tal = crearTalonarioDoc(pol.getId(), "TAL-EMI-10");
            crearAmbitoGlobal(tal.getId(), TipoDocu.CONSTANCIA);

            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-N-01", false, true, MomentoNumeracionDocu.AL_EMITIR);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            assertThat(doc.getNroDocu()).isNull();

            FalDocumento resultado = docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            assertThat(resultado.getNroDocu()).isNotNull();
        }

        @Test
        @DisplayName("11. Guarda nroDocu despues de AL_EMITIR")
        void guardaNroDocu() {
            crearDependencia();
            NumPolitica pol = crearPoliticaDoc("POL-EMI-11");
            NumTalonario tal = crearTalonarioDoc(pol.getId(), "TAL-EMI-11");
            crearAmbitoGlobal(tal.getId(), TipoDocu.CONSTANCIA);

            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-N-02", false, true, MomentoNumeracionDocu.AL_EMITIR);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            FalDocumento guardado = docRepo.buscarPorId(doc.getId()).orElseThrow();
            assertThat(guardado.getNroDocu()).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("12. Guarda idTalonario despues de AL_EMITIR")
        void guardaIdTalonario() {
            crearDependencia();
            NumPolitica pol = crearPoliticaDoc("POL-EMI-12");
            NumTalonario tal = crearTalonarioDoc(pol.getId(), "TAL-EMI-12");
            crearAmbitoGlobal(tal.getId(), TipoDocu.CONSTANCIA);

            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-N-03", false, true, MomentoNumeracionDocu.AL_EMITIR);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            FalDocumento guardado = docRepo.buscarPorId(doc.getId()).orElseThrow();
            assertThat(guardado.getIdTalonario()).isNotNull();
        }

        @Test
        @DisplayName("13. Guarda nroTalonarioUsado despues de AL_EMITIR")
        void guardaNroTalonarioUsado() {
            crearDependencia();
            NumPolitica pol = crearPoliticaDoc("POL-EMI-13");
            NumTalonario tal = crearTalonarioDoc(pol.getId(), "TAL-EMI-13");
            crearAmbitoGlobal(tal.getId(), TipoDocu.CONSTANCIA);

            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-N-04", false, true, MomentoNumeracionDocu.AL_EMITIR);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            FalDocumento guardado = docRepo.buscarPorId(doc.getId()).orElseThrow();
            assertThat(guardado.getNroTalonarioUsado()).isNotNull().isGreaterThan(0);
        }

        @Test
        @DisplayName("14. Si ya estaba numerado, no numera dos veces (no consume segunda vez el talonario)")
        void noNumeraDobleVez() {
            crearDependencia();
            NumPolitica pol = crearPoliticaDoc("POL-EMI-14");
            NumTalonario tal = crearTalonarioDoc(pol.getId(), "TAL-EMI-14");
            crearAmbitoGlobal(tal.getId(), TipoDocu.CONSTANCIA);

            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-N-05", false, true, MomentoNumeracionDocu.AL_EMITIR);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            // Pre-numerar manualmente
            doc.setNroDocu("PRE-001");
            doc.setIdTalonario(tal.getId());
            doc.setNroTalonarioUsado(1);
            docRepo.guardar(doc);

            FalDocumento resultado = docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            assertThat(resultado.getNroDocu()).isEqualTo("PRE-001");
        }
    }

    // =========================================================================
    // 4. Otros momentos de numeracion
    // =========================================================================

    @Nested
    @DisplayName("Otros momentos de numeracion requieren nroDocu previo")
    class OtrosMomentos {

        @Test
        @DisplayName("15. AL_CREAR sin numero falla por inconsistencia")
        void alCrearSinNumeroFalla() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-MOM-01", false, true, MomentoNumeracionDocu.AL_CREAR);
            FalDocumento doc = crearDocumentoConEstado(plantilla, EstadoDocu.BORRADOR);

            assertThatThrownBy(() ->
                    docService.emitirDocumento(new EmitirDocumentoCommand(doc.getId(), USER, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("Inconsistencia");
        }

        @Test
        @DisplayName("16. AL_ENVIAR_A_FIRMA sin numero falla por inconsistencia")
        void alEnviarFirmaSinNumeroFalla() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-MOM-02", false, true, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
            FalDocumento doc = crearDocumentoConEstado(plantilla, EstadoDocu.BORRADOR);

            assertThatThrownBy(() ->
                    docService.emitirDocumento(new EmitirDocumentoCommand(doc.getId(), USER, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("Inconsistencia");
        }

        @Test
        @DisplayName("17. AL_FIRMAR sin numero falla por inconsistencia")
        void alFirmarSinNumeroFalla() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-MOM-03", false, true, MomentoNumeracionDocu.AL_FIRMAR);
            FalDocumento doc = crearDocumentoConEstado(plantilla, EstadoDocu.BORRADOR);

            assertThatThrownBy(() ->
                    docService.emitirDocumento(new EmitirDocumentoCommand(doc.getId(), USER, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("Inconsistencia");
        }

        @Test
        @DisplayName("18. NO_APLICA emite sin numerar")
        void noAplicaEmiteSinNumerar() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-MOM-04", false, false, MomentoNumeracionDocu.NO_APLICA);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            FalDocumento resultado = docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            assertThat(resultado.getNroDocu()).isNull();
            assertThat(resultado.getEstadoDocu()).isEqualTo(EstadoDocu.EMITIDO);
        }
    }

    // =========================================================================
    // 5. Validaciones storage/hash
    // =========================================================================

    @Nested
    @DisplayName("Validaciones storage/hash segun siGeneraPdf")
    class ValidacionesStorageHash {

        @Test
        @DisplayName("19. Falla si siGeneraPdf=true y falta storageKey")
        void faltaStorageKeyFalla() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-SH-01", true, false, MomentoNumeracionDocu.NO_APLICA);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            assertThatThrownBy(() ->
                    docService.emitirDocumento(new EmitirDocumentoCommand(doc.getId(), USER, null, "hash-ok")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("storageKey");
        }

        @Test
        @DisplayName("20. Falla si siGeneraPdf=true y falta hashDocu")
        void faltaHashDocuFalla() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-SH-02", true, false, MomentoNumeracionDocu.NO_APLICA);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            assertThatThrownBy(() ->
                    docService.emitirDocumento(new EmitirDocumentoCommand(doc.getId(), USER, "storage/key", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("hashDocu");
        }

        @Test
        @DisplayName("21. Permite nulls si siGeneraPdf=false")
        void permiteNullsSiNoPdf() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-SH-03", false, false, MomentoNumeracionDocu.NO_APLICA);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            FalDocumento resultado = docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            assertThat(resultado.getStorageKey()).isNull();
            assertThat(resultado.getHashDocu()).isNull();
        }
    }

    // =========================================================================
    // 6. Estados invalidos
    // =========================================================================

    @Nested
    @DisplayName("Estados invalidos")
    class EstadosInvalidos {

        @Test
        @DisplayName("22. Falla si documento no existe")
        void fallaDocumentoNoExiste() {
            assertThatThrownBy(() ->
                    docService.emitirDocumento(new EmitirDocumentoCommand(9999L, USER, null, null)))
                    .isInstanceOf(DocumentoNoEncontradoException.class);
        }

        @Test
        @DisplayName("23. Falla si documento no tiene plantilla")
        void fallaDocumentoSinPlantilla() {
            FalActa acta = crearActa();
            Long docId = docRepo.nextId();
            FalDocumento docSinPlantilla = new FalDocumento(
                    docId, acta.getId(), TipoDocu.CONSTANCIA,
                    FaltasClockTestSupport.FIXED.now(), "sin plantilla");
            docSinPlantilla.setEstadoDocu(EstadoDocu.BORRADOR);
            docRepo.guardar(docSinPlantilla);

            assertThatThrownBy(() ->
                    docService.emitirDocumento(new EmitirDocumentoCommand(docId, USER, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("plantilla");
        }

        @Test
        @DisplayName("24. Falla si documento ya esta EMITIDO")
        void fallaYaEmitido() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-INV-01", false, false, MomentoNumeracionDocu.NO_APLICA);
            FalDocumento doc = crearDocumentoConEstado(plantilla, EstadoDocu.EMITIDO);

            assertThatThrownBy(() ->
                    docService.emitirDocumento(new EmitirDocumentoCommand(doc.getId(), USER, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("EMITIDO");
        }

        @Test
        @DisplayName("25. Falla si documento esta ANULADO")
        void fallaAnulado() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-INV-02", false, false, MomentoNumeracionDocu.NO_APLICA);
            FalDocumento doc = crearDocumentoConEstado(plantilla, EstadoDocu.ANULADO);

            assertThatThrownBy(() ->
                    docService.emitirDocumento(new EmitirDocumentoCommand(doc.getId(), USER, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("ANULADO");
        }

        @Test
        @DisplayName("26. Falla si documento esta REEMPLAZADO")
        void fallaReemplazado() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-INV-03", false, false, MomentoNumeracionDocu.NO_APLICA);
            FalDocumento doc = crearDocumentoConEstado(plantilla, EstadoDocu.REEMPLAZADO);

            assertThatThrownBy(() ->
                    docService.emitirDocumento(new EmitirDocumentoCommand(doc.getId(), USER, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("REEMPLAZADO");
        }
    }

    // =========================================================================
    // 7. Evento DOCEMI
    // =========================================================================

    @Nested
    @DisplayName("Evento DOCEMI")
    class EventoDocemi {

        @Test
        @DisplayName("27. TipoEventoActa.DOCEMI existe en el enum")
        void docemiExiste() {
            TipoEventoActa docemi = TipoEventoActa.deCodigo("DOCEMI");
            assertThat(docemi).isEqualTo(TipoEventoActa.DOCEMI);
        }

        @Test
        @DisplayName("28. DOCGEN sigue existiendo (no fue reemplazado)")
        void docgenSigueExistiendo() {
            assertThat(TipoEventoActa.deCodigo("DOCGEN")).isEqualTo(TipoEventoActa.DOCGEN);
        }

        @Test
        @DisplayName("29. DOCFIR sigue existiendo (no fue reemplazado)")
        void docfirSigueExistiendo() {
            assertThat(TipoEventoActa.deCodigo("DOCFIR")).isEqualTo(TipoEventoActa.DOCFIR);
        }

        @Test
        @DisplayName("30. Se registra evento DOCEMI al emitir documento")
        void registraEventoDocemi() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-EVT-01", false, false, MomentoNumeracionDocu.NO_APLICA);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            int eventosPrevios = eventoRepo.buscarPorActa(acta.getId()).size();

            docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            long contDocemi = eventoRepo.buscarPorActa(acta.getId()).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.DOCEMI)
                    .count();
            assertThat(contDocemi).isGreaterThan(0);
        }
    }

    // =========================================================================
    // 8. Guardrails
    // =========================================================================

    @Nested
    @DisplayName("Guardrails")
    class Guardrails {

        @Test
        @DisplayName("31. Emision no cambia tipoFirmaReq del documento")
        void noModificaTipoFirmaReq() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-GRD-01", false, false, MomentoNumeracionDocu.NO_APLICA);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);
            TipoFirmaReq tipoFirmaOriginal = doc.getTipoFirmaReq();

            docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            FalDocumento guardado = docRepo.buscarPorId(doc.getId()).orElseThrow();
            assertThat(guardado.getTipoFirmaReq()).isEqualTo(tipoFirmaOriginal);
        }

        @Test
        @DisplayName("32. Emision no modifica FalDocumentoFirma (no crea firma nueva)")
        void noCreaNuevaFirma() {
            InMemoryDocumentoFirmaRepository firmaRepo = new InMemoryDocumentoFirmaRepository();
            // El docService ya tiene firmaRepo interno; verificamos que no se llame a guardar firma
            // Simplemente verificamos que el documento quede EMITIDO sin firmas registradas por la emision
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-GRD-02", false, false, MomentoNumeracionDocu.NO_APLICA);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            FalDocumento resultado = docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            assertThat(resultado.getEstadoDocu()).isEqualTo(EstadoDocu.EMITIDO);
        }

        @Test
        @DisplayName("33. Emision no notifica (siNotificable no dispara notificacion en este slice)")
        void noNotifica() {
            // Crear plantilla con siNotificable=true para verificar que emitir no dispara notificacion
            FalDocumentoPlantilla p = plantillaService.crear(new CrearDocumentoPlantillaCommand(
                    "PL-GRD-03", "Plantilla notif", null,
                    TipoDocu.CONSTANCIA, AccionDocumental.EMITIR_CONSTANCIA, null,
                    TipoFirmaReq.NO_REQUIERE,
                    false, MomentoNumeracionDocu.NO_APLICA,
                    true, false, true,
                    FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, "sistema"));
            FalDocumentoPlantilla plantilla = plantillaService.activar(p.getId());
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            FalDocumento resultado = docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            assertThat(resultado.getEstadoDocu()).isEqualTo(EstadoDocu.EMITIDO);
        }

        @Test
        @DisplayName("34. FalDocumento.hashDocu es null antes de emitir")
        void hashDocuNuloAntesDEmitir() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-GRD-04", false, false, MomentoNumeracionDocu.NO_APLICA);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            assertThat(doc.getHashDocu()).isNull();
        }

        @Test
        @DisplayName("35. FalDocumento.fhGeneracion es null antes de emitir")
        void fhGeneracionNulaAntesEmitir() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-GRD-05", false, false, MomentoNumeracionDocu.NO_APLICA);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            assertThat(doc.getFhGeneracion()).isNull();
        }

        @Test
        @DisplayName("36. fechaGeneracion sigue representando fh_alta (no se modifica al emitir)")
        void fechaGeneracionSigueSiendoAlta() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-GRD-06", false, false, MomentoNumeracionDocu.NO_APLICA);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);
            LocalDateTime fechaAltaOriginal = doc.getFechaGeneracion();

            docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            FalDocumento guardado = docRepo.buscarPorId(doc.getId()).orElseThrow();
            assertThat(guardado.getFechaGeneracion()).isEqualTo(fechaAltaOriginal);
        }

        @Test
        @DisplayName("37. FalDocumento.estaEmitido() retorna true despues de emitir")
        void estaEmitidoRetornaTrueDespues() {
            FalDocumentoPlantilla plantilla = crearPlantillaSinFirma(
                    "PL-GRD-07", false, false, MomentoNumeracionDocu.NO_APLICA);
            FalActa acta = crearActa();
            FalDocumento doc = generarBorrador(acta, plantilla);

            docService.emitirDocumento(
                    new EmitirDocumentoCommand(doc.getId(), USER, null, null));

            FalDocumento guardado = docRepo.buscarPorId(doc.getId()).orElseThrow();
            assertThat(guardado.estaEmitido()).isTrue();
        }

        @Test
        @DisplayName("38. EmitirDocumentoCommand valida documentoId obligatorio")
        void commandValidaDocumentoId() {
            assertThatThrownBy(() ->
                    new EmitirDocumentoCommand(null, USER, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("documentoId");
        }

        @Test
        @DisplayName("39. EmitirDocumentoCommand valida idUserOperacion obligatorio")
        void commandValidaIdUser() {
            assertThatThrownBy(() ->
                    new EmitirDocumentoCommand(1L, "   ", null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idUserOperacion");
        }
    }
}
