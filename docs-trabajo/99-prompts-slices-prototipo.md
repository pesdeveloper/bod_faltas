
# 🧠 PATRÓN QUE TENÉS QUE APRENDER

Todos los slices siguen esta estructura:

### 1. Objetivo claro

> qué parte del sistema agrego

### 2. Endpoint

> cómo se dispara

### 3. Reglas de entrada

> desde qué estado se puede ejecutar

### 4. Mutación de estado

> cómo cambia ActaMock

### 5. Efectos secundarios

* eventos
* documentos
* notificaciones

### 6. Resultado

* JSON simple
* HTTP status

---

# 🎯 CLAVE PARA VOS

Si aprendés a escribir esto:

👉 “estado actual → acción → estado nuevo + efectos”

ya estás pensando como arquitecto del sistema.

---

# PRÓXIMO NIVEL

Lo que sigue ahora:

👉 construir la MATRIZ COMPLETA del sistema

(bandeja → acciones → destino)

Ahí es donde pasás de “slices” a “modelo completo”.

---



# PROMPTS — SLICES 01 a 09 — PROTOTIPO FALTAS

> Este documento consolida todos los prompts utilizados para construir el prototipo incremental del sistema de faltas.
> Objetivo: entender la mecánica de construcción de slices y poder replicarla.

---

# SLICE 01 — ESTRUCTURA BASE

**Objetivo:**
Crear la base mínima del proyecto Spring Boot.

**Prompt:**

* Crear proyecto Spring Boot
* Java 21
* Dependencias:

  * web
  * actuator
  * validation
* Configurar:

  * application.yml
  * puerto 8087

**Resultado esperado:**

* App levanta correctamente
* Endpoint actuator/health disponible

---

# SLICE 02 — MODELO MOCK + STORE

**Objetivo:**
Definir el modelo en memoria.

**Prompt:**

Crear:

### package `domain`

* ActaMock
* ActaEventoMock
* ActaDocumentoMock
* ActaNotificacionMock

### package `store`

* PrototipoStore

Características:

* uso de Map en memoria
* getters simples
* clearAll()

**Resultado esperado:**

* estructura lista para cargar datos

---

# SLICE 03 — BOOTSTRAP + HEALTH + RESET

**Objetivo:**
Inicializar dataset mock automáticamente.

**Prompt:**

* Crear `MockDataFactory`
* Cargar dataset mock (10 actas)
* Crear bootstrap con `ApplicationRunner`

Endpoints:

* GET /api/prototipo/health
* POST /api/prototipo/reset

**Resultado esperado:**

* dataset cargado al iniciar
* reset funcional

---

# SLICE 04 — CONSULTAS BASE

**Objetivo:**
Exponer lectura del sistema.

**Prompt:**

Endpoints:

* GET /bandejas
* GET /bandejas/{codigo}/actas
* GET /actas/{id}
* GET /actas/{id}/eventos

Reglas:

* sin services
* directo desde store
* DTOs mínimos

**Resultado esperado:**

* navegación completa del estado

---

# SLICE 05 — DOCUMENTOS Y NOTIFICACIONES (READ)

**Objetivo:**
Exponer datos asociados.

**Prompt:**

Endpoints:

* GET /actas/{id}/documentos
* GET /actas/{id}/notificaciones

Reglas:

* listas vacías si no hay datos
* 404 si no existe acta

**Resultado esperado:**

* visibilidad completa del expediente

---

# SLICE 06 — ACCIÓN: PASAR A NOTIFICACIÓN

**Objetivo:**
Primera acción mutante.

**Prompt:**

POST /actas/{id}/acciones/pasar-a-notificacion

Reglas:

* solo desde PENDIENTE_FIRMA
* cambios:

  * D3 → D4
  * estado → PENDIENTE_ENVIO
  * bandeja → PENDIENTE_NOTIFICACION
* crear evento FIRMA_COMPLETADA
* crear notificación si no existe

**Resultado esperado:**

* transición D3 → D4 funcional

---

# SLICE 07 — ACCIÓN: NOTIFICACIÓN POSITIVA

**Objetivo:**
Mover a análisis.

**Prompt:**

POST /actas/{id}/acciones/registrar-notificacion-positiva

Reglas:

* solo desde:

  * PENDIENTE_NOTIFICACION
  * EN_NOTIFICACION
* cambios:

  * D4 → D5
  * estado → PENDIENTE_REVISION
  * bandeja → PENDIENTE_ANALISIS
* actualizar notificación a ENTREGADA
* crear evento NOTIFICACION_ENTREGADA

**Resultado esperado:**

* transición D4 → D5 funcional

---

# SLICE 08 — ACCIÓN: CERRAR ACTA

**Objetivo:**
Cerrar expediente.

**Prompt:**

POST /actas/{id}/acciones/cerrar-acta

Reglas:

* solo desde PENDIENTE_ANALISIS
* cambios:

  * bloqueActual = CERRADA
  * estadoProcesoActual = CERRADA
  * situacionAdministrativaActual = CERRADA
  * estaCerrada = true
  * permiteReingreso = false
  * bandeja = CERRADAS
* evento CIERRE_ANALISIS

**Resultado esperado:**

* cierre completo

---

# SLICE 09 — ACCIÓN: ARCHIVAR ACTA

**Objetivo:**
Archivo operativo.

**Prompt:**

POST /actas/{id}/acciones/archivar-acta

Reglas:

* solo desde PENDIENTE_ANALISIS
* cambios:

  * bloqueActual = ARCHIVO
  * estadoProcesoActual = ARCHIVADA_OPERATIVA
  * situacionAdministrativaActual = ARCHIVO
  * estaCerrada = false
  * permiteReingreso = true
  * bandeja = ARCHIVO
* evento ARCHIVADO_DESDE_ANALISIS

**Resultado esperado:**

* archivo con posibilidad de reingreso

---

# SLICE 10 — ALTA DE BANDEJA: PENDIENTES DE RESOLUCIÓN / REDACCIÓN

**Objetivo:**
Incorporar al prototipo la bandeja de expedientes que requieren producir una pieza administrativa o documental no-fallo.

**Prompt:**

Agregar la nueva bandeja:

* código:
  * PENDIENTES_RESOLUCION_REDACCION

* nombre visible:
  * Pendientes de resolución / redacción

Reglas:

* la bandeja debe aparecer en el listado general de bandejas
* debe poder listar actas asociadas
* agregar al dataset mock al menos 4 actas de ejemplo en esta bandeja:
  * resolución pendiente
  * nulidad pendiente
  * medida preventiva pendiente
  * rectificación pendiente

* cada acta mock debe:
  * verse en `/api/prototipo/bandejas/{codigo}/actas`
  * verse en el detalle `/api/prototipo/actas/{id}`
  * tener estado coherente con una necesidad de pieza no-fallo todavía no producida

* no implementar todavía acciones nuevas en este slice
* no agregar framework genérico
* mantener patrón actual del proyecto

**Resultado esperado:**

* existe la bandeja `PENDIENTES_RESOLUCION_REDACCION`
* aparece en el listado de bandejas
* tiene actas mock visibles
* queda lista para implementar acciones en los siguientes slices

---


Importante:
- No uses `99-prompts-slices-prototipo.md` como fuente de reglas funcionales.
- Ese archivo es solo bitácora / registro de slices.
- No lo modifiques ni lo leas como input principal salvo que se te pida explícitamente actualizar el historial de prompts.
- Para implementar, priorizá el código actual del prototipo y las reglas expresadas en este prompt.
