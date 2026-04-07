# 00 — MODELO GENERAL CONSOLIDADO

## Sistema de Faltas — Enfoque Event-Driven (Spec-as-Source)

---

# 1. PRINCIPIO FUNDAMENTAL

El sistema modela el **viaje del Acta**.

El Acta representa un trámite administrativo que recorre un proceso no lineal (D1–D8), atravesando:

* interacciones
* decisiones
* documentos
* notificaciones
* posibles derivaciones
* y un cierre final

El objetivo del modelo es:

> representar de forma clara, compacta y consistente la historia, el estado actual y el soporte documental del Acta.

---

# 2. ESTRUCTURA CENTRAL DEL MODELO

El modelo se organiza en cuatro piezas principales:

## 2.1 Acta (Entidad central)

Representa el trámite.

Contiene:

* identidad del caso
* datos estructurales mínimos
* referencias necesarias

No contiene:

* historia
* lógica derivada
* estado operativo complejo

---

## 2.2 ActaEvento (Narrativa procesal)

Es la **bitácora del viaje del Acta**.

Registra únicamente:

* hechos relevantes del proceso
* interacciones significativas
* decisiones
* resultados
* cambios sustanciales en el recorrido

No registra:

* eventos técnicos
* operaciones internas del sistema
* logs de infraestructura

### Regla clave

Un evento pertenece a ActaEvento solo si:

* forma parte del recorrido del Acta
* impacta su estado o trayectoria
* es relevante para entender el caso

---

## 2.3 Documento (Soporte formal)

Representa la materialización documental de hechos del proceso.

Incluye:

* documentos generados
* documentos firmados
* documentos subidos
* documentos externos incorporados

Es único modelo documental del sistema.

---

## 2.4 ActaSnapshotOperativo (Estado actual)

Es una proyección del estado actual del Acta.

Sirve para:

* bandejas
* consultas rápidas
* filtros operativos

No reemplaza:

* la historia (ActaEvento)
* el dominio

---

# 3. RELACIONES CLAVE

## 3.1 Acta ↔ Evento

Relación 1:N
Define la historia completa del caso.

---

## 3.2 Acta ↔ Documento (vía ActaDocumento)

`ActaDocumento` es el vínculo universal entre:

* Acta
* Documento

Permite:

* evitar submodelos documentales paralelos
* asociar documentos a cualquier contexto funcional

---

## 3.3 Documento ↔ DocumentoFirma

Relación 1:1 opcional

Solo existe cuando el documento requiere firma.

---

# 4. MODELO DOCUMENTAL UNIFICADO

No existen modelos documentales específicos por proceso.

No se permiten entidades como:

* NotificacionDocumento
* ActoDocumento
* etc.

Todo documento del sistema se modela mediante:

* Documento
* ActaDocumento

---

# 5. REGLA DE EVENTOS

## 5.1 Qué es un evento válido

Ejemplos:

* ACTA_LABRADA
* ACTA_ENRIQUECIDA
* DESCARGO_PRESENTADO
* PAGO_REGISTRADO
* APELACION_PRESENTADA
* NOTIFICACION_ENVIADA
* ACUSE_RECIBIDO
* ACUSE_RECHAZADO
* NOTIFICACION_REENVIADA
* FALLO_DICTADO
* DERIVACION_EXTERNA
* RESULTADO_EXTERNO
* ACTA_ARCHIVADA

---

## 5.2 Qué NO es un evento

No se registran:

* sincronizaciones
* errores técnicos
* reintentos internos
* procesos batch
* eventos de infraestructura

Estos pertenecen a logs técnicos (ej: ActaLogs).

---

# 6. SATÉLITES DEL MODELO

Las entidades satélite solo existen si aportan:

* datos propios relevantes
* consultas funcionales
* lógica de negocio no trivial

---

## 6.1 Satélite válido

### Notificacion

Se mantiene como entidad porque tiene:

* canal
* destino
* intentos
* acuses
* estado operativo de entrega

---

## 6.2 Entidades eliminadas o absorbidas

Se eliminan como agregados:

* ActaPresentacion
* ActaActo
* ActaDerivacionExterna

Se resuelven mediante:

* ActaEvento
* Documento
* ActaDocumento
* Snapshot

---

## 6.3 Recurso / Apelación

No se modela como agregado complejo.

Se representa mediante:

* eventos
* documentos
* estado proyectado

Opcionalmente puede existir un satélite mínimo de metadatos si es estrictamente necesario.

---

# 7. SNAPSHOT OPERATIVO

El snapshot contiene:

* estado actual simplificado
* banderas operativas
* referencias rápidas

Ejemplos:

* TieneApelacionAbierta
* TieneDeuda
* NotificacionPendiente
* NotificacionFallida

---

## Regla clave

El snapshot es derivado.

Nunca es fuente primaria de verdad.

---

# 8. PROPIEDAD DEL DATO

## Fuente primaria

* Acta
* ActaEvento
* Documento
* Notificacion

---

## Proyección

* ActaSnapshotOperativo

---

## Soporte

* ActaDocumento
* DocumentoFirma

---

# 9. REGLA DE SIMPLIFICACIÓN

Una entidad solo existe si:

* no puede resolverse correctamente con
  evento + documento + snapshot

---

# 10. PATRÓN BASE DEL SISTEMA

Todo el sistema se construye sobre este patrón:

## Evento → Documento → Snapshot

* Evento: hecho real
* Documento: soporte formal
* Snapshot: estado actual

---

# 11. OBJETIVO FINAL DEL MODELO

El modelo debe ser:

* simple
* consistente
* legible
* implementable
* usable por IA
* útil como spec-as-source

---

## Resultado esperado

Que cualquier lector pueda entender:

* el recorrido del Acta
* los eventos que ocurrieron
* los documentos asociados
* el estado actual
* el resultado del trámite

sin necesidad de interpretar múltiples submodelos.

---
