# Roadmap vigente posterior a spec-as-source

> **Estado documental:** SUPPORTING_CURRENT
> **Autoridad DDL:** SUPPORTING
> Este documento describe unicamente el trabajo pendiente vigente despues del
> cierre formal de la etapa spec-as-source (ver
> [`../00-governance/ready-for-ddl-gate.md`](../00-governance/ready-for-ddl-gate.md)).
> La evolucion historica se conserva en Git y no forma parte de la spec-as-source
> activa.

**Fecha de actualización:** 2026-07-21
**Estado:** spec-as-source auditada transversalmente; puerta `READY_FOR_DDL` evaluada en
[`../00-governance/ready-for-ddl-gate.md`](../00-governance/ready-for-ddl-gate.md);
todas las `DECISION_DDL-*` cerradas (ver
[`../50-persistence/ddl-decisions.md`](../50-persistence/ddl-decisions.md));
script DDL canónico completo (64 tablas) aprobado para ejecución manual controlada
(trabajo DDL-MARIADB-MANUAL-001-FULL-R1; ver `database/ddl/create-bod-faltas-domain.sql`);
esquema en MariaDB aún no creado.

## 1. Baseline vigente

El modelo funcional InMemory actúa como oráculo de paridad para la persistencia.

Estado verificado:

- modelo de dominio y lifecycle estabilizados;
- contratos funcionales de los siete comandos canónicos de fallo (CMD-FALLO-001..007) y del resto
  del circuito cerrados y verificados contra el código vigente;
- modelo conceptual de tablas objetivo MariaDB listo (ver
  [`../50-persistence/mariadb-logical-model.md`](../50-persistence/mariadb-logical-model.md));
  todas las `DECISION_DDL-*` están cerradas (ver
  [`../50-persistence/ddl-decisions.md`](../50-persistence/ddl-decisions.md)); 0 enums
  0 enums `NO_EXPLICIT_CODE` vigentes;
- `FaltasClock` como abstracción temporal canónica;
- concurrencia optimista InMemory estabilizada;
- contrato global de errores HTTP cerrado mediante `ErrorResponse` y `GlobalFaltasControllerAdvice`;
- spec-as-source activa auditada transversalmente y saneada en UTF-8;
- infraestructura JDBC base incorporada: dependencia Spring JDBC, driver MariaDB, perfil `jdbc`,
  configuración de `DataSource` y prueba condicionada de infraestructura (ver
  [`../50-persistence/jdbc-infrastructure.md`](../50-persistence/jdbc-infrastructure.md));
- script DDL canónico aprobado para ejecución manual controlada: `database/ddl/create-bod-faltas-domain.sql`,
  64 tablas TO_CREATE + adopción de `fal_rubro_version`; guardrails estáticos en 3 clases de test
  (`DdlInventoryContractTest`, `DdlScriptContractTest`, `DdlDiagnosticsSafetyTest`); verdes en CI;
  esquema en MariaDB aún no creado;
- sin repositorios JDBC de dominio ni JPA/Hibernate todavía; sin Flyway/Liquibase por DECISION_DDL-EXEC-01 (no se incorporarán); sin Testcontainers ni DDL ejecutado todavía.

Las definiciones activas de paridad y estrategia de persistencia se encuentran en:

- [`../50-persistence/mariadb-logical-model.md`](../50-persistence/mariadb-logical-model.md)
- [`../50-persistence/ddl-decisions.md`](../50-persistence/ddl-decisions.md)
- [`../50-persistence/inmemory-mariadb-deltas.md`](../50-persistence/inmemory-mariadb-deltas.md)
- [`../50-persistence/jdbc-strategy.md`](../50-persistence/jdbc-strategy.md)

## 2. Siguiente secuencia de trabajo

### 2.1 Slice DDL cerrado y auditado — COMPLETADO

Slice `DDL-MARIADB-MANUAL-001-FULL-R1` cerrado y auditado externamente.
Reauditoría externa independiente declaró: `DDL APTO PARA EJECUCIÓN MANUAL CONTROLADA`.
Script aprobado con release gate formal; historia de auditoría preservada en Git.

### 2.2 Ejecutar script DDL manualmente

**[APROBADO — pendiente ejecución]**

Script canónico `database/ddl/create-bod-faltas-domain.sql` (64 tablas) aprobado para ejecución.
Ejecutar desde HeidiSQL contra MariaDB 12.3.2.
Verificar post-ejecución con `database/diagnostics/verify-domain-schema.sql` y
`database/diagnostics/verify-protected-baseline.sql`.

### 2.4 Implementar seeder

Ver contrato en `../50-persistence/ddl-execution-and-test-seeding.md`
(operaciones VERIFY/SEED/RESET_TEST_DATA/RESET_AND_SEED, allowlists, baseline
protegido).

### 2.5 Implementar adapters JDBC

Implementar `JdbcXxxRepository` para cada puerto de repositorio de dominio,
sin modificar las interfaces existentes ni los servicios de aplicación.
Tests de integración JDBC separados; no sustituir los tests InMemory.

### 2.6 Validar backend real contra MariaDB

- Suite existente completamente verde sin cambios funcionales.
- Tests de integración MariaDB verdes.
- OCC/bloqueos multinodo verificados (ver `inmemory-mariadb-deltas.md`,
  sección 12).

### 2.7 UX discovery/redesign (fase posterior)

Hoy existe una demo navegable in-memory. El frontend productivo y el
discovery de UX son fases posteriores a la validación con MariaDB real.

## 3. Integraciones externas

Estas integraciones dependen de sistemas externos y no bloquean el diseño ni la
generación del DDL de MariaDB:

- Integración real con la aplicación de Firmas (más allá del callback `firmar-real` ya implementado; posible ampliación de flujo).
- Integración real con el sistema de Notificaciones (envío postal/electrónico real; hoy simulado con lotes y acuses mock).
- Integración real con Ingresos/Tesorería (hoy la conciliación económica usa un input mock absoluto de Tesorería).
- Storage y comprobantes reales (hoy `storageKey` usa esquemas `mock://`; sin `file://` ni `s3://` reales).
- Sincronización real de calendario administrativo con un proveedor externo (hoy sin integración; ver `../10-domain/calendario-plazos-administrativos.md`, sección 14).
- Frontend productivo, cuando corresponda (hoy existe una demo navegable in-memory; ver `../40-api/http-contracts.md`, endpoints `/demo/**`).

## 4. Mejoras no bloqueantes

- Revisión del mapeo `INSPECTOR_NO_ENCONTRADO` para `FirmanteNoEncontradoException` (ver `../00-governance/command-contract-standard.md`, tabla de errores); requiere decisión explícita, no se corrige silenciosamente.
- Decisión funcional pendiente sobre la seguridad de `pago-condena/confirmar`, `pago-condena/observar` y
  `GET pago-condena` (hoy `permitAll` en `SecurityConfig`; ver `../40-api/http-contracts.md`).

## 5. Regla de mantenimiento de este documento

Este documento contiene únicamente el trabajo pendiente vigente. No agregar
cronologías de bloques cerrados, resultados históricos extensos ni copias de
handoffs. Eliminar cada ítem de este documento en cuanto quede cerrado,
trasladando el cierre a `../00-governance/ready-for-ddl-gate.md` o al historial de Git.
