# Infraestructura JDBC base

> **Estado documental:** SUPPORTING_CURRENT
> **Autoridad DDL:** SUPPORTING
> Inventario actual y verificable de la infraestructura JDBC base ya incorporada
> (dependencias, perfiles, configuracion, prueba condicionada) y del primer
> repositorio JDBC de dominio incorporado sobre ella (WAVE 0B, repositorio
> piloto). No contiene el DDL definitivo. Ver
> [`../00-governance/ready-for-ddl-gate.md`](../00-governance/ready-for-ddl-gate.md),
> [`jdbc-strategy.md`](jdbc-strategy.md), [`inmemory-mariadb-deltas.md`](inmemory-mariadb-deltas.md)
> y [`mariadb-logical-model.md`](mariadb-logical-model.md).

## 1. Alcance de este documento

Infraestructura minima JDBC incorporada, mas el primer repositorio JDBC de dominio
montado sobre ella:

- Dependencias Spring JDBC y driver MariaDB en `pom.xml`.
- Perfiles de configuracion (`default` in-memory, `jdbc` con MariaDB real).
- `JdbcClient` disponible como infraestructura (auto-configurado por Spring Boot cuando
  existe un `DataSource`).
- Prueba condicionada de infraestructura JDBC que no rompe el build normal.
- Un unico repositorio JDBC de dominio: `JdbcActaEventoRepository` sobre
  `fal_acta_evento` (WAVE 0B, repositorio piloto). Ver secciones 6 y 8.

## 2. Dependencias presentes

Verificado en `pom.xml`:

    spring-boot-starter-jdbc              - JdbcClient, JdbcTemplate, DataSource auto-config
    org.mariadb.jdbc:mariadb-java-client  - Driver MariaDB (scope runtime)

No presentes:
- `spring-boot-starter-data-jpa` (prohibido).
- Hibernate (prohibido).
- H2 / HSQLDB (no se usa embedded DB).
- Flyway / Liquibase (NO — ejecucion DDL exclusivamente manual; ver `../50-persistence/ddl-execution-and-test-seeding.md`).

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
    spring.datasource.url: ${FALTAS_DB_URL}
    spring.datasource.username: ${FALTAS_DB_USER}
    spring.datasource.password: ${FALTAS_DB_PASSWORD}
    spring.datasource.driver-class-name: org.mariadb.jdbc.Driver
    spring.datasource.hikari.connection-timeout: 20000
    spring.datasource.hikari.maximum-pool-size: 10
    spring.datasource.hikari.minimum-idle: 2
    spring.datasource.hikari.pool-name: faltas-pool
    spring.sql.init.mode: never

    app.persistence.mode: jdbc

- `DataSourceAutoConfiguration` activo; Spring Boot auto-configura `DataSource`,
  `JdbcTemplate`, `NamedParameterJdbcTemplate`, `JdbcClient` y
  `JdbcTransactionManager` (pool HikariCP `faltas-pool`).
- Credenciales por variables de entorno (no hardcodeadas).
- Las tres variables son obligatorias y no tienen valor por defecto: si falta
  alguna, el contexto no arranca. Es deliberado, para que el perfil `jdbc` no
  pueda conectarse silenciosamente a una base no autorizada.
- El pool principal de Faltas es independiente de los pools Geo
  (`geo-pool`, `geo-catalog-pool`), que siguen apagados salvo que se habiliten
  con sus propios flags.

## 4. Variables de entorno para perfil `jdbc`

    FALTAS_DB_URL      - URL de conexion MariaDB de la base autorizada
    FALTAS_DB_USER     - Usuario de la base de datos
    FALTAS_DB_PASSWORD - Password de la base de datos

Las tres son obligatorias. `application-jdbc.yml` no define valor por defecto
para ninguna: no hay URL, usuario ni password hardcodeados, ni de produccion ni
de desarrollo. Si falta una variable, Spring falla al arrancar con
"Could not resolve placeholder" en lugar de conectarse a una base equivocada.

## 5. JdbcConfig real

Archivo: `src/main/java/ar/gob/malvinas/faltas/core/infrastructure/jdbc/JdbcConfig.java`.

    @Configuration
    @Profile("jdbc")
    public class JdbcConfig { }

Spring Boot auto-configura `JdbcClient` cuando existe un `DataSource`. Esta clase es el
punto de extension para configuracion JDBC adicional durante el bloque de DDL.

## 6. Tests de infraestructura JDBC y su condicion real

Archivos:
- `src/test/java/ar/gob/malvinas/faltas/core/infrastructure/JdbcInfrastructureIT.java`
- `src/test/java/ar/gob/malvinas/faltas/core/infrastructure/JdbcProfileContextIT.java`
- `src/test/java/ar/gob/malvinas/faltas/core/infrastructure/JdbcActaEventoRepositoryIT.java`

Los tres con `@ActiveProfiles("jdbc")` y condicion de ejecucion
`@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")`.

`JdbcInfrastructureIT` (conectividad, 6 tests):
- `DataSource` y `JdbcClient` disponibles.
- `SELECT 1` devuelve `1`.
- `DATABASE()` es exactamente la base autorizada.
- Metadata de conexion coherente con MariaDB.
- El esquema autorizado ya tiene tablas instaladas (baseline DDL manual).

`JdbcProfileContextIT` (topologia de beans, 14 tests):
- Perfil `jdbc` activo y `app.persistence.mode=jdbc`.
- Exactamente un `DataSource`, el principal de Faltas.
- Ningun datasource Geo activo.
- `JdbcClient`, `JdbcTemplate` y `NamedParameterJdbcTemplate` auto-configurados.
- `PlatformTransactionManager` presente y operativo (transaccion de solo lectura).
- `spring.sql.init.mode=never` efectivo, sin inicializador de esquema.
- Sin Flyway, Liquibase ni JPA.
- Seleccion de adaptador por perfil para el puerto piloto `ActaEventoRepository`.

De esos 14, doce corren con perfil `jdbc` y dos en la clase anidada
`SinPerfilJdbc`, que verifica la topologia sin ese perfil. La clase anidada usa
`@NestedTestConfiguration(OVERRIDE)` porque una misma clase no puede levantar dos
contextos con perfiles distintos.

`JdbcActaEventoRepositoryIT` (repositorio piloto de dominio, 17 tests):
- Round-trip completo de `FalActaEvento` contra `fal_acta_evento`.
- Mapeo de enums por `codigo()`, orden del timeline, aislamiento por acta y
  semantica append-only.
- Conflicto por FK invalida y prueba de rollback real.

Los dos IT de WAVE 0A no ejecutan DDL ni DML: son de solo lectura.
`JdbcActaEventoRepositoryIT` si ejecuta DML, siempre dentro de una transaccion
que se revierte: no deja residuos y no ejecuta DDL, TRUNCATE ni DELETE.

Sin `FALTAS_DB_URL` definida se omiten automaticamente y el build no falla.

## 7. Como ejecutar

### Build normal (sin DB)

    cd backend/api-faltas-core
    mvn test

Surefire ejecuta las clases `*Test.java`. **No ejecuta las clases `*IT.java`**:
sus includes por defecto (`Test*.java`, `*Test.java`, `*Tests.java`,
`*TestCase.java`) no las alcanzan. Por lo tanto `mvn test` en verde no acredita
por si solo la fase de integracion.

### Fase de integracion (perfil Maven `integration-tests`)

El POM define el perfil `integration-tests`, que declara `maven-failsafe-plugin`
(version y ejecuciones `integration-test`/`verify` heredadas de
`spring-boot-starter-parent`). Es la unica via explicita para ejecutar los `*IT`:

    cd backend/api-faltas-core
    mvn -P integration-tests verify

### Con MariaDB disponible (PowerShell)

    $env:FALTAS_DB_URL = "jdbc:mariadb://<host>:<puerto>/<base-autorizada>"
    $env:FALTAS_DB_USER = "<usuario>"
    $env:FALTAS_DB_PASSWORD = "<password>"
    mvn -P integration-tests verify

`JdbcInfrastructureIT`, `JdbcProfileContextIT` y `JdbcActaEventoRepositoryIT` se
ejecutan contra MariaDB real. Sin esas variables se omiten, y el resto de los
`*IT` corre igual.

### Arrancar la aplicacion con JDBC

    java -jar target/api-faltas-core-0.0.1-SNAPSHOT.jar `
      --spring.profiles.active=jdbc

Con `FALTAS_DB_URL`, `FALTAS_DB_USER` y `FALTAS_DB_PASSWORD` provistas como
variables de entorno del sistema operativo. No pasar credenciales por linea de
comandos.

## 8. Confirmaciones vigentes

- Sin JPA/Hibernate: cero referencias en codigo Java.
- 9 repositorios JDBC de dominio activos bajo el perfil `jdbc`: `JdbcActaRepository`,
  `JdbcActaEventoRepository`, `JdbcActaSnapshotRepository`,
  `JdbcActaEvidenciaRepository`, `JdbcPersonaRepository`, `JdbcObservacionRepository`,
  `JdbcQrAccesoRepository`, `JdbcDiaNoComputableRepository`,
  `JdbcMotivoArchivoRepository`. El resto de los puertos sigue resuelto por su
  adaptador `InMemory*` (`DECISION-PERSISTENCE-INMEMORY-RETIREMENT-01`); esa mezcla es
  transitoria mientras se migra cada agregado, no una segunda plataforma productiva
  permanente.
- Sin tablas para enums cerrados. Sin seeds para enums cerrados.
- Los repositorios `InMemory*` de los 9 puertos ya migrados no se registran con el
  perfil `jdbc` (`@Profile("!jdbc")`); sus pares `Jdbc*` sí (`@Profile("jdbc")`),
  mutuamente excluyentes en runtime.
- Sin Flyway ni Liquibase (DECISION_DDL-EXEC-01 CERRADA: ejecucion DDL exclusivamente manual).
- Sin inicializacion SQL automatica: `spring.sql.init.mode=never`.
- Spring no crea, altera ni migra el esquema automaticamente.
- Script DDL canonico aprobado para ejecución manual controlada: `database/ddl/create-bod-faltas-domain.sql`
  (64 tablas TO_CREATE + adopcion de `fal_rubro_version`; trabajo DDL-MARIADB-MANUAL-001-FULL-R1;
  auditado externamente).
- El esquema canonico ya fue instalado y validado materialmente en el sandbox
  autorizado `sb_faltas_db` por el slice `MARIADB-DDL-EXEC-001`, con veredicto
  `DDL_VALIDATED_ON_MARIADB_SANDBOX`: 65/65 tablas canonicas, 20 objetos
  protegidos preservados y parity sin divergencias. Es un sandbox validado, no
  una instalacion productiva.
- La instalacion no cambia la politica: la ejecucion del DDL sigue siendo manual
  y controlada, fuera del ciclo de vida de la aplicacion.

## 9. Trabajo posterior

Ver [`../90-roadmap/current-roadmap.md`](../90-roadmap/current-roadmap.md) para la
secuencia de trabajo posterior al cierre del slice vigente (script DDL manual,
seeder, adapters JDBC de dominio y activacion via perfil `jdbc`). Ver [`jdbc-strategy.md`](jdbc-strategy.md)
para la estrategia de acceso a datos que ese bloque debe seguir.
