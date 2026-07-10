package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.result.DocumentoRenderizadoMock;
import ar.gob.malvinas.faltas.core.application.service.DocumentoPdfMockRenderer;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoRedaccionDocumento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoRedaccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Micro-slice 8F-3: DocumentoPdfMockRenderer")
class DocumentoPdfMockRendererTest {

    private DocumentoPdfMockRenderer renderer;

    private static final LocalDateTime FH = LocalDateTime.of(2024, 6, 1, 9, 0);

    @BeforeEach
    void setUp() {
        renderer = new DocumentoPdfMockRenderer(FaltasClockTestSupport.FIXED);
    }

    private FalDocumento docDemo(Long id, Long actaId) {
        return new FalDocumento(
                id, actaId, TipoDocu.ACTO_ADMINISTRATIVO, FaltasClockTestSupport.FIXED.now(), "Fallo Admin",
                EstadoDocu.BORRADOR, TipoFirmaReq.NO_REQUIERE, null, FaltasClockTestSupport.FIXED.now());
    }

    private FalDocumentoRedaccion redaccionConfirmada(Long id, Long docId, String contenido) {
        FalDocumentoRedaccion r = new FalDocumentoRedaccion(
                id, docId, 1L,
                EstadoRedaccionDocumento.BORRADOR,
                contenido,
                null, null, null,
                FH, "usr-creacion",
                FH, "usr-creacion",
                null, null);
        r.confirmar(FH, "usr-confirmacion");
        return r;
    }

    private FalDocumentoRedaccion redaccionBorrador(Long id, Long docId) {
        return new FalDocumentoRedaccion(
                id, docId, 1L,
                EstadoRedaccionDocumento.BORRADOR,
                "contenido sin confirmar",
                null, null, null,
                FH, "usr", FH, "usr", null, null);
    }

    @Nested
    @DisplayName("Renderizado exitoso")
    class RenderizadoExitoso {

        @Test
        @DisplayName("1. Renderiza contenido mock con marca PDF MOCK")
        void renderiza_con_marca_pdf_mock() {
            FalDocumento doc = docDemo(1L, 10L);
            FalDocumentoRedaccion red = redaccionConfirmada(1L, 1L, "Texto del acta.");
            DocumentoRenderizadoMock result = renderer.renderizar(doc, red);
            assertThat(result.contenidoMock()).contains("[PDF MOCK - SISTEMA DE FALTAS]");
        }

        @Test
        @DisplayName("2. Incluye el contenido editable en el contenido mock")
        void incluye_contenido_editable() {
            FalDocumento doc = docDemo(2L, 10L);
            FalDocumentoRedaccion red = redaccionConfirmada(2L, 2L, "Texto especifico de prueba.");
            DocumentoRenderizadoMock result = renderer.renderizar(doc, red);
            assertThat(result.contenidoMock()).contains("Texto especifico de prueba.");
        }

        @Test
        @DisplayName("3. Nombre de archivo deterministico incluye documentoId y redaccionId")
        void nombre_archivo_deterministico() {
            FalDocumento doc = docDemo(3L, 10L);
            FalDocumentoRedaccion red = redaccionConfirmada(3L, 3L, "Contenido.");
            DocumentoRenderizadoMock result = renderer.renderizar(doc, red);
            assertThat(result.nombreArchivo()).isEqualTo("documento-3-redaccion-3.mock.pdf");
        }

        @Test
        @DisplayName("4. StorageKey mock es esquema mock:// deterministico")
        void storage_key_mock_deterministico() {
            FalDocumento doc = docDemo(4L, 10L);
            FalDocumentoRedaccion red = redaccionConfirmada(4L, 4L, "Contenido.");
            DocumentoRenderizadoMock result = renderer.renderizar(doc, red);
            assertThat(result.storageKeyMock())
                    .startsWith("mock://")
                    .contains("documentos/4")
                    .contains("redacciones/4");
        }

        @Test
        @DisplayName("5. Hash es deterministico para mismo contenido")
        void hash_deterministico_mismo_contenido() {
            String contenido = "contenido-identico-para-hash";
            String hash1 = DocumentoPdfMockRenderer.calcularHash(contenido);
            String hash2 = DocumentoPdfMockRenderer.calcularHash(contenido);
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("6. Hash comienza con prefijo sha256-mock-")
        void hash_prefijo_sha256_mock() {
            FalDocumento doc = docDemo(5L, 10L);
            FalDocumentoRedaccion red = redaccionConfirmada(5L, 5L, "Contenido.");
            DocumentoRenderizadoMock result = renderer.renderizar(doc, red);
            assertThat(result.hashMock()).startsWith("sha256-mock-");
        }

        @Test
        @DisplayName("7. Hash es diferente para contenidos distintos")
        void hash_diferente_contenidos_distintos() {
            String hash1 = DocumentoPdfMockRenderer.calcularHash("contenido A");
            String hash2 = DocumentoPdfMockRenderer.calcularHash("contenido B");
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("8. sizeBytes es mayor que cero")
        void size_bytes_mayor_cero() {
            FalDocumento doc = docDemo(6L, 10L);
            FalDocumentoRedaccion red = redaccionConfirmada(6L, 6L, "Contenido.");
            DocumentoRenderizadoMock result = renderer.renderizar(doc, red);
            assertThat(result.sizeBytes()).isGreaterThan(0);
        }

        @Test
        @DisplayName("9. sizeBytes corresponde al tamano real del contenido mock en UTF-8")
        void size_bytes_correcto() {
            FalDocumento doc = docDemo(7L, 10L);
            FalDocumentoRedaccion red = redaccionConfirmada(7L, 7L, "Contenido exacto.");
            DocumentoRenderizadoMock result = renderer.renderizar(doc, red);
            long expectedSize = result.contenidoMock().getBytes(StandardCharsets.UTF_8).length;
            assertThat(result.sizeBytes()).isEqualTo(expectedSize);
        }

        @Test
        @DisplayName("10. MIME type es application/x-faltas-pdf-mock")
        void mime_type_correcto() {
            FalDocumento doc = docDemo(8L, 10L);
            FalDocumentoRedaccion red = redaccionConfirmada(8L, 8L, "Contenido.");
            DocumentoRenderizadoMock result = renderer.renderizar(doc, red);
            assertThat(result.mimeType()).isEqualTo("application/x-faltas-pdf-mock");
        }

        @Test
        @DisplayName("11. fhGeneracion no es null")
        void fh_generacion_no_null() {
            FalDocumento doc = docDemo(9L, 10L);
            FalDocumentoRedaccion red = redaccionConfirmada(9L, 9L, "Contenido.");
            DocumentoRenderizadoMock result = renderer.renderizar(doc, red);
            assertThat(result.fhGeneracion()).isNotNull();
        }

        @Test
        @DisplayName("12. Renderer no modifica FalDocumento directamente")
        void no_modifica_documento() {
            FalDocumento doc = docDemo(10L, 10L);
            FalDocumentoRedaccion red = redaccionConfirmada(10L, 10L, "Contenido.");
            String storageKeyAntes = doc.getStorageKey();
            String hashAntes = doc.getHashDocu();
            renderer.renderizar(doc, red);
            assertThat(doc.getStorageKey()).isEqualTo(storageKeyAntes);
            assertThat(doc.getHashDocu()).isEqualTo(hashAntes);
        }

        @Test
        @DisplayName("13. Contenido mock incluye tipo documental")
        void contenido_incluye_tipo_documental() {
            FalDocumento doc = docDemo(11L, 10L);
            FalDocumentoRedaccion red = redaccionConfirmada(11L, 11L, "Contenido.");
            DocumentoRenderizadoMock result = renderer.renderizar(doc, red);
            assertThat(result.contenidoMock()).contains("ACTO_ADMINISTRATIVO");
        }
    }

    @Nested
    @DisplayName("Guardrails del renderer")
    class Guardrails {

        @Test
        @DisplayName("14. Falla si la redaccion no esta CONFIRMADA")
        void falla_si_no_confirmada() {
            FalDocumento doc = docDemo(20L, 10L);
            FalDocumentoRedaccion red = redaccionBorrador(20L, 20L);
            assertThatThrownBy(() -> renderer.renderizar(doc, red))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("CONFIRMADA");
        }

        @Test
        @DisplayName("15. Falla si documento es null")
        void falla_si_documento_null() {
            FalDocumentoRedaccion red = redaccionConfirmada(21L, 21L, "Contenido.");
            assertThatThrownBy(() -> renderer.renderizar(null, red))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("16. Falla si redaccion es null")
        void falla_si_redaccion_null() {
            FalDocumento doc = docDemo(22L, 10L);
            assertThatThrownBy(() -> renderer.renderizar(doc, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("17. buildStorageKey genera URL esquema mock")
        void build_storage_key_esquema_mock() {
            String sk = DocumentoPdfMockRenderer.buildStorageKey(5L, 7L);
            assertThat(sk).isEqualTo("mock://documentos/5/redacciones/7/documento-final.pdf");
        }
    }
}
