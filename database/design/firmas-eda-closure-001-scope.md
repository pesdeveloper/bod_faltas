# Scope e inventario del paquete DDL — SPEC-FIRMAS-EDA-CLOSURE-001

> **Trabajo:** SPEC-FIRMAS-EDA-CLOSURE-001
> **Relación con el baseline:** extiende `DDL-MARIADB-MANUAL-001-FULL-R1`
> (`database/ddl/create-bod-faltas-domain.sql`, 65 tablas canónicas), sin
> alterar ese script ni sus artefactos de diseño (`canonical-table-inventory.md`,
> `dependency-order.md`, `ddl-full-scope.md`). Este documento describe
> exclusivamente el paquete incremental de este cierre.
> **Fuente normativa:** `backend/api-faltas-core/docs/spec-as-source/50-persistence/firma-eda-physical-model.md`
> **Estado:** `LOGICAL_MODEL_COMPLETE`, `PHYSICAL_MODEL_COMPLETE`, `DDL_READY`,
> `DDL_NOT_EXECUTED`, `IMPLEMENTATION_NOT_STARTED`,
> `READY_FOR_MARIADB_DDL_EXECUTION` (pendiente de autorización de ejecución).
> **Precondición:** `create-bod-faltas-domain.sql` ya ejecutado (las 3 tablas
> alteradas ya existen como parte del baseline de 65).
> **Modelo mínimo aprobado:** bus central reducido a una única tabla
> append-only (`bus_evento`), sin fan-out, sin entregas, sin suscripciones,
> sin permisos de publicación tabulares, sin rutinas `SQL SECURITY DEFINER`,
> sin vistas diagnósticas ni cuenta definidora. Cada aplicación municipal
> publica con `INSERT` directo y consume con `SELECT` directo, manteniendo
> su propio checkpoint/Outbox/Inbox/Inbox error locales.

## 1. Inventario de objetos de este paquete (propiedad Faltas)

| # | Objeto | Tipo | DECISION_DDL | Depende de |
|---|---|---|---|---|
| 1 | `fal_documento_firma_req` | `ALTER TABLE` (14 columnas: 10 `DEC-EDA-020` + 4 `DEC-EDA-030` + 4 CHECK + 3 índices/UNIQUE) | `DECISION_DDL-FIRMAREQ-01`/`-02` | Baseline (tabla ya existe) |
| 2 | `fal_documento_firma` | `ALTER TABLE` (27 columnas + `chk_firma_eda_completo` integral + `chk_firma_perfil_unico` + 8 CHECK adicionales + 3 índices/UNIQUE + `fk_firma_resultado` diferida) | `DECISION_DDL-FIRMA-02`/`-03`/`-04` | Baseline (tabla ya existe); `fk_firma_resultado` depende de #4 |
| 3 | `fal_documento` | `ALTER TABLE` (1 columna + 1 CHECK + backfill condicional por `storage_key`/`hash_docu`) | `DECISION_DDL-DOC-02`/`-03` | Baseline (tabla ya existe) |
| 4 | `fal_documento_firma_resultado` | `CREATE TABLE` (nueva; 35 columnas: staging + swap + binding) | `DECISION_DDL-RESULTADO-01`/`-02`/`-03` | `fal_documento`, `fal_documento_firma_req` (FK) |
| 5 | `fal_outbox` | `CREATE TABLE` (nueva; Outbox local Faltas) | `DECISION_DDL-BUS-01` | Ninguno de este paquete |
| 6 | `bus_evento` | `CREATE TABLE` (nueva; única tabla central, append-only) | `DECISION_DDL-BUS-01` | Ninguno de este paquete |
| 7 | `fal_bus_checkpoint` | `CREATE TABLE` (nueva; checkpoint local Faltas, singleton) + seed de 1 fila | `DECISION_DDL-BUS-02` | Ninguno de este paquete |
| 8 | `fal_inbox` | `CREATE TABLE` (nueva; Inbox local Faltas, `PK = bus_evento_id`) | `DECISION_DDL-BUS-01` | Ninguno de este paquete |
| 9 | `fal_inbox_error` | `CREATE TABLE` (nueva; errores locales de Inbox Faltas) | `DECISION_DDL-BUS-02` | `fal_inbox` (FK `bus_evento_id`) |

Conteo: **3 tablas alteradas** (sin romper compatibilidad; solo `ADD COLUMN` /
`ADD CONSTRAINT` / `ADD INDEX` / `MODIFY COLUMN` de comentario, ningún `DROP`)
+ **5 tablas nuevas** = **9 objetos** de propiedad Faltas + **1 fila** de
seed (`fal_bus_checkpoint`). `DESIGN_GAPS = 0` para este paquete.

Sin rutinas, sin vistas: cada aplicación municipal accede a `bus_evento`
con `INSERT`/`SELECT` directo (grants mínimos, §4 de este documento), sin
ninguna capa intermedia `SQL SECURITY DEFINER`.

Excluido de este paquete (bounded context Firmas, otra base de datos, propio
paquete DDL ejecutable — `DECISION_DDL-FIRMAS-01`):

```text
database/ddl/firmas/create-firmas-eda-outbox-inbox.sql
database/ddl/firmas/rollback-firmas-eda-outbox-inbox.sql
database/diagnostics/firmas/verify-firmas-eda-outbox-inbox-precheck.sql
database/diagnostics/firmas/verify-firmas-eda-outbox-inbox-postcheck.sql
database/diagnostics/firmas/verify-firmas-eda-outbox-inbox-rollback-precheck.sql
```

Adicional, fuera del inventario de tablas pero parte del cierre:

- Los 5 usuarios MariaDB de mínimo privilegio del bus central (aplicación),
  aprovisionados fuera de este repositorio y otorgados exclusivamente con
  `INSERT`/`SELECT`/`SELECT, DELETE` directo sobre `bus_evento`
  (`database/ddl/grants-firmas-eda-bus.sql`, con rollback en
  `database/ddl/rollback-grants-firmas-eda-bus.sql` y precheck de
  existencia en `database/diagnostics/verify-firmas-eda-bus-principals-precheck.sql`,
  `DECISION_DDL-BUS-03`).

## 2. Orden de ejecución (DAG del paquete)

```text
1.  ALTER TABLE fal_documento_firma_req            (sin FK a objetos nuevos de este paquete)
2.  ALTER TABLE fal_documento_firma                (columnas/CHECK propios; sin fk_firma_resultado todavia)
3.  ALTER TABLE fal_documento                       (sin FK a objetos nuevos de este paquete)
4.  CREATE TABLE fal_documento_firma_resultado      (FK -> fal_documento, fal_documento_firma_req; despues de 1 y 3)
5.  ALTER TABLE fal_documento_firma ADD fk_firma_resultado  (despues de 4)
6.  CREATE TABLE fal_outbox                         (sin FK)
7.  CREATE TABLE bus_evento                         (sin FK)
8.  CREATE TABLE fal_bus_checkpoint                 (sin FK)
9.  INSERT seed de fal_bus_checkpoint               (despues de 8)
10. CREATE TABLE fal_inbox                          (sin FK)
11. CREATE TABLE fal_inbox_error                    (FK -> fal_inbox; despues de 10)
12. [aparte] verify-firmas-eda-closure-001-postcheck.sql + -parity.sql (postcheck ESTRUCTURAL, despues de 1-11)
13. [aparte] verify-firmas-eda-bus-principals-precheck.sql   (antes de 14)
14. [aparte] grants-firmas-eda-bus.sql                       (despues de 13)
15. [aparte] verify-firmas-eda-closure-001-postcheck.sql seccion de seguridad/permisos (postcheck RUNTIME, despues de 14)
```

No hay ciclos en este paquete. `fk_firma_resultado` (paso 5) es la única
dependencia que obliga a partir la alteración de `fal_documento_firma` en
dos sentencias no consecutivas (a diferencia de 3 ciclos resueltos en el
baseline FULL-R1 con `ALTER ... ADD FK` diferido, ajenos a este cierre).
`fal_inbox_error` (paso 11) es la única tabla nueva de este paquete con FK
hacia otra tabla nueva del mismo paquete. El script
`alter-firmas-eda-closure-001-forward.sql` ejecuta los pasos 1-11 en este
orden exacto (`firma-eda-physical-model.md` §12); los pasos 12-15 están en
scripts separados, ejecutados después, en ese orden exacto (misma
secuencia que `database/README.md` "Orden operativo").

## 3. Relación con el orden topológico del baseline

Este paquete no reordena ni reclasifica ningún grupo `G1`-`G12` de
`database/design/dependency-order.md`. `fal_documento_firma_req` pertenece al
grupo `G7`, `fal_documento_firma` al grupo `G8` y `fal_documento` al grupo
`G6`/`G7` (según `dependency-order.md`) del baseline; las 3 ya existen cuando
este paquete se ejecuta. Las 5 tablas nuevas (`fal_documento_firma_resultado`,
`fal_outbox`, `bus_evento`, `fal_bus_checkpoint`, `fal_inbox`,
`fal_inbox_error`) no pertenecen a ningún grupo `G1`-`G12` porque no forman
parte del inventario canónico de 65 tablas de
`database/design/canonical-table-inventory.md`; son infraestructura de
staging y transporte EDA, no entidades permanentes del dominio Faltas.

## 4. Trazabilidad a decisiones físicas

| DECISION_DDL | Documento fuente | Objetos afectados |
|---|---|---|
| `DECISION_DDL-FIRMAREQ-01` | `50-persistence/ddl-decisions.md` | `fal_documento_firma_req` (columnas `DEC-EDA-020`) |
| `DECISION_DDL-FIRMAREQ-02` | `50-persistence/ddl-decisions.md` | `fal_documento_firma_req` (preparación persistida `DEC-EDA-030`) |
| `DECISION_DDL-FIRMA-02` | `50-persistence/ddl-decisions.md` | `fal_documento_firma` (columnas base) |
| `DECISION_DDL-FIRMA-03` | `50-persistence/ddl-decisions.md` | `fal_documento_firma` (`chk_firma_eda_completo` integral, `fk_firma_resultado`) |
| `DECISION_DDL-FIRMA-04` | `50-persistence/ddl-decisions.md` | `fal_documento_firma` (`chk_firma_perfil_unico`) |
| `DECISION_DDL-DOC-02` | `50-persistence/ddl-decisions.md` | `fal_documento` (`revision_artefacto`) |
| `DECISION_DDL-DOC-03` | `50-persistence/ddl-decisions.md` | `fal_documento` (backfill seguro por consistencia) |
| `DECISION_DDL-RESULTADO-01` | `50-persistence/ddl-decisions.md` | `fal_documento_firma_resultado` (estructura base) |
| `DECISION_DDL-RESULTADO-02` | `50-persistence/ddl-decisions.md` | `fal_documento_firma_resultado` (swap/cleanup) |
| `DECISION_DDL-RESULTADO-03` | `50-persistence/ddl-decisions.md` | `fal_documento_firma_resultado` (binding criptográfico) |
| `DECISION_DDL-RESULTADO-04` | `50-persistence/ddl-decisions.md` | `fal_documento_firma_resultado` (rechazo PKI local) |
| `DECISION_DDL-BUS-01` | `50-persistence/ddl-decisions.md` | `bus_evento` (tabla única central, append-only), `fal_outbox`, `fal_inbox` (estructura mínima) |
| `DECISION_DDL-BUS-02` | `50-persistence/ddl-decisions.md` | `fal_bus_checkpoint` (singleton + seed), `fal_inbox_error` (errores locales) |
| `DECISION_DDL-BUS-03` | `50-persistence/ddl-decisions.md` | Grants mínimos directos del bus central (`grants-firmas-eda-bus.sql`) |
| `DECISION_DDL-FIRMAS-01` | `50-persistence/ddl-decisions.md` | `fir_outbox`/`fir_bus_checkpoint`/`fir_inbox`/`fir_inbox_error` (paquete separado, bounded context Firmas) |

## 5. Artefactos de este paquete

```text
database/
├── ddl/
│   ├── alter-firmas-eda-closure-001-forward.sql        (ALTER + CREATE; sin rutinas ni vistas; no ejecutado)
│   ├── alter-firmas-eda-closure-001-rollback.sql       (reversion completa; no ejecutado)
│   ├── grants-firmas-eda-bus.sql                       (INSERT/SELECT/DELETE directo sobre bus_evento, sin passwords; no ejecutado)
│   ├── rollback-grants-firmas-eda-bus.sql              (REVOKE simetrico; no ejecutado)
│   └── firmas/
│       ├── create-firmas-eda-outbox-inbox.sql     (ejecutable; bounded context Firmas)
│       └── rollback-firmas-eda-outbox-inbox.sql    (ejecutable; bounded context Firmas)
├── diagnostics/
│   ├── verify-firmas-eda-closure-001-precheck.sql            (solo lectura; antes del forward)
│   ├── verify-firmas-eda-closure-001-postcheck.sql           (solo lectura; despues del forward y de los grants)
│   ├── verify-firmas-eda-closure-001-parity.sql              (solo lectura; paridad tipo-a-tipo)
│   ├── verify-firmas-eda-closure-001-rollback-precheck.sql   (solo lectura; antes del rollback)
│   ├── verify-firmas-eda-bus-principals-precheck.sql         (solo lectura; antes de los grants de aplicacion)
│   └── firmas/
│       ├── verify-firmas-eda-outbox-inbox-precheck.sql              (solo lectura; bounded context Firmas)
│       ├── verify-firmas-eda-outbox-inbox-postcheck.sql             (solo lectura; bounded context Firmas)
│       └── verify-firmas-eda-outbox-inbox-rollback-precheck.sql     (solo lectura; independiente del precheck de creacion)
└── design/
    └── firmas-eda-closure-001-scope.md              (este documento)
```

## 6. No mezcla con el baseline

Este paquete no modifica `create-bod-faltas-domain.sql`,
`canonical-table-inventory.md`, `dependency-order.md`, `ddl-full-scope.md` ni
`verify-domain-schema.sql`/`verify-canonical-table-inventory.sql`. El conteo de
65 tablas canónicas del baseline no cambia: las 5 tablas nuevas de este
paquete son infraestructura de staging y transporte EDA, fuera del
inventario canónico de dominio versionado en esos documentos.
