package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.application.command.FotografiaEntradaAlta;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaAltaTransientRetryService;
import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.repository.TalonarioRepository;
import ar.gob.malvinas.faltas.core.support.LabrarActaCommandTestFactory;
import ar.gob.malvinas.faltas.core.support.TalonarioElectronicoITSeedSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IT real de retry transaccional del alta ante errores transitorios de MariaDB (Wave 4 Parte
 * 3E §11-§13, §18).
 *
 * <h2>Diseño del fault injector (prompt §18, último párrafo)</h2>
 *
 * <p>El clasificador ({@code MariaDbTransientFailureClassifierRealIT}) ya está certificado
 * contra un deadlock y un lock wait timeout <b>reales</b> de MariaDB, provocados por dos
 * conexiones JDBC genuinas. Este IT no repite esa certificación: usa un decorador de test sobre
 * {@link TalonarioRepository} (bean Spring real, delegando siempre en el adaptador JDBC real)
 * que, en el punto exacto de {@code reclamarNumeroActaElectronico} — <b>después</b> de que
 * {@code ActaService.labrar} ya promovió las fotografías del lote (Wave 4 Parte 3D), dentro de
 * la misma transacción — lanza una excepción con la forma exacta que Spring produce para 1213/
 * 1205 reales (mismo tipo, misma causa {@link SQLException} con el código nativo real). Es una
 * ubicación controlada del fallo, no una simulación del clasificador.
 *
 * <p>Cada intento de {@link ActaAltaTransientRetryService#labrarConReintento} pasa por una
 * transacción Spring nueva y completa (el orquestador no es transaccional); verificar contra
 * MariaDB real que un intento fallido no deja acta, evidencia, evento, snapshot ni movimiento de
 * talonario es la única forma de certificar la frontera transaccional real, no solo su diseño.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@Import(AltaActaRetryTransactionIT.FalloTalonarioTestConfig.class)
@DisplayName("IT Wave 4 Parte 3E: retry transaccional del alta ante fallos transitorios de MariaDB")
class AltaActaRetryTransactionIT {

    private static final String MARCA = "W4P3E_RETRY_";
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 6, 12, 0, 0);
    private static final LocalDate F0 = LocalDate.of(2026, 8, 6);

    /**
     * Cola de fallos a lanzar en las próximas invocaciones de
     * {@code TalonarioRepository.nextNumeroTalonario}: una entrada por intento fallido deseado,
     * en orden. Vacía = delegar normalmente (intento exitoso).
     */
    static final Queue<RuntimeException> fallosPendientes = new ConcurrentLinkedQueue<>();

    /** Cuenta cuántas veces se invocó realmente el método decorado (incluye éxitos y fallos). */
    static final AtomicInteger invocacionesNextNumero = new AtomicInteger();

    @Autowired
    private ActaAltaTransientRetryService retryService;
    @Autowired
    private TalonarioService talonarioService;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @org.springframework.beans.factory.annotation.Value("${faltas.storage.evidencias.root-dir}")
    private String storageRootDir;

    private Long idDependencia;
    private short verDependencia;
    private Long idTalonarioSeed;

    @BeforeEach
    void preparar() {
        fallosPendientes.clear();
        invocacionesNextNumero.set(0);
        ActorContextHolder.set(new ActorContext(UUID.randomUUID().toString()));
        insertarDependencia();
        idTalonarioSeed = TalonarioElectronicoITSeedSupport.seedTalonarioElectronicoActivo(
                talonarioService, idDependencia, verDependencia, LocalDate.of(2026, 1, 1));
    }

    @AfterEach
    void limpiar() {
        fallosPendientes.clear();
        invocacionesNextNumero.set(0);
        ActorContextHolder.clear();
        borrarResiduos();
    }

    // =====================================================================
    // Fallos sintéticos con la forma exacta de Spring para 1213/1205 reales
    // =====================================================================

    private static RuntimeException deadlockSintetico() {
        SQLException causa = new SQLException(
                "Deadlock found when trying to get lock; try restarting transaction", "40001", 1213);
        return new DeadlockLoserDataAccessException("reclamarNumeroActaElectronico", causa);
    }

    private static RuntimeException lockWaitTimeoutSintetico() {
        SQLException causa = new SQLException("Lock wait timeout exceeded; try restarting transaction",
                "HY000", 1205);
        return new CannotAcquireLockException("reclamarNumeroActaElectronico", causa);
    }

    // =====================================================================
    // Fixtures de dependencia
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

    private void borrarResiduos() {
        List<Long> actas = jdbc.sql("SELECT id FROM fal_acta WHERE id_dep = :idDep")
                .param("idDep", idDependencia).query(Long.class).list();
        for (Long idActa : actas) {
            borrarArchivosFinalesDeEvidencias(idActa);
            jdbc.sql("DELETE FROM fal_acta_transito WHERE acta_id = :id").param("id", idActa).update();
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

    private static String actor() {
        return ActorContextHolder.subOr("SYS");
    }

    /**
     * Limpieza de filesystem real (Wave 4 Parte 3E §21: "0 evidencias residuales"). Se ejecuta
     * antes de borrar las filas de {@code fal_acta_evidencia}, porque necesita leer
     * {@code storage_key} para saber qué archivo final borrar.
     */
    private void borrarArchivosFinalesDeEvidencias(Long idActa) {
        List<String> claves = jdbc.sql("SELECT storage_key FROM fal_acta_evidencia WHERE id_acta = :id")
                .param("id", idActa).query(String.class).list();
        if (claves.isEmpty()) {
            return;
        }
        java.nio.file.Path raizFinal = java.nio.file.Path.of(storageRootDir).toAbsolutePath().normalize().resolve("final");
        for (String clave : claves) {
            try {
                java.nio.file.Files.deleteIfExists(raizFinal.resolve(clave));
            } catch (IOException ignorado) {
                // Limpieza de test best-effort: no debe romper @AfterEach si el archivo ya no existe.
            }
        }
    }

    // =====================================================================
    // Lecturas de verificación (estado ya commiteado; otra conexión del pool)
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

    /**
     * Correlativo electrónico realmente comprometido del talonario seedeado
     * ({@code num_talonario.ultimo_nro_emitido}). El alta electrónica
     * ({@code reclamarNumeroActaElectronico}) nunca inserta en
     * {@code num_talonario_movimiento} (esa tabla es del circuito de talonario manual físico);
     * el único rastro físico de un número reclamado y no revertido es este contador.
     */
    private int ultimoNroEmitido() {
        return jdbc.sql("SELECT ultimo_nro_emitido FROM num_talonario WHERE id = :id")
                .param("id", idTalonarioSeed).query(Integer.class).single();
    }

    private long contarEvidenciasFoto(Long idActa) {
        return jdbc.sql("SELECT COUNT(*) FROM fal_acta_evidencia WHERE id_acta = :id AND tipo_evid = :tipo")
                .param("id", idActa)
                .param("tipo", ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa.FOTO.codigo())
                .query(Long.class).single();
    }

    private void exigirCeroMutaciones() {
        assertThat(contarActas()).as("cero fal_acta").isZero();
        assertThat(contarEventosActlab()).as("cero ACTLAB").isZero();
        assertThat(contarSnapshots()).as("cero snapshot").isZero();
        assertThat(ultimoNroEmitido()).as("el correlativo del talonario no avanzo").isZero();
    }

    // =====================================================================
    // Comandos
    // =====================================================================

    private LabrarActaCommand comandoMinimo(String idTecnico) {
        return LabrarActaCommandTestFactory.builder(ar.gob.malvinas.faltas.core.domain.enums.TipoActa.TRANSITO)
                .idDependencia(idDependencia)
                .idInspector(null)
                .fechaActa(F0)
                .domicilioHecho(MARCA + "domicilio del hecho")
                .resultadoFirmaInfractor(ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE)
                .situacionIdentificacionInfractor(SituacionIdentificacionInfractor.AUSENTE)
                .idTecnico(idTecnico)
                .build();
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

    // =====================================================================
    // Casos: retry general (sin fotografías)
    // =====================================================================

    @Test
    @DisplayName("01. primer intento con deadlock real (forma Spring), segundo intento exitoso")
    void primerIntentoDeadlockSegundoExitoso() {
        fallosPendientes.add(deadlockSintetico());
        String idTecnico = UUID.randomUUID().toString();

        ComandoResultado resultado = retryService.labrarConReintento(comandoMinimo(idTecnico));

        assertThat(resultado.idActa()).isNotNull();
        assertThat(contarActas()).isEqualTo(1L);
        assertThat(contarEventosActlab()).isEqualTo(1L);
        assertThat(contarSnapshots()).isEqualTo(1L);
        assertThat(ultimoNroEmitido())
                .as("el intento fallido incremento y revirtio; solo el exitoso persiste").isEqualTo(1);
    }

    @Test
    @DisplayName("02. dos fallos transitorios (deadlock + lock wait timeout), tercer intento exitoso")
    void dosFallosTransitoriosTercerIntentoExitoso() {
        fallosPendientes.add(deadlockSintetico());
        fallosPendientes.add(lockWaitTimeoutSintetico());
        String idTecnico = UUID.randomUUID().toString();

        ComandoResultado resultado = retryService.labrarConReintento(comandoMinimo(idTecnico));

        assertThat(resultado.idActa()).isNotNull();
        assertThat(contarActas()).as("una sola acta a pesar de 2 intentos fallidos").isEqualTo(1L);
        assertThat(contarEventosActlab()).as("un solo evento ACTLAB, no 3").isEqualTo(1L);
        assertThat(contarSnapshots()).as("un solo snapshot, no 3").isEqualTo(1L);
        assertThat(ultimoNroEmitido())
                .as("un solo numero consumido: los 2 intentos fallidos revirtieron el suyo").isEqualTo(1);
    }

    @Test
    @DisplayName("03. agotamiento de 3 intentos transitorios propaga el error sin dejar residuos")
    void agotamientoDeTresIntentosPropagaError() {
        fallosPendientes.add(deadlockSintetico());
        fallosPendientes.add(deadlockSintetico());
        fallosPendientes.add(deadlockSintetico());
        String idTecnico = UUID.randomUUID().toString();

        assertThatThrownBy(() -> retryService.labrarConReintento(comandoMinimo(idTecnico)))
                .isInstanceOf(DeadlockLoserDataAccessException.class);

        exigirCeroMutaciones();
        assertThat(invocacionesNextNumero.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("04. error funcional (no transitorio) no se reintenta: un solo intento, cero mutaciones")
    void errorFuncionalNoSeReintenta() {
        LabrarActaCommand invalido = LabrarActaCommandTestFactory
                .builder(ar.gob.malvinas.faltas.core.domain.enums.TipoActa.TRANSITO)
                .idDependencia(idDependencia)
                .sinSatelites() // TRANSITO exige el bloque 'transito': rechazo antes de tocar el talonario.
                .idTecnico(UUID.randomUUID().toString())
                .build();

        assertThatThrownBy(() -> retryService.labrarConReintento(invalido))
                .isInstanceOf(PrecondicionVioladaException.class);

        assertThat(invocacionesNextNumero.get())
                .as("la validacion rechaza antes de llegar a la numeracion").isZero();
        exigirCeroMutaciones();
    }

    @Test
    @DisplayName("05. mismo id_tecnico en los 3 intentos posibles cuando el cliente no lo informa")
    void mismoIdTecnicoEnTodosLosIntentos() {
        fallosPendientes.add(deadlockSintetico());
        fallosPendientes.add(deadlockSintetico());

        LabrarActaCommand sinIdTecnico = LabrarActaCommandTestFactory
                .builder(ar.gob.malvinas.faltas.core.domain.enums.TipoActa.TRANSITO)
                .idDependencia(idDependencia)
                .idInspector(null)
                .fechaActa(F0)
                .domicilioHecho(MARCA + "domicilio del hecho")
                .resultadoFirmaInfractor(ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE)
                .situacionIdentificacionInfractor(SituacionIdentificacionInfractor.AUSENTE)
                .build();

        ComandoResultado resultado = retryService.labrarConReintento(sinIdTecnico);

        String idTecnicoPersistido = jdbc.sql("SELECT id_tecnico FROM fal_acta WHERE id = :id")
                .param("id", resultado.idActa()).query(String.class).single();
        assertThat(idTecnicoPersistido).isNotBlank();
        assertThat(contarActas()).isEqualTo(1L);
    }

    // =====================================================================
    // Frontera REQUIRES_NEW ante transacción ambiente (Wave 4 Parte 3E-B §3.4, §10)
    // =====================================================================
    //
    // Prueba definitiva: si cada intento realmente abre una transacción REQUIRES_NEW (que se
    // commitea de forma independiente), el acta creada por un intento exitoso debe SOBREVIVIR
    // a un rollback posterior de la transacción ambiente que envolvió la llamada al orquestador.
    // Si el diseño hubiera degradado a REQUIRED (heredando la transacción ambiente, el bug que
    // esta parte corrige), el rollback ambiente habría revertido también el acta, y el conteo
    // final sería 0, no 1.

    @Test
    @DisplayName("07. invocado desde una transaccion ambiente que luego rollbackea: el intento"
            + " exitoso sobrevive (REQUIRES_NEW real, no heredado)")
    void invocadoDesdeTransaccionAmbienteQueRollbackea_elIntentoSobrevive() {
        String idTecnico = UUID.randomUUID().toString();
        TransactionTemplate transaccionAmbiente = new TransactionTemplate(transactionManager);

        ComandoResultado resultado = transaccionAmbiente.execute(status -> {
            ComandoResultado r = retryService.labrarConReintento(comandoMinimo(idTecnico));
            status.setRollbackOnly();
            return r;
        });

        assertThat(resultado).isNotNull();
        assertThat(resultado.idActa()).isNotNull();
        assertThat(contarActas())
                .as("el acta persiste a pesar del rollback de la transaccion ambiente: se"
                        + " commiteo en su propia transaccion REQUIRES_NEW, no en la ambiente")
                .isEqualTo(1L);
        assertThat(contarEventosActlab()).isEqualTo(1L);
        assertThat(contarSnapshots()).isEqualTo(1L);
    }

    @Test
    @DisplayName("08. invocado desde una transaccion ambiente, con un fallo transitorio en el"
            + " primer intento: la transaccion ambiente no absorbe los intentos (cada uno sigue"
            + " siendo una transaccion nueva) y el resultado final sobrevive al rollback ambiente")
    void invocadoDesdeTransaccionAmbienteConReintento_laAmbienteNoAbsorbeLosIntentos() {
        fallosPendientes.add(deadlockSintetico());
        String idTecnico = UUID.randomUUID().toString();
        TransactionTemplate transaccionAmbiente = new TransactionTemplate(transactionManager);

        ComandoResultado resultado = transaccionAmbiente.execute(status -> {
            ComandoResultado r = retryService.labrarConReintento(comandoMinimo(idTecnico));
            status.setRollbackOnly();
            return r;
        });

        assertThat(resultado).isNotNull();
        assertThat(contarActas())
                .as("una sola acta (del intento ganador), sobrevive al rollback de la ambiente")
                .isEqualTo(1L);
        assertThat(contarEventosActlab()).isEqualTo(1L);
        assertThat(contarSnapshots()).isEqualTo(1L);
        assertThat(ultimoNroEmitido())
                .as("el intento fallido revirtio su numero (transaccion propia, no absorbida por"
                        + " la ambiente); solo el numero del intento ganador queda consumido")
                .isEqualTo(1);
        assertThat(invocacionesNextNumero.get()).isEqualTo(2);
    }

    // =====================================================================
    // Caso: retry con fotografías (Wave 4 Parte 3E §13)
    // =====================================================================

    @Test
    @DisplayName("06. intento fallido DESPUES de promover una fotografia: compensa filesystem/metadata"
            + " antes de que el segundo intento comience; resultado final: 1 foto, 1 numero, 1 evento, 1 snapshot")
    void retryConFotografiasCompensaAntesDelSiguienteIntento() throws IOException {
        fallosPendientes.add(deadlockSintetico());
        String idTecnico = UUID.randomUUID().toString();

        List<FotografiaEntradaAlta> fotos = List.of(
                new FotografiaEntradaAlta(jpeg(Color.RED), "foto-retry.jpg", "foto de prueba retry"));

        LabrarActaCommand conFoto = LabrarActaCommandTestFactory
                .builder(ar.gob.malvinas.faltas.core.domain.enums.TipoActa.TRANSITO)
                .idDependencia(idDependencia)
                .idInspector(null)
                .fechaActa(F0)
                .domicilioHecho(MARCA + "domicilio del hecho")
                .resultadoFirmaInfractor(ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE)
                .situacionIdentificacionInfractor(SituacionIdentificacionInfractor.AUSENTE)
                .idTecnico(idTecnico)
                .fotografias(fotos)
                .build();

        ComandoResultado resultado = retryService.labrarConReintento(conFoto);

        assertThat(contarActas()).as("una sola acta a pesar del fallo tras promover la foto").isEqualTo(1L);
        assertThat(contarEventosActlab()).isEqualTo(1L);
        assertThat(contarSnapshots()).isEqualTo(1L);
        assertThat(ultimoNroEmitido())
                .as("el numero consumido por el intento fallido se libero con el rollback").isEqualTo(1);
        assertThat(contarEvidenciasFoto(resultado.idActa()))
                .as("exactamente 1 metadata de fotografia, no 2 (una por intento)").isEqualTo(1L);

        String storageKey = jdbc.sql("SELECT storage_key FROM fal_acta_evidencia WHERE id_acta = :id AND tipo_evid = :tipo")
                .param("id", resultado.idActa())
                .param("tipo", ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa.FOTO.codigo())
                .query(String.class).single();
        assertThat(storageKey).isNotBlank();

        // Filesystem real: el archivo final del intento GANADOR existe (y es exactamente uno);
        // el del intento fallido (clave distinta, generada de nuevo en el segundo intento) fue
        // eliminado por la compensacion del rollback del primer intento y nunca queda como
        // temporal ni como final huerfano.
        java.nio.file.Path raiz = java.nio.file.Path.of(storageRootDir).toAbsolutePath().normalize();
        try (var finalesDeEstaActa = java.nio.file.Files.list(raiz.resolve("final"))) {
            long finalesConEsaClave = finalesDeEstaActa
                    .filter(p -> p.getFileName().toString().equals(storageKey)
                            || p.getFileName().toString().startsWith(storageKey))
                    .count();
            assertThat(finalesConEsaClave).as("exactamente 1 archivo final para la clave ganadora").isEqualTo(1L);
        }
    }

    // =====================================================================
    // Decorador de test: fault injector sobre TalonarioRepository (solo src/test)
    // =====================================================================

    /**
     * Envuelve el bean real de {@link TalonarioRepository} (adaptador JDBC productivo) con un
     * proxy dinámico que intercepta únicamente {@code nextNumeroTalonario}: cuando hay un fallo
     * pendiente en {@link #fallosPendientes}, <b>delega primero en el adaptador real</b> (el
     * {@code SELECT ... FOR UPDATE} + {@code UPDATE ultimo_nro_emitido} ocurre de verdad, dentro
     * de la transacción del llamador) y <b>después</b> descarta ese resultado y lanza el fallo
     * sintético — simulando que MariaDB reporta el error transitorio justo después de esa
     * escritura, con el commit todavía pendiente. Es la única forma honesta de ejercitar la
     * propiedad que el prompt exige verificar: que el rollback de Spring revierte también el
     * número tentativamente reclamado, y no sólo las filas de {@code fal_acta}. Si no hay fallo
     * pendiente, delega y devuelve el resultado real sin alterarlo.
     */
    @TestConfiguration
    static class FalloTalonarioTestConfig {

        @org.springframework.context.annotation.Bean
        static org.springframework.beans.factory.config.BeanPostProcessor talonarioRepositoryFalloInyectado() {
            return new org.springframework.beans.factory.config.BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (bean instanceof TalonarioRepository real) {
                        return Proxy.newProxyInstance(
                                TalonarioRepository.class.getClassLoader(),
                                new Class<?>[]{TalonarioRepository.class},
                                (proxy, method, args) -> {
                                    if (!"nextNumeroTalonario".equals(method.getName())) {
                                        try {
                                            return method.invoke(real, args);
                                        } catch (java.lang.reflect.InvocationTargetException e) {
                                            throw e.getCause();
                                        }
                                    }
                                    invocacionesNextNumero.incrementAndGet();
                                    RuntimeException fallo = fallosPendientes.poll();
                                    Object resultadoReal;
                                    try {
                                        resultadoReal = method.invoke(real, args);
                                    } catch (java.lang.reflect.InvocationTargetException e) {
                                        throw e.getCause();
                                    }
                                    if (fallo != null) {
                                        throw fallo;
                                    }
                                    return resultadoReal;
                                });
                    }
                    return bean;
                }
            };
        }
    }
}
