# [CAPA 05] — ACTOS ADMINISTRATIVOS CONSOLIDADOS

---

# 1. FINALIDAD DE LA CAPA

Esta capa modela las **decisiones administrativas** que afectan el recorrido del Acta.

Incluye:

* fallos
* resoluciones
* disposiciones

---

# 2. PRINCIPIO CENTRAL

Los actos administrativos:

## no se modelan como entidad independiente

Se representan mediante:

## ActaEvento + Documento + Snapshot

---

# 3. MODELO DE ACTOS

## 3.1 Evento

Cada acto relevante se registra como un evento en `ActaEvento`.

---

### Ejemplos de eventos

* FALLO_DICTADO
* RESOLUCION_EMITIDA
* DISPOSICION_APLICADA

---

## 3.2 Documento (obligatorio)

Todo acto administrativo tiene soporte documental.

---

### Ejemplos

* fallo firmado
* resolución administrativa
* disposición

---

## Regla

El documento:

* representa formalmente el acto
* contiene el contenido completo
* puede estar firmado

---

## 3.3 Snapshot

El acto impacta directamente el estado del Acta.

---

### Ejemplos

* EstadoFinal = CONDENA
* EstadoFinal = ABSOLUCION
* TieneResolucion

---

# 4. DATOS DEL ACTO

Los datos se distribuyen así:

## En el evento

* tipo de acto
* fecha
* autoridad que decide
* observación resumida

---

## En el documento

* contenido completo
* fundamentos
* firma

---

## Regla

No se crea entidad adicional para el acto.

---

# 5. RELACIÓN CON EL PROCESO

Los actos:

* determinan el rumbo del Acta
* pueden cerrar el proceso
* pueden generar nuevos eventos (ej: notificación)

---

# 6. REGLAS DE DISEÑO

## 6.1 No crear entidad ActaActo

El acto no se modela como tabla independiente.

---

## 6.2 No duplicar información

Evitar repetir datos entre:

* evento
* documento
* snapshot

---

## 6.3 Documento obligatorio

Todo acto debe tener documento asociado.

---

# 7. RESULTADO DE LA CAPA

Esta capa permite:

* representar decisiones administrativas
* mantener el modelo simple
* evitar submodelos innecesarios
* integrar naturalmente con eventos y documentos

---

## Resumen

Los actos administrativos son:

## decisiones dentro del viaje del Acta

Se representan como:

## evento + documento + proyección

---
