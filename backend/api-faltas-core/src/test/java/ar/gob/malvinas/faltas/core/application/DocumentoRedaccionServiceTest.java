package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoCombinacionService;
import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoVariableRegistry;
import ar.gob.malvinas.faltas.core.application.command.CrearRedaccionDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.result.DocumentoRedaccionResponse;
import ar.gob.malvinas.faltas.core.application.service.DocumentoPlantillaDefaultService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoRedaccionService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoRedaccionDocumento;
import ar.gob.malvinas.faltas.core.domain.enums.FormatoPlantillaContenido;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.exception.PlantillaContenidoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PlantillaDefaultNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaContenido;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaDefault;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaContenidoRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaDefaultRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRedaccionRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaContenidoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaDefaultRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRedaccionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Micro-slice 8F-1: DocumentoRedaccionService")
class DocumentoRedaccionServiceTest {

    private DocumentoRepository docRepo;
    private DocumentoPlantillaDefaultRepository defaultRepo;
    private DocumentoPlantillaContenidoRepository contenidoRepo;
    private DocumentoRedaccionRepository redaccionRepo;
    private DocumentoRedaccionService service;

    private static final LocalDateTime AYER = FaltasClockTestSupport.FIXED.now().minusDays(1);
    private static final Long PLANTILLA_ID = 100L;

    @BeforeEach
    void setUp() {
        docRepo = new InMemoryDocumentoRepository();
        defaultRepo = new InMemoryDocumentoPlantillaDefaultRepository();
        contenidoRepo = new InMemoryDocumentoPlantillaContenidoRepository();
        redaccionRepo = new InMemoryDocumentoRedaccionRepository();
        DocumentoCombinacionService combinacion =
                new DocumentoCombinacionService(new DocumentoVariableRegistry());
        DocumentoPlantillaDefaultService defaultSvc =
                new DocumentoPlantillaDefaultService(defaultRepo);
        service = new DocumentoRedaccionService(
                docRepo, defaultSvc, contenidoRepo, redaccionRepo, combinacion,
                FaltasClockTestSupport.FIXED);
    }

    private FalDocumento crearDocumento() {
        Long id = docRepo.nextId();
        FalDocumento doc = new FalDocumento(
                id, 1L, TipoDocu.ACTO_ADMINISTRATIVO, FaltasClockTestSupport.FIXED.now(), "Fallo",
                ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu.BORRADOR,
                ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq.NO_REQUIERE, null, FaltasClockTestSupport.FIXED.now());
        return docRepo.guardar(doc);
    }

    private void crearDefault() {
        Long id = defaultRepo.nextId();
        defaultRepo.guardar(new FalDocumentoPlantillaDefault(
                id, AccionDocumental.EMITIR_FALLO, null, TipoDocu.ACTO_ADMINISTRATIVO,
                null, null, PLANTILLA_ID, 10, AYER, null, true, AYER, "sistema"));
    }

    private void crearContenido(String template) {
        Long id = contenidoRepo.nextId();
        contenidoRepo.guardar(new FalDocumentoPlantillaContenido(
                id, PLANTILLA_ID, (short) 1, FormatoPlantillaContenido.HTML_SIMPLE,
                "Fallo Administrativo", template,
                null, null, null, true, AYER, null, AYER, "sistema"));
    }

    private CrearRedaccionDocumentoCommand cmd(Long docId, Map<String, Object> ctx) {
        return new CrearRedaccionDocumentoCommand(
                docId, 1L, AccionDocumental.EMITIR_FALLO, null, null, null, "usr", ctx);
    }

    @Nested
    @DisplayName("Creacion BORRADOR")
    class CreacionBorrador {

        @Test
        @DisplayName("1. Crea redaccion en estado BORRADOR")
        void crea_borrador() {
            FalDocumento doc = crearDocumento();
            crearDefault();
            crearContenido("Texto simple.");

            DocumentoRedaccionResponse r = service.crearRedaccionDesdePlantilla(
                    cmd(doc.getId(), Map.of()));

            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.BORRADOR);
            assertThat(r.id()).isNotNull();
        }

        @Test
        @DisplayName("2. Usa el contenido vigente de la plantilla")
        void usa_contenido_vigente() {
            FalDocumento doc = crearDocumento();
            crearDefault();
            crearContenido("Contenido vigente.");

            DocumentoRedaccionResponse r = service.crearRedaccionDesdePlantilla(
                    cmd(doc.getId(), Map.of()));

            assertThat(r.contenidoEditable()).isEqualTo("Contenido vigente.");
            assertThat(r.plantillaContenidoId()).isNotNull();
        }

        @Test
        @DisplayName("3. Guarda contenido combinado con variables reemplazadas")
        void guarda_contenido_combinado() {
            FalDocumento doc = crearDocumento();
            crearDefault();
            crearContenido("Infractor: {{infractor.nombreCompleto}}");

            DocumentoRedaccionResponse r = service.crearRedaccionDesdePlantilla(
                    cmd(doc.getId(), Map.of(
                            "infractor.nombreCompleto", "Pedro",
                            "infractor.documento", "99",
                            "acta.fechaLabrado", FaltasClockTestSupport.FIXED.now())));

            assertThat(r.contenidoEditable()).isEqualTo("Infractor: Pedro");
        }

        @Test
        @DisplayName("4. Guarda variables usadas en la respuesta")
        void guarda_variables_usadas() {
            FalDocumento doc = crearDocumento();
            crearDefault();
            crearContenido("{{infractor.nombreCompleto}} {{acta.nroActa}}");

            DocumentoRedaccionResponse r = service.crearRedaccionDesdePlantilla(
                    cmd(doc.getId(), Map.of(
                            "infractor.nombreCompleto", "Ana",
                            "infractor.documento", "11",
                            "acta.fechaLabrado", FaltasClockTestSupport.FIXED.now(),
                            "acta.nroActa", "ACT-001")));

            assertThat(r.variablesUsadas())
                    .containsExactlyInAnyOrder("infractor.nombreCompleto", "acta.nroActa");
            assertThat(r.completo()).isTrue();
        }

        @Test
        @DisplayName("Redaccion queda guardada en el repositorio")
        void redaccion_en_repo() {
            FalDocumento doc = crearDocumento();
            crearDefault();
            crearContenido("T.");

            DocumentoRedaccionResponse r = service.crearRedaccionDesdePlantilla(
                    cmd(doc.getId(), Map.of()));

            assertThat(redaccionRepo.buscarPorDocumento(doc.getId())).hasSize(1);
            assertThat(redaccionRepo.buscarPorId(r.id())).isPresent();
        }
    }

    @Nested
    @DisplayName("Guardrail PDF/storage")
    class GuardrailPdf {

        @Test
        @DisplayName("5. No genera PDF: storageKey permanece null en FalDocumento")
        void storage_key_null() {
            FalDocumento doc = crearDocumento();
            crearDefault();
            crearContenido("T.");
            service.crearRedaccionDesdePlantilla(cmd(doc.getId(), Map.of()));
            assertThat(docRepo.buscarPorId(doc.getId()).orElseThrow().getStorageKey()).isNull();
        }

        @Test
        @DisplayName("6. No setea storageKey")
        void no_storage_key() {
            FalDocumento doc = crearDocumento();
            crearDefault();
            crearContenido("T.");
            service.crearRedaccionDesdePlantilla(cmd(doc.getId(), Map.of()));
            assertThat(doc.getStorageKey()).isNull();
        }

        @Test
        @DisplayName("7. No setea hashDocu")
        void no_hash_docu() {
            FalDocumento doc = crearDocumento();
            crearDefault();
            crearContenido("T.");
            service.crearRedaccionDesdePlantilla(cmd(doc.getId(), Map.of()));
            assertThat(doc.getHashDocu()).isNull();
        }

        @Test
        @DisplayName("8. No setea fhGeneracion")
        void no_fh_generacion() {
            FalDocumento doc = crearDocumento();
            crearDefault();
            crearContenido("T.");
            service.crearRedaccionDesdePlantilla(cmd(doc.getId(), Map.of()));
            assertThat(doc.getFhGeneracion()).isNull();
        }

        @Test
        @DisplayName("9. No emite el documento - estado no es EMITIDO")
        void no_emite() {
            FalDocumento doc = crearDocumento();
            crearDefault();
            crearContenido("T.");
            service.crearRedaccionDesdePlantilla(cmd(doc.getId(), Map.of()));
            assertThat(doc.getEstadoDocu()).isNotEqualTo(EstadoDocu.EMITIDO);
        }

        @Test
        @DisplayName("10. No envia a firma - estado no es PENDIENTE_FIRMA")
        void no_envia_firma() {
            FalDocumento doc = crearDocumento();
            crearDefault();
            crearContenido("T.");
            service.crearRedaccionDesdePlantilla(cmd(doc.getId(), Map.of()));
            assertThat(doc.getEstadoDocu()).isNotEqualTo(EstadoDocu.PENDIENTE_FIRMA);
        }
    }

    @Nested
    @DisplayName("Errores")
    class Errores {

        @Test
        @DisplayName("11. Falla si plantilla default no existe")
        void falla_sin_default() {
            FalDocumento doc = crearDocumento();
            assertThatThrownBy(() -> service.crearRedaccionDesdePlantilla(
                    cmd(doc.getId(), Map.of())))
                    .isInstanceOf(PlantillaDefaultNoEncontradaException.class);
        }

        @Test
        @DisplayName("12. Falla si contenido vigente no existe")
        void falla_sin_contenido() {
            FalDocumento doc = crearDocumento();
            crearDefault();
            assertThatThrownBy(() -> service.crearRedaccionDesdePlantilla(
                    cmd(doc.getId(), Map.of())))
                    .isInstanceOf(PlantillaContenidoNoEncontradaException.class);
        }

        @Test
        @DisplayName("13. Falla si variable requerida falta en el contexto")
        void falla_variable_requerida_falta() {
            FalDocumento doc = crearDocumento();
            crearDefault();
            crearContenido("Fecha: {{acta.fechaLabrado}}");
            assertThatThrownBy(() -> service.crearRedaccionDesdePlantilla(
                    cmd(doc.getId(), Map.of())))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("requerida");
        }

        @Test
        @DisplayName("Falla si idDocumento es null")
        void falla_id_doc_null() {
            assertThatThrownBy(() -> service.crearRedaccionDesdePlantilla(
                    new CrearRedaccionDocumentoCommand(null, 1L,
                            AccionDocumental.EMITIR_FALLO, null, null, null, "usr", Map.of())))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idDocumento");
        }

        @Test
        @DisplayName("Falla si idActa es null")
        void falla_id_acta_null() {
            assertThatThrownBy(() -> service.crearRedaccionDesdePlantilla(
                    new CrearRedaccionDocumentoCommand(1L, null,
                            AccionDocumental.EMITIR_FALLO, null, null, null, "usr", Map.of())))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idActa");
        }

        @Test
        @DisplayName("Falla si idUserOperacion es blank")
        void falla_user_blank() {
            assertThatThrownBy(() -> service.crearRedaccionDesdePlantilla(
                    new CrearRedaccionDocumentoCommand(1L, 1L,
                            AccionDocumental.EMITIR_FALLO, null, null, null, "  ", Map.of())))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idUserOperacion");
        }
    }
}
