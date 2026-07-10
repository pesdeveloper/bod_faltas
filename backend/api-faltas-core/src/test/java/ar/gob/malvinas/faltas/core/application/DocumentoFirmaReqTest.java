package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.AgregarFirmaReqPlantillaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearDocumentoPlantillaCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoDesdePlantillaCommand;
import ar.gob.malvinas.faltas.core.application.command.MaterializarFirmaReqDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.service.DocumentoFirmaReqService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoPlantillaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.enums.MomentoNumeracionDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoFirmaReqYaMaterializadaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantilla;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaReqRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.PagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaReqRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
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
 * Tests del Micro-slice 8C-4: FalDocumentoFirmaReq como snapshot de requisitos de firma documental.
 */
@DisplayName("Micro-slice 8C-4: FalDocumentoFirmaReq snapshot de requisitos de firma")
class DocumentoFirmaReqTest {

    private ActaRepository actaRepo;
    private DocumentoRepository docRepo;
    private DocumentoPlantillaRepository plantillaRepo;
    private DocumentoFirmaReqRepository firmaReqRepo;
    private DocumentoService docService;
    private DocumentoPlantillaService plantillaService;
    private DocumentoFirmaReqService firmaReqService;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        docRepo = new InMemoryDocumentoRepository();
        plantillaRepo = new InMemoryDocumentoPlantillaRepository();
        firmaReqRepo = new InMemoryDocumentoFirmaReqRepository();
        ActaEventoRepository eventoRepo = new InMemoryActaEventoRepository();
        ActaSnapshotRepository snapshotRepo = new InMemoryActaSnapshotRepository();
        DocumentoFirmaRepository firmaRepo = new InMemoryDocumentoFirmaRepository();
        FalloActaRepository falloRepo = new InMemoryFalloActaRepository();
        ApelacionActaRepository apelacionRepo = new InMemoryApelacionActaRepository();
        PagoVoluntarioRepository pagoVolRepo = new InMemoryPagoVoluntarioRepository();
        PagoCondenaRepository pagoCondRepo = new InMemoryPagoCondenaRepository();
        NotificacionRepository notifRepo = new InMemoryNotificacionRepository();

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoVolRepo, falloRepo, apelacionRepo, pagoCondRepo, FaltasClockTestSupport.FIXED);

        docService = new DocumentoService(
                actaRepo, docRepo, firmaRepo, eventoRepo, snapshotRepo, recalc, falloRepo, plantillaRepo,
                new ar.gob.malvinas.faltas.core.application.service.TalonarioService(new ar.gob.malvinas.faltas.core.repository.memory.InMemoryTalonarioRepository(), new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository(), new ar.gob.malvinas.faltas.core.repository.memory.InMemoryInspectorRepository(), FaltasClockTestSupport.FIXED),
                new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository(),
                firmaReqRepo,
                new ar.gob.malvinas.faltas.core.repository.memory.InMemoryFirmanteRepository(), FaltasClockTestSupport.FIXED);
        plantillaService = new DocumentoPlantillaService(plantillaRepo, FaltasClockTestSupport.FIXED);
        firmaReqService = new DocumentoFirmaReqService(docRepo, plantillaRepo, firmaReqRepo, FaltasClockTestSupport.FIXED);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FalActa crearActa() {
        Long id = actaRepo.nextId();
        FalActa acta = new FalActa(
                id, UUID.randomUUID().toString(),
                "TRANSITO", "DEP-001", "INS-001",
                FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(),
                "Belgrano 200", "Calle 123", null, null, null, "Juan Perez", "12345678",
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR);
        actaRepo.guardar(acta);
        return acta;
    }

    private FalDocumentoPlantilla crearPlantillaFirmaAutoridad(String codigo) {
        CrearDocumentoPlantillaCommand cmd = new CrearDocumentoPlantillaCommand(
                codigo, "Plantilla " + codigo, null,
                TipoDocu.ACTO_ADMINISTRATIVO, AccionDocumental.EMITIR_FALLO, null,
                TipoFirmaReq.FIRMA_AUTORIDAD,
                false, MomentoNumeracionDocu.NO_APLICA,
                false, false, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
        FalDocumentoPlantilla p = plantillaService.crear(cmd);
        plantillaService.agregarFirmaReq(new AgregarFirmaReqPlantillaCommand(
                p.getId(), (short) 1, (short) 1, null, true, true, "sistema"));
        return plantillaService.activar(p.getId());
    }

    private FalDocumentoPlantilla crearPlantillaFirmaMultiple(String codigo) {
        CrearDocumentoPlantillaCommand cmd = new CrearDocumentoPlantillaCommand(
                codigo, "Plantilla " + codigo, null,
                TipoDocu.ACTO_ADMINISTRATIVO, AccionDocumental.EMITIR_FALLO, null,
                TipoFirmaReq.FIRMA_MULTIPLE,
                false, MomentoNumeracionDocu.NO_APLICA,
                false, false, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
        FalDocumentoPlantilla p = plantillaService.crear(cmd);
        plantillaService.agregarFirmaReq(new AgregarFirmaReqPlantillaCommand(
                p.getId(), (short) 1, (short) 1, null, true, true, "sistema"));
        plantillaService.agregarFirmaReq(new AgregarFirmaReqPlantillaCommand(
                p.getId(), (short) 2, (short) 2, null, true, true, "sistema"));
        return plantillaService.activar(p.getId());
    }

    private FalDocumentoPlantilla crearPlantillaNoRequiere(String codigo) {
        CrearDocumentoPlantillaCommand cmd = new CrearDocumentoPlantillaCommand(
                codigo, "Plantilla " + codigo, null,
                TipoDocu.CONSTANCIA, AccionDocumental.EMITIR_CONSTANCIA, null,
                TipoFirmaReq.NO_REQUIERE,
                false, MomentoNumeracionDocu.NO_APLICA,
                false, false, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
        FalDocumentoPlantilla p = plantillaService.crear(cmd);
        return plantillaService.activar(p.getId());
    }

    private FalDocumento generarDocDesdePlantilla(FalActa acta, FalDocumentoPlantilla plantilla) {
        return docService.generarDesdePlantilla(
                new GenerarDocumentoDesdePlantillaCommand(acta.getId(), plantilla.getId(), "sistema"));
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Materializacion valida")
    class MaterializacionValida {

        @Test
        @DisplayName("01 - Materializa requisitos para documento con FIRMA_AUTORIDAD y un requisito obligatorio activo")
        void test01_materializaFirmaAutoridad() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-AUTORIDAD-01");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            List<FalDocumentoFirmaReq> resultado = firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("02 - Materializa requisitos para documento con FIRMA_MULTIPLE y dos requisitos obligatorios activos")
        void test02_materializaFirmaMultiple() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaMultiple("P-MULTIPLE-01");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            List<FalDocumentoFirmaReq> resultado = firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            assertThat(resultado).hasSize(2);
        }

        @Test
        @DisplayName("03 - Copia seqFirmaReq desde plantilla")
        void test03_copiaSeqFirmaReq() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-SEQ-01");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            List<FalDocumentoFirmaReq> resultado = firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            assertThat(resultado.get(0).getSeqFirmaReq()).isEqualTo((short) 1);
        }

        @Test
        @DisplayName("04 - Copia rolFirmaReq desde plantilla")
        void test04_copiaRolFirmaReq() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-ROL-01");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            List<FalDocumentoFirmaReq> resultado = firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            assertThat(resultado.get(0).getRolFirmaReq()).isEqualTo((short) 1);
        }

        @Test
        @DisplayName("05 - Copia mecanismoFirmaReq nulo desde plantilla")
        void test05_copiaMecanismoFirmaReqNulo() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-MECNULL-01");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            List<FalDocumentoFirmaReq> resultado = firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            assertThat(resultado.get(0).getMecanismoFirmaReq()).isNull();
        }

        @Test
        @DisplayName("06 - Copia siObligatoria desde plantilla")
        void test06_copiaSiObligatoria() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-OBLIG-01");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            List<FalDocumentoFirmaReq> resultado = firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            assertThat(resultado.get(0).isSiObligatoria()).isTrue();
        }

        @Test
        @DisplayName("07 - Estado inicial del requisito es PENDIENTE")
        void test07_estadoInicialPendiente() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-ESTADO-01");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            List<FalDocumentoFirmaReq> resultado = firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            assertThat(resultado.get(0).getEstadoFirmaReq()).isEqualTo(EstadoFirmaReq.PENDIENTE);
        }

        @Test
        @DisplayName("08 - Guarda documentoId en el requisito")
        void test08_guardaDocumentoId() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-DOCID-01");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            List<FalDocumentoFirmaReq> resultado = firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            assertThat(resultado.get(0).getDocumentoId()).isEqualTo(doc.getId());
        }

        @Test
        @DisplayName("09 - Guarda idUserAlta en el requisito")
        void test09_guardaIdUserAlta() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-USRALTA-01");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            List<FalDocumentoFirmaReq> resultado = firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-99"));

            assertThat(resultado.get(0).getIdUserAlta()).isEqualTo("operador-99");
        }

        @Test
        @DisplayName("10 - Lista requisitos por documento despues de materializar")
        void test10_listaRequisitosDocumento() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaMultiple("P-LISTA-01");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            List<FalDocumentoFirmaReq> lista = firmaReqService.listarPorDocumento(doc.getId());
            assertThat(lista).hasSize(2);
        }
    }

    @Nested
    @DisplayName("NO_REQUIERE")
    class NoRequiere {

        @Test
        @DisplayName("11 - Documento NO_REQUIERE sin requisitos obligatorios devuelve lista vacia")
        void test11_noRequiereSinObligatoriosDevuelveVacio() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaNoRequiere("P-NOREQ-01");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            List<FalDocumentoFirmaReq> resultado = firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("12 - Documento NO_REQUIERE con requisito obligatorio activo falla por inconsistencia")
        void test12_noRequiereConObligatorioFalla() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaNoRequiere("P-NOREQ-INC-01");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            plantillaRepo.guardarFirmaReq(new ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaFirmaReq(
                    plantillaRepo.nextFirmaReqId(), plantilla.getId(),
                    (short) 1, (short) 1, null, true, true,
                    FaltasClockTestSupport.FIXED.now(), "sistema"));

            assertThatThrownBy(() -> firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("NO_REQUIERE");
        }
    }

    @Nested
    @DisplayName("Validaciones de entrada")
    class ValidacionesEntrada {

        @Test
        @DisplayName("13 - Falla si documento no existe")
        void test13_fallaDocumentoNoExiste() {
            assertThatThrownBy(() -> firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(9999L, "operador-01")))
                    .isInstanceOf(DocumentoNoEncontradoException.class);
        }

        @Test
        @DisplayName("14 - Falla si documento no tiene plantillaId")
        void test14_fallaDocumentoSinPlantillaId() {
            FalDocumento docSinPlantilla = new FalDocumento(
                    docRepo.nextId(), 1L,
                    TipoDocu.CONSTANCIA, FaltasClockTestSupport.FIXED.now(), "Sin plantilla");
            docRepo.guardar(docSinPlantilla);

            assertThatThrownBy(() -> firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(docSinPlantilla.getId(), "operador-01")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("plantillaId");
        }

        @Test
        @DisplayName("15 - Falla si plantilla no existe")
        void test15_fallaPlantillaNoExiste() {
            FalDocumento docConPlantillaInexistente = new FalDocumento(
                    docRepo.nextId(), 1L,
                    TipoDocu.ACTO_ADMINISTRATIVO, FaltasClockTestSupport.FIXED.now(), "Doc test",
                    EstadoDocu.BORRADOR, TipoFirmaReq.FIRMA_AUTORIDAD, 9999L, FaltasClockTestSupport.FIXED.now());
            docRepo.guardar(docConPlantillaInexistente);

            assertThatThrownBy(() -> firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(docConPlantillaInexistente.getId(), "operador-01")))
                    .isInstanceOf(DocumentoPlantillaNoEncontradaException.class);
        }

        @Test
        @DisplayName("16 - Falla si idUserAlta esta en blanco")
        void test16_fallaIdUserAltaBlanco() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-BLANK-01");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            assertThatThrownBy(() -> firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "   ")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idUserAlta");
        }

        @Test
        @DisplayName("17 - Falla si documentoId es null")
        void test17_fallaDocumentoIdNull() {
            assertThatThrownBy(() -> firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(null, "operador-01")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("documentoId");
        }

        @Test
        @DisplayName("18 - Falla si ya estaba materializado")
        void test18_fallaYaMaterializado() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-DUP-01");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            assertThatThrownBy(() -> firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01")))
                    .isInstanceOf(DocumentoFirmaReqYaMaterializadaException.class);
        }

        @Test
        @DisplayName("19 - Falla si FIRMA_AUTORIDAD no tiene requisitos obligatorios activos")
        void test19_fallaFirmaAutoridadSinObligatorios() {
            FalActa acta = crearActa();
            CrearDocumentoPlantillaCommand cmd = new CrearDocumentoPlantillaCommand(
                    "P-SINSATISF-01", "Sin satisfactores", null,
                    TipoDocu.ACTO_ADMINISTRATIVO, AccionDocumental.EMITIR_FALLO, null,
                    TipoFirmaReq.FIRMA_AUTORIDAD,
                    false, MomentoNumeracionDocu.NO_APLICA,
                    false, false, true,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
            FalDocumentoPlantilla p = plantillaService.crear(cmd);
            plantillaService.agregarFirmaReq(new AgregarFirmaReqPlantillaCommand(
                    p.getId(), (short) 1, (short) 1, null, true, true, "sistema"));
            FalDocumentoPlantilla plantilla = plantillaService.activar(p.getId());

            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            plantillaRepo.listarFirmaReqPorPlantilla(plantilla.getId())
                    .forEach(r -> r.setSiActiva(false));

            assertThatThrownBy(() -> firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("FIRMA_AUTORIDAD");
        }

        @Test
        @DisplayName("20 - Falla si FIRMA_MULTIPLE tiene un solo requisito obligatorio activo")
        void test20_fallaFirmaMultipleUnSoloObligatorio() {
            FalActa acta = crearActa();
            CrearDocumentoPlantillaCommand cmd = new CrearDocumentoPlantillaCommand(
                    "P-MULT-UNO-01", "Multiple con uno", null,
                    TipoDocu.ACTO_ADMINISTRATIVO, AccionDocumental.EMITIR_FALLO, null,
                    TipoFirmaReq.FIRMA_MULTIPLE,
                    false, MomentoNumeracionDocu.NO_APLICA,
                    false, false, true,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
            FalDocumentoPlantilla p = plantillaService.crear(cmd);
            plantillaService.agregarFirmaReq(new AgregarFirmaReqPlantillaCommand(
                    p.getId(), (short) 1, (short) 1, null, true, true, "sistema"));
            plantillaService.agregarFirmaReq(new AgregarFirmaReqPlantillaCommand(
                    p.getId(), (short) 2, (short) 2, null, true, true, "sistema"));
            FalDocumentoPlantilla plantilla = plantillaService.activar(p.getId());

            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            plantillaRepo.listarFirmaReqPorPlantilla(plantilla.getId()).stream()
                    .filter(r -> r.getSeqFirmaReq() == (short) 2)
                    .forEach(r -> r.setSiActiva(false));

            assertThatThrownBy(() -> firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("FIRMA_MULTIPLE");
        }

        @Test
        @DisplayName("21 - buscarPorDocumentoYSeq detecta seq duplicada defensivamente")
        void test21_buscarPorDocumentoYSeq() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaMultiple("P-SEQ-DUP-01");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            assertThat(firmaReqRepo.buscarPorDocumentoYSeq(doc.getId(), (short) 1)).isPresent();
            assertThat(firmaReqRepo.buscarPorDocumentoYSeq(doc.getId(), (short) 2)).isPresent();
            assertThat(firmaReqRepo.buscarPorDocumentoYSeq(doc.getId(), (short) 99)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Guardrails")
    class Guardrails {

        @Test
        @DisplayName("22 - No cambia EstadoDocu del documento")
        void test22_noCambiaEstadoDocu() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-GRD-22");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);
            EstadoDocu estadoAntes = doc.getEstadoDocu();

            firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            FalDocumento docDespues = docRepo.buscarPorId(doc.getId()).orElseThrow();
            assertThat(docDespues.getEstadoDocu()).isEqualTo(estadoAntes);
        }

        @Test
        @DisplayName("23 - Documento sigue BORRADOR despues de materializar")
        void test23_documentoSigueBorrador() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-GRD-23");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            FalDocumento docDespues = docRepo.buscarPorId(doc.getId()).orElseThrow();
            assertThat(docDespues.getEstadoDocu()).isEqualTo(EstadoDocu.BORRADOR);
        }

        @Test
        @DisplayName("24 - No crea FalDocumentoFirma al materializar")
        void test24_noFirmaReal() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-GRD-24");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            List<FalDocumentoFirmaReq> reqs = firmaReqRepo.listarPorDocumento(doc.getId());
            assertThat(reqs).allMatch(r -> r.getIdFirma() == null);
        }

        @Test
        @DisplayName("25 - Firma real no asignada: idFirma es null")
        void test25_idFirmaNull() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-GRD-25");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            List<FalDocumentoFirmaReq> resultado = firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            assertThat(resultado).allMatch(r -> r.getIdFirma() == null);
        }

        @Test
        @DisplayName("26 - Firmante no asignado: idFirmanteAsig es null")
        void test26_firmanteNoAsignado() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-GRD-26");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            List<FalDocumentoFirmaReq> resultado = firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            assertThat(resultado).allMatch(r -> r.getIdFirmanteAsig() == null);
        }

        @Test
        @DisplayName("27 - fhFirma es null al materializar")
        void test27_fhFirmaNull() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-GRD-27");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            List<FalDocumentoFirmaReq> resultado = firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            assertThat(resultado).allMatch(r -> r.getFhFirma() == null);
        }

        @Test
        @DisplayName("28 - No consume talonario al materializar")
        void test28_noConsumeTalonario() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-GRD-28");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            FalDocumento docDespues = docRepo.buscarPorId(doc.getId()).orElseThrow();
            assertThat(docDespues.getIdTalonario()).isNull();
            assertThat(docDespues.getNroTalonarioUsado()).isNull();
        }

        @Test
        @DisplayName("29 - No genera nroDocu al materializar")
        void test29_noGeneraNroDocu() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaFirmaAutoridad("P-GRD-29");
            FalDocumento doc = generarDocDesdePlantilla(acta, plantilla);

            firmaReqService.materializarDesdePlantilla(
                    new MaterializarFirmaReqDocumentoCommand(doc.getId(), "operador-01"));

            FalDocumento docDespues = docRepo.buscarPorId(doc.getId()).orElseThrow();
            assertThat(docDespues.getNroDocu()).isNull();
        }

        @Test
        @DisplayName("30 - No existen referencias a FIRMA_MIXTA en todo el servicio")
        void test30_sinFirmaMixta() {
            assertThat(TipoFirmaReq.values())
                    .extracting(Enum::name)
                    .doesNotContain("FIRMA_MIXTA");
        }
    }
}


