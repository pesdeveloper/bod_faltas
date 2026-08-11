package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuracion del perfil JDBC de dominio Faltas.
 *
 * Activo con: --spring.profiles.active=jdbc
 *
 * Esta clase NO declara beans a proposito. Spring Boot 3.5.3 ya auto-configura,
 * a partir del DataSource definido en application-jdbc.yml, todos los beans que
 * la fundacion JDBC necesita (verificado por JdbcProfileContextIT):
 *
 *   DataSource                     -> DataSourceAutoConfiguration (HikariCP, pool faltas-pool)
 *   JdbcTemplate                   -> JdbcTemplateAutoConfiguration
 *   NamedParameterJdbcTemplate     -> JdbcTemplateAutoConfiguration
 *   JdbcClient                     -> JdbcClientAutoConfiguration
 *   PlatformTransactionManager     -> JdbcTransactionManagerAutoConfiguration
 *
 * Declarar cualquiera de esos beans aqui seria duplicacion sin valor. La clase
 * se mantiene como punto de extension unico del perfil para lo que Boot no
 * auto-configura (por ejemplo, beans de soporte de los adaptadores Jdbc* de
 * dominio que se introduciran en WAVE 0B).
 *
 * Variables de entorno obligatorias del perfil:
 *   FALTAS_DB_URL, FALTAS_DB_USER, FALTAS_DB_PASSWORD
 *
 * Estado en WAVE 0A: ningun puerto de repositorio de dominio esta migrado. Los
 * adaptadores InMemory* siguen activos incluso con el perfil jdbc habilitado
 * (GAP-004); la seleccion de exactamente un adaptador por puerto se resuelve
 * materialmente en WAVE 0B junto con el repositorio piloto.
 */
@Configuration
@Profile("jdbc")
public class JdbcConfig {

}
