package ar.gob.malvinas.faltas.core.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IT de conectividad JDBC contra la base MariaDB autorizada.
 *
 * Solo se ejecuta cuando FALTAS_DB_URL esta definida; en el build normal se
 * omite y no rompe nada. Los *IT no los corre Surefire: usar el perfil Maven
 * de integracion.
 *
 *   $env:FALTAS_DB_URL      = "jdbc:mariadb://<host>:3306/sb_faltas_db"
 *   $env:FALTAS_DB_USER     = "<usuario>"
 *   $env:FALTAS_DB_PASSWORD = "<password>"
 *   mvn -P integration-tests verify
 *
 * Solo lectura: este IT no ejecuta DDL ni DML.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@DisplayName("IT WAVE-0A: conectividad JDBC con MariaDB")
class JdbcInfrastructureIT {

    /** Unica base autorizada para el perfil jdbc en sandbox (guardrail anti-produccion). */
    private static final String BASE_AUTORIZADA = "sb_faltas_db";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    @DisplayName("1. DataSource disponible con perfil jdbc")
    void datasourceDisponible() {
        assertThat(dataSource).isNotNull();
    }

    @Test
    @DisplayName("2. JdbcClient disponible con perfil jdbc")
    void jdbcClientDisponible() {
        assertThat(jdbcClient).isNotNull();
    }

    @Test
    @DisplayName("3. SELECT 1 ejecuta correctamente")
    void selectUnoEjecuta() {
        Integer result = jdbcClient.sql("SELECT 1").query(Integer.class).single();
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("4. DATABASE() es exactamente la base autorizada sb_faltas_db")
    void baseEfectivaEsLaAutorizada() {
        String base = jdbcClient.sql("SELECT DATABASE()").query(String.class).single();
        assertThat(base).isEqualTo(BASE_AUTORIZADA);
    }

    @Test
    @DisplayName("5. El servidor es MariaDB y la conexion entrega metadata coherente")
    void servidorEsMariaDb() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            assertThat(conn.getMetaData().getDatabaseProductName()).containsIgnoringCase("mariadb");
            assertThat(conn.getCatalog()).isEqualTo(BASE_AUTORIZADA);
            assertThat(conn.isReadOnly()).isFalse();
        }
    }

    @Test
    @DisplayName("6. El esquema autorizado ya tiene tablas instaladas (baseline DDL manual)")
    void esquemaTieneTablasInstaladas() {
        Integer tablas = jdbcClient
                .sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = :schema")
                .param("schema", BASE_AUTORIZADA)
                .query(Integer.class)
                .single();
        assertThat(tablas).isGreaterThan(0);
    }
}
