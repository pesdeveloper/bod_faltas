# [CAPA 09] SNAPSHOT OPERATIVO / BANDEJAS / VISTAS DE TRABAJO

## Finalidad de la capa

Esta capa modela la **lectura operativa resumida de la causa** para permitir trabajo diario, bandejas y consultas rápidas, sin tener que reconstruir todo desde:

- ActaEvento
- Documento
- Notificacion
- Presentacion
- Acto
- Recurso
- DerivacionExterna
- Integración económica

---

## Qué representa esta capa

Representa el **estado operativo actual resumido** de una causa.

Su objetivo es servir para:

- bandejas
- filtros
- listados
- lectura rápida
- priorización de trabajo

---

## Qué NO representa esta capa

Esta capa NO reemplaza:

- el historial real (`ActaEvento`)
- el flujo D1–D8
- los actos
- los documentos
- las notificaciones
- las presentaciones
- el recurso
- la derivación externa
- la lógica económica

👉 Solo resume para operar.

---

## Principios de diseño

### 1. Una sola entidad de snapshot

Se utiliza una única entidad:

- `ActaSnapshotOperativo`

No se crean tablas separadas por bandeja.

Las bandejas salen por consulta.

---

### 2. No reemplaza el modelo append-only

La fuente de verdad sigue siendo el modelo principal.

Este snapshot es una proyección operativa.

---

### 3. Debe ser simple

Solo debe guardar lo necesario para responder rápido:

- dónde está la causa
- qué tiene pendiente
- qué situación operativa presenta
- qué acción o bandeja corresponde

---

## ActaSnapshotOperativo

Entidad central de la capa.

Representa el resumen operativo actual de una causa.

---

## Qué debe resolver

Como mínimo, debe permitir responder:

- en qué etapa operativa está la causa
- si está en enriquecimiento
- si requiere notificación
- si tiene presentaciones pendientes
- si tiene acto pendiente de tratamiento posterior
- si tiene recurso abierto
- si está en tramo externo
- si tiene deuda
- si está cerrada o archivada
- cuál fue su último movimiento relevante

---

## Etapa operativa actual

La capa debe poder resumir la etapa operativa dominante de la causa.

Ejemplos de etapas resumidas:

- LABRADA
- EN_ENRIQUECIMIENTO
- EN_NOTIFICACION_ACTA
- EN_ANALISIS
- CON_ACTO
- EN_NOTIFICACION_ACTO
- EN_RECURSO
- EN_GESTION_EXTERNA
- EN_APREMIO
- CERRADA
- ARCHIVADA

Esto no reemplaza el flujo detallado.

Solo da lectura rápida.

---

## Banderas operativas útiles

Ejemplos de banderas simples:

- `TienePresentacionesPendientes`
- `TieneActoRegistrado`
- `TieneNotificacionPendiente`
- `TieneRecursoAbierto`
- `TieneDerivacionExternaAbierta`
- `TieneDeuda`
- `TienePlanPagos`
- `TieneResultadoExternoPendienteProcesar`

No deben ser demasiadas.  
Solo las que realmente ayuden a operar.

---

## Bandejas

Las bandejas no se modelan como tablas separadas.

Salen por consulta sobre el snapshot.

Ejemplos de bandejas:

- en enriquecimiento
- pendientes de notificación de acta
- con presentaciones pendientes
- con acto pendiente de notificación
- con recurso abierto
- en gestión externa
- con resultado externo pendiente de procesar
- con deuda
- archivadas / cerradas

---

## Relación con el resto del modelo

`ActaSnapshotOperativo` se alimenta a partir de:

- Capa 01 → Acta / ActaEvento
- Capa 02 → Documento
- Capa 03 → Notificacion
- Capa 04 → ActaPresentacion
- Capa 05 → ActaActo
- Capa 06 → SujBieFaltas / integración económica
- Capa 07 → ActaRecurso
- Capa 08 → ActaDerivacionExterna

---

## Actualización del snapshot

El snapshot debe actualizarse a partir de eventos o procesos del sistema.

No debe ser editado manualmente como fuente principal de negocio.

Su función es de proyección operativa.

---

## Qué NO debe hacer la capa

No debe:

- convertirse en fuente de verdad
- reemplazar eventos
- duplicar detalle documental
- duplicar detalle económico
- duplicar historial
- resolver reglas complejas por sí sola

---

## Qué debe responder la capa

- cuál es la etapa actual resumida
- cuál es la situación operativa actual
- qué banderas operativas están activas
- en qué bandeja debería aparecer la causa
- cuál fue la última fecha operativa relevante

---

## Regla clave

👉 Capa 09 existe para **operar**, no para definir la verdad del dominio.

---

## Cierre

Esta capa completa el modelo permitiendo pasar de un sistema bien modelado a un sistema realmente utilizable por operadores.

Su función es concreta:

👉 **resumir la causa para trabajo diario, bandejas y vistas operativas**