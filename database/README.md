# Base de datos BOD Faltas — DDL manual

> **Trabajo:** DDL-MARIADB-MANUAL-001-FULL-R1
> **Estado:** APROBADO PARA EJECUCIÓN MANUAL CONTROLADA
>
> Este directorio contiene el script canónico de creación del dominio BOD Faltas
> en MariaDB y los artefactos de diseño y diagnóstico asociados. El script ha sido
> auditado externamente y declarado apto para ejecución manual controlada.
> El DDL **no ha sido ejecutado todavía**; el esquema aún no existe en la base de datos.

## Arquitectura de persistencia

Este proyecto **no usa ORM** de ningún tipo:

- No hay JPA, Hibernate, EclipseLink ni Spring Data JPA.
- No hay `@Entity`, `@Table`, `@Column` ni `ddl-auto`.
- No hay Flyway, Liquibase ni migraciones automáticas.
- No hay ejecución SQL al iniciar Spring.
- No hay scripts bajo `src/main/resources/db`.

La persistencia productiva usa **Spring JDBC explícito**:

- SQL escrito y revisado manualmente.
- `JdbcClient` como API preferida.
- `RowMapper` y mapeos explícitos.
- Transacciones explícitas.
- OCC explícito con `version_row`.
- Constraints MariaDB como segunda línea de defensa.

## Motor objetivo

| Parámetro | Valor |
|---|---|
| Motor | MariaDB 12.3.2, Windows x64 |
| Storage | InnoDB |
| Charset | utf8mb4 |
| Collation | utf8mb4_uca1400_ai_ci |
| lower_case_table_names | 1 |
| sql_mode | STRICT_TRANS_TABLES, ERROR_FOR_DIVISION_BY_ZERO, NO_AUTO_CREATE_USER, NO_ENGINE_SUBSTITUTION |

## Ejecución exclusivamente manual

El DBA (Pablo) ejecuta el script desde **HeidiSQL** contra la instancia
`sb_faltas_db`. No existe runner automático. Spring JDBC puede conectar y operar
una vez ejecutado el DDL, pero nunca crea, altera ni migra el esquema.

## Estructura de este directorio

```text
database/
├── README.md                        (este archivo)
├── ddl/
│   └── create-bod-faltas-domain.sql (script canónico acumulativo; único; APROBADO — pendiente ejecución)
├── diagnostics/
│   ├── verify-protected-baseline.sql        (verifica objetos protegidos pre-existentes)
│   ├── verify-canonical-table-inventory.sql (verifica inventario de 65 tablas)
│   └── verify-domain-schema.sql             (verifica las 64 tablas TO_CREATE del dominio)
└── design/
    ├── canonical-table-inventory.md     (inventario de las 65 tablas canónicas)
    ├── dependency-order.md              (orden topológico de creación; DAG real)
    └── ddl-full-scope.md                (scope completo FULL-R1: 64 tablas, decisiones, gaps=0)
```

## Baseline protegido

Los objetos siguientes son **pre-existentes** y el DDL del dominio **no los crea,
altera, borra, trunca ni renombra** bajo ninguna circunstancia:

### Tablas protegidas (16)

- `fal_informix_sync_error`
- `fal_informix_sync_run`
- `fal_rubro_version` ← **PREEXISTING_CANONICAL_ADOPTED** (adoptada como tabla canónica Faltas)
- `geo_bahra_asentamiento`
- `geo_calle_alturas_barrio`
- `geo_dataset_load_error`
- `geo_dataset_load_run`
- `geo_dataset_row_version`
- `geo_ign_departamento`
- `geo_ign_municipio`
- `geo_ign_provincia`
- `geo_indec_calles`
- `geo_indec_localidad`
- `geo_indec_localidad_censal`
- `geo_malv_calle_version`
- `geo_malv_localidad_version`

### Vistas protegidas (4)

- `vw_fal_rubro_actual`
- `vw_geo_malv_calle_actual`
- `vw_geo_malv_localidad_actual`
- `vw_geo_municipio_departamento`

## Conteo canónico

- **65** tablas canónicas Faltas al final del DDL completo (FULL-R1)
- **1** ya existente: `fal_rubro_version` (PREEXISTING_CANONICAL_ADOPTED)
- **64** a crear por el script canónico

Los guardrails usan la lista exacta de 65 tablas, no `LIKE 'fal_%'`, porque
`fal_informix_sync_error` y `fal_informix_sync_run` existen pero no pertenecen
al inventario canónico del dominio Faltas.

## Orden de instalación completo (desde cero)

1. Crear la base `sb_faltas_db`.
2. Ejecutar los procesos existentes que crean/cargan GEO, Rubros y sincronización.
3. Verificar el baseline protegido (`diagnostics/verify-protected-baseline.sql`).
4. Ejecutar manualmente `ddl/create-bod-faltas-domain.sql` desde HeidiSQL
   (el script está aprobado para ejecución; el esquema aún no ha sido creado).
5. Ejecutar diagnósticos estructurales (`diagnostics/verify-canonical-table-inventory.sql`
   y `diagnostics/verify-domain-schema.sql`).
6. Ejecutar opcionalmente el seeder de escenarios en un entorno autorizado
   (seeder no implementado todavía; ver `50-persistence/ddl-execution-and-test-seeding.md`).

## Estado del trabajo DDL-MARIADB-MANUAL-001-FULL-R1

| Aspecto | Estado |
|---|---|
| Auditoría externa | APROBADO — reauditoría externa declaró DDL APTO PARA EJECUCIÓN MANUAL CONTROLADA |
| Ejecución DDL | PENDIENTE — el script está aprobado pero aún no ha sido ejecutado |
| Esquema en MariaDB | AÚN NO CREADO |
| Etapa siguiente | Ejecución controlada y diagnósticos posteriores |
| Tablas a crear | 64 (FULL-R1 completo) |
| Tablas adoptadas | 1 (`fal_rubro_version`) |
| Total canónico | 65 |
| PK / FK / UNIQUE / CHECK / índices | Incluidos para todas las 64 tablas |
| COMMENT de tabla y columnas | Incluidos en todas las tablas |
| ENGINE InnoDB / utf8mb4 / utf8mb4_uca1400_ai_ci | Todos los bloques |
| Ciclos resueltos via ALTER TABLE | 3 (G1-self, G4-cross, G8-cross) |
| Entrega por slices | No — entrega integral única |

## Fuentes canónicas

La spec-as-source que rige el diseño del DDL está en:

`backend/api-faltas-core/docs/spec-as-source/`

Documentos de persistencia aplicables:

- `50-persistence/mariadb-logical-model.md` — modelo lógico e inventario de tablas
- `50-persistence/ddl-decisions.md` — 24 decisiones físicas cerradas
- `50-persistence/inmemory-mariadb-deltas.md` — deltas transversales
- `50-persistence/ddl-execution-and-test-seeding.md` — mecanismo de ejecución y seeder
- `00-governance/ready-for-ddl-gate.md` — gate READY_FOR_DDL

## Guardrails automatizados

Las clases de guardrails en el módulo Java verifican estáticamente:

- `DdlInventoryContractTest`: inventario 65/1/64, sin duplicados, coincide con modelo lógico.
- `DdlScriptContractTest`: 64 CREATE TABLE únicos, conjunto = TO_CREATE, no ORM, no Flyway,
  ENGINE/charset/collation por bloque, COMMENT de tabla y columnas, DATETIME(6), release gate aprobado, USE.
- `DdlDiagnosticsSafetyTest`: diagnósticos solo lectura, objetos protegidos intactos.

Ninguna de estas clases usa Mockito, base de datos, ni Testcontainers.
