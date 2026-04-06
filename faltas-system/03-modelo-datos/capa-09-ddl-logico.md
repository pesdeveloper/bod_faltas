# [ANEXO] DDL LÓGICO — CAPA 09 — SNAPSHOT OPERATIVO / BANDEJAS / VISTAS DE TRABAJO

> Este anexo resume la definición lógica de tablas, campos y tipos de datos de la Capa 09.  
> No representa aún el SQL físico final ni incluye la totalidad de índices, constraints ni detalles de implementación.

---

# 1) FINALIDAD DE LA CAPA

La Capa 09 modela la proyección operativa resumida de la causa.

Su objetivo es permitir:

- bandejas
- filtros
- listados
- lectura rápida
- priorización de trabajo

sin tener que reconstruir cada vez el estado de la causa desde todas las capas del dominio.

---

# 2) CRITERIOS DE DISEÑO CONSOLIDADOS

## 2.1 Una sola entidad de snapshot

Se adopta una única entidad base:

- `ActaSnapshotOperativo`

No se crean tablas separadas por bandeja.

Las bandejas se obtienen por consulta.

---

## 2.2 No es fuente de verdad

La fuente de verdad sigue siendo el modelo principal:

- Acta
- ActaEvento
- Documento
- Notificacion
- Presentacion
- Acto
- Integración económica
- Recurso
- Derivación externa

El snapshot solo resume.

---

## 2.3 Proyección operativa simple

La capa debe guardar solo lo necesario para operar.

No debe duplicar detalle innecesario.

---

# 3) ENTIDAD DE LA CAPA

La Capa 09 queda compuesta, en principio, por una única entidad principal:

- `ActaSnapshotOperativo`

---

# 4) TABLA: ActaSnapshotOperativo

## 4.1 Finalidad

Representa el estado operativo resumido actual de una causa.

Sirve como base para:

- bandejas
- filtros
- listados
- lectura operativa

---

## 4.2 PK

- `PK_ActaSnapshotOperativo (IdActa)`

> Observación:
> Se propone usar `IdActa` como PK porque el snapshot es uno por causa.

---

## 4.3 FK

- `FK_ActaSnapshotOperativo_Acta (IdActa -> Acta.Id)`

---

## 4.4 Índices sugeridos

- `IX_ActaSnapshotOperativo_EtapaOperativaActual`
- `IX_ActaSnapshotOperativo_FechaUltimoMovimiento`
- `IX_ActaSnapshotOperativo_TienePresentacionesPendientes`
- `IX_ActaSnapshotOperativo_TieneNotificacionPendiente`
- `IX_ActaSnapshotOperativo_TieneRecursoAbierto`
- `IX_ActaSnapshotOperativo_TieneDerivacionExternaAbierta`
- `IX_ActaSnapshotOperativo_TieneDeuda`

---

## 4.5 Campos

### 4.5.1 Identidad

- `IdActa` → INT8 NOT NULL  
  Identificador del acta al que corresponde el snapshot operativo.

---

### 4.5.2 Etapa operativa actual

- `EtapaOperativaActual` → SMALLINT NOT NULL  
  Etapa operativa resumida actual de la causa.

  Valores lógicos admitidos:

  - 1 = LABRADA
  - 2 = EN_ENRIQUECIMIENTO
  - 3 = EN_NOTIFICACION_ACTA
  - 4 = EN_ANALISIS
  - 5 = CON_ACTO
  - 6 = EN_NOTIFICACION_ACTO
  - 7 = EN_RECURSO
  - 8 = EN_GESTION_EXTERNA
  - 9 = EN_APREMIO
  - 10 = CERRADA
  - 11 = ARCHIVADA

---

### 4.5.3 Banderas operativas

- `TienePresentacionesPendientes` → SMALLINT NOT NULL DEFAULT 0  
  Valores:
  - 0 = NO
  - 1 = SI

- `TieneActoRegistrado` → SMALLINT NOT NULL DEFAULT 0  
  Valores:
  - 0 = NO
  - 1 = SI

- `TieneNotificacionPendiente` → SMALLINT NOT NULL DEFAULT 0  
  Valores:
  - 0 = NO
  - 1 = SI

- `TieneRecursoAbierto` → SMALLINT NOT NULL DEFAULT 0  
  Valores:
  - 0 = NO
  - 1 = SI

- `TieneDerivacionExternaAbierta` → SMALLINT NOT NULL DEFAULT 0  
  Valores:
  - 0 = NO
  - 1 = SI

- `TieneDeuda` → SMALLINT NOT NULL DEFAULT 0  
  Valores:
  - 0 = NO
  - 1 = SI

- `TienePlanPagos` → SMALLINT NOT NULL DEFAULT 0  
  Valores:
  - 0 = NO
  - 1 = SI

- `TieneResultadoExternoPendienteProcesar` → SMALLINT NOT NULL DEFAULT 0  
  Valores:
  - 0 = NO
  - 1 = SI

---

### 4.5.4 Fechas operativas

- `FechaUltimoMovimiento` → DATETIME YEAR TO SECOND NULL  
  Última fecha operativa relevante conocida para la causa.

---

### 4.5.5 Trazabilidad mínima del snapshot

- `FechaActualizacion` → DATETIME YEAR TO SECOND NOT NULL  
  Fecha/hora de última actualización del snapshot.

- `OrigenRegistro` → SMALLINT NOT NULL  
  Origen técnico/operativo de la actualización.

  Valores previstos:

  - 1 = MANUAL
  - 2 = SISTEMA
  - 3 = PROCESO
  - 4 = INTEGRACION

---

## 4.6 Reglas lógicas

### Regla 1
Debe existir como máximo un snapshot operativo por `Acta`.

### Regla 2
El snapshot no reemplaza la fuente de verdad del dominio.

### Regla 3
Las banderas operativas deben derivarse del modelo principal y no mantenerse manualmente como regla general.

### Regla 4
`EtapaOperativaActual` debe expresar lectura resumida, no detalle completo del flujo.

### Regla 5
Las bandejas deben salir por consulta sobre esta tabla, no por tablas separadas.

---

## 4.7 Observaciones de diseño

### Observación A
Se usa una sola entidad para evitar proliferación de tablas operativas.

### Observación B
Las banderas deben mantenerse en número reducido, solo si aportan valor real de operación.

### Observación C
La capa existe para mejorar operación y performance de lectura, no para redefinir el dominio.

---

# 5) INTEGRIDAD CRUZADA DE LA CAPA

## 5.1 Coherencia con Acta

- `ActaSnapshotOperativo.IdActa` debe referir a una `Acta` existente.

---

## 5.2 Coherencia con el resto del modelo

El snapshot debe alimentarse a partir de:

- Capa 01 → Acta / ActaEvento
- Capa 02 → Documento
- Capa 03 → Notificacion
- Capa 04 → ActaPresentacion
- Capa 05 → ActaActo
- Capa 06 → SujBieFaltas / integración económica
- Capa 07 → ActaRecurso
- Capa 08 → ActaDerivacionExterna

---

# 6) CATÁLOGOS / ENUMS LÓGICOS DE LA CAPA

## 6.1 EtapaOperativaActual

- 1 = LABRADA
- 2 = EN_ENRIQUECIMIENTO
- 3 = EN_NOTIFICACION_ACTA
- 4 = EN_ANALISIS
- 5 = CON_ACTO
- 6 = EN_NOTIFICACION_ACTO
- 7 = EN_RECURSO
- 8 = EN_GESTION_EXTERNA
- 9 = EN_APREMIO
- 10 = CERRADA
- 11 = ARCHIVADA

---

## 6.2 OrigenRegistro

- 1 = MANUAL
- 2 = SISTEMA
- 3 = PROCESO
- 4 = INTEGRACION

---

# 7) REGLAS DE NEGOCIO TRANSVERSALES SUGERIDAS

## Regla A
La etapa operativa actual debe reflejar la situación dominante de la causa para trabajo diario.

## Regla B
Las banderas operativas deben existir solo si realmente ayudan a armar bandejas y consultas.

## Regla C
La actualización del snapshot debería producirse por procesos automáticos o proyecciones derivadas del modelo principal.

## Regla D
La capa 09 no reemplaza ninguna capa de dominio; solo facilita operación.

---

# 8) CONSULTAS OPERATIVAS QUE LA CAPA DEBE HABILITAR

La capa debe permitir consultas como:

- causas en enriquecimiento
- causas para notificar acta
- causas con presentaciones pendientes
- causas con acto pendiente de tratamiento posterior
- causas con recurso abierto
- causas en gestión externa
- causas en apremio
- causas con deuda
- causas cerradas
- causas archivadas

---

# 9) CIERRE DEL ANEXO

La Capa 09 queda lógicamente definida como una capa simple de proyección operativa, basada en una única entidad:

- `ActaSnapshotOperativo`

Su función es concreta:

- resumir la situación actual de la causa
- facilitar bandejas y vistas de trabajo
- evitar reconstrucciones complejas para operación diaria

👉 Capa 09 no define la verdad del dominio: **la resume para operar**.