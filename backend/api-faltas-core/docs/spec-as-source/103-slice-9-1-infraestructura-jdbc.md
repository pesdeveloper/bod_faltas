# 103 - Infraestructura JDBC base

> **Estado documental:** SUPPORTING_CURRENT
> **Autoridad DDL:** SUPPORTING
> Inventario actual y verificable de la infraestructura JDBC base ya incorporada
> (dependencias, perfiles, configuracion, prueba condicionada), previa a los
> repositorios JDBC de dominio. No contiene el DDL definitivo. Ver
> `101-auditoria-pre-jdbc-mariadb.md`, `102-slice-9-estrategia-jdbc-mariadb.md`,
> `109-delta-modelo-mariadb-inmemory.md` y `110-matriz-maestra-paridad-mariadb-inmemory.md`.

## 1. Alcance de este documento

Infraestructura minima JDBC incorporada, sin migrar todavia ningun repositorio de dominio:

- Dependencias Spring JDBC y driver MariaDB en `pom.xml`.
- Perfiles de configuracion (`default` in-memory, `jdbc` con MariaDB real).
- `JdbcClient` disponible como infraestructura (auto-configurado por Spring Boot cuando
  existe un `DataSource`).
- Prueba condicionada de infraestructura JDBC que no rompe el build normal.
- Sin repositorios JDBC de dominio.

## 2. Dependencias presentes

Verificado en `pom.xml`:

    spring-boot-starter-jdbc              - JdbcClient, JdbcTemplate, DataSource auto-config
    org.mariadb.jdbc:mariadb-java-client  - Driver MariaDB (scope runtime)

No presentes:
- `spring-boot-starter-data-jpa` (prohibido).
- Hibernate (prohibido).
- H2 / HSQLDB (no se usa embedded DB).
- Flyway / Liquibase (decision pendiente; ver `99-pendientes-siguientes-slices.md`).

## 3. Perfiles de persistencia

### Perfil `default` (sin DB requerida)

Archivo: `src/main/resources/application.yml`.

    spring.autoconfigure.exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration

    app.persistence.mode: memory

- `DataSourceAutoConfiguration` se excluye para no requerir base de datos real.
- Los repositorios `InMemory*` permanecen activos.
- La aplicacion arranca sin base de datos.

### Perfil `jdbc` (MariaDB real)

Archivo: `src/main/resources/application-jdbc.yml`.

    spring.autoconfigure.exclude: []   -- anula la exclusion del perfil default
    spring.datasource.url: ${FALTAS_DB_URL:jdbc:mariadb://localhost:3306/faltas_db}
    spring.datasource.username: ${FALTAS_DB_USER:faltas}
    spring.datasource.password: ${FALTAS_DB_PASSWORD:}
    spring.datasource.driver-class-name: org.mariadb.jdbc.Driver
    spring.datasource.hikari.connection-timeout: 20000
    spring.datasource.hikari.maximum-pool-size: 10
    spring.datasource.hikari.minimum-idle: 2
    spring.sql.init.mode: never

    app.persistence.mode: jdbc

- `DataSourceAutoConfiguration` activo; Spring Boot auto-configura `DataSource`,
  `JdbcTemplate` y `JdbcClient` (pool HikariCP).
- Credenciales por variables de entorno (no hardcodeadas).

## 4. Variables de entorno para perfil `jdbc`

    FALTAS_DB_URL      - URL de conexion MariaDB (ej: jdbc:mariadb://localhost:3306/faltas_db)
    FALTAS_DB_USER     - Usuario de la base de datos
    FALTAS_DB_PASSWORD - Password de la base de datos

Sin valores por defecto productivos hardcodeados. El valor por defecto de
`FALTAS_DB_URL` en `application-jdbc.yml` es solo de referencia para desarrollo local.

## 5. JdbcConfig real

Archivo: `src/main/java/ar/gob/malvinas/faltas/core/infrastructure/jdbc/JdbcConfig.java`.

    @Configuration
    @Profile("jdbc")
    public class JdbcConfig { }

Spring Boot auto-configura `JdbcClient` cuando existe un `DataSource`. Esta clase es el
punto de extension para configuracion JDBC adicional durante el bloque de DDL.

## 6. JdbcInfrastructureIT y su condicion real

Archivo: `src/test/java/ar/gob/malvinas/faltas/core/infrastructure/JdbcInfrastructureIT.java`.

Condicion de ejecucion: `@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")`.

Tests:
- `datasourceDisponible()`: `DataSource != null`.
- `jdbcClientDisponible()`: `JdbcClient != null`.
- `selectUnoEjecuta()`: `SELECT 1` devuelve `1`.

Sin `FALTAS_DB_URL` definida: el test se omite automaticamente; el build no falla.
Con `FALTAS_DB_URL` definida: el test se ejecuta contra MariaDB real (perfil `jdbc`).

## 7. Como ejecutar

### Build normal (sin DB)

    cd backend/api-faltas-core
    mvn test

`JdbcInfrastructureIT` se omite automaticamente.

### Con MariaDB disponible (PowerShell)

    $env:FALTAS_DB_URL = "jdbc:mariadb://localhost:3306/faltas_db"
    $env:FALTAS_DB_USER = "faltas"
    $env:FALTAS_DB_PASSWORD = "secret"
    mvn test

`JdbcInfrastructureIT` se ejecuta (3 tests adicionales).

### Arrancar la aplicacion con JDBC

    java -jar target/api-faltas-core-0.0.1-SNAPSHOT.jar `
      --spring.profiles.active=jdbc `
      --FALTAS_DB_URL=jdbc:mariadb://localhost:3306/faltas_db `
      --FALTAS_DB_USER=faltas `
      --FALTAS_DB_PASSWORD=secret

O con variables de entorno del sistema operativo.

## 8. Confirmaciones vigentes

- Sin JPA/Hibernate: cero referencias en codigo Java.
- Sin repositorios JDBC de dominio todavia.
- Sin tablas para enums cerrados. Sin seeds para enums cerrados.
- Repositorios `InMemory*` siguen activos y sin modificar.
- Sin migraciones versionadas (Flyway/Liquibase) todavia.

## 9. Trabajo posterior

Ver `99-pendientes-siguientes-slices.md` para el bloque obligatorio de diseño y DDL
MariaDB (resolucion de `DECISION_DDL-*`, DDL inicial versionado, adapters JDBC de
dominio y activacion via perfil `jdbc`). Ver `102-slice-9-estrategia-jdbc-mariadb.md`
para la estrategia de acceso a datos que ese bloque debe seguir.
