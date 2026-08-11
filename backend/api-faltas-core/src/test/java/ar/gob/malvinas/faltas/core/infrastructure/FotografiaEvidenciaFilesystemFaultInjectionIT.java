package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.application.command.FotografiaEntradaAlta;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.port.AlmacenamientoEvidenciaPort;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaAltaTransientRetryService;
import ar.gob.malvinas.faltas.core.application.service.FotografiaEvidenciaService;
import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.support.LabrarActaCommandTestFactory;
import ar.gob.malvinas.faltas.core.support.TalonarioElectronicoITSeedSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IT real de fault injection de E/S de filesystem en el pipeline de fotografías (Wave 4 Parte
 * 3F-B, hardening §5).
 *
 * <h2>Diseño del fault injector</h2>
 *
 * <p>Envuelve el bean real de {@link AlmacenamientoEvidenciaPort} (adaptador filesystem
 * productivo, {@code FilesystemEvidenciaAlmacenamientoAdapter}) con un proxy dinámico que
 * intercepta {@code escribirTemporal}/{@code promover}: cuando hay una fase de fallo activa,
 * lanza una excepción de E/S <b>antes</b> de delegar al adaptador real (el archivo nunca llega a
 * escribirse/moverse), simulando un disco lleno o un error de escritura real. No se usa
 * "formato inválido" (eso ya lo certifica {@code FotografiaEvidenciaAtomicidadYConcurrenciaIT}
 * caso 6): esto es una categoría de fallo distinta, de infraestructura, no de contenido.
 *
 * <p>{@link MariaDbTransientFailureClassifier} no reconoce {@link UncheckedIOException} (no hay
 * ningún {@link java.sql.SQLException} en su cadena de causas): un fallo de filesystem propaga
 * en un único intento, nunca dispara el retry transitorio de MariaDB (Wave 4 Parte 3E). Se
 * certifica contando las invocaciones reales al puerto decorado.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@Import(FotografiaEvidenciaFilesystemFaultInjectionIT.FalloAlmacenamientoTestConfig.class)
@DisplayName("IT Wave 4 Parte 3F-B: fault injection de E/S de filesystem en fotografías")
class FotografiaEvidenciaFilesystemFaultInjectionIT {

    private static final String MARCA = "W4P3FB_IOFAULT_";
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 7, 9, 0, 0);
    private static final LocalDate F0 = LocalDate.of(2026, 8, 7);

    enum FaseFallo { NINGUNA, ESCRIBIR_TEMPORAL, PROMOVER }

    static volatile FaseFallo faseFallo = FaseFallo.NINGUNA;
    static volatile int fallarEnInvocacionNumero = 1;
    static final AtomicInteger invocacionesEscribirTemporal = new AtomicInteger();
    static final AtomicInteger invocacionesPromover = new AtomicInteger();

    @Autowired private ActaAltaTransientRetryService retryService;
    @Autowired private FotografiaEvidenciaService fotografiaEvidenciaService;
    @Autowired private TalonarioService talonarioService;
    @Autowired private JdbcClient jdbc;

    @Value("${faltas.storage.evidencias.root-dir}")
    private String storageRootDir;

    private Long idDependencia;
    private short verDependencia;
    private Long idTalonarioSeed;

    @BeforeEach
    void preparar() {
        faseFallo = FaseFallo.NINGUNA;
        fallarEnInvocacionNumero = 1;
        invocacionesEscribirTemporal.set(0);
        invocacionesPromover.set(0);
        ActorContextHolder.set(new ActorContext(UUID.randomUUID().toString()));
        insertarDependencia();
        idTalonarioSeed = TalonarioElectronicoITSeedSupport.seedTalonarioElectronicoActivo(
                talonarioService, idDependencia, verDependencia, LocalDate.of(2026, 1, 1));
    }

    @AfterEach
    void limpiar() {
        faseFallo = FaseFallo.NINGUNA;
        ActorContextHolder.clear();
        borrarResiduos();
    }

    // =====================================================================
    // Fixtures
    // =====================================================================

    private void insertarDependencia() {
        jdbc.sql("""
                INSERT INTO fal_dependencia (nom_dep, fh_alta, id_user_alta)
                VALUES (:nom, :fhAlta, :usuario)
                """)
                .param("nom", MARCA + "DEP")
                .param("fhAlta", T0)
                .param("usuario", actor())
                .update();
        idDependencia = jdbc.sql("SELECT MAX(id) FROM fal_dependencia WHERE nom_dep = :nom")
                .param("nom", MARCA + "DEP")
                .query(Long.class)
                .single();
        verDependencia = 1;
        jdbc.sql("""
                INSERT INTO fal_dependencia_version
                    (id_dep, ver_dep, tipo_acta, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:idDep, :verDep, :tipoActa, :vigDesde, :fhAlta, :usuario)
                """)
                .param("idDep", idDependencia)
                .param("verDep", verDependencia)
                .param("tipoActa", (short) 1)
                .param("vigDesde", LocalDate.of(2026, 1, 1))
                .param("fhAlta", T0)
                .param("usuario", actor())
                .update();
    }

    private static String actor() {
        return ActorContextHolder.subOr("SYS");
    }

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

    private LabrarActaCommand comandoConFotos(String idTecnico, List<FotografiaEntradaAlta> fotos) {
        return LabrarActaCommandTestFactory.builder(TipoActa.TRANSITO)
                .idDependencia(idDependencia)
                .idInspector(null)
                .fechaActa(F0)
                .domicilioHecho(MARCA + "domicilio del hecho")
                .resultadoFirmaInfractor(ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE)
                .situacionIdentificacionInfractor(SituacionIdentificacionInfractor.AUSENTE)
                .idTecnico(idTecnico)
                .fotografias(fotos)
                .build();
    }

    // =====================================================================
    // Lecturas de verificación
    // =====================================================================

    private long contarActas() {
        return jdbc.sql("SELECT COUNT(*) FROM fal_acta WHERE id_dep = :idDep")
                .param("idDep", idDependencia).query(Long.class).single();
    }

    private long contarEventosActlab() {
        return jdbc.sql("""
                SELECT COUNT(*) FROM fal_acta_evento e
                 WHERE e.tipo_evt = 'ACTLAB' AND e.acta_id IN (SELECT a.id FROM fal_acta a WHERE a.id_dep = :idDep)
                """).param("idDep", idDependencia).query(Long.class).single();
    }

    private long contarSnapshots() {
        return jdbc.sql("""
                SELECT COUNT(*) FROM fal_acta_snapshot s
                 WHERE s.id_acta IN (SELECT a.id FROM fal_acta a WHERE a.id_dep = :idDep)
                """).param("idDep", idDependencia).query(Long.class).single();
    }

    private long contarValorizaciones() {
        return jdbc.sql("""
                SELECT COUNT(*) FROM fal_acta_valorizacion v
                 WHERE v.id_acta IN (SELECT a.id FROM fal_acta a WHERE a.id_dep = :idDep)
                """).param("idDep", idDependencia).query(Long.class).single();
    }

    private long contarEvidenciasFoto(Long idActa) {
        return jdbc.sql("SELECT COUNT(*) FROM fal_acta_evidencia WHERE id_acta = :id AND tipo_evid = :tipo")
                .param("id", idActa)
                .param("tipo", TipoEvidenciaActa.FOTO.codigo())
                .query(Long.class).single();
    }

    private int ultimoNroEmitido() {
        return jdbc.sql("SELECT ultimo_nro_emitido FROM num_talonario WHERE id = :id")
                .param("id", idTalonarioSeed).query(Integer.class).single();
    }

    private long contarArchivosEn(String subdir) throws IOException {
        Path dir = Path.of(storageRootDir).toAbsolutePath().normalize().resolve(subdir);
        if (!Files.exists(dir)) {
            return 0L;
        }
        try (var stream = Files.list(dir)) {
            return stream.count();
        }
    }

    private void exigirCeroMutacionesDeActa() {
        assertThat(contarActas()).as("cero fal_acta").isZero();
        assertThat(contarEventosActlab()).as("cero ACTLAB").isZero();
        assertThat(contarSnapshots()).as("cero snapshot").isZero();
        assertThat(contarValorizaciones()).as("cero valorizacion").isZero();
        assertThat(ultimoNroEmitido()).as("numeracion no consumida").isZero();
    }

    private void borrarResiduos() {
        List<Long> actas = jdbc.sql("SELECT id FROM fal_acta WHERE id_dep = :idDep")
                .param("idDep", idDependencia).query(Long.class).list();
        for (Long idActa : actas) {
            borrarArchivosFinalesDeEvidencias(idActa);
            jdbc.sql("DELETE FROM fal_acta_transito WHERE acta_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_valorizacion_item WHERE id_acta_articulo IN "
                    + "(SELECT id FROM fal_acta_articulo_infringido WHERE id_acta = :id)").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_articulo_infringido WHERE id_acta = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_valorizacion WHERE id_acta = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_evidencia WHERE id_acta = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_snapshot WHERE id_acta = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_evento WHERE acta_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_observacion WHERE entidad_tipo = :tipo AND entidad_id = :id")
                    .param("tipo", EntidadTipoObservada.ACTA.codigo()).param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta WHERE id = :id").param("id", idActa).update();
        }
        borrarTalonarioDeLaDependencia();
        jdbc.sql("DELETE FROM fal_dependencia_version WHERE id_dep = :idDep")
                .param("idDep", idDependencia).update();
        jdbc.sql("DELETE FROM fal_dependencia WHERE id = :idDep").param("idDep", idDependencia).update();
    }

    private void borrarTalonarioDeLaDependencia() {
        List<Long> talonarioIds = jdbc.sql(
                        "SELECT talonario_id FROM num_talonario_ambito WHERE id_dep = :idDep")
                .param("idDep", idDependencia).query(Long.class).list();
        jdbc.sql("DELETE FROM num_talonario_ambito WHERE id_dep = :idDep")
                .param("idDep", idDependencia).update();
        for (Long idTalonario : talonarioIds) {
            jdbc.sql("DELETE FROM num_talonario_movimiento WHERE id_talonario = :id")
                    .param("id", idTalonario).update();
            Long idPolitica = jdbc.sql("SELECT politica_id FROM num_talonario WHERE id = :id")
                    .param("id", idTalonario).query(Long.class).single();
            jdbc.sql("DELETE FROM num_talonario WHERE id = :id").param("id", idTalonario).update();
            jdbc.sql("DELETE FROM num_politica WHERE id = :id").param("id", idPolitica).update();
        }
    }

    private void borrarArchivosFinalesDeEvidencias(Long idActa) {
        List<String> claves = jdbc.sql("SELECT storage_key FROM fal_acta_evidencia WHERE id_acta = :id")
                .param("id", idActa).query(String.class).list();
        if (claves.isEmpty()) {
            return;
        }
        Path raizFinal = Path.of(storageRootDir).toAbsolutePath().normalize().resolve("final");
        for (String clave : claves) {
            try {
                Files.deleteIfExists(raizFinal.resolve(clave));
            } catch (IOException ignorado) {
                // Limpieza de test best-effort.
            }
        }
    }

    // =====================================================================
    // A/B. Fallo de E/S durante ALTA (JSON/multipart inicial)
    // =====================================================================

    @Test
    @DisplayName("A. fallo real de E/S al escribir temporal durante el ALTA: rollback total, 0 numero, sin retry MariaDB")
    void fallaEscrituraTemporalDuranteAlta() {
        faseFallo = FaseFallo.ESCRIBIR_TEMPORAL;
        fallarEnInvocacionNumero = 1;
        List<FotografiaEntradaAlta> fotos = List.of(new FotografiaEntradaAlta(jpeg(Color.RED), "foto.jpg", null));
        LabrarActaCommand cmd = comandoConFotos(UUID.randomUUID().toString(), fotos);

        assertThatThrownBy(() -> retryService.labrarConReintento(cmd))
                .isInstanceOfAny(UncheckedIOException.class, RuntimeException.class)
                .hasMessageContaining("Fallo de E/S simulado");

        exigirCeroMutacionesDeActa();
        assertThat(invocacionesEscribirTemporal.get())
                .as("un unico intento: el fallo de filesystem no es transitorio para MariaDB, no se reintenta")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("B. fallo real de E/S al promover a final durante el ALTA: rollback total, 0 numero, sin retry MariaDB")
    void fallaPromoverDuranteAlta() {
        faseFallo = FaseFallo.PROMOVER;
        fallarEnInvocacionNumero = 1;
        List<FotografiaEntradaAlta> fotos = List.of(new FotografiaEntradaAlta(jpeg(Color.GREEN), "foto.jpg", null));
        LabrarActaCommand cmd = comandoConFotos(UUID.randomUUID().toString(), fotos);

        assertThatThrownBy(() -> retryService.labrarConReintento(cmd))
                .isInstanceOfAny(UncheckedIOException.class, RuntimeException.class)
                .hasMessageContaining("Fallo de E/S simulado");

        exigirCeroMutacionesDeActa();
        assertThat(invocacionesPromover.get())
                .as("un unico intento de promocion: el fallo de filesystem no dispara retry MariaDB")
                .isEqualTo(1);
    }

    // =====================================================================
    // C/D. Fallo de E/S durante CARGA POSTERIOR
    // =====================================================================

    @Test
    @DisplayName("C. fallo real de E/S al escribir temporal durante CARGA POSTERIOR: lote completo revertido")
    void fallaEscrituraTemporalDuranteCargaPosterior() throws IOException {
        ComandoResultado alta = retryService.labrarConReintento(
                comandoConFotos(UUID.randomUUID().toString(), List.of()));
        Long idActa = alta.idActa();
        long archivosFinalesAntes = contarArchivosEn("final");

        faseFallo = FaseFallo.ESCRIBIR_TEMPORAL;
        fallarEnInvocacionNumero = 1;
        List<FotografiaEntradaAlta> lote = List.of(
                new FotografiaEntradaAlta(jpeg(Color.BLUE), "posterior-1.jpg", null),
                new FotografiaEntradaAlta(jpeg(Color.YELLOW), "posterior-2.jpg", null));

        assertThatThrownBy(() -> fotografiaEvidenciaService.incorporarLotePosterior(idActa, lote))
                .isInstanceOfAny(UncheckedIOException.class, RuntimeException.class)
                .hasMessageContaining("Fallo de E/S simulado");

        assertThat(contarEvidenciasFoto(idActa)).as("0 metadata nueva para el lote fallido").isZero();
        assertThat(contarArchivosEn("final"))
                .as("0 archivos finales nuevos").isEqualTo(archivosFinalesAntes);
    }

    @Test
    @DisplayName("D. fallo real de E/S al promover durante CARGA POSTERIOR: lote completo revertido")
    void fallaPromoverDuranteCargaPosterior() throws IOException {
        ComandoResultado alta = retryService.labrarConReintento(
                comandoConFotos(UUID.randomUUID().toString(), List.of()));
        Long idActa = alta.idActa();
        long archivosFinalesAntes = contarArchivosEn("final");

        faseFallo = FaseFallo.PROMOVER;
        fallarEnInvocacionNumero = 1;
        List<FotografiaEntradaAlta> lote = List.of(
                new FotografiaEntradaAlta(jpeg(Color.MAGENTA), "posterior-3.jpg", null));

        assertThatThrownBy(() -> fotografiaEvidenciaService.incorporarLotePosterior(idActa, lote))
                .isInstanceOfAny(UncheckedIOException.class, RuntimeException.class)
                .hasMessageContaining("Fallo de E/S simulado");

        assertThat(contarEvidenciasFoto(idActa)).as("0 metadata nueva para el lote fallido").isZero();
        assertThat(contarArchivosEn("final"))
                .as("0 archivos finales nuevos").isEqualTo(archivosFinalesAntes);
    }

    // =====================================================================
    // Decorador de test: fault injector sobre AlmacenamientoEvidenciaPort (solo src/test)
    // =====================================================================

    /**
     * Envuelve el bean real de {@link AlmacenamientoEvidenciaPort} con un proxy dinámico que
     * intercepta {@code escribirTemporal}/{@code promover}: si hay una fase de fallo activa y la
     * invocación coincide con el número configurado, lanza un {@link UncheckedIOException} antes
     * de delegar (el archivo real nunca se escribe ni se mueve). Sin fallo pendiente, delega
     * siempre en el adaptador real sin alterar nada.
     */
    @TestConfiguration
    static class FalloAlmacenamientoTestConfig {

        @Bean
        static BeanPostProcessor almacenamientoEvidenciaFalloInyectado() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (bean instanceof AlmacenamientoEvidenciaPort real) {
                        return Proxy.newProxyInstance(
                                AlmacenamientoEvidenciaPort.class.getClassLoader(),
                                new Class<?>[]{AlmacenamientoEvidenciaPort.class},
                                (proxy, method, args) -> {
                                    String nombre = method.getName();
                                    if ("escribirTemporal".equals(nombre)) {
                                        int n = invocacionesEscribirTemporal.incrementAndGet();
                                        if (faseFallo == FaseFallo.ESCRIBIR_TEMPORAL && n == fallarEnInvocacionNumero) {
                                            throw fallaIoSimulada("escribirTemporal");
                                        }
                                    } else if ("promover".equals(nombre)) {
                                        int n = invocacionesPromover.incrementAndGet();
                                        if (faseFallo == FaseFallo.PROMOVER && n == fallarEnInvocacionNumero) {
                                            throw fallaIoSimulada("promover");
                                        }
                                    }
                                    try {
                                        return method.invoke(real, args);
                                    } catch (InvocationTargetException e) {
                                        throw e.getCause();
                                    }
                                });
                    }
                    return bean;
                }
            };
        }

        private static RuntimeException fallaIoSimulada(String operacion) {
            return new UncheckedIOException(
                    "Fallo de E/S simulado en " + operacion + " (fault injection Wave 4 Parte 3F-B, disco lleno/escritura imposible).",
                    new IOException("No space left on device (simulado)"));
        }
    }
}
