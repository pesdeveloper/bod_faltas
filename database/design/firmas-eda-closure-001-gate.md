# Gate — `SPEC-FIRMAS-EDA-CLOSURE-001`

> **Estado documental:** SUPPORTING_CURRENT
> **Autoridad DDL:** SUPPORTING
> Este documento es un gate de conformidad técnica del paquete DDL incremental
> `SPEC-FIRMAS-EDA-CLOSURE-001` (extensión de `fal_documento*` y modelo mínimo
> aprobado del bus de eventos EDA Faltas↔Firmas). No redefine reglas de
> dominio ni decisiones físicas: certifica que el SQL, los diagnósticos, los
> grants y la suite Maven de este paquete son coherentes entre sí y con
> `../../backend/api-faltas-core/docs/spec-as-source/50-persistence/firma-eda-physical-model.md`
> (fuente normativa). Ante contradicción, prevalece la spec canónica.

**Fecha de verificación:** 2026-08-02.
**Alcance:** `database/ddl/alter-firmas-eda-closure-001-{forward,rollback}.sql`,
`database/ddl/grants-firmas-eda-bus.sql`,
`database/ddl/rollback-grants-firmas-eda-bus.sql`,
`database/ddl/firmas/{create,rollback}-firmas-eda-outbox-inbox.sql`,
`database/diagnostics/verify-firmas-eda-*.sql`,
`database/diagnostics/firmas/verify-firmas-eda-outbox-inbox-*.sql`,
`backend/api-faltas-core/docs/spec-as-source/50-persistence/firma-eda-physical-model.md`,
`backend/api-faltas-core/docs/spec-as-source/30-integrations/{municipal-eventing,eventing-operations}.md`,
`database/README.md`, guardrail `FirmasEdaBusSimpleContractTest` y suite
Maven completa del módulo `backend/api-faltas-core`.
**Naturaleza:** verificación estática de diseño DDL, sin conexión a
MariaDB. **DDL no ejecutado** (`DDL_NOT_EXECUTED`).

## Modelo mínimo aprobado (objeto de este gate)

`bus_evento` es la única tabla central del bus, append-only e inmutable
tras el `INSERT`. Sin fan-out central, sin tabla de entregas, sin
suscripciones, sin permisos de publicación tabulares, sin rutinas
`SQL SECURITY DEFINER`, sin vistas diagnósticas y sin cuenta definidora
(`bus_definer`). Cada aplicación municipal (Faltas, Firmas) mantiene su
propio Outbox/checkpoint/Inbox/Inbox error locales y accede a `bus_evento`
con `INSERT` **column-level** (publishers, excluyendo `bus_evento_id`) o
`SELECT` de tabla completa (consumers) directo, mediante 5 cuentas SQL de
mínimo privilegio (`grants-firmas-eda-bus.sql`). Un evento sin consumidores
es válido y no requiere ninguna escritura adicional.

## Veredicto

## READY_FOR_MARIADB_DDL_EXECUTION

Los 13 ítems del checklist están en `PASS`. No hay bloqueadores internos de
diseño. La ejecución real contra MariaDB permanece condicionada, en este
orden exacto (`database/README.md` "Orden operativo",
`firma-eda-physical-model.md` §12), a:

1. Aprovisionamiento externo (fuera de este repositorio) de las 5 cuentas
   de aplicación del bus (`bus_publisher_faltas`, `bus_publisher_firmas`,
   `bus_consumer_faltas`, `bus_consumer_firmas`, `bus_maintenance`), con
   `REQUIRE SSL`.
2. Ejecución real de `verify-firmas-eda-closure-001-precheck.sql` y
   `verify-firmas-eda-bus-principals-precheck.sql` contra el servidor
   MariaDB real, con veredicto `READY` en ambos.
3. Autorización explícita y humana para ejecutar el DDL (este gate certifica
   que el **diseño** está listo; no autoriza por sí solo la ejecución).

Si cualquier condición externa anterior no se cumple al momento de ejecutar,
el estado operativo real es `NOT_READY_FOR_MARIADB_DDL_EXECUTION` aunque
este gate de diseño esté en `PASS`.

## Checklist (13 ítems)

| # | Ítem | Resultado | Evidencia |
|---|---|---|---|
| 1 | `bus_evento` es la única tabla central, append-only, con exactamente sus 13 columnas/constraints aprobados; sin `UPDATE`/`DELETE` sobre ella en el forward | `[PASS]` | `alter-firmas-eda-closure-001-forward.sql` §5.2; `FirmasEdaBusSimpleContractTest$G1_BusEventoCentral` (4 tests) |
| 2 | `fal_outbox`/`fal_bus_checkpoint`/`fal_inbox`/`fal_inbox_error` existen con la estructura mínima aprobada (checkpoint singleton + seed, Inbox con `PK = bus_evento_id` y envelope completo con `UNIQUE(evento_id)`, Inbox error con FK `RESTRICT` y `error_detail` acotado a 255 caracteres) | `[PASS]` | `alter-firmas-eda-closure-001-forward.sql` §5.1/§5.3/§5.4/§5.5; `FirmasEdaBusSimpleContractTest$G2_TablasLocalesFaltas` (5 tests) |
| 3 | El paquete Firmas (`fir_outbox`/`fir_bus_checkpoint`/`fir_inbox`/`fir_inbox_error`) espeja exactamente las columnas de su contraparte `fal_*`, envelope y `UNIQUE(evento_id)` incluidos; script ejecutable sin `USE` | `[PASS]` | `database/ddl/firmas/create-firmas-eda-outbox-inbox.sql`; `FirmasEdaBusSimpleContractTest$G3_PaqueteFirmasEspejo` (5 tests) |
| 4 | Ausencia total, en el SQL ejecutable de `database/`, de la arquitectura eliminada (`bus_evento_entrega`, `bus_suscripcion`, `bus_publicacion_permiso`, `bus_definer`, rutinas `SQL SECURITY DEFINER`, `sp_bus_*`, vistas `v_bus_*`) | `[PASS]` | `FirmasEdaBusSimpleContractTest$G4_SinArquitecturaEliminada` (2 tests); ningún `.sql` de `database/` contiene sintaxis ejecutable de esos objetos |
| 5 | Grants de aplicación directos y mínimos sobre `bus_evento` (`INSERT` **column-level** publishers —excluyendo `bus_evento_id`—, `SELECT` de tabla completa consumers, `SELECT`+`DELETE` de tabla completa maintenance); sin `GRANT EXECUTE`, sin `CREATE USER`, sin `IDENTIFIED BY`; ninguna cuenta `bus_*` con privilegio sobre tablas locales | `[PASS]` | `grants-firmas-eda-bus.sql`; `FirmasEdaBusSimpleContractTest$G5_GrantsMinimos` (5 tests) |
| 6 | Rollback de grants simétrico exacto (5 `REVOKE` column-level en publishers, sin `DROP USER`) | `[PASS]` | `rollback-grants-firmas-eda-bus.sql`; `FirmasEdaBusSimpleContractTest$G5_GrantsMinimos.rollbackDeGrantsEsSimetrico` |
| 7 | Rollback de tablas simétrico exacto al forward: `DROP TABLE` de las 5 tablas nuevas del bus en orden de dependencias (`fal_inbox_error` antes de `fal_inbox`) y del paquete Firmas (`fir_inbox_error` antes de `fir_inbox`) | `[PASS]` | `alter-firmas-eda-closure-001-rollback.sql` §01; `rollback-firmas-eda-outbox-inbox.sql`; `FirmasEdaBusSimpleContractTest$G6_RollbackSimetrico` (2 tests) |
| 8 | Ningún script del bus (forward/rollback/grants/paquete Firmas) contiene `CREATE PROCEDURE`, `CREATE FUNCTION`, `CREATE VIEW` ni `CREATE DEFINER` | `[PASS]` | `FirmasEdaBusSimpleContractTest$G4_SinArquitecturaEliminada.sinRutinasNiVistas` |
| 9 | Diagnósticos (`precheck`/`postcheck`/`parity`/`rollback-precheck`, principal y de Firmas) coherentes con el inventario de 6 tablas nuevas (`fal_documento_firma_resultado` + 5 tablas del bus), con el envelope de Inbox y sin referencias a rutinas/vistas/`bus_definer` | `[PASS]` | `database/diagnostics/verify-firmas-eda-closure-001-{precheck,postcheck,parity,rollback-precheck}.sql`, `verify-firmas-eda-bus-principals-precheck.sql`, `database/diagnostics/firmas/verify-firmas-eda-outbox-inbox-*.sql` |
| 10 | La ingestión (Inbox+checkpoint) y el procesamiento por fila son transacciones locales independientes; la lectura central usa `START TRANSACTION READ ONLY` + `LOCK IN SHARE MODE` (documentado de forma idéntica en `eventing-operations.md` y `firma-eda-physical-model.md`) | `[PASS]` | `FirmasEdaBusSimpleContractTest$G7_TransaccionesSeparadasYLecturaBloqueante` (3 tests) |
| 11 | `evento_id` (identidad lógica, `UNIQUE` en ambas Inbox) es distinto de `bus_evento_id` (cursor técnico); un rechazo funcional de dominio cierra la Inbox en `PROCESADO`/`si_error = FALSE` sin fila en `*_inbox_error`; sin cuarentena central afirmativa ni `X-Api-Key` para el bus | `[PASS]` | `FirmasEdaBusSimpleContractTest$G8_IdempotenciaYRechazoFuncional` (5 tests) |
| 12 | El reintento manual de Outbox conserva `evento_id` y payload; la retención de `bus_evento` es exclusivamente por antigüedad de `publicado_en`; el publisher reconcilia por `duplicate key` sobre `uq_bus_evento_evento_id`, sin `SELECT` adicional | `[PASS]` | `FirmasEdaBusSimpleContractTest$G9_RetryYRetencion` (3 tests) |
| 13 | Suite Maven completa verde | `[PASS]` | `mvn -o clean test` (módulo `backend/api-faltas-core`): **3529 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS** (evidencia completa en la sección "Evidencia de tests" de este documento) |

## Base normativa

- [`../../backend/api-faltas-core/docs/spec-as-source/50-persistence/firma-eda-physical-model.md`](../../backend/api-faltas-core/docs/spec-as-source/50-persistence/firma-eda-physical-model.md) — §6 (bus + Outbox/checkpoint/Inbox/Inbox error locales), §8 (bounded context Firmas), §9 (transporte JDBC/SQL directo), §10 (seguridad SQL mínima, grants), §11 (retención), §12 (orden de ejecución), §14 (rollback).
- [`../../backend/api-faltas-core/docs/spec-as-source/30-integrations/municipal-eventing.md`](../../backend/api-faltas-core/docs/spec-as-source/30-integrations/municipal-eventing.md) — modelo conceptual del bus simplificado.
- [`../../backend/api-faltas-core/docs/spec-as-source/30-integrations/eventing-operations.md`](../../backend/api-faltas-core/docs/spec-as-source/30-integrations/eventing-operations.md) — operación: checkpoint local, retención, reconciliación.
- [`firmas-eda-closure-001-scope.md`](firmas-eda-closure-001-scope.md) — inventario de objetos y orden de ejecución de este paquete.
- [`../README.md`](../README.md) — "Orden operativo" de 6 pasos.

## Bloqueadores internos

Ninguno.

## Dependencias externas no bloqueantes para este gate de diseño

- Aprovisionamiento real de las 5 cuentas de aplicación del bus (secret
  manager/pipeline de la plataforma, fuera de este repositorio).
- Ejecución real de los prechecks contra un servidor MariaDB disponible.
- Autorización humana explícita para ejecutar el DDL (este gate no la
  otorga).

## Evidencia de tests

```text
mvn -o clean test (backend/api-faltas-core)

Tests run: 3529, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

(`clean` es obligatorio: sin él, `target/surefire-reports` conserva reportes
residuales de ejecuciones previas acotadas o de clases de test ya
renombradas/eliminadas, que inflarían el total agregado.)

Incluye, entre otros, los 34 tests de `FirmasEdaBusSimpleContractTest`
(guardrails `G1`-`G9`: estructura mínima de `bus_evento`/Outbox/checkpoint/
Inbox con envelope completo/Inbox error, espejo exacto del paquete Firmas,
ausencia de la arquitectura eliminada, grants column-level y rollback
simétrico, separación de transacciones de ingestión/procesamiento con
`LOCK IN SHARE MODE`, idempotencia `evento_id`/`bus_evento_id` y rechazo
funcional sin `inbox_error`, retry de Outbox y retención por antigüedad) y
los 40 tests de `DdlScriptContractTest` (guardrails del DDL canónico
`create-bod-faltas-domain.sql`, sin regresión).

`git diff --check` sobre el árbol de trabajo de este cierre: sin errores de
espacio en blanco. `git status --short`/`git diff --cached --name-only`:
revisados; ningún archivo de este cierre fue agregado al staging.

## Entrada del siguiente bloque

1. Aprovisionar las 5 cuentas de aplicación del bus fuera de este
   repositorio (secret manager/pipeline), con `REQUIRE SSL`.
2. Ejecutar `verify-firmas-eda-closure-001-precheck.sql` contra MariaDB
   real; confirmar `READY`.
3. Obtener autorización humana explícita para ejecutar
   `alter-firmas-eda-closure-001-forward.sql`.
4. Ejecutar el forward, luego `verify-firmas-eda-closure-001-postcheck.sql`
   (secciones 1-5, postcheck estructural) y `-parity.sql`.
5. Ejecutar `verify-firmas-eda-bus-principals-precheck.sql`; confirmar
   `READY`.
6. Ejecutar `grants-firmas-eda-bus.sql`.
7. Ejecutar `verify-firmas-eda-closure-001-postcheck.sql` sección 6
   (postcheck runtime de seguridad y permisos).

No alterar contratos funcionales, estados, eventos ni transiciones vigentes
como parte de esta ejecución. No mezclar esta ejecución con la
implementación de adapters JDBC de dominio (fuera de alcance de este
cierre).
