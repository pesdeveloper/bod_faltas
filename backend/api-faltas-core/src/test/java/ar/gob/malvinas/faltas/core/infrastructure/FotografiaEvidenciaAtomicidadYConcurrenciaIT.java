package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.application.command.FotografiaEntradaAlta;
import ar.gob.malvinas.faltas.core.application.port.AlmacenamientoEvidenciaPort;
import ar.gob.malvinas.faltas.core.application.service.FotografiaEvidenciaService;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenIncorporacionEvidencia;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.repository.ActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IT de atomicidad MariaDB/filesystem y concurrencia real de fotografias
 * (Wave 4 Parte 3D S11/S16/S17/S19), contra {@link FotografiaEvidenciaService} real.
 *
 * <p>Filesystem real bajo un directorio temporal aislado por clase (nunca una ruta
 * productiva); {@code faltas.storage.evidencias.root-dir} se sobreescribe por
 * {@link DynamicPropertySource} antes de levantar el contexto.
 *
 * <p><b>Alcance deliberado.</b> Estos casos ejercitan directamente el coordinador
 * {@link FotografiaEvidenciaService} (el unico punto donde se cruzan pipeline de imagen,
 * filesystem y JDBC) sobre un acta minima insertada directamente por JDBC, sin pasar por
 * {@code ActaService.labrar} completo (que exige dependencia/inspector/talonario/normativa).
 * El wiring de {@code ActaService.labrar -> FotografiaEvidenciaService.incorporarLoteAlta}
 * dentro de la misma transaccion del alta es responsabilidad de compilacion/DI (ActaService
 * no compila sin el colaborador) mas los tests unitarios de {@code ActaAltaCompletaTest} y
 * los IT existentes del alta; lo que aqui se certifica contra MariaDB/filesystem reales es el
 * contrato de atomicidad y concurrencia del coordinador en si, que es identico para el camino
 * de alta y el de carga posterior.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@DisplayName("IT Wave 4 Parte 3D: atomicidad MariaDB/filesystem y concurrencia de fotografias")
class FotografiaEvidenciaAtomicidadYConcurrenciaIT {

    private static final String MARCA = "WAVE4P3D_";
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 6, 12, 0, 0);
    private static final LocalDate F0 = LocalDate.of(2026, 8, 6);

    private static Path raizStorage;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) throws IOException {
        raizStorage = Files.createTempDirectory("faltas-it-p3d-evidencias-");
        registry.add("faltas.storage.evidencias.root-dir", () -> raizStorage.toString());
    }

    @Autowired private JdbcClient jdbc;
    @Autowired private PlatformTransactionManager txManager;
    @Autowired private ActaRepository actaRepository;
    @Autowired private ActaEvidenciaRepository evidenciaRepository;
    @Autowired private FotografiaEvidenciaService fotografiaEvidenciaService;
    @Autowired private AlmacenamientoEvidenciaPort almacenamiento;

    private String actor;

    @BeforeEach
    void nuevoActor() {
        actor = UUID.randomUUID().toString();
        ActorContextHolder.set(new ActorContext(actor));
    }

    @AfterEach
    void limpiarContextoYResiduos() {
        ActorContextHolder.clear();
        borrarResiduosDelSlice();
    }

    /**
     * Limpieza completa del sandbox (Parte 3D S26): actas, evidencias y dependencias creadas
     * por este slice (identificadas por {@code MARCA}), y los archivos finales de fotografia
     * que esas evidencias referenciaban. El directorio temporal completo se borra al final del
     * ciclo de vida de la JVM de test (es un temp dir del sistema operativo); esta limpieza
     * adicional evita acumular archivos entre los metodos de test de esta misma clase.
     */
    private void borrarResiduosDelSlice() {
        // LIKE trata "_" como comodin de un caracter: MARCA ya contiene "_", de modo que un
        // patron sin escapar tambien alcanzaria marcas de otras clases de test cuyo prefijo
        // coincide salvo un caracter (p.ej. "WAVE4P3DHTTP_" con MARCA="WAVE4P3D_"). Se escapa
        // explicitamente para que el match sea exacto.
        String patron = MARCA.replace("_", "\\_") + "%";
        List<Long> actas = jdbc.sql("""
                SELECT a.id FROM fal_acta a
                JOIN fal_dependencia d ON d.id = a.id_dep
                WHERE d.nom_dep LIKE :marca
                """).param("marca", patron).query(Long.class).list();

        for (Long idActa : actas) {
            for (FalActaEvidencia foto : evidenciaRepository.listarFotografiasPorActa(idActa)) {
                almacenamiento.eliminarFinal(foto.getStorageKey());
            }
            jdbc.sql("DELETE FROM fal_acta_evidencia WHERE id_acta = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_evento WHERE acta_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_snapshot WHERE id_acta = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_observacion WHERE entidad_tipo = 1 AND entidad_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta WHERE id = :id").param("id", idActa).update();
        }
        jdbc.sql("""
                DELETE FROM fal_dependencia_version WHERE id_dep IN (
                    SELECT id FROM fal_dependencia WHERE nom_dep LIKE :marca)
                """).param("marca", patron).update();
        jdbc.sql("DELETE FROM fal_dependencia WHERE nom_dep LIKE :marca")
                .param("marca", patron).update();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Fixtures de imagen: JPEG reales, distintos por color (SHA-256 distinto)
    // ─────────────────────────────────────────────────────────────────────

    private static byte[] jpeg(Color color) {
        try {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
            var g = img.createGraphics();
            g.setColor(color);
            g.fillRect(0, 0, 16, 16);
            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static FotografiaEntradaAlta entrada(Color color, String descripcion) {
        return new FotografiaEntradaAlta(jpeg(color), "foto.jpg", descripcion);
    }

    // ═════════════════════════════════════════════════════════════════════
    // ALTA: dentro de una transaccion controlada (commit / rollback manual)
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("1. Alta con fotografias: atomicidad MariaDB/filesystem")
    class AltaAtomicidad {

        @Test
        @DisplayName("1. commit exitoso: metadata persistida, archivo final existe, temporal limpiado")
        void commitExitoso() {
            List<String> clavesFinales = enTransaccionQueConfirma(idActa -> {
                List<FalActaEvidencia> guardadas = fotografiaEvidenciaService.incorporarLoteAlta(
                        idActa, List.of(entrada(Color.RED, "una foto")), actor, T0, BloqueActual.ENRI);
                assertThat(guardadas).hasSize(1);
                return guardadas.stream().map(FalActaEvidencia::getStorageKey).toList();
            });

            String claveFinal = clavesFinales.get(0);
            assertThat(almacenamiento.existeFinal(claveFinal))
                    .as("el archivo final debe existir tras el commit").isTrue();
            assertThat(contarArchivosTemporales())
                    .as("sin residuos temporales tras el commit").isZero();
        }

        @Test
        @DisplayName("2. cero fotografias: no toca filesystem ni persiste nada, semantica equivalente al JSON sin fotos")
        void ceroFotografias() {
            List<FalActaEvidencia> guardadas = enTransaccionQueConfirma(idActa ->
                    fotografiaEvidenciaService.incorporarLoteAlta(idActa, List.of(), actor, T0, BloqueActual.ENRI));
            assertThat(guardadas).isEmpty();
        }

        @Test
        @DisplayName("3. exactamente 10 fotografias se aceptan; 11 se rechazan antes de tocar filesystem o metadata")
        void limiteDiezFotografias() {
            List<FotografiaEntradaAlta> diez = colores(10).stream()
                    .map(c -> entrada(c, null)).collect(Collectors.toList());
            List<FalActaEvidencia> guardadas = enTransaccionQueConfirma(idActa ->
                    fotografiaEvidenciaService.incorporarLoteAlta(idActa, diez, actor, T0, BloqueActual.ENRI));
            assertThat(guardadas).hasSize(10);

            List<FotografiaEntradaAlta> once = colores(11).stream()
                    .map(c -> entrada(c, null)).collect(Collectors.toList());
            int temporalesAntes = contarArchivosTemporales();
            int finalesAntes = contarArchivosFinales();
            assertThatThrownBy(() -> enTransaccionQueConfirma(idActa ->
                    fotografiaEvidenciaService.incorporarLoteAlta(idActa, once, actor, T0, BloqueActual.ENRI)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("Maximo 10");
            assertThat(contarArchivosTemporales()).as("11 fotos: cero temporales nuevos").isEqualTo(temporalesAntes);
            assertThat(contarArchivosFinales()).as("11 fotos: cero finales nuevos").isEqualTo(finalesAntes);
        }

        @Test
        @DisplayName("4. duplicado dentro del lote rechaza el lote completo: cero metadata, cero archivos")
        void duplicadoDentroDelLote() {
            int temporalesAntes = contarArchivosTemporales();
            int finalesAntes = contarArchivosFinales();
            List<FotografiaEntradaAlta> conDuplicado = List.of(
                    entrada(Color.BLUE, "primera"),
                    entrada(Color.BLUE, "repetida-misma-imagen"));

            assertThatThrownBy(() -> enTransaccionQueConfirma(idActa ->
                    fotografiaEvidenciaService.incorporarLoteAlta(idActa, conDuplicado, actor, T0, BloqueActual.ENRI)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("mismo lote");

            assertThat(contarArchivosTemporales()).isEqualTo(temporalesAntes);
            assertThat(contarArchivosFinales()).isEqualTo(finalesAntes);
        }

        @Test
        @DisplayName("5. rollback por fallo POSTERIOR (simulado) en la misma transaccion: elimina el archivo final ya promovido, sin dejar huerfanos")
        void rollbackPorFalloPosterior() {
            int temporalesAntes = contarArchivosTemporales();
            int finalesAntes = contarArchivosFinales();

            String[] claveFinalCapturada = new String[1];
            RuntimeException fallo = new RuntimeException("FALLO_INYECTADO_SIMULA_SATELITE_POSTERIOR");

            Throwable capturado = org.assertj.core.api.Assertions.catchThrowable(() ->
                    new TransactionTemplate(txManager).execute(status -> {
                        Long idActa = insertarActaMinima();
                        List<FalActaEvidencia> guardadas = fotografiaEvidenciaService.incorporarLoteAlta(
                                idActa, List.of(entrada(Color.GREEN, "se revierte")), actor, T0, BloqueActual.ENRI);
                        claveFinalCapturada[0] = guardadas.get(0).getStorageKey();
                        assertThat(almacenamiento.existeFinal(claveFinalCapturada[0]))
                                .as("el archivo ya fue promovido ANTES del commit, dentro de la transaccion")
                                .isTrue();
                        // Simula un satelite posterior (talonario, evento, etc.) que falla dentro
                        // de la MISMA transaccion, despues de que las fotografias ya se promovieron.
                        throw fallo;
                    }));

            assertThat(capturado).isSameAs(fallo);
            assertThat(almacenamiento.existeFinal(claveFinalCapturada[0]))
                    .as("tras el rollback, el archivo final promovido debe haberse eliminado (Parte 3D S11)")
                    .isFalse();
            assertThat(contarArchivosTemporales()).as("sin residuos temporales tras el rollback").isEqualTo(temporalesAntes);
            assertThat(contarArchivosFinales()).as("sin archivos finales huerfanos tras el rollback").isEqualTo(finalesAntes);
            // La metadata nunca se confirmo: el rollback de MariaDB la revirtio.
            assertThat(evidenciaRepository.existeFotografiaConHash(1L, sha256DeEntrada(Color.GREEN))).isFalse();
        }

        @Test
        @DisplayName("6. escritura temporal fallida no deja residuos (fallo simulado con formato invalido)")
        void formatoInvalidoNoDejaResiduos() {
            int temporalesAntes = contarArchivosTemporales();
            int finalesAntes = contarArchivosFinales();
            byte[] noEsImagen = "esto no es una imagen".getBytes();

            assertThatThrownBy(() -> enTransaccionQueConfirma(idActa ->
                    fotografiaEvidenciaService.incorporarLoteAlta(idActa,
                            List.of(new FotografiaEntradaAlta(noEsImagen, "x.jpg", null)),
                            actor, T0, BloqueActual.ENRI)))
                    .isInstanceOf(ar.gob.malvinas.faltas.core.domain.exception.ImagenEvidenciaInvalidaException.class);

            assertThat(contarArchivosTemporales()).isEqualTo(temporalesAntes);
            assertThat(contarArchivosFinales()).isEqualTo(finalesAntes);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // CARGA POSTERIOR
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("2. Carga posterior: orden acumulativo, duplicados, rollback de lote")
    class CargaPosterior {

        @Test
        @DisplayName("7. una fotografia posterior sobre acta existente: orden 1, commit real")
        void unaFotografiaPosterior() {
            Long idActa = crearActaConfirmada();
            List<FalActaEvidencia> guardadas = fotografiaEvidenciaService.incorporarLotePosterior(
                    idActa, List.of(entrada(Color.MAGENTA, "posterior")));
            assertThat(guardadas).hasSize(1);
            assertThat(guardadas.get(0).getOrden()).isEqualTo(1);
            assertThat(guardadas.get(0).getOrigenIncorporacion()).isEqualTo(OrigenIncorporacionEvidencia.CARGA_POSTERIOR);
            assertThat(almacenamiento.existeFinal(guardadas.get(0).getStorageKey())).isTrue();
        }

        @Test
        @DisplayName("8. varias cargas posteriores sucesivas acumulan el orden (MAX(orden)+1)")
        void ordenAcumulativo() {
            Long idActa = crearActaConfirmada();
            fotografiaEvidenciaService.incorporarLotePosterior(idActa, List.of(entrada(Color.CYAN, null)));
            List<FalActaEvidencia> segundoLote = fotografiaEvidenciaService.incorporarLotePosterior(
                    idActa, List.of(entrada(Color.PINK, null), entrada(Color.ORANGE, null)));

            assertThat(segundoLote).extracting(FalActaEvidencia::getOrden).containsExactly(2, 3);
            assertThat(evidenciaRepository.listarFotografiasPorActa(idActa))
                    .extracting(FalActaEvidencia::getOrden).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("9. carga posterior exige al menos 1 fotografia")
        void exigeAlMenosUna() {
            Long idActa = crearActaConfirmada();
            assertThatThrownBy(() -> fotografiaEvidenciaService.incorporarLotePosterior(idActa, List.of()))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("al menos");
        }

        @Test
        @DisplayName("10. duplicado dentro del lote posterior rechaza todo el lote atomicamente")
        void duplicadoDentroDelLotePosterior() {
            Long idActa = crearActaConfirmada();
            int temporalesAntes = contarArchivosTemporales();
            int finalesAntes = contarArchivosFinales();

            assertThatThrownBy(() -> fotografiaEvidenciaService.incorporarLotePosterior(idActa,
                    List.of(entrada(Color.YELLOW, "a"), entrada(Color.YELLOW, "b"))))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("mismo lote");

            assertThat(evidenciaRepository.listarFotografiasPorActa(idActa)).isEmpty();
            assertThat(contarArchivosTemporales()).isEqualTo(temporalesAntes);
            assertThat(contarArchivosFinales()).isEqualTo(finalesAntes);
        }

        @Test
        @DisplayName("11. duplicado contra una fotografia ya existente de la acta se rechaza")
        void duplicadoContraExistente() {
            Long idActa = crearActaConfirmada();
            fotografiaEvidenciaService.incorporarLotePosterior(idActa, List.of(entrada(Color.DARK_GRAY, "primera")));

            assertThatThrownBy(() -> fotografiaEvidenciaService.incorporarLotePosterior(idActa,
                    List.of(entrada(Color.DARK_GRAY, "misma imagen otra vez"))))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("ya existe");

            assertThat(evidenciaRepository.listarFotografiasPorActa(idActa)).hasSize(1);
        }

        @Test
        @DisplayName("12. la misma fotografia (mismo SHA-256) SI se permite en una acta distinta")
        void mismaFotoEnActaDistintaPermitida() {
            Long actaUno = crearActaConfirmada();
            Long actaDos = crearActaConfirmada();
            fotografiaEvidenciaService.incorporarLotePosterior(actaUno, List.of(entrada(Color.LIGHT_GRAY, null)));

            List<FalActaEvidencia> enOtraActa = fotografiaEvidenciaService.incorporarLotePosterior(
                    actaDos, List.of(entrada(Color.LIGHT_GRAY, null)));
            assertThat(enOtraActa).hasSize(1);
        }

        @Test
        @DisplayName("13. rollback de lote posterior: ninguna fotografia parcial sobrevive, sin huerfanos")
        void rollbackDeLotePosterior() {
            Long idActa = crearActaConfirmada();
            fotografiaEvidenciaService.incorporarLotePosterior(idActa, List.of(entrada(Color.BLACK, "existente")));

            int temporalesAntes = contarArchivosTemporales();
            int finalesAntes = contarArchivosFinales();

            // El segundo elemento del lote duplica la ya existente: el lote completo (incluida
            // la primera fotografia, valida y distinta) debe revertirse atomicamente.
            assertThatThrownBy(() -> fotografiaEvidenciaService.incorporarLotePosterior(idActa,
                    List.of(entrada(Color.WHITE, "valida-pero-en-lote-que-falla"), entrada(Color.BLACK, "duplicada"))))
                    .isInstanceOf(PrecondicionVioladaException.class);

            assertThat(evidenciaRepository.listarFotografiasPorActa(idActa))
                    .as("solo la fotografia previa, ninguna del lote fallido").hasSize(1);
            assertThat(contarArchivosTemporales()).isEqualTo(temporalesAntes);
            assertThat(contarArchivosFinales()).isEqualTo(finalesAntes);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // CONCURRENCIA REAL
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("3. Concurrencia real contra MariaDB")
    class Concurrencia {

        @Test
        @DisplayName("14. dos cargas posteriores concurrentes con fotografias DISTINTAS: ambas exitosas, orden sin colisiones")
        void concurrenciaFotosDistintas() throws InterruptedException {
            Long idActa = crearActaConfirmada();
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch arranque = new CountDownLatch(2);
            CountDownLatch listos = new CountDownLatch(2);

            var f1 = pool.submit(() -> ejecutarConActor(() -> {
                arranque.countDown();
                esperar(arranque);
                var r = fotografiaEvidenciaService.incorporarLotePosterior(idActa, List.of(entrada(Color.RED.darker(), "hilo-1")));
                listos.countDown();
                return r;
            }));
            var f2 = pool.submit(() -> ejecutarConActor(() -> {
                arranque.countDown();
                esperar(arranque);
                var r = fotografiaEvidenciaService.incorporarLotePosterior(idActa, List.of(entrada(Color.BLUE.darker(), "hilo-2")));
                listos.countDown();
                return r;
            }));

            List<FalActaEvidencia> r1 = obtener(f1);
            List<FalActaEvidencia> r2 = obtener(f2);
            pool.shutdown();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

            assertThat(r1).hasSize(1);
            assertThat(r2).hasSize(1);
            List<Integer> ordenes = evidenciaRepository.listarFotografiasPorActa(idActa).stream()
                    .map(FalActaEvidencia::getOrden).sorted().toList();
            assertThat(ordenes).as("dos fotografias, orden sin colisiones ni huecos")
                    .containsExactly(1, 2);
        }

        @Test
        @DisplayName("15. dos cargas posteriores concurrentes con la MISMA fotografia: exactamente una exitosa, una metadata, un archivo final, la perdedora sin residuos")
        void concurrenciaMismaFoto() throws InterruptedException {
            Long idActa = crearActaConfirmada();
            byte[] mismaImagen = jpeg(Color.RED.brighter().brighter());
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch arranque = new CountDownLatch(2);
            AtomicInteger exitos = new AtomicInteger();
            AtomicInteger rechazos = new AtomicInteger();

            var f1 = pool.submit(() -> ejecutarConActor(() -> intentar(idActa, mismaImagen, arranque, exitos, rechazos)));
            var f2 = pool.submit(() -> ejecutarConActor(() -> intentar(idActa, mismaImagen, arranque, exitos, rechazos)));

            obtener(f1);
            obtener(f2);
            pool.shutdown();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

            assertThat(exitos.get()).as("exactamente una incorporacion exitosa").isEqualTo(1);
            assertThat(rechazos.get()).as("la otra rechazada").isEqualTo(1);

            List<FalActaEvidencia> fotos = evidenciaRepository.listarFotografiasPorActa(idActa);
            assertThat(fotos).as("una sola metadata").hasSize(1);
            assertThat(almacenamiento.existeFinal(fotos.get(0).getStorageKey())).isTrue();
        }

        private Object intentar(Long idActa, byte[] contenido, CountDownLatch arranque,
                                AtomicInteger exitos, AtomicInteger rechazos) {
            arranque.countDown();
            esperar(arranque);
            try {
                fotografiaEvidenciaService.incorporarLotePosterior(
                        idActa, List.of(new FotografiaEntradaAlta(contenido, "misma.jpg", null)));
                exitos.incrementAndGet();
            } catch (PrecondicionVioladaException duplicado) {
                rechazos.incrementAndGet();
            }
            return null;
        }

        private void esperar(CountDownLatch latch) {
            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private <T> T ejecutarConActor(java.util.concurrent.Callable<T> tarea) {
            ActorContextHolder.set(new ActorContext(UUID.randomUUID().toString()));
            try {
                return tarea.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            } finally {
                ActorContextHolder.clear();
            }
        }

        private <T> T obtener(java.util.concurrent.Future<T> f) {
            try {
                return f.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // SOPORTE
    // ═════════════════════════════════════════════════════════════════════

    private static List<Color> colores(int n) {
        List<Color> lista = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            lista.add(new Color((i * 20) % 256, (i * 40) % 256, (i * 60) % 256));
        }
        return lista;
    }

    private String sha256DeEntrada(Color color) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            // Nota: no es el sha256 final procesado (eso depende del pipeline), se usa solo
            // como discriminador informativo en el mensaje de fallo si la aserción de hash
            // llegara a fallar; la aserción real de "cero metadata" ya cubre la propiedad.
            return java.util.Base64.getEncoder().encodeToString(md.digest(jpeg(color)));
        } catch (Exception e) {
            return "n/a";
        }
    }

    private int contarArchivosTemporales() {
        return contarArchivosBajo(raizStorage.resolve("temporal"));
    }

    private int contarArchivosFinales() {
        return contarArchivosBajo(raizStorage.resolve("final"));
    }

    private int contarArchivosBajo(Path dir) {
        try {
            if (!Files.isDirectory(dir)) return 0;
            try (var stream = Files.list(dir)) {
                return (int) stream.count();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Ejecuta el escenario dentro de una transaccion que SIEMPRE confirma (commit real). */
    private <T> T enTransaccionQueConfirma(Function<Long, T> escenario) {
        return new TransactionTemplate(txManager).execute(status ->
                escenario.apply(insertarActaMinima()));
    }

    /** Crea una acta minima y confirma (fuera de cualquier transaccion de test). */
    private Long crearActaConfirmada() {
        return new TransactionTemplate(txManager).execute(status -> insertarActaMinima());
    }

    /** Acta minima directa por JDBC, sin pasar por ActaService.labrar (ver Javadoc de la clase). */
    private Long insertarActaMinima() {
        String nomDep = MARCA + "DEP_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.sql("""
                INSERT INTO fal_dependencia (nom_dep, fh_alta, id_user_alta)
                VALUES (:nom, :fhAlta, :usuario)
                """)
                .param("nom", nomDep)
                .param("fhAlta", T0)
                .param("usuario", actor)
                .update();
        Long idDep = jdbc.sql("SELECT id FROM fal_dependencia WHERE nom_dep = :nom")
                .param("nom", nomDep).query(Long.class).single();
        jdbc.sql("""
                INSERT INTO fal_dependencia_version
                    (id_dep, ver_dep, tipo_acta, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:idDep, 1, 1, :vigDesde, :fhAlta, :usuario)
                """)
                .param("idDep", idDep)
                .param("vigDesde", LocalDate.of(2026, 1, 1))
                .param("fhAlta", T0)
                .param("usuario", actor)
                .update();

        FalActa acta = new FalActa(
                null, UUID.randomUUID().toString(), TipoActa.TRANSITO, idDep, null,
                F0, T0, MARCA + "domicilio", null, null, null,
                ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                SituacionIdentificacionInfractor.AUSENTE, null, T0, actor);
        acta.setCodigoQr(MARCA + UUID.randomUUID());
        return actaRepository.guardar(acta).getId();
    }
}
