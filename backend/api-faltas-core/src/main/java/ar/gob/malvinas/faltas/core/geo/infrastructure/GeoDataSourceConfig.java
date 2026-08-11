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
 * Datasource secundario para datos GIS/GeoDomicilios.
 * Apunta a PostgreSQL/PostGIS con tablas geo_gmat_*.
 *
 * Activo cuando geo.datasource.url esta configurado.
 * Independiente del datasource principal de Faltas (MariaDB).
 *
 * Variables de entorno:
 *   GEO_POSTGRES_URL      - URL JDBC (default: construida con PGHOST/PGPORT/PGDATABASE)
 *   GEO_POSTGRES_USER     - Usuario (default: PGUSER)
 *   GEO_POSTGRES_PASSWORD - Password
 *
 * GEO-MODEL-002 / E4.
 */
@Configuration
@ConditionalOnProperty(name = "geo.datasource.enabled", havingValue = "true")
public class GeoDataSourceConfig {

    @Bean(name = "geoDataSource", destroyMethod = "close")
    public DataSource geoDataSource(
            @Value("${geo.datasource.url}") String url,
            @Value("${geo.datasource.username}") String username,
            @Value("${geo.datasource.password:}") String password,
            @Value("${geo.datasource.hikari.pool-name:geo-pool}") String poolName,
            @Value("${geo.datasource.hikari.connection-timeout:10000}") long connectionTimeout,
            @Value("${geo.datasource.hikari.maximum-pool-size:5}") int maxPoolSize,
            @Value("${geo.datasource.hikari.minimum-idle:1}") int minIdle) {
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

    @Bean(name = "geoJdbcClient")
    public JdbcClient geoJdbcClient(@Qualifier("geoDataSource") DataSource geoDataSource) {
        return JdbcClient.create(geoDataSource);
    }
}
