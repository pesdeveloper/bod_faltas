# Scope completo DDL — DDL-MARIADB-MANUAL-001-FULL-R1

> **Trabajo:** DDL-MARIADB-MANUAL-001-FULL-R1
> **Estado documental:** APROBADO PARA EJECUCIÓN MANUAL CONTROLADA
> **Fuente normativa:** `50-persistence/mariadb-logical-model.md`, `50-persistence/ddl-decisions.md`
>
> Este documento reemplaza `ddl-r1-scope.md`. No existe R2..Rn: todas las
> 64 tablas se materializan en este único trabajo.
> El DDL ha sido auditado externamente y declarado apto. El esquema aún no ha sido creado.

## 1. Alcance completo

| Categoría | Cantidad |
|---|---|
| Tablas TO_CREATE | 64 |
| Tabla PREEXISTING_CANONICAL_ADOPTED | 1 (`fal_rubro_version`) |
| Total canónico | 65 |
| Gaps físicos abiertos | 0 |

## 2. Correcciones incorporadas en FULL-R1

### 2.1 `fal_persona`

| Campo | Spec anterior | Definición canónica |
|---|---|---|
| `apellido` | `VARCHAR NULL` (sin longitud) | `VARCHAR(80) NULL` |
| `nombres` | `VARCHAR NULL` (sin longitud) | `VARCHAR(100) NULL` |
| `razon_social` | correcto | `VARCHAR(64) NULL` |
| `nombre_mostrar` | correcto | `VARCHAR(64) NULL` |

Fuente: longitudes presentes en el modelo físico histórico; restauradas como definición
explícita (FULL-R1). Corrección aplicada en: `mariadb-logical-model.md`,
`FalPersona.java` (Javadoc), DDL.

GAP cerrado: `GAP-DDL-R1-PERSONA-LONGITUD-01`.

### 2.2 `fal_inspector`

| Aspecto | Spec histórica incorrecta | Definición canónica |
|---|---|---|
| `legajo` | `VARCHAR(20)` | `legajo_insp INT NOT NULL` (numérico; CHECK > 0) |
| Campos `apellido`, `nombre` | presentes | no existen (no tienen respaldo en `FalInspector.java`) |
| `id_user` | ausente en spec | `CHAR(36) NOT NULL UNIQUE` |
| `nom_insp` | ausente en spec (solo `nombre`) | `VARCHAR(120) NOT NULL` |

Corrección aplicada en: `mariadb-logical-model.md`, `FalInspector.java` (Javadoc), DDL.

### 2.3 `fal_dependencia`

| Campo | Nombre histórico incorrecto | Nombre canónico |
|---|---|---|
| `codigo_dependencia` | `codigo_dependencia VARCHAR(20)` | `cod_dep VARCHAR(20) NULL` |
| `nombre` | `nombre VARCHAR(200)` | `nom_dep VARCHAR(120) NOT NULL` |
| `tipo_acta` | en maestro | no existe en el maestro; vive en `fal_dependencia_version` |
| `id_dep_padre` | ausente en spec | `BIGINT NULL` (FK auto-referencial, nullable) |

Corrección aplicada en: `mariadb-logical-model.md`, `FalDependencia.java` (Javadoc), DDL.

## 3. Secciones del script DDL

El script `database/ddl/create-bod-faltas-domain.sql` está organizado
en 11 secciones funcionales:

| Sección | Grupo | Tablas | Rango # |
|---|---|---|---|
| 01 | G1 | Catálogos raíz (sin dependencias externas) | 1–12 |
| 02 | G2 | Versiones y catálogos secundarios (FK a G1) | 14–23 |
| 03 | G3 | Habilitaciones y catálogos terciarios (FK a G1+G2) | 24–28 |
| 04 | G4 | Núcleo del acta — personas/domicilios + fal_acta | 29–30 |
| 05 | G5 | Satélites del acta | 31–45 |
| 06 | G6 | Documentos directos | 46–47 |
| 07 | G7 | Documentos y asociados | 48–53 |
| 08 | G8 | Firma, apelación y obligación de pago | 54–59 |
| 09 | G9 | Snapshot, proyecciones parciales y forma de pago | 60–62 |
| 10 | G10–G11 | Plan de pago y movimientos | 63–64 |
| 11 | G12 | Proyección económica | 65 |

## 4. Ciclos resueltos vía ALTER TABLE

| Ciclo | Tablas | Estrategia | Posición en script |
|---|---|---|---|
| G1-self | `fal_dependencia.id_dep_padre → fal_dependencia` | Tabla sin FK self; ALTER post-G1 | Post-sección 01 |
| G4-cross | `fal_acta ↔ fal_persona_domicilio` | `fal_persona_domicilio` sin FK a `fal_acta`; ALTER post-G4 | Post-sección 04 |
| G8-cross | `fal_acta_obligacion_pago ↔ fal_acta_forma_pago` | `fal_acta_obligacion_pago` sin `forma_pago_vigente_id`; ALTER post-G9 | Post-sección 09 |

## 5. Constraints transversales

| Tipo | Regla |
|---|---|
| PK | Todas las tablas tienen PK explícita |
| FK | Todas las FK conceptuales cerradas están definidas; ninguna FK degradada |
| UNIQUE | Unicidades funcionales explícitas (incluyendo NULL-permisivas en MariaDB) |
| CHECK | Enums con código explícito; valores verificables en la DB |
| OCC | `version_row INT NOT NULL DEFAULT 0` en todas las entidades mutables que lo requieren |
| DATETIME | Siempre `DATETIME(6)`; nunca `DATETIME` sin precisión |
| Booleanos | `BOOLEAN` con valor lógico MariaDB |
| Enums | `SMALLINT` con CHECK de conjunto exacto (excepto `tipo_evt CHAR(6)` por decisión explícita) |
| Append-only | Sin `version_row`; sin UPDATE; sin DELETE funcional |
| Columnas generadas | `PERSISTENT` para unicidades condicionales (PAGRES, vigente) |

## 6. Riesgos post-ejecución (solo verificables en MariaDB)

Los siguientes aspectos no se pueden verificar en tiempo de test estático:

| Riesgo | Descripción |
|---|---|
| Normalización de CHECK | MariaDB puede reescribir la cláusula CHECK al almacenarla; `verify-domain-schema.sql` compara cláusula normalizada |
| BIGINT en FK | Si el tipo de la columna referenciada en el baseline (`geo_malv_*`) difiere, la FK puede fallar |
| Collation de FK | Si la collation del servidor difiere de `utf8mb4_uca1400_ai_ci`, comparaciones de `CHAR`/`VARCHAR` pueden dar sorpresas |
| Columnas generadas | La sintaxis `GENERATED ALWAYS AS ... PERSISTENT` puede variar entre MariaDB 10.x y 12.x |
| DECIMAL scale | MariaDB puede rechazar valores fuera de escala en tiempo de inserción |

## 7. Gaps físicos abiertos

**Ninguno.** Todas las 64 tablas están definidas en el DDL.

Los únicos "riesgos" son post-ejecución y están documentados arriba.
