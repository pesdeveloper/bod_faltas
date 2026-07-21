package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoCombinacionService;
import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoVariableRegistry;
import ar.gob.malvinas.faltas.core.application.demo.GraphDemoActaFactory;
import ar.gob.malvinas.faltas.core.application.demo.PlantillasMockSeeder;
import ar.gob.malvinas.faltas.core.application.result.DocumentoRedaccionResponse;
import ar.gob.malvinas.faltas.core.application.service.DocumentoPlantillaDefaultService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoRedaccionService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoRedaccionDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalPagoVoluntario;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
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

@DisplayName("Micro-slice 8F-2: Redaccion con contexto del graph demo")
class DocumentoRedaccionGraphDemoTest {

    private ActaRepository actaRepo;
    private DocumentoRepository docRepo;
    private DocumentoPlantillaRepository plantillaRepo;
    private DocumentoPlantillaContenidoRepository contenidoRepo;
    private DocumentoPlantillaDefaultRepository defaultRepo;
    private DocumentoRedaccionRepository redaccionRepo;
    private FalloActaRepository falloRepo;
    private PagoVoluntarioRepository pagoRepo;
    private DocumentoRedaccionService service;

    private FalActa actaDemo;
    private FalActaFallo falloDemo;
    private FalPagoVoluntario pagoDemo;

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
        service = new DocumentoRedaccionService(
                docRepo, defaultSvc, contenidoRepo, redaccionRepo, combinacion,
                actaRepo, falloRepo, pagoRepo, FaltasClockTestSupport.FIXED);

        actaDemo = actaRepo.guardar(GraphDemoActaFactory.crearActaDemo(actaRepo.nextId()));
        falloDemo = falloRepo.guardar(GraphDemoActaFactory.crearFalloCondenatorioDemo(actaDemo.getId()));
        pagoDemo = pagoRepo.guardar(GraphDemoActaFactory.crearPagoVoluntarioDemo(actaDemo.getId()));
    }

    private FalDocumento crearDocDemo(TipoDocu tipoDocu) {
        Long id = docRepo.nextId();
        FalDocumento doc = GraphDemoActaFactory.crearDocumentoDemo(id, actaDemo.getId(), tipoDocu, FaltasClockTestSupport.FIXED.now());
        return docRepo.guardar(doc);
    }

    @Nested
    @DisplayName("Redacciones de los 8 casos operativos")
    class OchoCasos {

        @Test
        @DisplayName("1. Crea redaccion FALLO en estado BORRADOR")
        void crea_redaccion_fallo() {
            FalDocumento doc = crearDocDemo(TipoDocu.ACTO_ADMINISTRATIVO);
            DocumentoRedaccionResponse r = service.crearRedaccionConContextoActa(
                    actaDemo.getId(), doc.getId(),
                    AccionDocumental.EMITIR_FALLO, null, null, null, "usr");
            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.BORRADOR);
            assertThat(r.completo()).isTrue();
        }

        @Test
        @DisplayName("2. Crea redaccion NOTIFICACION_ACTA en estado BORRADOR")
        void crea_redaccion_notificacion_acta() {
            FalDocumento doc = crearDocDemo(TipoDocu.NOTIFICACION_ACTA);
            DocumentoRedaccionResponse r = service.crearRedaccionConContextoActa(
                    actaDemo.getId(), doc.getId(),
                    AccionDocumental.EMITIR_NOTIFICACION_ACTA, null, null, null, "usr");
            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.BORRADOR);
            assertThat(r.completo()).isTrue();
        }

        @Test
        @DisplayName("3. Crea redaccion NOTIFICACION_FALLO en estado BORRADOR")
        void crea_redaccion_notificacion_fallo() {
            FalDocumento doc = crearDocDemo(TipoDocu.NOTIFICACION_ACTO_ADMINISTRATIVO);
            DocumentoRedaccionResponse r = service.crearRedaccionConContextoActa(
                    actaDemo.getId(), doc.getId(),
                    AccionDocumental.EMITIR_NOTIFICACION_FALLO, null, null, null, "usr");
            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.BORRADOR);
            assertThat(r.completo()).isTrue();
        }

        @Test
        @DisplayName("4. Crea redaccion INTIMACION_PAGO en estado BORRADOR")
        void crea_redaccion_intimacion_pago() {
            FalDocumento doc = crearDocDemo(TipoDocu.INTIMACION_PAGO);
            DocumentoRedaccionResponse r = service.crearRedaccionConContextoActa(
                    actaDemo.getId(), doc.getId(),
                    AccionDocumental.EMITIR_INTIMACION_PAGO, null, null, null, "usr");
            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.BORRADOR);
            assertThat(r.completo()).isTrue();
        }

        @Test
        @DisplayName("5. Crea redaccion MEDIDA_PREVENTIVA en estado BORRADOR")
        void crea_redaccion_medida_preventiva() {
            FalDocumento doc = crearDocDemo(TipoDocu.MEDIDA_PREVENTIVA);
            DocumentoRedaccionResponse r = service.crearRedaccionConContextoActa(
                    actaDemo.getId(), doc.getId(),
                    AccionDocumental.EMITIR_MEDIDA_PREVENTIVA, null, null, null, "usr");
            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.BORRADOR);
            assertThat(r.completo()).isTrue();
        }

        @Test
        @DisplayName("6. Crea redaccion CONSTANCIA en estado BORRADOR")
        void crea_redaccion_constancia() {
            FalDocumento doc = crearDocDemo(TipoDocu.CONSTANCIA);
            DocumentoRedaccionResponse r = service.crearRedaccionConContextoActa(
                    actaDemo.getId(), doc.getId(),
                    AccionDocumental.EMITIR_CONSTANCIA, null, null, null, "usr");
            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.BORRADOR);
            assertThat(r.completo()).isTrue();
        }

        @Test
        @DisplayName("7. Crea redaccion ANEXO en estado BORRADOR")
        void crea_redaccion_anexo() {
            FalDocumento doc = crearDocDemo(TipoDocu.ANEXO);
            DocumentoRedaccionResponse r = service.crearRedaccionConContextoActa(
                    actaDemo.getId(), doc.getId(),
                    AccionDocumental.EMITIR_ANEXO, null, null, null, "usr");
            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.BORRADOR);
            assertThat(r.completo()).isTrue();
        }

        @Test
        @DisplayName("8. Crea redaccion RESOLUTORIO_BLOQUEANTE en estado BORRADOR")
        void crea_redaccion_resolutorio_bloqueante() {
            FalDocumento doc = crearDocDemo(TipoDocu.RESOLUTORIO_BLOQUEANTE);
            DocumentoRedaccionResponse r = service.crearRedaccionConContextoActa(
                    actaDemo.getId(), doc.getId(),
                    AccionDocumental.EMITIR_RESOLUTORIO_BLOQUEANTE, null, null, null, "usr");
            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.BORRADOR);
            assertThat(r.completo()).isTrue();
        }
    }

    @Nested
    @DisplayName("Guardrails PDF/storage para los 8 casos")
    class GuardrailsPdf {

        @Test
        @DisplayName("9. Ninguna redaccion setea storageKey en el documento")
        void no_storage_key_fallo() {
            FalDocumento doc = crearDocDemo(TipoDocu.ACTO_ADMINISTRATIVO);
            service.crearRedaccionConContextoActa(
                    actaDemo.getId(), doc.getId(),
                    AccionDocumental.EMITIR_FALLO, null, null, null, "usr");
            assertThat(docRepo.buscarPorId(doc.getId()).orElseThrow().getStorageKey()).isNull();
        }

        @Test
        @DisplayName("10. Ninguna redaccion setea hashDocu en el documento")
        void no_hash_docu_notif_acta() {
            FalDocumento doc = crearDocDemo(TipoDocu.NOTIFICACION_ACTA);
            service.crearRedaccionConContextoActa(
                    actaDemo.getId(), doc.getId(),
                    AccionDocumental.EMITIR_NOTIFICACION_ACTA, null, null, null, "usr");
            assertThat(docRepo.buscarPorId(doc.getId()).orElseThrow().getHashDocu()).isNull();
        }

        @Test
        @DisplayName("11. Ninguna redaccion setea fhGeneracion en el documento")
        void no_fh_generacion_constancia() {
            FalDocumento doc = crearDocDemo(TipoDocu.CONSTANCIA);
            service.crearRedaccionConContextoActa(
                    actaDemo.getId(), doc.getId(),
                    AccionDocumental.EMITIR_CONSTANCIA, null, null, null, "usr");
            assertThat(docRepo.buscarPorId(doc.getId()).orElseThrow().getFhGeneracion()).isNull();
        }

        @Test
        @DisplayName("12. Las redacciones quedan guardadas en el repositorio")
        void redacciones_guardadas_en_repo() {
            FalDocumento doc = crearDocDemo(TipoDocu.ACTO_ADMINISTRATIVO);
            DocumentoRedaccionResponse r = service.crearRedaccionConContextoActa(
                    actaDemo.getId(), doc.getId(),
                    AccionDocumental.EMITIR_FALLO, null, null, null, "usr");
            assertThat(redaccionRepo.buscarPorId(r.id())).isPresent();
            assertThat(redaccionRepo.buscarPorDocumento(doc.getId())).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Contenido combinado")
    class ContenidoCombinado {

        @Test
        @DisplayName("13. El contenido combinado incluye el nombre del infractor")
        void contenido_incluye_nombre_infractor() {
            FalDocumento doc = crearDocDemo(TipoDocu.ACTO_ADMINISTRATIVO);
            DocumentoRedaccionResponse r = service.crearRedaccionConContextoActa(
                    actaDemo.getId(), doc.getId(),
                    AccionDocumental.EMITIR_FALLO, null, null, null, "usr");
            assertThat(r.contenidoEditable()).contains("Juan Carlos Perez");
        }

        @Test
        @DisplayName("14. El contenido combinado incluye el municipio")
        void contenido_incluye_municipio() {
            FalDocumento doc = crearDocDemo(TipoDocu.CONSTANCIA);
            DocumentoRedaccionResponse r = service.crearRedaccionConContextoActa(
                    actaDemo.getId(), doc.getId(),
                    AccionDocumental.EMITIR_CONSTANCIA, null, null, null, "usr");
            assertThat(r.contenidoEditable()).contains("Malvinas Argentinas");
        }

        @Test
        @DisplayName("15. El contenido de fallo incluye tipo de fallo del fallo condenatorio")
        void contenido_fallo_incluye_tipo_fallo() {
            FalDocumento doc = crearDocDemo(TipoDocu.ACTO_ADMINISTRATIVO);
            DocumentoRedaccionResponse r = service.crearRedaccionConContextoActa(
                    actaDemo.getId(), doc.getId(),
                    AccionDocumental.EMITIR_FALLO, null, null, null, "usr");
            assertThat(r.contenidoEditable()).contains("CONDENATORIO");
        }

        @Test
        @DisplayName("16. El contenido de intimacion incluye monto de pago")
        void contenido_intimacion_incluye_monto_pago() {
            FalDocumento doc = crearDocDemo(TipoDocu.INTIMACION_PAGO);
            DocumentoRedaccionResponse r = service.crearRedaccionConContextoActa(
                    actaDemo.getId(), doc.getId(),
                    AccionDocumental.EMITIR_INTIMACION_PAGO, null, null, null, "usr");
            assertThat(r.contenidoEditable()).contains("15000");
        }
    }
}
