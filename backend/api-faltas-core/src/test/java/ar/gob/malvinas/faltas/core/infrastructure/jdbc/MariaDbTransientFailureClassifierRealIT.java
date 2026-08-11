package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IT real de {@link MariaDbTransientFailureClassifier} (Wave 4 Parte 3E §10, §18).
 *
 * <p>Certifica el clasificador contra dos errores <b>genuinos</b> de MariaDB, provocados con
 * conexiones JDBC reales sobre {@code sb_faltas_db} (no simulados): un deadlock real (1213,
 * dos transacciones que bloquean dos filas en orden inverso) y un lock wait timeout real (1205,
 * una transacción que espera una fila bloqueada por otra hasta agotar
 * {@code innodb_lock_wait_timeout} de sesión). {@code AltaActaRetryTransactionIT} certifica la
 * orquestación de retry con fallos de la misma <b>forma</b> pero inyectados en un punto
 * controlado; este IT certifica que esa forma corresponde exactamente a lo que MariaDB reporta
 * de verdad.
 *
 * <p>Usa dos filas de {@code fal_dependencia} exclusivas de este IT (marca en {@code nom_dep})
 * como recurso a bloquear: es una tabla ya presente, sin datos ajenos en riesgo, y sin FK
 * entrantes que compliquen el borrado en {@code @AfterEach}.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@DisplayName("IT Wave 4 Parte 3E: MariaDbTransientFailureClassifier contra errores reales de MariaDB")
class MariaDbTransientFailureClassifierRealIT {

    private static final String MARCA = "W4P3E_CLASSIFIER_REAL_";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcClient jdbc;

    private final MariaDbTransientFailureClassifier clasificador = new MariaDbTransientFailureClassifier();

    @AfterEach
    void limpiar() {
        jdbc.sql("DELETE FROM fal_dependencia WHERE nom_dep LIKE :marca")
                .param("marca", MARCA + "%")
                .update();
    }

    private long insertarFilaDependencia(String sufijo) {
        jdbc.sql("""
                INSERT INTO fal_dependencia (nom_dep, fh_alta, id_user_alta)
                VALUES (:nom, :fhAlta, :usuario)
                """)
                .param("nom", MARCA + sufijo)
                .param("fhAlta", LocalDateTime.now())
                .param("usuario", "IT_CLASSIFIER_REAL")
                .update();
        return jdbc.sql("SELECT MAX(id) FROM fal_dependencia WHERE nom_dep = :nom")
                .param("nom", MARCA + sufijo)
                .query(Long.class)
                .single();
    }

    // =====================================================================
    // 1213 — deadlock real (dos conexiones, dos filas, orden inverso)
    // =====================================================================

    @Test
    @DisplayName("01. deadlock real (1213): el clasificador reconoce DEADLOCK desde el SQLException real")
    void deadlockRealSeClasificaComoDeadlock() throws Exception {
        long filaA = insertarFilaDependencia("DEADLOCK_A");
        long filaB = insertarFilaDependencia("DEADLOCK_B");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch primerLockTomado = new CountDownLatch(2);
        try (Connection connA = dataSource.getConnection();
             Connection connB = dataSource.getConnection()) {
            connA.setAutoCommit(false);
            connB.setAutoCommit(false);

            Future<SQLException> futA = executor.submit(
                    () -> intentarSecuenciaDeLocks(connA, filaA, filaB, primerLockTomado));
            Future<SQLException> futB = executor.submit(
                    () -> intentarSecuenciaDeLocks(connB, filaB, filaA, primerLockTomado));

            SQLException errorA = futA.get(20, TimeUnit.SECONDS);
            SQLException errorB = futB.get(20, TimeUnit.SECONDS);

            rollbackSilencioso(connA);
            rollbackSilencioso(connB);

            // Exactamente una de las dos transacciones debe haber sido elegida victima del
            // deadlock por MariaDB; la otra completa sin error (obtuvo ambas filas).
            List<SQLException> errores = java.util.stream.Stream.of(errorA, errorB)
                    .filter(java.util.Objects::nonNull).toList();
            assertThat(errores).as("exactamente una transaccion victima del deadlock").hasSize(1);

            SQLException deadlockReal = errores.get(0);
            assertThat(deadlockReal.getErrorCode()).as("codigo nativo MariaDB del deadlock real").isEqualTo(1213);

            assertThat(clasificador.clasificar(deadlockReal))
                    .isEqualTo(CategoriaFalloTransitorioMariaDb.DEADLOCK);

            // Envuelto como lo haria Spring (SQLErrorCodeSQLExceptionTranslator real de JdbcTemplate/
            // JdbcClient), la clasificacion debe seguir siendo la misma: recorre la cadena de causas.
            Exception envueltoComoSpring = new org.springframework.dao.DeadlockLoserDataAccessException(
                    "PreparedStatementCallback", deadlockReal);
            assertThat(clasificador.clasificar(envueltoComoSpring))
                    .isEqualTo(CategoriaFalloTransitorioMariaDb.DEADLOCK);
        } finally {
            executor.shutdownNow();
        }
    }

    /** Toma el lock de {@code primeraFila}, espera a que la otra transaccion tambien tenga el suyo, e intenta {@code segundaFila}. */
    private SQLException intentarSecuenciaDeLocks(
            Connection conn, long primeraFila, long segundaFila, CountDownLatch primerLockTomado)
            throws InterruptedException {
        try {
            lockForUpdate(conn, primeraFila);
            primerLockTomado.countDown();
            primerLockTomado.await(15, TimeUnit.SECONDS);
            lockForUpdate(conn, segundaFila);
            return null;
        } catch (SQLException deadlockOAlgunOtroError) {
            return deadlockOAlgunOtroError;
        }
    }

    private static void lockForUpdate(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM fal_dependencia WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
            }
        }
    }

    private static void rollbackSilencioso(Connection conn) {
        try {
            if (!conn.isClosed()) {
                conn.rollback();
            }
        } catch (SQLException ignorado) {
            // best-effort de limpieza de test
        }
    }

    // =====================================================================
    // 1205 — lock wait timeout real (una conexion mantiene el lock, la otra agota su espera)
    // =====================================================================

    @Test
    @DisplayName("02. lock wait timeout real (1205): el clasificador reconoce LOCK_WAIT_TIMEOUT desde el SQLException real")
    void lockWaitTimeoutRealSeClasificaComoLockWaitTimeout() throws Exception {
        long fila = insertarFilaDependencia("LOCKWAIT");

        try (Connection tenedora = dataSource.getConnection();
             Connection esperando = dataSource.getConnection()) {
            tenedora.setAutoCommit(false);
            esperando.setAutoCommit(false);

            lockForUpdate(tenedora, fila);

            try (Statement st = esperando.createStatement()) {
                st.execute("SET innodb_lock_wait_timeout = 1");
            }

            SQLException timeoutReal = org.junit.jupiter.api.Assertions.assertThrows(
                    SQLException.class, () -> lockForUpdate(esperando, fila),
                    "la segunda conexion debe agotar su espera de lock real");

            rollbackSilencioso(tenedora);
            rollbackSilencioso(esperando);

            assertThat(timeoutReal.getErrorCode())
                    .as("codigo nativo MariaDB del lock wait timeout real").isEqualTo(1205);
            assertThat(clasificador.clasificar(timeoutReal))
                    .isEqualTo(CategoriaFalloTransitorioMariaDb.LOCK_WAIT_TIMEOUT);

            Exception envueltoComoSpring = new org.springframework.dao.CannotAcquireLockException(
                    "PreparedStatementCallback", timeoutReal);
            assertThat(clasificador.clasificar(envueltoComoSpring))
                    .isEqualTo(CategoriaFalloTransitorioMariaDb.LOCK_WAIT_TIMEOUT);
        }
    }

    // =====================================================================
    // Errores reales NO transitorios: no deben clasificarse como reintentables
    // =====================================================================

    @Test
    @DisplayName("03. duplicate key real (1062) NO se clasifica como transitorio")
    void duplicateKeyRealNoEsTransitorio() throws Exception {
        long fila = insertarFilaDependencia("DUPLICADO");
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(true);
            SQLException duplicado = org.junit.jupiter.api.Assertions.assertThrows(SQLException.class, () -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO fal_dependencia (id, nom_dep, fh_alta, id_user_alta) "
                                + "VALUES (?, ?, NOW(), 'IT_CLASSIFIER_REAL')")) {
                    ps.setLong(1, fila);
                    ps.setString(2, MARCA + "DUPLICADO_2");
                    ps.executeUpdate();
                }
            });
            assertThat(duplicado.getErrorCode()).isEqualTo(1062);
            assertThat(clasificador.clasificar(duplicado)).isNull();
        }
    }

    @Test
    @DisplayName("04. error de sintaxis SQL real (1064) NO se clasifica como transitorio")
    void errorDeSintaxisRealNoEsTransitorio() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            SQLException sintaxis = org.junit.jupiter.api.Assertions.assertThrows(SQLException.class, () -> {
                try (Statement st = conn.createStatement()) {
                    st.execute("SELECCIONAR * DESDE fal_dependencia");
                }
            });
            assertThat(clasificador.clasificar(sintaxis)).isNull();
        }
    }
}
