package ar.gob.malvinas.faltas.core.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de infraestructura JDBC.
 *
 * Solo se ejecuta cuando la variable de entorno FALTAS_DB_URL esta definida.
 * En el build normal (sin DB) este test se omite automaticamente.
 *
 * Para ejecutar manualmente (PowerShell):
 *   $env:FALTAS_DB_URL = "jdbc:mariadb://localhost:3306/faltas_db"
 *   $env:FALTAS_DB_USER = "faltas"
 *   $env:FALTAS_DB_PASSWORD = "secret"
 *   mvn test -Dspring.profiles.active=jdbc
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@DisplayName("IT: infraestructura JDBC con MariaDB")
class JdbcInfrastructureIT {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    @DisplayName("DataSource disponible con perfil jdbc")
    void datasourceDisponible() {
        assertThat(dataSource).isNotNull();
    }

    @Test
    @DisplayName("JdbcClient disponible con perfil jdbc")
    void jdbcClientDisponible() {
        assertThat(jdbcClient).isNotNull();
    }

    @Test
    @DisplayName("SELECT 1 ejecuta correctamente")
    void selectUnoEjecuta() {
        Integer result = jdbcClient.sql("SELECT 1").query(Integer.class).single();
        assertThat(result).isEqualTo(1);
    }
}
