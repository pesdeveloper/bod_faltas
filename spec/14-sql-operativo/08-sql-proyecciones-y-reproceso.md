# [14-SQL-OPERATIVO] 08 - SQL DE PROYECCIONES Y REPROCESO

## Finalidad

Este archivo define el enfoque SQL para mantenimiento, actualización y reproceso de proyecciones operativas, principalmente `FalActaSnapshot`.

---

## Tabla principal

- `FalActaSnapshot`

---

## Objetivos de la proyección

La proyección debe permitir:

- operar bandejas
- mostrar estado actual resumido
- evitar reconstrucción histórica completa en cada listado
- reflejar punteros o flags útiles para operación inmediata

---

## Tipos de operación

### 1. Inicialización de snapshot

Cuando nace una nueva `FalActa`, debe crearse o inicializarse su snapshot mínimo.

### 2. Actualización incremental en línea

Cuando una operación impacta un dato visible para la operación inmediata, puede actualizarse la porción mínima del snapshot dentro de la misma transacción.

Ejemplos:

- nuevo evento relevante
- cambio de bloque o estado operativo
- documento pendiente de firma / firmado
- notificación abierta / cerrada

### 3. Reproceso completo

Cuando sea necesario recomponer consistencia global, puede ejecutarse un reproceso que relea:

- `FalActa`
- `FalActaEvento`
- `FalDocumento`
- `FalDocumentoFirma`
- `FalNotificacion`
- tablas satélite relevantes

y regenere el snapshot derivado.

---

## Reglas

- no recalcular todo siempre en línea
- no dejar el snapshot sin estrategia
- documentar qué campos se mantienen incrementalmente y cuáles pueden regenerarse

---

## Consultas y tareas esperadas

- obtener snapshot por `IdActa`
- regenerar snapshot de una acta puntual
- regenerar snapshots por rango o lote
- recalcular flags documentales
- recalcular flags de notificación
- recalcular pendientes materiales

---

## Criterio operativo de actualización

### Actualización incremental en línea

Debe aplicarse solo cuando el cambio impacta directamente la operación inmediata y puede resolverse sin releer todo el expediente.

Casos típicos:

- alta inicial de acta
- nuevo evento con impacto en bloque o estado actual
- alta o cambio documental que afecte flags visibles
- apertura o cierre de notificación
- cambios en pendientes materiales o medidas preventivas activas

### Reproceso completo

Debe aplicarse cuando:

- exista riesgo de inconsistencia entre snapshot y fuente de verdad
- cambie una regla de derivación
- se deba recalcular una acta puntual o un lote
- una operación histórica no haya reproyectado correctamente

### Regla de consistencia

En caso de discrepancia, la fuente de verdad debe ser siempre:

- `FalActa`
- `FalActaEvento`
- tablas documentales
- tablas de notificación
- satélites relevantes

`FalActaSnapshot` debe poder regenerarse completamente a partir de esas fuentes.

### Regla de idempotencia

El reproceso de una misma acta debe poder ejecutarse más de una vez sin producir duplicación ni degradación del estado proyectado.

---