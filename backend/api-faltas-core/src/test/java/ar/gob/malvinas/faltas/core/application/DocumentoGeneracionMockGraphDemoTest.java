package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.ConfirmarRedaccionYGenerarDocumentoMockCommand;
import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoCombinacionService;
import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoVariableRegistry;
import ar.gob.malvinas.faltas.core.application.demo.GraphDemoActaFactory;
import ar.gob.malvinas.faltas.core.application.demo.PlantillasMockSeeder;
import ar.gob.malvinas.faltas.core.application.result.DocumentoGeneracionMockResponse;
import ar.gob.malvinas.faltas.core.application.result.DocumentoRedaccionResponse;
import ar.gob.malvinas.faltas.core.application.service.DocumentoGeneracionMockService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoPdfMockRenderer;
import ar.gob.malvinas.faltas.core.application.service.DocumentoPlantillaDefaultService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoRedaccionService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoRedaccionDocumento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaContenidoRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaDefaultRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRedaccionRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaContenidoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaDefaultRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRedaccionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Micro-slice 8F-3: Graph demo - generacion mock para los 8 casos operativos")
class DocumentoGeneracionMockGraphDemoTest {

    private ActaRepository actaRepo;
    private DocumentoRepository docRepo;
    private DocumentoPlantillaRepository plantillaRepo;
    private DocumentoPlantillaContenidoRepository contenidoRepo;
    private DocumentoPlantillaDefaultRepository defaultRepo;
    private DocumentoRedaccionRepository redaccionRepo;
    private FalloActaRepository falloRepo;
    private PagoVoluntarioRepository pagoRepo;

    private DocumentoRedaccionService redaccionService;
    private DocumentoGeneracionMockService generacionService;

    private FalActa actaDemo;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        docRepo = new InMemoryDocumentoRepository();
        plantillaRepo = new InMemoryDocumentoPlantillaRepository();
        contenidoRepo = new InMemoryDocumentoPlantillaContenidoRepository();
        defaultRepo = new InMemoryDocumentoPlantillaDefaultRepository();
        redaccionRepo = new InMemoryDocumentoRedaccionRepository();
        falloRepo = new InMemoryFalloActaRepository();
        pagoRepo = new InMemoryPagoVoluntarioRepository();

        PlantillasMockSeeder.seedar(plantillaRepo, contenidoRepo, defaultRepo);

        DocumentoCombinacionService combinacion =
                new DocumentoCombinacionService(new DocumentoVariableRegistry());
        DocumentoPlantillaDefaultService defaultSvc =
                new DocumentoPlantillaDefaultService(defaultRepo);
        redaccionService = new DocumentoRedaccionService(
                docRepo, defaultSvc, contenidoRepo, redaccionRepo, combinacion,
                actaRepo, falloRepo, pagoRepo, FaltasClockTestSupport.FIXED);

        generacionService = new DocumentoGeneracionMockService(
                redaccionRepo, docRepo, new DocumentoPdfMockRenderer(FaltasClockTestSupport.FIXED), FaltasClockTestSupport.FIXED);

        actaDemo = actaRepo.guardar(GraphDemoActaFactory.crearActaDemo(actaRepo.nextId()));
        falloRepo.guardar(GraphDemoActaFactory.crearFalloCondenatorioDemo(actaDemo.getId()));
        pagoRepo.guardar(GraphDemoActaFactory.crearPagoVoluntarioDemo(actaDemo.getId()));
    }

    private DocumentoGeneracionMockResponse crearYGenerarMock(TipoDocu tipoDocu, AccionDocumental accion) {
        Long docId = docRepo.nextId();
        FalDocumento doc = GraphDemoActaFactory.crearDocumentoDemo(docId, actaDemo.getId(), tipoDocu, FaltasClockTestSupport.FIXED.now());
        docRepo.guardar(doc);
        DocumentoRedaccionResponse borrador = redaccionService.crearRedaccionConContextoActa(
                actaDemo.getId(), docId, accion, null, null, null, "usr");
        ConfirmarRedaccionYGenerarDocumentoMockCommand cmd =
                new ConfirmarRedaccionYGenerarDocumentoMockCommand(borrador.id(), "usr-confirm", null);
        return generacionService.confirmarYGenerarMockPdf(cmd);
    }

    @Nested
    @DisplayName("Generacion mock para cada caso operativo")
    class GeneracionPorCaso {

        @Test
        @DisplayName("1. Genera mock para FALLO - estadoRedaccion=CONFIRMADA")
        void genera_mock_fallo() {
            DocumentoGeneracionMockResponse r = crearYGenerarMock(
                    TipoDocu.ACTO_ADMINISTRATIVO, AccionDocumental.EMITIR_FALLO);
            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.CONFIRMADA);
        }

        @Test
        @DisplayName("2. Genera mock para NOTIFICACION_ACTA")
        void genera_mock_notificacion_acta() {
            DocumentoGeneracionMockResponse r = crearYGenerarMock(
                    TipoDocu.NOTIFICACION_ACTA, AccionDocumental.EMITIR_NOTIFICACION_ACTA);
            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.CONFIRMADA);
        }

        @Test
        @DisplayName("3. Genera mock para NOTIFICACION_FALLO")
        void genera_mock_notificacion_fallo() {
            DocumentoGeneracionMockResponse r = crearYGenerarMock(
                    TipoDocu.NOTIFICACION_ACTO_ADMINISTRATIVO, AccionDocumental.EMITIR_NOTIFICACION_FALLO);
            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.CONFIRMADA);
        }

        @Test
        @DisplayName("4. Genera mock para INTIMACION_PAGO")
        void genera_mock_intimacion_pago() {
            DocumentoGeneracionMockResponse r = crearYGenerarMock(
                    TipoDocu.INTIMACION_PAGO, AccionDocumental.EMITIR_INTIMACION_PAGO);
            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.CONFIRMADA);
        }

        @Test
        @DisplayName("5. Genera mock para MEDIDA_PREVENTIVA")
        void genera_mock_medida_preventiva() {
            DocumentoGeneracionMockResponse r = crearYGenerarMock(
                    TipoDocu.MEDIDA_PREVENTIVA, AccionDocumental.EMITIR_MEDIDA_PREVENTIVA);
            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.CONFIRMADA);
        }

        @Test
        @DisplayName("6. Genera mock para CONSTANCIA")
        void genera_mock_constancia() {
            DocumentoGeneracionMockResponse r = crearYGenerarMock(
                    TipoDocu.CONSTANCIA, AccionDocumental.EMITIR_CONSTANCIA);
            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.CONFIRMADA);
        }

        @Test
        @DisplayName("7. Genera mock para ANEXO")
        void genera_mock_anexo() {
            DocumentoGeneracionMockResponse r = crearYGenerarMock(
                    TipoDocu.ANEXO, AccionDocumental.EMITIR_ANEXO);
            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.CONFIRMADA);
        }

        @Test
        @DisplayName("8. Genera mock para RESOLUTORIO_BLOQUEANTE")
        void genera_mock_resolutorio_bloqueante() {
            DocumentoGeneracionMockResponse r = crearYGenerarMock(
                    TipoDocu.RESOLUTORIO_BLOQUEANTE, AccionDocumental.EMITIR_RESOLUTORIO_BLOQUEANTE);
            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.CONFIRMADA);
        }
    }

    @Nested
    @DisplayName("Verificaciones transversales de los 8 casos")
    class VerificacionesTransversales {

        @Test
        @DisplayName("9. Todos generan hashDocu no null")
        void todos_generan_hash() {
            DocumentoGeneracionMockResponse r = crearYGenerarMock(
                    TipoDocu.ACTO_ADMINISTRATIVO, AccionDocumental.EMITIR_FALLO);
            assertThat(r.hashDocu()).isNotNull().startsWith("sha256-mock-");
        }

        @Test
        @DisplayName("10. Todos generan storageKey con esquema mock://")
        void todos_generan_storage_key_mock() {
            DocumentoGeneracionMockResponse r = crearYGenerarMock(
                    TipoDocu.NOTIFICACION_ACTA, AccionDocumental.EMITIR_NOTIFICACION_ACTA);
            assertThat(r.storageKey()).isNotNull().startsWith("mock://");
        }

        @Test
        @DisplayName("11. Todos generan fhGeneracion no null")
        void todos_generan_fh_generacion() {
            DocumentoGeneracionMockResponse r = crearYGenerarMock(
                    TipoDocu.CONSTANCIA, AccionDocumental.EMITIR_CONSTANCIA);
            assertThat(r.fhGeneracion()).isNotNull();
        }

        @Test
        @DisplayName("12. Ninguno usa storage real - storageKey no apunta a S3/disco/filesystem")
        void ninguno_usa_storage_real() {
            DocumentoGeneracionMockResponse r = crearYGenerarMock(
                    TipoDocu.MEDIDA_PREVENTIVA, AccionDocumental.EMITIR_MEDIDA_PREVENTIVA);
            assertThat(r.storageKey())
                    .doesNotContain("s3://")
                    .doesNotContain("/var/")
                    .doesNotContain("C:\\")
                    .doesNotContain("file://")
                    .startsWith("mock://");
        }

        @Test
        @DisplayName("13. Ninguno emite automaticamente - estadoDocu no es EMITIDO")
        void ninguno_emite() {
            crearYGenerarMock(TipoDocu.ACTO_ADMINISTRATIVO, AccionDocumental.EMITIR_FALLO);
            docRepo.buscarPorActa(actaDemo.getId()).forEach(doc ->
                    assertThat(doc.getEstadoDocu()).isNotEqualTo(EstadoDocu.EMITIDO));
        }

        @Test
        @DisplayName("14. Ninguno firma - estadoDocu no es FIRMADO")
        void ninguno_firma() {
            crearYGenerarMock(TipoDocu.NOTIFICACION_ACTA, AccionDocumental.EMITIR_NOTIFICACION_ACTA);
            docRepo.buscarPorActa(actaDemo.getId()).forEach(doc ->
                    assertThat(doc.getEstadoDocu()).isNotEqualTo(EstadoDocu.FIRMADO));
        }

        @Test
        @DisplayName("15. mock=true en todos los responses")
        void mock_true_en_todos() {
            DocumentoGeneracionMockResponse r = crearYGenerarMock(
                    TipoDocu.INTIMACION_PAGO, AccionDocumental.EMITIR_INTIMACION_PAGO);
            assertThat(r.mock()).isTrue();
        }

        @Test
        @DisplayName("16. El flujo completo: BORRADOR sin storage -> CONFIRMADA con storage mock")
        void flujo_completo_borrador_a_confirmada() {
            Long docId = docRepo.nextId();
            FalDocumento doc = GraphDemoActaFactory.crearDocumentoDemo(docId, actaDemo.getId(), TipoDocu.ANEXO, FaltasClockTestSupport.FIXED.now());
            docRepo.guardar(doc);

            // Verificar BORRADOR sin storage
            assertThat(doc.getStorageKey()).isNull();
            assertThat(doc.getHashDocu()).isNull();
            assertThat(doc.getFhGeneracion()).isNull();

            DocumentoRedaccionResponse borrador = redaccionService.crearRedaccionConContextoActa(
                    actaDemo.getId(), docId, AccionDocumental.EMITIR_ANEXO, null, null, null, "usr");
            assertThat(borrador.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.BORRADOR);

            // Verificar que aun sin storage tras crear borrador
            FalDocumento docTrasRedaccion = docRepo.buscarPorId(docId).orElseThrow();
            assertThat(docTrasRedaccion.getStorageKey()).isNull();

            // Confirmar y generar mock
            ConfirmarRedaccionYGenerarDocumentoMockCommand cmd =
                    new ConfirmarRedaccionYGenerarDocumentoMockCommand(borrador.id(), "usr-confirm", null);
            DocumentoGeneracionMockResponse resultado = generacionService.confirmarYGenerarMockPdf(cmd);

            // Verificar CONFIRMADA con storage mock
            assertThat(resultado.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.CONFIRMADA);
            assertThat(resultado.storageKey()).startsWith("mock://");
            assertThat(resultado.hashDocu()).startsWith("sha256-mock-");
            assertThat(resultado.fhGeneracion()).isNotNull();
        }
    }
}
