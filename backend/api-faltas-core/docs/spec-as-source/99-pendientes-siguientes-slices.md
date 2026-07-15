# 99 — Roadmap posterior a spec-as-source

> **Estado documental:** SUPPORTING_CURRENT
> **Autoridad DDL:** SUPPORTING
> Este documento describe unicamente el trabajo pendiente vigente despues del
> cierre formal de la etapa spec-as-source (ver `101-auditoria-pre-jdbc-mariadb.md`).
> La evolucion historica se conserva en Git y no forma parte de la spec-as-source
> activa.

**Fecha de actualización:** 2026-07-13
**Estado:** spec-as-source auditada transversalmente; puerta `READY_FOR_DDL` evaluada en `101-auditoria-pre-jdbc-mariadb.md`.

## 1. Baseline vigente

El modelo funcional InMemory actúa como oráculo de paridad para la persistencia.

Estado verificado:

- modelo de dominio y lifecycle estabilizados;
- contratos funcionales de los siete comandos canónicos de fallo (CMD-FALLO-001..007) y del resto
  del circuito cerrados y verificados contra el código vigente;
- modelo conceptual de tablas objetivo MariaDB listo (ver `110-matriz-maestra-paridad-mariadb-inmemory.md`,
  sección 1); las decisiones físicas todavía abiertas quedan registradas como `DECISION_DDL-*` en `110`
  y no bloquean el inicio del diseño físico;
- `FaltasClock` como abstracción temporal canónica;
- concurrencia optimista InMemory estabilizada;
- contrato global de errores HTTP cerrado mediante `ErrorResponse` y `GlobalFaltasControllerAdvice`;
- spec-as-source activa auditada transversalmente y saneada en UTF-8;
- infraestructura JDBC base incorporada: dependencia Spring JDBC, driver MariaDB, perfil `jdbc`, configuración de `DataSource` y prueba condicionada de infraestructura (ver `103-slice-9-1-infraestructura-jdbc.md`);
- sin repositorios JDBC de dominio, migraciones versionadas, DDL productivo, Flyway/Liquibase, Testcontainers ni JPA/Hibernate todavía.

Las definiciones activas de paridad y estrategia de persistencia se encuentran en:

- `110-matriz-maestra-paridad-mariadb-inmemory.md`
- `109-delta-modelo-mariadb-inmemory.md`
- `102-slice-9-estrategia-jdbc-mariadb.md`

## 2. Siguiente bloque obligatorio: diseño y DDL MariaDB

Objetivo:

Resolver las decisiones físicas todavía abiertas (`DECISION_DDL-*` en `110`) y diseñar/incorporar
el DDL versionado de MariaDB, sin alterar el comportamiento funcional ya validado por la
implementación InMemory.

Guardrails:

- InMemory continúa siendo el oráculo de paridad.
- No cambiar reglas de dominio para adaptarlas a la persistencia.
- No cambiar estados, eventos, transiciones, bandejas ni contratos HTTP.
- No introducir JPA si la estrategia vigente define JDBC explícito.
- Toda decisión física debe derivarse del modelo canónico y de las matrices de paridad (`109`, `110`).
- Las migraciones deben ser versionadas, deterministas y reproducibles.
- Las restricciones de base deben reforzar, no redefinir, las invariantes de dominio.
- La suite existente debe continuar completamente verde.
- Los tests de integración MariaDB deben agregarse sin sustituir los tests InMemory.

Pendientes reales que quedan bajo este bloque:

- Resolución de cada `DECISION_DDL-*` listada en `110` (incluye `DECISION_DDL-ENUM-01`: representación
  física de los enums persistibles sin `codigo()`).
- DDL MariaDB versionado (migraciones deterministas y reproducibles).
- Implementaciones JDBC de los puertos de repositorio de dominio.
- Transacciones por comando (una única transacción por operación de aplicación).
- Unicidades y FKs físicas (ver `109`, secciones 3 y 5).
- OCC/bloqueos multinodo (ver `109`, sección 12).
- Migración de datos existentes (si aplica) al esquema físico definitivo.

### Criterio de cierre del bloque DDL

Este bloque podrá cerrarse únicamente con:

- cada `DECISION_DDL-*` de `110` resuelta o explícitamente diferida con justificación;
- infraestructura MariaDB definida;
- configuración por entorno documentada;
- DDL inicial versionado;
- convenciones de nombres, tipos, claves e índices verificadas contra `110`;
- estrategia transaccional y de concurrencia documentada (ver `109`, secciones 6 y 12);
- compatibilidad con el modelo vigente demostrada;
- build completo exitoso;
- cero regresiones en la suite InMemory;
- sin cambios funcionales fuera del alcance autorizado.

## 3. Integraciones externas

Estas integraciones dependen de sistemas externos y no bloquean el diseño ni la
generación del DDL de MariaDB:

- Integración real con la aplicación de Firmas (más allá del callback `firmar-real` ya implementado; posible ampliación de flujo).
- Integración real con el sistema de Notificaciones (envío postal/electrónico real; hoy simulado con lotes y acuses mock).
- Integración real con Ingresos/Tesorería (hoy la conciliación económica usa un input mock absoluto de Tesorería).
- Storage y comprobantes reales (hoy `storageKey` usa esquemas `mock://`; sin `file://` ni `s3://` reales).
- Sincronización real de calendario administrativo con un proveedor externo (hoy sin integración; ver `10-domain/calendario-plazos-administrativos.md`, sección 14).
- Frontend productivo, cuando corresponda (hoy existe una demo navegable in-memory; ver `108-frontend-ready-demo.md`).

## 4. Mejoras no bloqueantes

- Revisión del mapeo `INSPECTOR_NO_ENCONTRADO` para `FirmanteNoEncontradoException` (ver `00-governance/command-contract-standard.md`, tabla de errores); requiere decisión explícita, no se corrige silenciosamente.
- Refinamiento de la clasificación `DECISION_DDL` por columna en `109-delta-modelo-mariadb-inmemory.md` a medida que avance el diseño físico.
- Decisión funcional pendiente sobre la seguridad de `pago-condena/confirmar`, `pago-condena/observar` y
  `GET pago-condena` (hoy `permitAll` en `SecurityConfig`; ver `05-api-core-endpoints.md`).

## 5. Regla de mantenimiento de este documento

Este documento contiene únicamente el trabajo pendiente vigente. No agregar
cronologías de bloques cerrados, resultados históricos extensos ni copias de
handoffs. Eliminar cada ítem de este documento en cuanto quede cerrado,
trasladando el cierre a `101-auditoria-pre-jdbc-mariadb.md` o al historial de Git.
