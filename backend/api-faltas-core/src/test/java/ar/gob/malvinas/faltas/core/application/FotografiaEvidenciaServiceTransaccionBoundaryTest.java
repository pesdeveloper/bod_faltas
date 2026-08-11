package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.FotografiaEntradaAlta;
import ar.gob.malvinas.faltas.core.application.service.FotografiaEvidenciaService;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;
import ar.gob.malvinas.faltas.core.infrastructure.imagen.TwelveMonkeysImagenEvidenciaProcesador;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.infrastructure.storage.FilesystemEvidenciaAlmacenamientoAdapter;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Wave 4 Parte 3F-B, hardening §4: {@code FotografiaEvidenciaService} productivo solo puede
 * operar dentro de una transacción Spring real y sincronizada (decisión cerrada por el prompt,
 * no una regla de negocio visible al usuario final).
 *
 * <p>No usa {@code @SpringBootTest}: instancia el servicio directamente (sin proxy de Spring),
 * de modo que llamarlo tal cual reproduce exactamente "fuera de transacción" sin necesidad de
 * infraestructura adicional. Para el caso positivo, activa manualmente
 * {@link TransactionSynchronizationManager} — la misma API que usa el propio
 * {@code PlatformTransactionManager} de Spring por debajo — para simular una transacción real
 * sincronizada sin levantar un contexto Spring completo.
 */
class FotografiaEvidenciaServiceTransaccionBoundaryTest {

    private Path raiz;
    private InMemoryActaEvidenciaRepository evidenciaRepository;
    private FotografiaEvidenciaService service;
    private Long idActa;

    @BeforeEach
    void setUp() throws IOException {
        raiz = Files.createTempDirectory("faltas-test-tx-boundary-");
        InMemoryActaRepository actaRepository = new InMemoryActaRepository();
        evidenciaRepository = new InMemoryActaEvidenciaRepository();
        service = new FotografiaEvidenciaService(
                actaRepository,
                evidenciaRepository,
                new TwelveMonkeysImagenEvidenciaProcesador(5_242_880L, 40_000_000L, 1920, 0.85f),
                new FilesystemEvidenciaAlmacenamientoAdapter(raiz.toString(), raiz.toString(), new MockEnvironment()),
                FaltasClockTestSupport.FIXED,
                10);

        FalActa acta = actaRepository.guardar(new FalActa(
                null, UUID.randomUUID().toString(), TipoActa.TRANSITO, 1L, 1L,
                LocalDate.of(2026, 8, 7), LocalDateTime.of(2026, 8, 7, 9, 0, 0),
                "Dom. de test", null, null, null,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, SituacionIdentificacionInfractor.DATOS_INSUFICIENTES,
                null, LocalDateTime.of(2026, 8, 7, 9, 0, 0), "actor-test"));
        idActa = acta.getId();
        ActorContextHolder.set(new ActorContext("11111111-1111-1111-1111-111111111111"));
    }

    @AfterEach
    void tearDown() {
        ActorContextHolder.clear();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    private static byte[] jpeg() {
        try {
            BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            var g = img.createGraphics();
            g.setColor(Color.CYAN);
            g.fillRect(0, 0, 10, 10);
            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private long contarArchivosBajo(String subdir) throws IOException {
        Path dir = raiz.resolve(subdir);
        if (!Files.exists(dir)) {
            return 0L;
        }
        try (var stream = Files.list(dir)) {
            return stream.count();
        }
    }

    // =====================================================================
    // 1-4: fuera de transaccion -> rechazo explicito, 0 metadata, 0 finales, 0 temporales
    // =====================================================================

    @Test
    @DisplayName("incorporarLoteAlta fuera de transaccion: rechazo explicito, 0 metadata, 0 archivos")
    void incorporarLoteAltaFueraDeTransaccionRechaza() throws IOException {
        assertThat(TransactionSynchronizationManager.isSynchronizationActive())
                .as("precondicion del test: sin transaccion Spring activa").isFalse();

        List<FotografiaEntradaAlta> fotos = List.of(new FotografiaEntradaAlta(jpeg(), "foto.jpg", null));

        assertThatThrownBy(() -> service.incorporarLoteAlta(
                idActa, fotos, "actor-test", LocalDateTime.now(), BloqueActual.ENRI))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transaccion");

        assertThat(evidenciaRepository.listarFotografiasPorActa(idActa)).as("0 metadata").isEmpty();
        assertThat(contarArchivosBajo("temporal")).as("0 temporales").isZero();
        assertThat(contarArchivosBajo("final")).as("0 finales").isZero();
    }

    @Test
    @DisplayName("incorporarLotePosterior invocado sin el proxy transaccional de Spring: rechazo explicito")
    void incorporarLotePosteriorSinProxySpringRechaza() throws IOException {
        // @Transactional en el metodo no tiene efecto sobre una instancia creada con "new": esto
        // reproduce exactamente "fuera de transaccion" sin necesidad de infraestructura Spring.
        List<FotografiaEntradaAlta> fotos = List.of(new FotografiaEntradaAlta(jpeg(), "foto.jpg", null));

        assertThatThrownBy(() -> service.incorporarLotePosterior(idActa, fotos))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transaccion");

        assertThat(evidenciaRepository.listarFotografiasPorActa(idActa)).as("0 metadata").isEmpty();
        assertThat(contarArchivosBajo("temporal")).as("0 temporales").isZero();
        assertThat(contarArchivosBajo("final")).as("0 finales").isZero();
    }

    // =====================================================================
    // 5: dentro de una transaccion activa y sincronizada -> comportamiento normal
    // =====================================================================

    @Test
    @DisplayName("dentro de una transaccion Spring real y sincronizada: comportamiento normal, no rechaza")
    void dentroDeTransaccionActivaFuncionaNormal() throws IOException {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        List<FalActaEvidencia> resultado;
        try {
            List<FotografiaEntradaAlta> fotos = List.of(new FotografiaEntradaAlta(jpeg(), "foto.jpg", null));
            resultado = service.incorporarLoteAlta(idActa, fotos, "actor-test", LocalDateTime.now(), BloqueActual.ENRI);
            // Simula el commit: dispara la compensacion post-transaccional registrada por el
            // servicio (limpia el temporal, conserva el final), igual que Spring en produccion.
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }

        assertThat(resultado).hasSize(1);
        assertThat(evidenciaRepository.listarFotografiasPorActa(idActa)).hasSize(1);
        assertThat(contarArchivosBajo("final")).as("1 archivo final promovido y conservado tras el commit").isEqualTo(1L);
        assertThat(contarArchivosBajo("temporal")).as("temporal limpiado tras el commit").isZero();
    }
}
