# [CAPA 01] — NÚCLEO OPERATIVO CONSOLIDADO

---

# 1. FINALIDAD DE LA CAPA

Esta capa define el **núcleo del sistema**.

Es responsable de:

* representar el Acta como entidad central
* registrar la narrativa procesal del trámite
* permitir reconstruir el recorrido completo del Acta
* soportar el modelo event-driven
* mantener la identidad y coherencia del caso

Esta capa **no modela procesos específicos**, sino la base sobre la cual todo el sistema funciona.

---

# 2. ENTIDADES PRINCIPALES

## 2.1 Acta

### Descripción

Representa el trámite administrativo.

Es el **agregado raíz** del sistema.

---

### Responsabilidades

* identificar el caso
* contener datos estructurales mínimos
* permitir vinculación con el resto del modelo

---

### Contiene

* Id
* datos básicos del acta
* referencias estructurales (si aplica)

---

### No contiene

* historia del trámite
* eventos
* estado operativo derivado
* lógica compleja

---

### Regla

La Acta debe mantenerse:

* liviana
* estable
* no derivada

---

## 2.2 ActaEvento

### Descripción

Representa la **bitácora procesal del viaje del Acta**.

---

### Responsabilidades

Registrar:

* hechos relevantes del trámite
* decisiones
* interacciones significativas
* resultados
* cambios en la situación del Acta

---

### Características

* append-only
* orden temporal
* tipificado por tipo de evento

---

### Contiene

* Id
* IdActa
* TipoEvento
* FechaHora
* Usuario / Origen
* Observaciones (si aplica)

---

### No contiene

* eventos técnicos
* logs del sistema
* pasos internos de procesos
* sincronizaciones

---

### Regla clave

Solo se registran eventos que describen el recorrido real del Acta.

---

### Ejemplos válidos

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

## 2.3 ActaDocumento (Vínculo)

### Descripción

Relaciona documentos con el Acta.

---

### Responsabilidades

* vincular Documento con Acta
* evitar submodelos documentales paralelos
* permitir reutilización del modelo documental

---

### Contiene

* Id
* IdActa
* IdDocumento
* TipoLogico (opcional)
* ReferenciaContexto (opcional)

---

### Regla

Todos los documentos del sistema se vinculan al Acta a través de esta entidad.

---

## 2.4 ActaSnapshotOperativo

### Descripción

Representa el **estado actual proyectado del Acta**.

---

### Responsabilidades

* facilitar consultas rápidas
* alimentar bandejas
* evitar reconstrucción constante desde eventos

---

### Contiene

* IdActa
* EtapaOperativaActual
* Banderas operativas

---

### Ejemplos de banderas

* TieneApelacionAbierta
* TieneDeuda
* NotificacionPendiente
* NotificacionFallida

---

### Regla clave

El snapshot:

* es derivado
* no es fuente de verdad
* no reemplaza el dominio

---

# 3. ENTIDADES COMPLEMENTARIAS

## 3.1 ActaEvidencia

### Descripción

Representa evidencia incorporada al Acta.

---

### Responsabilidades

* almacenar evidencia (ej: imágenes)
* vincular evidencia al contexto del acta

---

## 3.2 ActaObservacion

### Descripción

Permite registrar observaciones internas relevantes.

---

### Regla

Debe usarse solo para información útil del caso, no como log técnico.

---

## 3.3 Subtipos del Acta

* ActaTransito
* ActaContravencion

---

### Responsabilidad

Permitir especialización del Acta según el tipo de infracción.

---

## 3.4 Inspectores y Snapshot

Se mantiene:

* Inspector (fuente)
* InspectorSnapshot (captura al momento del acta)

---

### Objetivo

Preservar la información histórica sin depender de cambios posteriores.

---

# 4. REGLAS DE DISEÑO DE LA CAPA

## 4.1 El Acta es el centro

Todo el sistema gira alrededor de Acta.

---

## 4.2 El historial vive en ActaEvento

La historia completa se reconstruye leyendo eventos.

---

## 4.3 El modelo es event-driven

No se modelan procesos, sino hechos.

---

## 4.4 No duplicar información

Evitar repetir datos entre:

* Acta
* Evento
* Snapshot

---

## 4.5 Separación de responsabilidades

* Acta → identidad
* Evento → historia
* Documento → soporte formal
* Snapshot → estado actual

---

# 5. RESULTADO DE LA CAPA

Esta capa permite:

* reconstruir completamente el recorrido del Acta
* entender qué ocurrió en cada momento
* mantener consistencia del dominio
* soportar el resto de las capas

---

## Resumen

Esta capa define:

## el pasajero

## el viaje

## la bitácora

y deja preparado el sistema para:

* documentación
* notificación
* decisiones
* integración

---
