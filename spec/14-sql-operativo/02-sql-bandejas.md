# [14-SQL-OPERATIVO] 02 - SQL DE BANDEJAS

## Finalidad

Este archivo define el enfoque operativo para las consultas SQL de bandejas del sistema.

La regla general es:

- las bandejas deben apoyarse preferentemente en `FalActaSnapshot`
- el detalle profundo se resuelve fuera de la bandeja
- la bandeja devuelve solo información operativa para decidir y abrir el caso

---

## Reglas generales

### Tabla base principal

Salvo excepción justificada, la base primaria será:

- `FalActaSnapshot`

### Joins mínimos esperados

Podrán incorporarse joins controlados a:

- `FalActa`
- `FalDependenciaVersion`
- `FalInspectorVersion`
- `FalDocumento`
- `FalNotificacion`

### Shape general esperado

Cada fila debería poder devolver, según corresponda:

- `IdActa`
- `NroActa`
- `TipoActa`
- `BloqueActual`
- `EstadoProcesoActual`
- `SituacionAdministrativaActual`
- `FechaHoraUltimoEvento`
- `FechaHoraCreacion`
- `TienePendienteMaterial`
- `TieneMedidaPreventivaActiva`
- `TieneDocumentosPendientesFirma`
- `TieneNotificacionEnCurso`
- `PrioridadOperativa`

---

## Lógica de campos derivados

Varios campos del shape de bandeja no deben quedar librados a interpretación en cada consulta.

La decisión operativa recomendada es:

- estos campos deben existir como datos proyectados o persistidos en `FalActaSnapshot`
- no deben recalcularse completamente en cada query de bandeja
- su lógica debe mantenerse consistente con el proceso de eventos y reproyección

### `TienePendienteMaterial`

Debe indicar si el acta requiere una acción material u operativa pendiente para poder avanzar en el flujo.

Puede depender, según el caso, de:

- estado documental
- estado de notificación
- faltantes operativos
- requisitos previos no cumplidos
- flags de negocio proyectados

### `TieneMedidaPreventivaActiva`

Debe indicar si el acta posee alguna medida preventiva vigente no levantada.

### `TieneDocumentosPendientesFirma`

Debe indicar si existen documentos requeridos que todavía no están firmados o consolidados documentalmente.

### `TieneNotificacionEnCurso`

Debe indicar si existe una notificación activa, abierta o en progreso que impacta la operación actual.

### `PrioridadOperativa`

Debe ser un dato resumido que permita ordenar bandejas por urgencia o relevancia operativa.

La lógica exacta puede responder a combinación de factores, por ejemplo:

- medida preventiva activa
- pendiente material
- vencimientos o antigüedad
- estado de notificación
- urgencia administrativa

### Regla de implementación

Estos campos deben resolverse de forma consistente en el módulo de proyecciones y mantenerse alineados con:

- `FalActaEvento`
- estados operativos
- situación administrativa
- estado documental
- estado de notificación

---

## Orden recomendado

1. prioridad operativa descendente
2. fecha/hora del último evento o movimiento relevante descendente
3. `IdActa` como desempate estable

La paginación debe ser estable y compatible con Informix 12.10.

---

## Bandejas y criterio de filtro

Cada bandeja debe definirse por combinación de:

- `BloqueActual`
- `EstadoProcesoActual`
- `SituacionAdministrativaActual`
- flags operativos proyectados en `FalActaSnapshot`
- punteros documentales o de notificación cuando correspondan

Este archivo no congela todavía el `WHERE` literal final de cada bandeja, pero sí deja fijado el criterio operativo que deberá respetarse.

### 1. Actas Labradas / Revisión Inicial

Debe incluir actas recién labradas o todavía pendientes de revisión inicial.

Criterio principal:
- bloque o estado compatible con inicio del trámite
- sin cierre
- sin archivo definitivo

### 2. Enriquecimiento

Debe incluir actas cuyo bloque actual sea enriquecimiento o que requieran completar, revisar o consolidar información posterior al labrado.

Criterio principal:
- bloque actual de enriquecimiento
- necesidad de completar información operativa o de contexto

### 3. Preparación para notificación del acta

Debe incluir actas que ya no están en labrado puro, pero todavía requieren pasos previos antes de quedar listas para notificar.

Criterio principal:
- faltantes documentales, materiales o administrativos previos a notificación
- presencia de pendientes impeditivos

### 4. Actas listas para notificar

Debe incluir actas que ya cumplieron requisitos previos y pueden ingresar al circuito de notificación.

Criterio principal:
- sin pendientes impeditivos
- requisitos previos completos
- condiciones documentales o administrativas cumplidas

### 5. Notificación del acta en proceso

Debe incluir actas con notificación abierta, activa o en curso.

Criterio principal:
- `TieneNotificacionEnCurso = 1`
- o estado compatible con notificación activa

### 6. Análisis de presentaciones / pagos

Debe incluir actas con presentaciones, pagos o movimientos que requieren análisis o decisión posterior.

Criterio principal:
- existencia de disparador material o administrativo a analizar
- necesidad de intervención del área

### 7. Pendiente de acto administrativo

Debe incluir actas que requieran decisión formal, acto o resolución administrativa.

Criterio principal:
- estado procesal que exige acto pendiente
- ausencia de acto aún no producido

### 8. Acto administrativo en proceso

Debe incluir actas cuyo acto administrativo esté siendo preparado, redactado, firmado o completado.

Criterio principal:
- proceso documental/administrativo de acto aún abierto

### 9. Pendiente de notificación de acto

Debe incluir actas con acto ya producido pero todavía no notificado.

Criterio principal:
- acto existente
- notificación del acto todavía no iniciada o no completada

### 10. Notificación de acto en proceso

Debe incluir actas con notificación del acto abierta o en curso.

Criterio principal:
- proceso de notificación del acto vigente

### 11. Apelación

Debe incluir actas con trámite recursivo o de apelación activo.

Criterio principal:
- bloque o estado compatible con apelación
- sin resolución definitiva del recurso

### 12. Gestión externa

Debe incluir actas derivadas a circuitos externos o con seguimiento fuera del núcleo principal.

Criterio principal:
- bloque o situación compatible con intervención externa
- necesidad de seguimiento o control posterior

### 13. Paralizadas

Debe incluir actas con situación administrativa de paralización vigente.

Criterio principal:
- situación administrativa paralizada
- sin cierre definitivo

### 14. Cerradas

Debe incluir actas cerradas, archivadas o finalizadas según la lógica vigente del dominio.

Criterio principal:
- situación administrativa o estado final compatible con cierre
- sin operación activa pendiente en bandeja principal

---

## Regla de detalle

La bandeja no debe reconstruir el expediente completo.

El detalle del caso debe resolverse después, con lecturas específicas.

La query de bandeja debe priorizar:

- lectura rápida
- filtros claros
- orden estable
- shape corto
- uso directo del snapshot y joins mínimos

## Nota de alcance

Este archivo define:

- principios de diseño
- shape esperado
- campos derivados proyectados
- criterio operativo de cada bandeja

No congela todavía el `WHERE` SQL literal final de cada bandeja.

La definición exacta de filtros SQL sobre:

- `BloqueActual`
- `EstadoProcesoActual`
- `SituacionAdministrativaActual`
- flags proyectados

deberá documentarse en una etapa posterior, una vez cerrada por completo la codificación final de `FalActaSnapshot` y sus enumeraciones operativas.

Mientras tanto, este archivo debe tomarse como la referencia funcional y operativa para el diseño de bandejas.
