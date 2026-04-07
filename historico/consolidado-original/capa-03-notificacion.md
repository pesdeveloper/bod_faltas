# [CAPA 03] — NOTIFICACIÓN CONSOLIDADA

---

# 1. FINALIDAD DE LA CAPA

Esta capa modela el proceso de **notificación del Acta**.

Es responsable de:

* gestionar el envío de notificaciones
* registrar intentos de entrega
* almacenar información de acuse
* permitir seguimiento del estado de entrega

---

# 2. PRINCIPIO CENTRAL

La notificación es:

## un proceso operativo del viaje del Acta

## con entidad propia, pero sin submodelo documental paralelo

---

# 3. ENTIDAD PRINCIPAL

## 3.1 Notificacion

### Descripción

Representa una instancia de envío de un mensaje o documento hacia el destinatario del Acta.

---

### Responsabilidades

* registrar el intento de notificación
* almacenar datos del canal y destino
* registrar resultados de acuse
* permitir reintentos

---

### Contiene

* Id
* IdActa
* Canal (postal, electrónico, etc.)
* Destinatario / Dirección
* FechaEnvio
* EstadoOperativo (simple)
* CantidadIntentos

---

### Datos de resultado (si aplica)

* FechaAcuse
* ResultadoAcuse (RECIBIDO / RECHAZADO)
* MotivoRechazo (opcional)

---

# 4. RELACIÓN CON DOCUMENTOS

## Regla fundamental

No existe un modelo documental específico de notificación.

---

### Asociación de documentos

Los documentos vinculados a una notificación se modelan mediante:

* Documento
* ActaDocumento

---

### Ejemplos

* cédula
* carta documento
* notificación electrónica
* comprobante de envío

---

# 5. RELACIÓN CON EVENTOS

La notificación se refleja en `ActaEvento` mediante eventos relevantes.

---

## Eventos válidos

* NOTIFICACION_ENVIADA
* ACUSE_RECIBIDO
* ACUSE_RECHAZADO
* NOTIFICACION_REENVIADA

---

## Reglas

* los eventos representan hechos reales
* no representan pasos internos
* no representan estados derivados

---

## Ejemplo de flujo

1. NOTIFICACION_ENVIADA
2. ACUSE_RECHAZADO
3. NOTIFICACION_REENVIADA
4. ACUSE_RECIBIDO

---

# 6. RELACIÓN CON SNAPSHOT

El estado operativo del Acta puede proyectar:

* NotificacionPendiente
* NotificacionRecibida
* NotificacionFallida
* TieneReintentos

---

## Regla

El snapshot:

* deriva de eventos
* no se persiste como verdad primaria

---

# 7. REGLAS DE DISEÑO

## 7.1 No duplicar lógica

* el estado no debe vivir en múltiples lugares
* evitar lógica paralela entre entidad y snapshot

---

## 7.2 Notificacion es operativa, no documental

* no representa documentos
* no reemplaza Documento

---

## 7.3 Notificacion no es evento

* es contexto del evento
* el evento describe lo ocurrido
* la entidad guarda los datos

---

# 8. RESULTADO DE LA CAPA

Esta capa permite:

* modelar correctamente el envío de notificaciones
* registrar resultados reales
* integrar con el modelo de eventos
* evitar duplicaciones documentales

---

## Resumen

Esta capa define:

## cómo el Acta se comunica con el mundo externo

y permite responder:

* si se notificó
* cuándo se notificó
* si fue recibido o rechazado
* si hubo reintentos

---
