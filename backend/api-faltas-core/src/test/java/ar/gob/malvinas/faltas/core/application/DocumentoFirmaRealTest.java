package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.*;
import ar.gob.malvinas.faltas.core.application.service.*;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.*;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.application.result.RegistrarFirmaDocumentalResultado;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del Micro-slice 8C-6B-1: Registro de firma documental real.
 */
@DisplayName("Micro-slice 8C-6B-1: Firma documental real")
class DocumentoFirmaRealTest {

    private static final String DEP_COD = "DEP-FIRMA-01";
    private static final String USER = "user-firma-test";

    private ActaRepository actaRepo;
    private DocumentoRepository docRepo;
    private DocumentoPlantillaRepository plantillaRepo;
    private DocumentoFirmaRepository firmaRepo;
    private DocumentoFirmaReqRepository firmaReqRepo;
    private FirmanteRepository firmanteRepo;
    private TalonarioRepository talonarioRepo;
    private DependenciaRepository depRepo;

    private InMemoryNotificacionRepository notifRepo;
    private InMemoryFalloActaRepository falloRepo;
    private ActaEventoRepository eventoRepo;

    private DocumentoService docService;
    private DocumentoPlantillaService plantillaService;
    private FirmanteService firmanteService;
    private TalonarioService talonarioService;
    private DependenciaService depService;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        docRepo = new InMemoryDocumentoRepository();
        plantillaRepo = new InMemoryDocumentoPlantillaRepository();
        firmaRepo = new InMemoryDocumentoFirmaRepository();
        firmaReqRepo = new InMemoryDocumentoFirmaReqRepository();
        firmanteRepo = new InMemoryFirmanteRepository();
        talonarioRepo = new InMemoryTalonarioRepository();
        depRepo = new InMemoryDependenciaRepository();

        eventoRepo = new InMemoryActaEventoRepository();
        InMemoryActaSnapshotRepository snapshotRepo = new InMemoryActaSnapshotRepository();
        falloRepo = new InMemoryFalloActaRepository();
        InMemoryInspectorRepository inspectorRepo = new InMemoryInspectorRepository();
        notifRepo = new InMemoryNotificacionRepository();

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo,
                notifRepo,
                new InMemoryPagoVoluntarioRepository(),
                falloRepo,
                new InMemoryApelacionActaRepository(),
                new InMemoryPagoCondenaRepository(), FaltasClockTestSupport.FIXED, snapshotRepo);

        talonarioService = new TalonarioService(talonarioRepo, depRepo, inspectorRepo, FaltasClockTestSupport.FIXED);
        depService = new DependenciaService(depRepo, FaltasClockTestSupport.FIXED);
        plantillaService = new DocumentoPlantillaService(plantillaRepo, FaltasClockTestSupport.FIXED);
        firmanteService = new FirmanteService(firmanteRepo, depRepo, FaltasClockTestSupport.FIXED);

        docService = new DocumentoService(
                actaRepo, docRepo, firmaRepo, eventoRepo, snapshotRepo, recalc, falloRepo,
                plantillaRepo, talonarioService, depRepo, firmaReqRepo, firmanteRepo,
                notifRepo, FaltasClockTestSupport.FIXED);
    }

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

    private void crearDependencia() {
        depService.crear(new CrearDependenciaCommand(
                DEP_COD, "Dep Firma", null, TipoActa.TRANSITO,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(30), "sistema"));
    }

    private void crearTalonarioGlobalParaNotifActa() {
        NumPolitica pol = talonarioService.crearPolitica(new CrearPoliticaNumeracionCommand(
                "POL-FIRMA-NOTIF", "Politica firma notif", ClaseNumeracion.DOCUMENTO,
                false, false, null, false, null, false, null,
                "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, "sistema"));
        NumTalonario tal = talonarioService.crearTalonario(new CrearTalonarioCommand(
                pol.getId(), "TAL-FIRMA-NOTIF", "Talonario firma notif",
                TipoTalonario.ELECTRONICO, ClaseNumeracion.DOCUMENTO,
                null, null, 1, 9999, "seq_firma_notif",
                true, false, null, null, "sistema"));
        talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                tal.getId(), ClaseNumeracion.DOCUMENTO,
                TipoDocu.NOTIFICACION_ACTA.codigo(), null, null, null,
                AlcanceTalonario.GLOBAL, (short) 10,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, true, "sistema"));
    }

    private FalDocumentoPlantilla crearPlantillaConReq(String codigo, MomentoNumeracionDocu momento, short rolFirmaReq) {
        boolean requiereNum = (momento != MomentoNumeracionDocu.NO_APLICA);
        FalDocumentoPlantilla p = plantillaService.crear(new CrearDocumentoPlantillaCommand(
                codigo, "Plantilla " + codigo,
                TipoDocu.NOTIFICACION_ACTA, AccionDocumental.EMITIR_NOTIFICACION_ACTA, null,
                TipoFirmaReq.FIRMA_INTERNA,
                requiereNum, momento,
                false, false, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, USER));
        plantillaService.agregarFirmaReq(new AgregarFirmaReqPlantillaCommand(
                p.getId(), (short) 1, rolFirmaReq, null, true, true, USER));
        return plantillaService.activar(p.getId());
    }

    private FalDocumentoPlantilla crearPlantillaConDosReqs(String codigo) {
        FalDocumentoPlantilla p = plantillaService.crear(new CrearDocumentoPlantillaCommand(
                codigo, "Plantilla dos reqs " + codigo,
                TipoDocu.NOTIFICACION_ACTA, AccionDocumental.EMITIR_NOTIFICACION_ACTA, null,
                TipoFirmaReq.FIRMA_MULTIPLE,
                false, MomentoNumeracionDocu.NO_APLICA,
                false, false, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, USER));
        plantillaService.agregarFirmaReq(new AgregarFirmaReqPlantillaCommand(
                p.getId(), (short) 1, (short) 1, null, true, true, USER));
        plantillaService.agregarFirmaReq(new AgregarFirmaReqPlantillaCommand(
                p.getId(), (short) 2, (short) 2, null, true, true, USER));
        return plantillaService.activar(p.getId());
    }

    private FalFirmante crearFirmanteConHabilitacion(Short tipoDocu, Short rolFirmaReq, Short mecanismo) {
        FalFirmante f = firmanteService.crear(new CrearFirmanteCommand(
                "user-" + UUID.randomUUID(), "Inspector Firmante",
                "Inspector", "Cargo", null, null,
                FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(30), USER));
        firmanteService.agregarHabilitacion(new AgregarHabilitacionFirmanteCommand(
                f.getIdFirmante(), 1, tipoDocu, rolFirmaReq, mecanismo, USER));
        return f;
    }

    private FalDocumento crearDocPendienteFirma(FalActa acta, FalDocumentoPlantilla plantilla) {
        FalDocumento doc = docService.generarDesdePlantilla(new GenerarDocumentoDesdePlantillaCommand(
                acta.getId(), plantilla.getId(), USER));
        return docService.enviarAFirma(new EnviarAFirmaCommand(doc.getId(), USER));
    }

    private void setOrdenFirmaEnReq(Long docId, short seq, Short orden) {
        FalDocumentoFirmaReq orig = firmaReqRepo.buscarPorDocumentoYSeq(docId, seq).orElseThrow();
        FalDocumentoFirmaReq conOrden = new FalDocumentoFirmaReq(
                orig.getId(), orig.getDocumentoId(), orig.getSeqFirmaReq(), orig.getRolFirmaReq(),
                orig.getMecanismoFirmaReq(), orden,
                orig.isSiObligatoria(), orig.isSiActiva(),
                orig.getFhAlta(), orig.getIdUserAlta());
        firmaReqRepo.guardar(conOrden);
    }

    private RegistrarFirmaDocumentalCommand cmdFirmaElectronica(Long docId, short seq, Long idFirmante) {
        return new RegistrarFirmaDocumentalCommand(
                docId, seq, idFirmante, TipoFirma.ELECTRONICA, USER, null, null, null);
    }

    private RegistrarFirmaDocumentalCommand cmdFirmaDigital(Long docId, short seq, Long idFirmante) {
        return new RegistrarFirmaDocumentalCommand(
                docId, seq, idFirmante, TipoFirma.DIGITAL, USER, "hash-abc123", "ref-ext-001", null);
    }

    @Nested
    @DisplayName("Firma valida")
    class FirmaValida {

        @Test
        @DisplayName("01. Crea FalDocumentoFirma con id Long")
        void test01_creaFirmaConIdLong() {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-F01", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            FalDocumentoFirma firma = docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 1, firmante.getIdFirmante())).firma();

            assertThat(firma).isNotNull();
            assertThat(firma.getId()).isInstanceOf(Long.class).isPositive();
        }

        @Test
        @DisplayName("02. Guarda idDocumento, seqFirmaReq, idFirmante, verFirmante")
        void test02_guardaCamposBasicos() {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-F02", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            FalDocumentoFirma firma = docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 1, firmante.getIdFirmante())).firma();

            assertThat(firma.getIdDocumento()).isEqualTo(doc.getId());
            assertThat(firma.getSeqFirmaReq()).isEqualTo((short) 1);
            assertThat(firma.getIdFirmante()).isEqualTo(firmante.getIdFirmante());
            assertThat(firma.getVerFirmante()).isEqualTo((short) 1);
        }

        @Test
        @DisplayName("03. Guarda TipoFirma.ELECTRONICA y EstadoFirma.FIRMADA")
        void test03_guardaTipoYEstado() {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-F03", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            FalDocumentoFirma firma = docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 1, firmante.getIdFirmante())).firma();

            assertThat(firma.getTipoFirma()).isEqualTo(TipoFirma.ELECTRONICA);
            assertThat(firma.getEstadoFirma()).isEqualTo(EstadoFirma.FIRMADA);
        }

        @Test
        @DisplayName("04. Marca FalDocumentoFirmaReq como FIRMADO con idFirma y fhFirma")
        void test04_marcaReqFirmadoConIdYFecha() {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-F04", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            FalDocumentoFirma firma = docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 1, firmante.getIdFirmante())).firma();

            FalDocumentoFirmaReq req = firmaReqRepo.buscarPorDocumentoYSeq(doc.getId(), (short) 1).orElseThrow();
            assertThat(req.getEstadoFirmaReq()).isEqualTo(EstadoFirmaReq.FIRMADO);
            assertThat(req.getIdFirma()).isEqualTo(firma.getId());
            assertThat(req.getFhFirma()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Cierre del documento")
    class CierreDocumento {

        @Test
        @DisplayName("11. Unico req obligatorio firmado: documento pasa a FIRMADO")
        void test11_unicoReq_documentoPasaFirmado() {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-C01", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            docService.registrarFirmaDocumental(cmdFirmaElectronica(doc.getId(), (short) 1, firmante.getIdFirmante()));

            assertThat(docRepo.buscarPorId(doc.getId()).orElseThrow().getEstadoDocu()).isEqualTo(EstadoDocu.FIRMADO);
        }

        @Test
        @DisplayName("12. Primera firma de dos reqs: documento sigue PENDIENTE_FIRMA")
        void test12_firmasParciales_documentoSiguePendiente() {
            FalActa acta = crearActa();
            FalFirmante f1 = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConDosReqs("PL-C02");
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            docService.registrarFirmaDocumental(cmdFirmaElectronica(doc.getId(), (short) 1, f1.getIdFirmante()));

            assertThat(docRepo.buscarPorId(doc.getId()).orElseThrow().getEstadoDocu())
                    .isEqualTo(EstadoDocu.PENDIENTE_FIRMA);
        }

        @Test
        @DisplayName("13. Firma multiple: primero mantiene PENDIENTE_FIRMA, ultimo cierra FIRMADO")
        void test13_firmaMultiple_primeroMantieneLuegoFirma() {
            FalActa acta = crearActa();
            FalFirmante f1 = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalFirmante f2 = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 2, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConDosReqs("PL-C03");
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            docService.registrarFirmaDocumental(cmdFirmaElectronica(doc.getId(), (short) 1, f1.getIdFirmante()));
            assertThat(docRepo.buscarPorId(doc.getId()).orElseThrow().getEstadoDocu())
                    .isEqualTo(EstadoDocu.PENDIENTE_FIRMA);

            docService.registrarFirmaDocumental(cmdFirmaElectronica(doc.getId(), (short) 2, f2.getIdFirmante()));
            assertThat(docRepo.buscarPorId(doc.getId()).orElseThrow().getEstadoDocu())
                    .isEqualTo(EstadoDocu.FIRMADO);
        }
    }

    @Nested
    @DisplayName("Validaciones documento/requisito")
    class ValidacionesDocReq {

        @Test
        @DisplayName("14. Falla si documento no existe")
        void test14_falla_documentoNoExiste() {
            assertThatThrownBy(() -> docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(9999L, (short) 1, 1L)))
                    .isInstanceOf(DocumentoNoEncontradoException.class);
        }

        @Test
        @DisplayName("15. Falla si documento no esta PENDIENTE_FIRMA")
        void test15_falla_documentoNoPendienteFirma() {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-V01", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = docService.generarDesdePlantilla(new GenerarDocumentoDesdePlantillaCommand(
                    acta.getId(), plantilla.getId(), USER));

            assertThatThrownBy(() -> docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 1, firmante.getIdFirmante())))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("PENDIENTE_FIRMA");
        }

        @Test
        @DisplayName("16. Falla si requisito no existe")
        void test16_falla_requisitoNoExiste() {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-V02", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            assertThatThrownBy(() -> docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 99, firmante.getIdFirmante())))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("17. Falla si requisito esta inactivo")
        void test17_falla_requisitoInactivo() {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-V03", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            FalDocumentoFirmaReq req = firmaReqRepo.buscarPorDocumentoYSeq(doc.getId(), (short) 1).orElseThrow();
            req.setSiActiva(false);
            firmaReqRepo.guardar(req);

            assertThatThrownBy(() -> docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 1, firmante.getIdFirmante())))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("inactivo");
        }

        @Test
        @DisplayName("18. Falla si requisito ya esta FIRMADO")
        void test18_falla_requisitoYaFirmado() {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-V04", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            docService.registrarFirmaDocumental(cmdFirmaElectronica(doc.getId(), (short) 1, firmante.getIdFirmante()));

            // Documento pasa a FIRMADO, nueva firma falla
            assertThatThrownBy(() -> docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 1, firmante.getIdFirmante())))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("19. Falla si requisito esta ANULADO")
        void test19_falla_requisitoAnulado() {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-V05", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            FalDocumentoFirmaReq req = firmaReqRepo.buscarPorDocumentoYSeq(doc.getId(), (short) 1).orElseThrow();
            req.setEstadoFirmaReq(EstadoFirmaReq.ANULADO);
            firmaReqRepo.guardar(req);

            assertThatThrownBy(() -> docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 1, firmante.getIdFirmante())))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("PENDIENTE");
        }
    }

    @Nested
    @DisplayName("Firmante y habilitacion")
    class FirmanteHabilitacion {

        @Test
        @DisplayName("20. Falla si firmante no existe")
        void test20_falla_firmanteNoExiste() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-H01", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            assertThatThrownBy(() -> docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 1, 9999L)))
                    .isInstanceOf(FirmanteNoEncontradoException.class);
        }

        @Test
        @DisplayName("21. Falla si firmante no tiene version vigente")
        void test21_falla_sinVersionVigente() {
            FalActa acta = crearActa();
            FalFirmante firmante = firmanteService.crear(new CrearFirmanteCommand(
                    "user-sv-01", "Sin Version", "Inspector", "Cargo", null, null,
                    FaltasClockTestSupport.FIXED.now().toLocalDate().plusDays(999), USER));
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-H02", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            assertThatThrownBy(() -> docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 1, firmante.getIdFirmante())))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("version vigente");
        }

        @Test
        @DisplayName("22. Falla si no tiene habilitacion para tipoDocu/rol")
        void test22_falla_sinHabilitacion() {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 99, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-H03", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            assertThatThrownBy(() -> docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 1, firmante.getIdFirmante())))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("habilitacion activa");
        }

        @Test
        @DisplayName("23. Falla si req exige mecanismo y habilitacion tiene otro mecanismo")
        void test23_falla_mecanismoIncompatible() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla p = plantillaService.crear(new CrearDocumentoPlantillaCommand(
                    "PL-M01", "Plantilla mecanismo 5",
                    TipoDocu.NOTIFICACION_ACTA, AccionDocumental.EMITIR_NOTIFICACION_ACTA, null,
                    TipoFirmaReq.FIRMA_INTERNA, false, MomentoNumeracionDocu.NO_APLICA,
                    false, false, true, FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, USER));
            plantillaService.agregarFirmaReq(new AgregarFirmaReqPlantillaCommand(
                    p.getId(), (short) 1, (short) 1, (short) 5, true, true, USER));
            FalDocumentoPlantilla plantilla = plantillaService.activar(p.getId());

            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, (short) 3);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            assertThatThrownBy(() -> docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 1, firmante.getIdFirmante())))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("mecanismoFirmaReq");
        }

        @Test
        @DisplayName("24. Firma OK si habilitacion coincide con mecanismo requerido")
        void test24_firmaOK_mecanismoCompatible() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla p = plantillaService.crear(new CrearDocumentoPlantillaCommand(
                    "PL-M02", "Plantilla mecanismo OK",
                    TipoDocu.NOTIFICACION_ACTA, AccionDocumental.EMITIR_NOTIFICACION_ACTA, null,
                    TipoFirmaReq.FIRMA_INTERNA, false, MomentoNumeracionDocu.NO_APLICA,
                    false, false, true, FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, USER));
            plantillaService.agregarFirmaReq(new AgregarFirmaReqPlantillaCommand(
                    p.getId(), (short) 1, (short) 1, (short) 5, true, true, USER));
            FalDocumentoPlantilla plantilla = plantillaService.activar(p.getId());

            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, (short) 5);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            FalDocumentoFirma firma = docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 1, firmante.getIdFirmante())).firma();
            assertThat(firma.getId()).isPositive();
        }
    }

    @Nested
    @DisplayName("Orden de firma")
    class OrdenFirma {

        private FalDocumento crearDocConOrden(String cod, FalActa acta) {
            FalDocumentoPlantilla p = crearPlantillaConDosReqs(cod);
            FalDocumento doc = crearDocPendienteFirma(acta, p);
            setOrdenFirmaEnReq(doc.getId(), (short) 1, (short) 1);
            setOrdenFirmaEnReq(doc.getId(), (short) 2, (short) 2);
            return doc;
        }

        @Test
        @DisplayName("25. Falla firmar orden 2 si orden 1 esta pendiente")
        void test25_falla_orden2ConOrden1Pendiente() {
            FalActa acta = crearActa();
            FalFirmante f2 = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 2, null);
            FalDocumento doc = crearDocConOrden("PL-O01", acta);

            assertThatThrownBy(() -> docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 2, f2.getIdFirmante())))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("orden");
        }

        @Test
        @DisplayName("26. Permite firmar orden 2 si orden 1 ya esta firmado")
        void test26_permite_orden2ConOrden1Firmado() {
            FalActa acta = crearActa();
            FalFirmante f1 = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalFirmante f2 = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 2, null);
            FalDocumento doc = crearDocConOrden("PL-O02", acta);

            docService.registrarFirmaDocumental(cmdFirmaElectronica(doc.getId(), (short) 1, f1.getIdFirmante()));

            FalDocumentoFirma firma2 = docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 2, f2.getIdFirmante())).firma();
            assertThat(firma2.getId()).isPositive();
        }

        @Test
        @DisplayName("27. Sin ordenFirma: no bloquea por orden")
        void test27_sinOrden_noBloquea() {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-O03", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            FalDocumentoFirma firma = docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 1, firmante.getIdFirmante())).firma();
            assertThat(firma.getId()).isPositive();
        }
    }

    @Nested
    @DisplayName("Numeracion AL_FIRMAR")
    class NumeracionAlFirmar {

        @Test
        @DisplayName("28. AL_FIRMAR: numera antes de firmar si no estaba numerado")
        void test28_alFirmar_numeraAntesDeFirmar() {
            crearDependencia();
            crearTalonarioGlobalParaNotifActa();
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-AL01", MomentoNumeracionDocu.AL_FIRMAR, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            assertThat(doc.getNroDocu()).isNull();

            docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 1, firmante.getIdFirmante()));

            FalDocumento docAct = docRepo.buscarPorId(doc.getId()).orElseThrow();
            assertThat(docAct.getNroDocu()).isNotNull();
            assertThat(docAct.getIdTalonario()).isNotNull();
            assertThat(docAct.getNroTalonarioUsado()).isNotNull();
        }

        @Test
        @DisplayName("29. AL_FIRMAR: si ya estaba numerado, no numera dos veces")
        void test29_alFirmar_yaNumerad_noReNumera() {
            crearDependencia();
            crearTalonarioGlobalParaNotifActa();
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-AL02", MomentoNumeracionDocu.AL_FIRMAR, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            docService.numerarDocumento(new NumerarDocumentoCommand(doc.getId(), USER));
            String nroOriginal = docRepo.buscarPorId(doc.getId()).orElseThrow().getNroDocu();

            docService.registrarFirmaDocumental(
                    cmdFirmaElectronica(doc.getId(), (short) 1, firmante.getIdFirmante()));

            assertThat(docRepo.buscarPorId(doc.getId()).orElseThrow().getNroDocu()).isEqualTo(nroOriginal);
        }
    }

    @Nested
    @DisplayName("Firma DIGITAL")
    class FirmaDigital {

        @Test
        @DisplayName("32. Falla firma DIGITAL sin hashDocumento")
        void test32_falla_digitalSinHash() {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-D01", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            assertThatThrownBy(() -> docService.registrarFirmaDocumental(new RegistrarFirmaDocumentalCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(),
                    TipoFirma.DIGITAL, USER, null, "ref-ext", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("hashDocumento");
        }

        @Test
        @DisplayName("33. Falla firma DIGITAL sin referenciaFirmaExt")
        void test33_falla_digitalSinReferencia() {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-D02", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            assertThatThrownBy(() -> docService.registrarFirmaDocumental(new RegistrarFirmaDocumentalCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(),
                    TipoFirma.DIGITAL, USER, "hash-abc", null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("referenciaFirmaExt");
        }

        @Test
        @DisplayName("34. Firma DIGITAL valida con hash y referencia")
        void test34_firmaDigital_valida() {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-D03", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            FalDocumentoFirma firma = docService.registrarFirmaDocumental(
                    cmdFirmaDigital(doc.getId(), (short) 1, firmante.getIdFirmante())).firma();

            assertThat(firma.getTipoFirma()).isEqualTo(TipoFirma.DIGITAL);
            assertThat(firma.getHashDocumento()).isEqualTo("hash-abc123");
            assertThat(firma.getReferenciaFirmaExt()).isEqualTo("ref-ext-001");
        }
    }

    @Nested
    @DisplayName("Guardrails")
    class Guardrails {

        @Test
        @DisplayName("36. No existe FIRMA_MIXTA en ningun enum de firma")
        void test36_noExisteFirmaMixta() {
            for (TipoFirma t : TipoFirma.values()) assertThat(t.name()).isNotEqualTo("FIRMA_MIXTA");
            for (TipoFirmaReq t : TipoFirmaReq.values()) assertThat(t.name()).isNotEqualTo("FIRMA_MIXTA");
        }

        @Test
        @DisplayName("37. TipoFirma tiene exactamente DIGITAL, ELECTRONICA, OLOGRAFA, SISTEMA")
        void test37_tipoFirma_tieneValoresDefinidos() {
            assertThat(TipoFirma.values()).containsExactlyInAnyOrder(
                    TipoFirma.DIGITAL, TipoFirma.ELECTRONICA, TipoFirma.OLOGRAFA, TipoFirma.SISTEMA);
        }

        @Test
        @DisplayName("38. TipoFirma codigos son 1,2,3,4")
        void test38_tipoFirma_codigos() {
            assertThat(TipoFirma.DIGITAL.codigo()).isEqualTo((short) 1);
            assertThat(TipoFirma.ELECTRONICA.codigo()).isEqualTo((short) 2);
            assertThat(TipoFirma.OLOGRAFA.codigo()).isEqualTo((short) 3);
            assertThat(TipoFirma.SISTEMA.codigo()).isEqualTo((short) 4);
        }

        @Test
        @DisplayName("39. FalDocumentoFirma.id es Long, no String")
        void test39_firmaId_esLong() throws Exception {
            java.lang.reflect.Field f = FalDocumentoFirma.class.getDeclaredField("id");
            assertThat(f.getType()).isEqualTo(Long.class);
        }

        @Test
        @DisplayName("40. No hay campo idActa en FalDocumentoFirma")
        void test40_noExiste_idActa_enFalDocumentoFirma() {
            boolean tieneIdActa = false;
            for (java.lang.reflect.Field f : FalDocumentoFirma.class.getDeclaredFields()) {
                if (f.getName().equals("idActa")) tieneIdActa = true;
            }
            assertThat(tieneIdActa).as("FalDocumentoFirma no debe tener campo idActa").isFalse();
        }

        @Test
        @DisplayName("41. TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR es independiente de firma documental")
        void test41_firmaInfractor_esIndependiente() {
            assertThat(TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR.codigo()).isEqualTo((short) 48);
            for (TipoFirma t : TipoFirma.values()) assertThat(t.name()).doesNotContain("INFRACTOR");
        }
    }
    // =========================================================================
    // Idempotencia real de firma (FIX-FALLO-NOTI-01-R1, seccion 7)
    // =========================================================================

    @Nested
    @DisplayName("Idempotencia real de firma")
    class Idempotencia {

        @Test
        @DisplayName("51. Reintento identico: segunda llamada devuelve yaExistia=true sin registros nuevos")
        void test51_reintentoIdentico_yaExistia() {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            // Plantilla notificable (siNotificable=true) para verificar creacion de FalNotificacion
            FalDocumentoPlantilla plantillaBase = plantillaService.crear(new CrearDocumentoPlantillaCommand(
                    "PL-IDP01", "Plantilla IDP01",
                    TipoDocu.NOTIFICACION_ACTA, AccionDocumental.EMITIR_NOTIFICACION_ACTA, null,
                    TipoFirmaReq.FIRMA_INTERNA, false, MomentoNumeracionDocu.NO_APLICA,
                    true, false, true,
                    FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null, USER));
            plantillaService.agregarFirmaReq(new AgregarFirmaReqPlantillaCommand(
                    plantillaBase.getId(), (short) 1, (short) 1, null, true, true, USER));
            FalDocumentoPlantilla plantilla = plantillaService.activar(plantillaBase.getId());
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            // Fallo activo apuntando al documento
            Long falloId = falloRepo.nextId();
            FalActaFallo fallo = new FalActaFallo(falloId, acta.getId(), TipoFalloActa.CONDENATORIO,
                    FaltasClockTestSupport.FIXED.now(), FaltasClockTestSupport.FIXED.now(), USER);
            fallo.setDocumentoId(doc.getId());
            falloRepo.guardar(fallo);

            RegistrarFirmaDocumentalCommand cmd = new RegistrarFirmaDocumentalCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(),
                    TipoFirma.DIGITAL, USER, "hash-idp-001", "ref-idp-001", null);

            // Primera llamada
            RegistrarFirmaDocumentalResultado r1 = docService.registrarFirmaDocumental(cmd);
            assertThat(r1.yaExistia()).isFalse();

            // Segunda llamada identica
            RegistrarFirmaDocumentalResultado r2 = docService.registrarFirmaDocumental(cmd);
            assertThat(r2.yaExistia()).isTrue();
            assertThat(r2.firma().getId()).isEqualTo(r1.firma().getId());

            // Una sola FalDocumentoFirma para el documento
            List<FalDocumentoFirma> firmas = firmaRepo.buscarPorDocumento(doc.getId());
            assertThat(firmas).hasSize(1);
            assertThat(firmas.get(0).getFhFirma()).isNotNull();

            // Documento FIRMADO
            assertThat(docRepo.buscarPorId(doc.getId()).orElseThrow().getEstadoDocu())
                    .isEqualTo(EstadoDocu.FIRMADO);

            // Fallo PENDIENTE_NOTIFICACION
            FalActaFallo falloPost = falloRepo.buscarActivo(acta.getId()).orElseThrow();
            assertThat(falloPost.getEstadoFallo()).isEqualTo(EstadoFalloActa.PENDIENTE_NOTIFICACION);
            assertThat(falloPost.getFhFirma()).isNotNull();

            // Una sola FalNotificacion activa (plantilla siNotificable=true)
            List<FalNotificacion> notifs = notifRepo.buscarPorActa(acta.getId());
            assertThat(notifs).hasSize(1);
            assertThat(notifs.get(0).getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE_ENVIO);
        }

        @Test
        @DisplayName("52. Payload incompatible: misma referenciaFirmaExt con datos distintos lanza PrecondicionVioladaException")
        void test52_payloadIncompatible_lanzaExcepcion() {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-IDP02", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            RegistrarFirmaDocumentalCommand cmdOrig = new RegistrarFirmaDocumentalCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(),
                    TipoFirma.DIGITAL, USER, "hash-idp-002", "ref-idp-002", null);

            // Primera llamada
            docService.registrarFirmaDocumental(cmdOrig);

            // Segunda llamada con misma referenciaFirmaExt pero hash distinto
            RegistrarFirmaDocumentalCommand cmdIncompat = new RegistrarFirmaDocumentalCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(),
                    TipoFirma.DIGITAL, USER, "hash-DIFERENTE", "ref-idp-002", null);

            assertThatThrownBy(() -> docService.registrarFirmaDocumental(cmdIncompat))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("incompatibles");

            // Sin registros adicionales: solo 1 firma
            assertThat(firmaRepo.buscarPorDocumento(doc.getId())).hasSize(1);
        }

        @Test
        @DisplayName("53. Dos llamadas concurrentes con misma referenciaFirmaExt: una crea, la otra devuelve yaExistia=true")
        void test53_concurrente_unaGana_otraYaExistia() throws Exception {
            FalActa acta = crearActa();
            FalFirmante firmante = crearFirmanteConHabilitacion(TipoDocu.NOTIFICACION_ACTA.codigo(), (short) 1, null);
            FalDocumentoPlantilla plantilla = crearPlantillaConReq("PL-IDP03", MomentoNumeracionDocu.NO_APLICA, (short) 1);
            FalDocumento doc = crearDocPendienteFirma(acta, plantilla);

            Long falloId = falloRepo.nextId();
            FalActaFallo fallo = new FalActaFallo(falloId, acta.getId(), TipoFalloActa.ABSOLUTORIO,
                    FaltasClockTestSupport.FIXED.now(), FaltasClockTestSupport.FIXED.now(), USER);
            fallo.setDocumentoId(doc.getId());
            falloRepo.guardar(fallo);

            RegistrarFirmaDocumentalCommand cmd = new RegistrarFirmaDocumentalCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(),
                    TipoFirma.DIGITAL, USER, "hash-conc-001", "ref-conc-001", null);

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            AtomicInteger nuevasCount = new AtomicInteger(0);
            AtomicInteger existentesCount = new AtomicInteger(0);

            ExecutorService pool = Executors.newFixedThreadPool(2);
            Future<RegistrarFirmaDocumentalResultado> f1 = pool.submit(() -> {
                ready.countDown();
                go.await();
                return docService.registrarFirmaDocumental(cmd);
            });
            Future<RegistrarFirmaDocumentalResultado> f2 = pool.submit(() -> {
                ready.countDown();
                go.await();
                return docService.registrarFirmaDocumental(cmd);
            });

            ready.await();
            go.countDown();

            RegistrarFirmaDocumentalResultado r1 = f1.get();
            RegistrarFirmaDocumentalResultado r2 = f2.get();
            pool.shutdown();

            if (!r1.yaExistia()) nuevasCount.incrementAndGet(); else existentesCount.incrementAndGet();
            if (!r2.yaExistia()) nuevasCount.incrementAndGet(); else existentesCount.incrementAndGet();

            // Exactamente una nueva y una existente
            assertThat(nuevasCount.get()).isEqualTo(1);
            assertThat(existentesCount.get()).isEqualTo(1);

            // Ambas devuelven la misma firma
            assertThat(r1.firma().getId()).isEqualTo(r2.firma().getId());

            // Una sola FalDocumentoFirma para el documento
            assertThat(firmaRepo.buscarPorDocumento(doc.getId())).hasSize(1);

            // Un solo evento DOCFIR
            long docfirCount = eventoRepo.buscarPorActa(acta.getId()).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.DOCFIR)
                    .count();
            assertThat(docfirCount).isEqualTo(1);

            // Una sola notificacion (plantilla no-notificable en este caso, sin FalNotificacion)
            // El fallo debe estar en PENDIENTE_NOTIFICACION
            FalActaFallo falloPost = falloRepo.buscarActivo(acta.getId()).orElseThrow();
            assertThat(falloPost.getEstadoFallo()).isEqualTo(EstadoFalloActa.PENDIENTE_NOTIFICACION);
            assertThat(falloPost.getFhFirma()).isNotNull();
        }
    }

}
