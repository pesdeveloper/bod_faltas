# 99 — Próximos slices vigentes

**Fecha de actualización:** 2026-07-10
**Estado:** frontera pre-MariaDB preparada.

Este documento contiene únicamente el trabajo pendiente vigente. La evolución histórica se conserva en Git y no forma parte de la spec-as-source activa.

## 1. Baseline vigente

El modelo funcional InMemory está cerrado y actúa como oráculo de paridad para la persistencia.

Estado verificado:

- modelo de dominio y lifecycle estabilizados;
- paridad funcional InMemory cerrada;
- `FaltasClock` como abstracción temporal canónica;
- concurrencia optimista InMemory estabilizada;
- contrato global de errores HTTP cerrado mediante `ErrorResponse` y `GlobalFaltasControllerAdvice`;
- suite completa: **2478 tests, 0 failures, 0 errors, BUILD SUCCESS**;
- spec-as-source activa saneada en UTF-8;
- infraestructura JDBC base incorporada: dependencia Spring JDBC, driver MariaDB, perfil `jdbc`, configuración de `DataSource` y prueba condicionada de infraestructura;
- sin repositorios JDBC de dominio, migraciones versionadas, DDL productivo, Flyway/Liquibase, Testcontainers ni JPA/Hibernate todavía.

Las definiciones activas de paridad y estrategia de persistencia se encuentran en:

- `110-matriz-maestra-paridad-mariadb-inmemory.md`
- `102-slice-9-estrategia-jdbc-mariadb.md`

## 2. Próximo slice autorizado

### 8F-12A / Slice 9 — Infraestructura y DDL MariaDB

Objetivo:

Incorporar la infraestructura inicial y el DDL versionado de MariaDB sin alterar el comportamiento funcional ya validado por la implementación InMemory.

Guardrails:

- InMemory continúa siendo el oráculo de paridad.
- No cambiar reglas de dominio para adaptarlas a la persistencia.
- No cambiar estados, eventos, transiciones, bandejas ni contratos HTTP.
- No introducir JPA si la estrategia vigente define JDBC explícito.
- Toda decisión física debe derivarse del modelo canónico y de la matriz de paridad.
- Las migraciones deben ser versionadas, deterministas y reproducibles.
- Las restricciones de base deben reforzar, no redefinir, las invariantes de dominio.
- La suite existente debe continuar completamente verde.
- Los tests de integración MariaDB deben agregarse sin sustituir los tests InMemory.

## 3. Criterio de cierre de 8F-12A

El slice podrá cerrarse únicamente con:

- infraestructura MariaDB definida;
- configuración por entorno documentada;
- DDL inicial versionado;
- convenciones de nombres, tipos, claves e índices verificadas;
- estrategia transaccional y de concurrencia documentada;
- compatibilidad con el modelo vigente demostrada;
- build completo exitoso;
- cero regresiones en la suite InMemory;
- handoff de cierre generado;
- sin cambios funcionales fuera del alcance autorizado.

## 4. Trabajo posterior

Los siguientes slices se definirán al cerrar 8F-12A, de acuerdo con la estrategia establecida en `102-slice-9-estrategia-jdbc-mariadb.md`.

No agregar en este archivo cronologías de slices cerrados, resultados históricos extensos ni copias de handoffs.
