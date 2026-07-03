# 103 - Slice 9-1: Infraestructura JDBC base

**Slice:** 9-1
**Fecha:** 2026-07-02
**Tipo:** Infraestructura Java / configuracion Spring.
**Build cerrado:** 908/908 tests passing. BUILD SUCCESS.

---

## 1. Objetivo

Implementar la infraestructura minima JDBC sin migrar todavia ningun repositorio de dominio.

Este slice:
- Agrego dependencias Spring JDBC y driver MariaDB en pom.xml.
- Creo configuracion de perfiles (default in-memory, jdbc con MariaDB real).
- Configuro JdbcClient como infraestructura disponible (auto-configurado por Spring Boot).
- Creo test condicionado de infraestructura JDBC que no rompe el build normal.
- Mantuvo todos los tests existentes verdes.
- No migro ningun repositorio.

---

## 2. Dependencias agregadas

En pom.xml (Spring Boot 3.5.3 gestiona versiones):

    spring-boot-starter-jdbc  - JdbcClient, JdbcTemplate, DataSource auto-config
    org.mariadb.jdbc:mariadb-java-client (scope runtime) - Driver MariaDB

No se agrego:
- spring-boot-starter-data-jpa (prohibido)
- Hibernate (prohibido)
- H2 / HSQLDB (no se usa embedded DB)
- Flyway / Liquibase (decision pendiente para slices posteriores)

---

## 3. Perfiles de persistencia

### Perfil default (sin DB requerida)

Archivo: src/main/resources/application.yml

    spring.autoconfigure.exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration

    app.persistence.mode: memory

- DataSourceAutoConfiguration se excluye para no requerir base de datos real.
- Los repositorios InMemory* permanecen activos.
- Todos los tests existentes siguen funcionando.
- La aplicacion arranca sin base de datos.

### Perfil jdbc (MariaDB real)

Archivo: src/main/resources/application-jdbc.yml

    spring.autoconfigure.exclude: []   -- anula la exclusion del perfil default
    spring.datasource.url: ${FALTAS_DB_URL:jdbc:mariadb://localhost:3306/faltas_db}
    spring.datasource.username: ${FALTAS_DB_USER:faltas}
    spring.datasource.password: ${FALTAS_DB_PASSWORD:}
    spring.datasource.driver-class-name: org.mariadb.jdbc.Driver
    spring.sql.init.mode: never

    app.persistence.mode: jdbc

- DataSourceAutoConfiguration activo.
- Spring Boot auto-configura DataSource, JdbcTemplate y JdbcClient.
- Credenciales por variables de entorno (no hardcodeadas).

---

## 4. Variables de entorno requeridas para perfil jdbc

    FALTAS_DB_URL      - URL de conexion MariaDB
                         Ejemplo: jdbc:mariadb://localhost:3306/faltas_db
    FALTAS_DB_USER     - Usuario de la base de datos
    FALTAS_DB_PASSWORD - Password de la base de datos

No hay valores por defecto productivos hardcodeados.
El valor por defecto de FALTAS_DB_URL en application-jdbc.yml es solo de referencia para dev local.

---

## 5. JdbcConfig.java

Clase marcador del perfil jdbc:

    src/main/java/ar/gob/malvinas/faltas/core/infrastructure/jdbc/JdbcConfig.java

    @Configuration
    @Profile("jdbc")
    public class JdbcConfig { }

Spring Boot auto-configura JdbcClient cuando existe un DataSource.
Esta clase es el punto de extension para configuracion JDBC adicional en futuros slices.

---

## 6. Cómo ejecutar

### Build normal (sin DB)

    cd backend/api-faltas-core
    mvn test

Resultado: 908/908 tests passing. JdbcInfrastructureIT omitido automaticamente.

### Con MariaDB disponible (PowerShell)

    $env:FALTAS_DB_URL = "jdbc:mariadb://localhost:3306/faltas_db"
    $env:FALTAS_DB_USER = "faltas"
    $env:FALTAS_DB_PASSWORD = "secret"
    mvn test

Resultado: JdbcInfrastructureIT se ejecuta (3 tests adicionales).

### Arrancar la aplicacion con JDBC

    java -jar target/api-faltas-core-0.0.1-SNAPSHOT.jar `
      --spring.profiles.active=jdbc `
      --FALTAS_DB_URL=jdbc:mariadb://localhost:3306/faltas_db `
      --FALTAS_DB_USER=faltas `
      --FALTAS_DB_PASSWORD=secret

O con variables de entorno del sistema operativo.

---

## 7. Test JdbcInfrastructureIT

Archivo: src/test/java/ar/gob/malvinas/faltas/core/infrastructure/JdbcInfrastructureIT.java

Condicion de ejecucion: @EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")

Tests:
- datasourceDisponible(): DataSource != null
- jdbcClientDisponible(): JdbcClient != null
- selectUnoEjecuta(): SELECT 1 devuelve 1

En build normal (FALTAS_DB_URL no definida): test omitido. Build no falla.
Con FALTAS_DB_URL definida: test se ejecuta contra MariaDB real.

---

## 8. Confirmaciones

- Sin JPA / Hibernate. Verificado: cero referencias en codigo Java.
- Sin repositorios JDBC de dominio todavia. Se implementan desde 9-2.
- Sin tablas para enums cerrados.
- Sin seeds para enums cerrados.
- InMemory* repositorios siguen activos y sin modificar.
- FlujoCoreIT (test @SpringBootTest existente): sigue pasando con perfil default.
- 908/908 tests passing. BUILD SUCCESS.

---

## 9. Proximo slice

9-2 - Piloto JdbcDocumentoPlantillaRepository con JdbcClient.

Objetivo:
- DDL: crear fal_documento_plantilla y fal_documento_plantilla_firma_req.
- Implementar JdbcDocumentoPlantillaRepository.
- Mapeo: enums como SMALLINT, ids BIGINT.
- Activar via perfil jdbc.
- Tests de integracion con MariaDB real.