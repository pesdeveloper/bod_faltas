package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.ConfirmarRedaccionYGenerarDocumentoMockCommand;
import ar.gob.malvinas.faltas.core.application.result.DocumentoGeneracionMockResponse;
import ar.gob.malvinas.faltas.core.application.service.DocumentoGeneracionMockService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoPdfMockRenderer;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoRedaccionDocumento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoRedaccion;
import ar.gob.malvinas.faltas.core.repository.DocumentoRedaccionRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRedaccionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Micro-slice 8F-3: DocumentoGeneracionMockService")
class DocumentoGeneracionMockServiceTest {

    private DocumentoRedaccionRepository redaccionRepo;
    private DocumentoRepository docRepo;
    private DocumentoGeneracionMockService service;

    private static final LocalDateTime FH = LocalDateTime.of(2024, 7, 1, 12, 0);

    @BeforeEach
    void setUp() {
        redaccionRepo = new InMemoryDocumentoRedaccionRepository();
        docRepo = new InMemoryDocumentoRepository();
        service = new DocumentoGeneracionMockService(
                redaccionRepo, docRepo, new DocumentoPdfMockRenderer());
    }

    private FalDocumento guardarDoc(Long id) {
        FalDocumento doc = new FalDocumento(
                id, 10L, TipoDocu.ACTO_ADMINISTRATIVO, LocalDateTime.now(), "Fallo Test",
                EstadoDocu.BORRADOR, TipoFirmaReq.NO_REQUIERE, null);
        return docRepo.guardar(doc);
    }

    private FalDocumentoRedaccion guardarRedaccionBorrador(Long id, Long docId, String contenido) {
        FalDocumentoRedaccion r = new FalDocumentoRedaccion(
                id, docId, 1L,
                EstadoRedaccionDocumento.BORRADOR,
                contenido,
                null, null, null,
                FH, "usr", FH, "usr", null, null);
        return redaccionRepo.guardar(r);
    }

    private FalDocumentoRedaccion guardarRedaccionConEstado(Long id, Long docId, EstadoRedaccionDocumento estado) {
        FalDocumentoRedaccion r = guardarRedaccionBorrador(id, docId, "contenido");
        r.setEstadoRedaccion(estado);
        return redaccionRepo.guardar(r);
    }

    private ConfirmarRedaccionYGenerarDocumentoMockCommand cmd(Long redaccionId) {
        return new ConfirmarRedaccionYGenerarDocumentoMockCommand(redaccionId, "usr-confirm", FH);
    }

    @Nested
    @DisplayName("Flujo nominal")
    class FlujoNominal {

        @Test
        @DisplayName("1. Confirma y genera mock final desde BORRADOR")
        void confirma_y_genera_mock() {
            FalDocumento doc = guardarDoc(1L);
            FalDocumentoRedaccion red = guardarRedaccionBorrador(1L, 1L, "Contenido del acta.");
            DocumentoGeneracionMockResponse r = service.confirmarYGenerarMockPdf(cmd(red.getId()));
            assertThat(r).isNotNull();
            assertThat(r.estadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.CONFIRMADA);
        }

        @Test
        @DisplayName("2. Setea storageKey en FalDocumento")
        void setea_storage_key_en_documento() {
            FalDocumento doc = guardarDoc(2L);
            FalDocumentoRedaccion red = guardarRedaccionBorrador(2L, 2L, "Contenido.");
            service.confirmarYGenerarMockPdf(cmd(red.getId()));
            FalDocumento docActualizado = docRepo.buscarPorId(2L).orElseThrow();
            assertThat(docActualizado.getStorageKey()).isNotNull().startsWith("mock://");
        }

        @Test
        @DisplayName("3. Setea hashDocu en FalDocumento")
        void setea_hash_docu_en_documento() {
            FalDocumento doc = guardarDoc(3L);
            FalDocumentoRedaccion red = guardarRedaccionBorrador(3L, 3L, "Contenido.");
            service.confirmarYGenerarMockPdf(cmd(red.getId()));
            FalDocumento docActualizado = docRepo.buscarPorId(3L).orElseThrow();
            assertThat(docActualizado.getHashDocu()).isNotNull().startsWith("sha256-mock-");
        }

        @Test
        @DisplayName("4. Setea fhGeneracion en FalDocumento")
        void setea_fh_generacion_en_documento() {
            FalDocumento doc = guardarDoc(4L);
            FalDocumentoRedaccion red = guardarRedaccionBorrador(4L, 4L, "Contenido.");
            service.confirmarYGenerarMockPdf(cmd(red.getId()));
            FalDocumento docActualizado = docRepo.buscarPorId(4L).orElseThrow();
            assertThat(docActualizado.getFhGeneracion()).isNotNull();
        }

        @Test
        @DisplayName("5. Deja redaccion en CONFIRMADA")
        void deja_redaccion_confirmada() {
            FalDocumento doc = guardarDoc(5L);
            FalDocumentoRedaccion red = guardarRedaccionBorrador(5L, 5L, "Contenido.");
            service.confirmarYGenerarMockPdf(cmd(red.getId()));
            FalDocumentoRedaccion redActualizada = redaccionRepo.buscarPorId(red.getId()).orElseThrow();
            assertThat(redActualizada.getEstadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.CONFIRMADA);
        }

        @Test
        @DisplayName("6. Response tiene mock=true")
        void response_mock_true() {
            FalDocumento doc = guardarDoc(6L);
            FalDocumentoRedaccion red = guardarRedaccionBorrador(6L, 6L, "Contenido.");
            DocumentoGeneracionMockResponse r = service.confirmarYGenerarMockPdf(cmd(red.getId()));
            assertThat(r.mock()).isTrue();
        }

        @Test
        @DisplayName("7. StorageKey en response empieza con mock://")
        void storage_key_response_mock() {
            FalDocumento doc = guardarDoc(7L);
            FalDocumentoRedaccion red = guardarRedaccionBorrador(7L, 7L, "Contenido.");
            DocumentoGeneracionMockResponse r = service.confirmarYGenerarMockPdf(cmd(red.getId()));
            assertThat(r.storageKey()).startsWith("mock://");
        }

        @Test
        @DisplayName("8. sizeBytes es positivo")
        void size_bytes_positivo() {
            FalDocumento doc = guardarDoc(8L);
            FalDocumentoRedaccion red = guardarRedaccionBorrador(8L, 8L, "Contenido.");
            DocumentoGeneracionMockResponse r = service.confirmarYGenerarMockPdf(cmd(red.getId()));
            assertThat(r.sizeBytes()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Guardrails post-generacion")
    class Guardrails {

        @Test
        @DisplayName("9. No emite automaticamente - estadoDocu no es EMITIDO")
        void no_emite_automaticamente() {
            FalDocumento doc = guardarDoc(10L);
            FalDocumentoRedaccion red = guardarRedaccionBorrador(10L, 10L, "Contenido.");
            service.confirmarYGenerarMockPdf(cmd(red.getId()));
            FalDocumento docActualizado = docRepo.buscarPorId(10L).orElseThrow();
            assertThat(docActualizado.getEstadoDocu()).isNotEqualTo(EstadoDocu.EMITIDO);
        }

        @Test
        @DisplayName("10. No envia a firma - estadoDocu no es PENDIENTE_FIRMA (si era BORRADOR)")
        void no_envia_a_firma() {
            FalDocumento doc = guardarDoc(11L);
            FalDocumentoRedaccion red = guardarRedaccionBorrador(11L, 11L, "Contenido.");
            service.confirmarYGenerarMockPdf(cmd(red.getId()));
            FalDocumento docActualizado = docRepo.buscarPorId(11L).orElseThrow();
            assertThat(docActualizado.getEstadoDocu()).isNotEqualTo(EstadoDocu.FIRMADO);
        }

        @Test
        @DisplayName("11. Preserva guardrail: BORRADOR no tenia storage antes de confirmar")
        void borrador_sin_storage_antes() {
            FalDocumento doc = guardarDoc(12L);
            assertThat(doc.getStorageKey()).isNull();
            assertThat(doc.getHashDocu()).isNull();
            assertThat(doc.getFhGeneracion()).isNull();
            FalDocumentoRedaccion red = guardarRedaccionBorrador(12L, 12L, "Contenido.");
            service.confirmarYGenerarMockPdf(cmd(red.getId()));
            // Ahora si tiene storage (mock)
            FalDocumento docActualizado = docRepo.buscarPorId(12L).orElseThrow();
            assertThat(docActualizado.getStorageKey()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Errores de generacion")
    class ErroresGeneracion {

        @Test
        @DisplayName("12. No permite generar desde ANULADA")
        void no_permite_generar_desde_anulada() {
            FalDocumento doc = guardarDoc(20L);
            FalDocumentoRedaccion red = guardarRedaccionConEstado(20L, 20L, EstadoRedaccionDocumento.ANULADA);
            assertThatThrownBy(() -> service.confirmarYGenerarMockPdf(cmd(red.getId())))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("ANULADA");
        }

        @Test
        @DisplayName("13. No permite generar dos veces desde CONFIRMADA")
        void no_permite_generar_dos_veces() {
            FalDocumento doc = guardarDoc(21L);
            FalDocumentoRedaccion red = guardarRedaccionBorrador(21L, 21L, "Contenido.");
            service.confirmarYGenerarMockPdf(cmd(red.getId()));
            assertThatThrownBy(() -> service.confirmarYGenerarMockPdf(cmd(red.getId())))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("CONFIRMADA");
        }

        @Test
        @DisplayName("14. Falla si redaccion no encontrada")
        void falla_redaccion_no_encontrada() {
            assertThatThrownBy(() -> service.confirmarYGenerarMockPdf(
                    new ConfirmarRedaccionYGenerarDocumentoMockCommand(999L, "usr", null)))
                    .isInstanceOf(ar.gob.malvinas.faltas.core.domain.exception.DocumentoRedaccionNoEncontradaException.class);
        }

        @Test
        @DisplayName("15. Falla si command tiene redaccionId null")
        void falla_command_redaccion_id_null() {
            assertThatThrownBy(() -> new ConfirmarRedaccionYGenerarDocumentoMockCommand(null, "usr", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("redaccionId");
        }

        @Test
        @DisplayName("16. Falla si command tiene idUserOperacion blank")
        void falla_command_user_blank() {
            assertThatThrownBy(() -> new ConfirmarRedaccionYGenerarDocumentoMockCommand(1L, "  ", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idUserOperacion");
        }
    }
}
