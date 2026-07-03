package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuracion del perfil JDBC.
 *
 * Activo con: --spring.profiles.active=jdbc
 *
 * Spring Boot auto-configura JdbcClient y JdbcTemplate cuando existe un DataSource.
 * Esta clase es el punto de extension para configuracion JDBC adicional en futuros slices.
 *
 * Variables de entorno requeridas:
 *   FALTAS_DB_URL      - URL de conexion MariaDB
 *   FALTAS_DB_USER     - Usuario
 *   FALTAS_DB_PASSWORD - Password
 *
 * En modo por defecto (sin perfil jdbc) los repositorios InMemory* siguen activos.
 * Los repositorios Jdbc* (a implementar en 9-2 en adelante) se activaran solo con este perfil.
 */
@Configuration
@Profile("jdbc")
public class JdbcConfig {

}
