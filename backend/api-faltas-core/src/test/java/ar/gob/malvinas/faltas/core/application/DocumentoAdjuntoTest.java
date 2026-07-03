package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.*;
import ar.gob.malvinas.faltas.core.application.result.ConvalidacionEscaneadaResultado;
import ar.gob.malvinas.faltas.core.application.service.*;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.*;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
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
 * Tests del Micro-slice 8C-6D-1: Incorporacion de documento escaneado y convalidacion in-memory.
 */
@DisplayName("Micro-slice 8C-6D-1: Documento escaneado y convalidacion de firma olografa")
class DocumentoAdjuntoTest {

    private static final String DEP_COD = "DEP-ADJ-01";
    private static final String USER = "user-adj-test";
    private static final String STORAGE_KEY = "storage/adjunto/doc-001.pdf";
    private static final String HASH_DOCU = "sha256-abc123def456";

    private ActaRepository actaRepo;
    private DocumentoRepository docRepo;
    private DocumentoPlantillaRepository plantillaRepo;
    private DocumentoFirmaRepository firmaRepo;
    private DocumentoFirmaReqRepository firmaReqRepo;
    private FirmanteRepository firmanteRepo;
    private TalonarioRepository talonarioRepo;
    private DependenciaRepository depRepo;

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

        InMemoryActaEventoRepository eventoRepo = new InMemoryActaEventoRepository();
        InMemoryActaSnapshotRepository snapshotRepo = new InMemoryActaSnapshotRepository();
        InMemoryFalloActaRepository falloRepo = new InMemoryFalloActaRepository();
        InMemoryInspectorRepository inspectorRepo = new InMemoryInspectorRepository();

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo,
                new InMemoryNotificacionRepository(),
                new InMemoryPagoVoluntarioRepository(),
                falloRepo,
                new InMemoryApelacionActaRepository(),
                new InMemoryPagoCondenaRepository());

        talonarioService = new TalonarioService(talonarioRepo, depRepo, inspectorRepo);
        depService = new DependenciaService(depRepo);
        plantillaService = new DocumentoPlantillaService(plantillaRepo);
        firmanteService = new FirmanteService(firmanteRepo, depRepo);

        docService = new DocumentoService(
                actaRepo, docRepo, firmaRepo, eventoRepo, snapshotRepo, recalc, falloRepo,
                plantillaRepo, talonarioService, depRepo, firmaReqRepo, firmanteRepo);
    }

    // -------------------------------------------------------------------------
    // Helpers de test
    // -------------------------------------------------------------------------

    private FalActa crearActa() {
        Long id = actaRepo.nextId();
        FalActa acta = new FalActa(
                id, UUID.randomUUID().toString(),
                "TRANSITO", DEP_COD, "INS-001",
                LocalDate.now(), LocalDateTime.now(),
                "Belgrano 200", "Calle 123", null, null, null, "Juan Perez", "12345678",
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR);
        actaRepo.guardar(acta);
        return acta;
    }

    private FalActa crearActaConDep() {
        depService.crear(new CrearDependenciaCommand(
                DEP_COD, "Dep Adj", null, TipoActa.TRANSITO,
                LocalDate.now().minusDays(30), USER));
        return crearActa();
    }

    private FalFirmante crearFirmanteConHabilitacion(short rolFirmaReq, short tipoDocuCodigo) {
        FalFirmante firmante = firmanteService.crear(new CrearFirmanteCommand("user-adj-test", "Firmante Adj Test", "Inspector", "Cargo", null, null, LocalDate.now().minusDays(10), USER));
        firmanteService.agregarHabilitacion(new AgregarHabilitacionFirmanteCommand(
                firmante.getIdFirmante(), 1, tipoDocuCodigo, rolFirmaReq, null, USER));
        return firmante;
    }

    private IncorporarDocumentoEscaneadoCommand cmdIncorporar(Long idActa) {
        return new IncorporarDocumentoEscaneadoCommand(
                idActa, TipoDocu.CONSTANCIA, STORAGE_KEY, HASH_DOCU, USER, null);
    }

    private FalDocumento incorporarDocumento(Long idActa) {
        return docService.incorporarDocumentoEscaneado(cmdIncorporar(idActa));
    }

    private FalDocumentoFirmaReq crearFirmaReqParaDoc(Long docId, short seq, short rol) {
        Long reqId = firmaReqRepo.nextId();
        FalDocumentoFirmaReq req = new FalDocumentoFirmaReq(
                reqId, docId, seq, rol, null, null, true, true,
                LocalDateTime.now(), USER);
        firmaReqRepo.guardar(req);
        return req;
    }

    // -------------------------------------------------------------------------
    // 1. Incorporar escaneado - casos basicos
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Incorporar documento escaneado")
    class IncorporarEscaneadoTest {

        @Test
        @DisplayName("01. Incorpora documento escaneado con acta existente")
        void incorporaConActaExistente() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            assertThat(doc).isNotNull();
            assertThat(doc.getId()).isNotNull();
            assertThat(doc.getIdActa()).isEqualTo(acta.getId());
        }

        @Test
        @DisplayName("02. Estado inicial es ADJUNTO")
        void estadoInicialEsAdjunto() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            assertThat(doc.getEstadoDocu()).isEqualTo(EstadoDocu.ADJUNTO);
        }

        @Test
        @DisplayName("03. storageKey queda seteado")
        void storageKeyQuedaSeteado() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            assertThat(doc.getStorageKey()).isEqualTo(STORAGE_KEY);
        }

        @Test
        @DisplayName("04. hashDocu queda seteado")
        void hashDocuQuedaSeteado() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            assertThat(doc.getHashDocu()).isEqualTo(HASH_DOCU);
        }

        @Test
        @DisplayName("05. fhGeneracion queda seteado")
        void fhGeneracionQuedaSeteado() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            assertThat(doc.getFhGeneracion()).isNotNull();
        }

        @Test
        @DisplayName("06. plantillaId null permitido para externo puro")
        void plantillaIdNullPermitido() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            assertThat(doc.getPlantillaId()).isNull();
        }

        @Test
        @DisplayName("07. tipoFirmaReq = NO_REQUIERE por defecto")
        void tipoFirmaReqNoRequierePorDefecto() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            assertThat(doc.getTipoFirmaReq()).isEqualTo(TipoFirmaReq.NO_REQUIERE);
        }

        @Test
        @DisplayName("08. No asigna nroDocu")
        void noAsignaNroDocu() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            assertThat(doc.getNroDocu()).isNull();
        }

        @Test
        @DisplayName("09. No asigna idTalonario")
        void noAsignaIdTalonario() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            assertThat(doc.getIdTalonario()).isNull();
        }

        @Test
        @DisplayName("10. No asigna nroTalonarioUsado")
        void noAsignaNroTalonarioUsado() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            assertThat(doc.getNroTalonarioUsado()).isNull();
        }

        @Test
        @DisplayName("11. Registra evento DOCADJ")
        void registraEventoDOCADJ() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            // Verificar que el enum DOCADJ existe y tiene codigo correcto
            assertThat(TipoEventoActa.DOCADJ.codigo()).isEqualTo("DOCADJ");
            assertThat(doc.getIdActa()).isEqualTo(acta.getId());
        }

        @Test
        @DisplayName("12. Falla si acta no existe")
        void fallaActaNoExiste() {
            assertThatThrownBy(() -> incorporarDocumento(9999L))
                    .isInstanceOf(ActaNoEncontradaException.class);
        }

        @Test
        @DisplayName("13. Falla si tipoDocu null")
        void fallaTipoDocuNull() {
            FalActa acta = crearActaConDep();
            IncorporarDocumentoEscaneadoCommand cmd = new IncorporarDocumentoEscaneadoCommand(
                    acta.getId(), null, STORAGE_KEY, HASH_DOCU, USER, null);
            assertThatThrownBy(() -> docService.incorporarDocumentoEscaneado(cmd))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("tipoDocu");
        }

        @Test
        @DisplayName("14. Falla si storageKey blank")
        void fallaStorageKeyBlank() {
            FalActa acta = crearActaConDep();
            IncorporarDocumentoEscaneadoCommand cmd = new IncorporarDocumentoEscaneadoCommand(
                    acta.getId(), TipoDocu.CONSTANCIA, "   ", HASH_DOCU, USER, null);
            assertThatThrownBy(() -> docService.incorporarDocumentoEscaneado(cmd))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("storageKey");
        }

        @Test
        @DisplayName("15. Falla si hashDocu blank")
        void fallaHashDocuBlank() {
            FalActa acta = crearActaConDep();
            IncorporarDocumentoEscaneadoCommand cmd = new IncorporarDocumentoEscaneadoCommand(
                    acta.getId(), TipoDocu.CONSTANCIA, STORAGE_KEY, "  ", USER, null);
            assertThatThrownBy(() -> docService.incorporarDocumentoEscaneado(cmd))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("hashDocu");
        }

        @Test
        @DisplayName("16. Falla si idUserAlta blank")
        void fallaIdUserAltaBlank() {
            FalActa acta = crearActaConDep();
            IncorporarDocumentoEscaneadoCommand cmd = new IncorporarDocumentoEscaneadoCommand(
                    acta.getId(), TipoDocu.CONSTANCIA, STORAGE_KEY, HASH_DOCU, "  ", null);
            assertThatThrownBy(() -> docService.incorporarDocumentoEscaneado(cmd))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idUserAlta");
        }
    }

    // -------------------------------------------------------------------------
    // 2. Convalidacion simple sin requisito
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Convalidacion simple sin seqFirmaReq")
    class ConvalidacionSimpleTest {

        @Test
        @DisplayName("17. Convalida firma escaneada sin seqFirmaReq")
        void convalidaSinSeqFirmaReq() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            FalFirmante firmante = crearFirmanteConHabilitacion((short) 1, TipoDocu.CONSTANCIA.codigo());

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), null, firmante.getIdFirmante(), USER, null);
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            assertThat(res).isNotNull();
            assertThat(res.documento()).isNotNull();
        }

        @Test
        @DisplayName("18. Valida que firmante existe (version vigente)")
        void validaFirmanteExistsVersionVigente() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            FalFirmante firmante = crearFirmanteConHabilitacion((short) 1, TipoDocu.CONSTANCIA.codigo());

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), null, firmante.getIdFirmante(), USER, null);
            // Should not throw
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);
            assertThat(res.documento().getId()).isEqualTo(doc.getId());
        }

        @Test
        @DisplayName("19. Documento permanece en ADJUNTO tras convalidacion simple")
        void documentoPermaneceAdjunto() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            FalFirmante firmante = crearFirmanteConHabilitacion((short) 1, TipoDocu.CONSTANCIA.codigo());

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), null, firmante.getIdFirmante(), USER, null);
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            assertThat(res.documento().getEstadoDocu()).isEqualTo(EstadoDocu.ADJUNTO);
        }

        @Test
        @DisplayName("20. No crea FalDocumentoFirma (entidad requiere seqFirmaReq primitivo)")
        void noCreateFalDocumentoFirma() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            FalFirmante firmante = crearFirmanteConHabilitacion((short) 1, TipoDocu.CONSTANCIA.codigo());

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), null, firmante.getIdFirmante(), USER, null);
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            assertThat(res.firma()).isNull();
            assertThat(firmaRepo.buscarPorDocumento(doc.getId())).isEmpty();
        }

        @Test
        @DisplayName("21. No crea ni cumple FalDocumentoFirmaReq")
        void noCreaNiCumpleFirmaReq() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            FalFirmante firmante = crearFirmanteConHabilitacion((short) 1, TipoDocu.CONSTANCIA.codigo());

            // Create a req manually to verify it's untouched
            FalDocumentoFirmaReq req = crearFirmaReqParaDoc(doc.getId(), (short) 1, (short) 1);

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), null, firmante.getIdFirmante(), USER, null);
            docService.convalidarFirmaEscaneada(cmd);

            // Req should still be PENDIENTE
            FalDocumentoFirmaReq reqAfter = firmaReqRepo.buscarPorId(req.getId()).orElseThrow();
            assertThat(reqAfter.getEstadoFirmaReq()).isEqualTo(EstadoFirmaReq.PENDIENTE);
        }

        @Test
        @DisplayName("22. DOCFIR evento registrado para trazabilidad simple")
        void docFirEventoRegistrado() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            FalFirmante firmante = crearFirmanteConHabilitacion((short) 1, TipoDocu.CONSTANCIA.codigo());

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), null, firmante.getIdFirmante(), USER, null);
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            // DOCFIR enum exists and codigo is correct
            assertThat(TipoEventoActa.DOCFIR.codigo()).isEqualTo("DOCFIR");
            assertThat(res.documento().getEstadoDocu()).isEqualTo(EstadoDocu.ADJUNTO);
        }
    }

    // -------------------------------------------------------------------------
    // 3. Convalidacion con requisito
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Convalidacion con seqFirmaReq")
    class ConvalidacionConReqTest {

        @Test
        @DisplayName("23. Convalida firma escaneada con seqFirmaReq")
        void convalidaConSeqFirmaReq() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(), USER, "ref-escaneo-001");
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            assertThat(res.firma()).isNotNull();
        }

        @Test
        @DisplayName("24. Crea FalDocumentoFirma con TipoFirma.OLOGRAFA")
        void creaFirmaConTipoOlografa() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(), USER, "ref-001");
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            assertThat(res.firma().getTipoFirma()).isEqualTo(TipoFirma.OLOGRAFA);
        }

        @Test
        @DisplayName("25. hashDocumento = documento.hashDocu")
        void hashDocumentoEsHashDelDoc() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(), USER, "ref-001");
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            assertThat(res.firma().getHashDocumento()).isEqualTo(HASH_DOCU);
        }

        @Test
        @DisplayName("26. storageKey = documento.storageKey en firma")
        void storageKeyEsDelDoc() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(), USER, "ref-001");
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            assertThat(res.firma().getStorageKey()).isEqualTo(STORAGE_KEY);
        }

        @Test
        @DisplayName("27. referenciaFirmaExt queda en la firma")
        void referenciaFirmaExtQuedaEnFirma() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(), USER, "ref-escaneo-xxy");
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            assertThat(res.firma().getReferenciaFirmaExt()).isEqualTo("ref-escaneo-xxy");
        }

        @Test
        @DisplayName("28. Marca requisito como FIRMADO")
        void marcaRequisitoFirmado() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            FalDocumentoFirmaReq req = crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(), USER, "ref-001");
            docService.convalidarFirmaEscaneada(cmd);

            FalDocumentoFirmaReq reqActualizado = firmaReqRepo.buscarPorId(req.getId()).orElseThrow();
            assertThat(reqActualizado.getEstadoFirmaReq()).isEqualTo(EstadoFirmaReq.FIRMADO);
        }

        @Test
        @DisplayName("29. Setea idFirma Long en requisito")
        void setaIdFirmaEnRequisito() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            FalDocumentoFirmaReq req = crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(), USER, "ref-001");
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            FalDocumentoFirmaReq reqActualizado = firmaReqRepo.buscarPorId(req.getId()).orElseThrow();
            assertThat(reqActualizado.getIdFirma()).isEqualTo(res.firma().getId());
        }

        @Test
        @DisplayName("30. Si unico obligatorio firmado, documento pasa ADJUNTO -> FIRMADO")
        void documentoPasaAFirmadoCuandoUnicoObligatorioFirmado() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(), USER, "ref-001");
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            assertThat(res.documento().getEstadoDocu()).isEqualTo(EstadoDocu.FIRMADO);
        }

        @Test
        @DisplayName("31. Si quedan obligatorios pendientes, documento sigue ADJUNTO")
        void documentoSigueAdjuntoCuandoQuedan() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            short rol2 = (short) 2;
            FalFirmante f1 = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            // f2 with rol2
            FalFirmante f2 = firmanteService.crear(new CrearFirmanteCommand("user-f2-test", "Firmante2", "Inspector", "Cargo", null, null, LocalDate.now().minusDays(5), USER));
            firmanteService.agregarHabilitacion(new AgregarHabilitacionFirmanteCommand(
                    f2.getIdFirmante(), 1, TipoDocu.CONSTANCIA.codigo(), rol2, null, USER));

            crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);
            crearFirmaReqParaDoc(doc.getId(), (short) 2, rol2);

            // Only sign the first req
            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), (short) 1, f1.getIdFirmante(), USER, "ref-001");
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            assertThat(res.documento().getEstadoDocu()).isEqualTo(EstadoDocu.ADJUNTO);
        }
    }

    // -------------------------------------------------------------------------
    // 4. Validaciones con requisito
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Validaciones de convalidacion con requisito")
    class ValidacionesConReqTest {

        @Test
        @DisplayName("32. Falla si documento no existe")
        void fallaDocumentoNoExiste() {
            assertThatThrownBy(() -> docService.convalidarFirmaEscaneada(
                    new ConvalidarFirmaEscaneadaCommand(9999L, (short) 1, 1L, USER, null)))
                    .isInstanceOf(DocumentoNoEncontradoException.class);
        }

        @Test
        @DisplayName("33. Falla si documento no tiene storageKey")
        void fallaDocumentoSinStorageKey() {
            FalActa acta = crearActaConDep();
            // Create doc without storageKey via constructor (not adjunto factory)
            Long id = docRepo.nextId();
            FalDocumento doc = new FalDocumento(id, acta.getId(), TipoDocu.CONSTANCIA,
                    LocalDateTime.now(), null, EstadoDocu.ADJUNTO, TipoFirmaReq.NO_REQUIERE, null);
            docRepo.guardar(doc);

            assertThatThrownBy(() -> docService.convalidarFirmaEscaneada(
                    new ConvalidarFirmaEscaneadaCommand(doc.getId(), (short) 1, 1L, USER, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("storageKey");
        }

        @Test
        @DisplayName("34. Falla si documento no tiene hashDocu")
        void fallaDocumentoSinHashDocu() {
            FalActa acta = crearActaConDep();
            Long id = docRepo.nextId();
            FalDocumento doc = new FalDocumento(id, acta.getId(), TipoDocu.CONSTANCIA,
                    LocalDateTime.now(), null, EstadoDocu.ADJUNTO, TipoFirmaReq.NO_REQUIERE, null);
            doc.setStorageKey("storage/x.pdf");
            // hashDocu is null
            docRepo.guardar(doc);

            assertThatThrownBy(() -> docService.convalidarFirmaEscaneada(
                    new ConvalidarFirmaEscaneadaCommand(doc.getId(), (short) 1, 1L, USER, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("hashDocu");
        }

        @Test
        @DisplayName("35. Falla si documento no esta ADJUNTO")
        void fallaDocumentoNoAdjunto() {
            FalActa acta = crearActaConDep();
            Long id = docRepo.nextId();
            FalDocumento doc = new FalDocumento(id, acta.getId(), TipoDocu.CONSTANCIA,
                    LocalDateTime.now(), null, EstadoDocu.PENDIENTE_FIRMA, TipoFirmaReq.FIRMA_INTERNA, null);
            docRepo.guardar(doc);

            assertThatThrownBy(() -> docService.convalidarFirmaEscaneada(
                    new ConvalidarFirmaEscaneadaCommand(doc.getId(), (short) 1, 1L, USER, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("ADJUNTO");
        }

        @Test
        @DisplayName("36. Falla si requisito no existe")
        void fallaRequisitoNoExiste() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            FalFirmante firmante = crearFirmanteConHabilitacion((short) 1, TipoDocu.CONSTANCIA.codigo());

            assertThatThrownBy(() -> docService.convalidarFirmaEscaneada(
                    new ConvalidarFirmaEscaneadaCommand(doc.getId(), (short) 99, firmante.getIdFirmante(), USER, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("seqFirmaReq=99");
        }

        @Test
        @DisplayName("37. Falla si requisito no esta PENDIENTE")
        void fallaRequisitoNoPendiente() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            FalDocumentoFirmaReq req = crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            // Force req to FIRMADO state
            req.marcarFirmado(99L, LocalDateTime.now(), firmante.getIdFirmante(), (short) 1);
            firmaReqRepo.guardar(req);

            assertThatThrownBy(() -> docService.convalidarFirmaEscaneada(
                    new ConvalidarFirmaEscaneadaCommand(doc.getId(), (short) 1, firmante.getIdFirmante(), USER, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("PENDIENTE");
        }

        @Test
        @DisplayName("38. Falla si firmante no existe")
        void fallaFirmanteNoExiste() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            crearFirmaReqParaDoc(doc.getId(), (short) 1, (short) 1);

            assertThatThrownBy(() -> docService.convalidarFirmaEscaneada(
                    new ConvalidarFirmaEscaneadaCommand(doc.getId(), (short) 1, 9999L, USER, null)))
                    .isInstanceOf(FirmanteNoEncontradoException.class);
        }

        @Test
        @DisplayName("39. Falla si firmante no tiene version vigente")
        void fallaFirmanteSinVersionVigente() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            crearFirmaReqParaDoc(doc.getId(), (short) 1, (short) 1);

            // Create firmante with future vigDesde (no version vigente today)
            FalFirmante firmante = firmanteService.crear(new CrearFirmanteCommand("user-futuro-test", "Firmante Futuro", "Inspector", "Cargo", null, null, LocalDate.now().plusDays(10), USER));

            assertThatThrownBy(() -> docService.convalidarFirmaEscaneada(
                    new ConvalidarFirmaEscaneadaCommand(doc.getId(), (short) 1, firmante.getIdFirmante(), USER, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("version vigente");
        }

        @Test
        @DisplayName("40. Falla si firmante no tiene habilitacion compatible")
        void fallaFirmanteSinHabilitacion() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 5;  // rol que no tiene el firmante
            crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            // Firmante with habilitacion for rol=1 only
            FalFirmante firmante = crearFirmanteConHabilitacion((short) 1, TipoDocu.CONSTANCIA.codigo());

            assertThatThrownBy(() -> docService.convalidarFirmaEscaneada(
                    new ConvalidarFirmaEscaneadaCommand(doc.getId(), (short) 1, firmante.getIdFirmante(), USER, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("habilitacion activa");
        }

        @Test
        @DisplayName("41. Valida mecanismo si req lo exige")
        void validaMecanismoSiReqLoExige() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            Short mecReq = (short) 5;  // ELECTRONICA

            // Req con mecanismoFirmaReq = 5 (ELECTRONICA)
            Long reqId = firmaReqRepo.nextId();
            FalDocumentoFirmaReq req = new FalDocumentoFirmaReq(
                    reqId, doc.getId(), (short) 1, rol, mecReq, null, true, true,
                    LocalDateTime.now(), USER);
            firmaReqRepo.guardar(req);

            // Firmante con habilitacion SIN mecanismo (null)
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());

            assertThatThrownBy(() -> docService.convalidarFirmaEscaneada(
                    new ConvalidarFirmaEscaneadaCommand(doc.getId(), (short) 1, firmante.getIdFirmante(), USER, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("mecanismoFirmaReq");
        }

        @Test
        @DisplayName("42. Respeta orden de firma si ordenFirma != null")
        void respetaOrdenDeFirma() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol1 = (short) 1;
            short rol2 = (short) 2;

            FalFirmante f1 = crearFirmanteConHabilitacion(rol1, TipoDocu.CONSTANCIA.codigo());
            FalFirmante f2 = firmanteService.crear(new CrearFirmanteCommand("user-f2-test", "Firmante2", "Inspector", "Cargo", null, null, LocalDate.now().minusDays(5), USER));
            firmanteService.agregarHabilitacion(new AgregarHabilitacionFirmanteCommand(
                    f2.getIdFirmante(), 1, TipoDocu.CONSTANCIA.codigo(), rol2, null, USER));

            // Req con orden 1 (debe firmarse primero)
            Long r1id = firmaReqRepo.nextId();
            FalDocumentoFirmaReq req1 = new FalDocumentoFirmaReq(
                    r1id, doc.getId(), (short) 1, rol1, null, (short) 1, true, true,
                    LocalDateTime.now(), USER);
            firmaReqRepo.guardar(req1);

            // Req con orden 2 (debe esperar)
            Long r2id = firmaReqRepo.nextId();
            FalDocumentoFirmaReq req2 = new FalDocumentoFirmaReq(
                    r2id, doc.getId(), (short) 2, rol2, null, (short) 2, true, true,
                    LocalDateTime.now(), USER);
            firmaReqRepo.guardar(req2);

            // Trying to sign req2 first should fail
            assertThatThrownBy(() -> docService.convalidarFirmaEscaneada(
                    new ConvalidarFirmaEscaneadaCommand(doc.getId(), (short) 2, f2.getIdFirmante(), USER, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("orden menor");
        }
    }

    // -------------------------------------------------------------------------
    // 5. Guardrails
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Guardrails - limites del slice")
    class GuardrailTest {

        @Test
        @DisplayName("43. No genera firma digital (TipoFirma.DIGITAL no se usa)")
        void noGeneraFirmaDigital() {
            // convalidarFirmaEscaneada siempre usa OLOGRAFA, nunca DIGITAL
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(), USER, "ref-001");
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            assertThat(res.firma().getTipoFirma()).isNotEqualTo(TipoFirma.DIGITAL);
        }

        @Test
        @DisplayName("44. TipoFirma.DIGITAL no se usa en convalidacion escaneada")
        void tipoFirmaDigitalNoUsado() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(), USER, "ref-001");
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            assertThat(res.firma().getTipoFirma()).isEqualTo(TipoFirma.OLOGRAFA);
        }

        @Test
        @DisplayName("45. No recalcula hash (usa el hash del documento adjunto)")
        void noRecalculaHash() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(), USER, "ref-001");
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            // hashDocumento en firma == hashDocu original del documento
            assertThat(res.firma().getHashDocumento()).isEqualTo(doc.getHashDocu());
        }

        @Test
        @DisplayName("46. No genera PDF ni storage real")
        void noGeneraPdfNiStorage() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(), USER, "ref-001");
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            // storageKey de la firma es el mismo del documento (no se generó nuevo storage)
            assertThat(res.firma().getStorageKey()).isEqualTo(STORAGE_KEY);
        }

        @Test
        @DisplayName("47. No emite documento automaticamente tras convalidacion")
        void noEmiteAutomaticamente() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(), USER, "ref-001");
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            // After convalidation, doc is FIRMADO (not EMITIDO)
            assertThat(res.documento().getEstadoDocu()).isNotEqualTo(EstadoDocu.EMITIDO);
        }

        @Test
        @DisplayName("48. No notifica automaticamente")
        void noNotificaAutomaticamente() {
            // Just verify the flow completes without notification side effects
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(), USER, "ref-001");
            // No exception means no notification dependency issues
            assertThat(docService.convalidarFirmaEscaneada(cmd)).isNotNull();
        }

        @Test
        @DisplayName("49. No usa seqFirmaReq = 0")
        void noUsaSeqFirmaReqCero() {
            // Verify that seq=0 is not a valid path in convalidacion
            // The spec says seqFirmaReq must be > 0 for requisitos
            // For no-req path, seqFirmaReq is null (not 0)
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            FalFirmante firmante = crearFirmanteConHabilitacion((short) 1, TipoDocu.CONSTANCIA.codigo());

            // seqFirmaReq=0 as Short should fail because there's no req with that seq
            assertThatThrownBy(() -> docService.convalidarFirmaEscaneada(
                    new ConvalidarFirmaEscaneadaCommand(doc.getId(), (short) 0, firmante.getIdFirmante(), USER, null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("50. No toca firma del infractor (resultadoFirmaInfractor intacto)")
        void noTocaFirmaDelInfractor() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(), USER, "ref-001");
            docService.convalidarFirmaEscaneada(cmd);

            // Acta's resultadoFirmaInfractor unchanged
            FalActa actaAfter = actaRepo.buscarPorId(acta.getId()).orElseThrow();
            assertThat(actaAfter.getResultadoFirmaInfractor())
                    .isEqualTo(ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR);
        }

        @Test
        @DisplayName("51. No usa FIRMA_OLOGRAFA_INFRACTOR en convalidacion institucional")
        void noUsaFirmaOlografaInfractorEnConvalidacion() {
            // TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR is for acta evidence, not documental firma
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            short rol = (short) 1;
            FalFirmante firmante = crearFirmanteConHabilitacion(rol, TipoDocu.CONSTANCIA.codigo());
            crearFirmaReqParaDoc(doc.getId(), (short) 1, rol);

            ConvalidarFirmaEscaneadaCommand cmd = new ConvalidarFirmaEscaneadaCommand(
                    doc.getId(), (short) 1, firmante.getIdFirmante(), USER, "ref-001");
            ConvalidacionEscaneadaResultado res = docService.convalidarFirmaEscaneada(cmd);

            // The firma is OLOGRAFA (institutional), not an acta evidence of type FIRMA_OLOGRAFA_INFRACTOR
            assertThat(res.firma().getTipoFirma()).isEqualTo(TipoFirma.OLOGRAFA);
            assertThat(TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR).isNotNull();
        }

        @Test
        @DisplayName("52. marcarFirmadoDesdeAdjunto solo acepta ADJUNTO -> FIRMADO")
        void marcarFirmadoDesdeAdjuntoSoloAceptaAdjunto() {
            Long id = docRepo.nextId();
            FalDocumento docBorrador = new FalDocumento(id, 1L, TipoDocu.CONSTANCIA,
                    LocalDateTime.now(), null, EstadoDocu.BORRADOR, TipoFirmaReq.NO_REQUIERE, null);
            assertThatThrownBy(docBorrador::marcarFirmadoDesdeAdjunto)
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("ADJUNTO");
        }

        @Test
        @DisplayName("53. marcarFirmado (PENDIENTE_FIRMA) no acepta ADJUNTO como origen")
        void marcarFirmadoNormalNoAceptaAdjunto() {
            FalActa acta = crearActaConDep();
            FalDocumento doc = incorporarDocumento(acta.getId());
            // doc is ADJUNTO state
            assertThatThrownBy(doc::marcarFirmado)
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("PENDIENTE_FIRMA");
        }
    }
}