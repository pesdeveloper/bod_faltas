# [CAPA 04] — PRESENTACIONES CONSOLIDADAS

---

# 1. FINALIDAD DE LA CAPA

Esta capa modela las **interacciones del administrado o terceros con el Acta**.

Incluye:

* descargos
* notas
* solicitudes
* aportes documentales
* manifestaciones
* presentaciones espontáneas

---

# 2. PRINCIPIO CENTRAL

Las presentaciones:

## no son un agregado independiente del modelo

Se representan mediante:

## ActaEvento + Documento + Snapshot

---

# 3. MODELO DE PRESENTACIONES

## 3.1 Evento

Cada presentación relevante se registra como evento en `ActaEvento`.

---

### Ejemplos de eventos

* DESCARGO_PRESENTADO
* NOTA_PRESENTADA
* DOCUMENTACION_APORTADA
* SOLICITUD_REALIZADA

---

## 3.2 Documento (opcional)

Si la presentación incluye soporte documental:

* se crea un Documento
* se vincula mediante ActaDocumento

---

### Ejemplos

* escrito de descargo
* nota firmada
* archivo adjunto
* documentación probatoria

---

## 3.3 Snapshot (opcional)

El sistema puede proyectar:

* TieneDescargo
* TienePresentaciones
* TieneDocumentacionAdjunta

---

# 4. DATOS DE LA PRESENTACIÓN

Los datos relevantes de la presentación se distribuyen así:

## En el evento

* tipo de presentación
* fecha
* origen (infractor, abogado, etc.)
* observación resumida

---

## En el documento (si existe)

* contenido completo
* archivo
* firma (si aplica)

---

## Regla

No se crea una entidad adicional para almacenar estos datos.

---

# 5. REGLAS DE DISEÑO

## 5.1 No crear entidad ActaPresentacion

Las presentaciones no se modelan como tabla independiente.

---

## 5.2 Evitar duplicación

No duplicar datos entre:

* evento
* documento
* snapshot

---

## 5.3 Mantener simplicidad

Una presentación debe poder leerse como:

* un evento
* opcionalmente un documento

---

# 6. RELACIÓN CON EL PROCESO

Las presentaciones:

* pueden modificar el recorrido del Acta
* pueden influir en decisiones posteriores
* pueden generar nuevos eventos

Pero no abren subprocesos independientes.

---

# 7. RESULTADO DE LA CAPA

Esta capa permite:

* registrar interacciones del administrado
* mantener el modelo simple
* evitar sobre-modelado
* integrar naturalmente con eventos y documentos

---

## Resumen

Las presentaciones son:

## parte del viaje del Acta

## no una estructura paralela

Se representan como:

## evento + documento + proyección

---
