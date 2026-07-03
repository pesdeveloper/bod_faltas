# Fuentes base — Faltas / Dirección de Faltas

Esta carpeta contiene documentos fuente obligatorios para el backend productivo de Faltas.

## Documentos base

### 1. Modelo MariaDB final

Archivo:

`MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md`

Rol:
- Fuente estructural del modelo MariaDB productivo.
- Define tablas, campos, convenciones técnicas, catálogos funcionales, eventos, snapshots y reglas de persistencia futura.
- Debe consultarse antes de crear o modificar entidades, enums, catálogos, repositorios, persistencia JDBC/MariaDB o scripts SQL.

### 2. Matriz de proceso

Archivo:

`MATRIZ_PROCESO_FALTAS_CIERRE_COMPLETA_2026-06-23.md`

Rol:
- Fuente funcional del motor de proceso.
- Define acciones, precondiciones, eventos, transiciones, snapshot esperado, cerrabilidad y restricciones.
- Debe consultarse antes de crear o modificar servicios de dominio, comandos, eventos, snapshot, bandejas, acciones pendientes o tests de flujo.

## Regla de prioridad

Cuando haya dudas o conflicto, usar esta jerarquía:

1. Decisiones cerradas explícitas posteriores, si están documentadas en spec-as-source o delta aprobado.
2. `docs/faltas/MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md`
3. `docs/faltas/MATRIZ_PROCESO_FALTAS_CIERRE_COMPLETA_2026-06-23.md`
4. `backend/api-faltas-core/docs/spec-as-source/` (spec viva)
5. Tests ejecutables actuales.
6. Código in-memory actual.
7. Prototipo/Angular sólo como evidencia UX, nunca como fuente de dominio.

## Política de cambios

Los documentos base no deben editarse de forma oportunista en cada slice.

Si la implementación in-memory descubre una corrección necesaria:
1. documentarla primero en `DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md`;
2. agregar o ajustar tests guardrail;
3. revisar la decisión;
4. recién después generar una nueva versión del modelo MariaDB o actualizar scripts SQL futuros.

## Regla para agentes Cursor/Codex

Antes de implementar cualquier slice de dominio, revisar:

- este README;
- el modelo MariaDB base;
- la matriz de proceso;
- `backend/api-faltas-core/docs/spec-as-source/`;
- `backend/api-faltas-core/docs/spec-as-source/99-pendientes-siguientes-slices.md`.

No inventar eventos, bloques, estados, bandejas, acciones ni catálogos que no estén justificados por estas fuentes.
