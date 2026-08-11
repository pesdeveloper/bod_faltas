package ar.gob.malvinas.faltas.core.geo.infrastructure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;

/**
 * Datasource secundario para catálogos IGN/INDEC/BAHRA en MariaDB.
 * Apunta a la base sb_faltas_db (la misma que Faltas operativo, o una réplica).
 *
 * Activo cuando geo.catalog.mariadb.enabled=true.
 * Independiente del datasource PostGIS (geo.datasource.*).
 *
 * No requiere ningún servicio externo. Todos los catálogos son locales:
 *   geo_ign_provincia, geo_ign_municipio, geo_ign_departamento,
 *   geo_indec_localidad_censal, geo_indec_calles,
 *   vw_geo_municipio_departamento
 *
 * Variables de entorno:
 *   GEO_CATALOG_MARIADB_URL      - URL JDBC MariaDB (default construida desde MARIADB_HOST/PORT/DATABASE)
 *   GEO_CATALOG_MARIADB_USER     - Usuario
 *   GEO_CATALOG_MARIADB_PASSWORD - Password
 *
 * GEO-MODEL-002 / E4-CORRECCION (sin GeoRef).
 */
@Configuration
@ConditionalOnProperty(name = "geo.catalog.mariadb.enabled", havingValue = "true")
public class GeoCatalogMariaDbDataSourceConfig {

    @Bean(name = "geoCatalogMariaDbDataSource", destroyMethod = "close")
    public DataSource geoCatalogMariaDbDataSource(
            @Value("${geo.catalog.mariadb.url}") String url,
            @Value("${geo.catalog.mariadb.username}") String username,
            @Value("${geo.catalog.mariadb.password:}") String password,
            @Value("${geo.catalog.mariadb.hikari.pool-name:geo-catalog-pool}") String poolName,
            @Value("${geo.catalog.mariadb.hikari.connection-timeout:10000}") long connectionTimeout,
            @Value("${geo.catalog.mariadb.hikari.maximum-pool-size:3}") int maxPoolSize,
            @Value("${geo.catalog.mariadb.hikari.minimum-idle:1}") int minIdle) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(username);
        cfg.setPassword(password);
        cfg.setPoolName(poolName);
        cfg.setConnectionTimeout(connectionTimeout);
        cfg.setMaximumPoolSize(maxPoolSize);
        cfg.setMinimumIdle(minIdle);
        cfg.setConnectionTestQuery("SELECT 1");
        return new HikariDataSource(cfg);
    }

    @Bean(name = "geoCatalogJdbcClient")
    public JdbcClient geoCatalogJdbcClient(
            @Qualifier("geoCatalogMariaDbDataSource") DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }
}
