>> QR, ese debe generarse en 01 !
>> Catalogos de calles
>> Tabla de obtencion de Calle x Altura de barrio , localidad etc
>> Tabla o servicio para obtener por GPS la ubicacion completa, calle, altura todo !!!!!

>> Tabla de municipios
>> Tabla de provincias , localidades, departamentos, etc ! o servicio web que lo devuelva !

---

Estamos retomando el diseño del sistema de faltas municipal event-driven de Malvinas Argentinas.

Quiero que uses TODO el contexto previo ya acordado, sin redefinir nada desde cero ni volver a discutir decisiones ya cerradas, salvo que yo lo pida explícitamente.

⚠️ IMPORTANTE
- Documentos largos → SIEMPRE entregarlos en múltiples partes
- Formato siempre copy/paste limpio
- No usar canvas
- Mantener lenguaje técnico claro
- No resumir en exceso
- Si detectás inconsistencias, marcarlas antes de avanzar

---

# ESTADO ACTUAL DEL PROYECTO

## Flujo general

Ya están completamente definidos y cerrados:

- flujo D1–D8
- procesos transversales:
  - P1 = documental
  - P2 = económico
  - P3 = notificación

---

## Reglas estructurales del sistema

- modelo event-driven
- append-only (histórico)
- no se reabre: se reingresa por evento
- separación clara entre:
  - flujo
  - acto
  - documento
- snapshots controlados para operación
- catálogos cerrados (enum / constantes)
- tipos compatibles con Informix
- no usar texto libre para estados importantes

---

# CAPAS YA IMPLEMENTADAS

## CAPA 01 — NÚCLEO OPERATIVO

Entidades:

- Acta
- ActaEvento (append-only)
- ActaEvidencia
- ActaObservacion
- ActaTransito
- ActaContravencion
- ActaContravencionMedida
- Inspectores
- InspectoresSnapshot
- ActaDomicilio (refactor ya integrado)

Decisiones clave:

- Acta es entidad central
- snapshot operativo vive en Acta
- reingreso por eventos
- evidencias pertenecen al acta
- domicilio se sacó de Acta → ahora es ActaDomicilio
- Acta tiene:
  - IdActaDomicilioPrincipal

---

## CAPA 02 — DOCUMENTAL

Entidades:

- Documento
- ActaDocumento
- DocumentoFirma
- DocumentoObservacion

Decisiones:

- Documento es entidad propia
- 1 documento → 1 firma
- observaciones separadas
- anulado es estado válido
- evidencias NO pertenecen a esta capa
- hash SHA-256
- relación con notificación se resuelve en Capa 03

---

## CAPA 03 — NOTIFICACIÓN

Entidades:

- ActaDomicilio (reutilizada)
- Notificacion
- NotificacionDocumento
- NotificacionResultado
- NotificacionObservacion
- LoteNotificacion
- LoteNotificacionDetalle

Decisiones clave:

- notificación = intento
- reintentos = nueva fila
- resultado separado
- múltiples documentos por notificación
- lote desacoplado
- Notificacion NO tiene IdLote
- NumeroDocumentoReceptor es numérico
- historial completo (no delete)

---

# ESTADO ACTUAL

Capas 01, 02 y 03 están:

✔ diseñadas  
✔ documentadas  
✔ consistentes  

---

# PRÓXIMO PASO (MUY IMPORTANTE)

Vamos a construir:

## 👉 CAPA 04 — PRESENTACIONES E INTERACCIÓN ADMINISTRATIVA

YA está definida la decisión clave:

✔ usar **OPCIÓN A → entidad genérica**

---

# DEFINICIÓN BASE DE CAPA 04

La capa debe modelar:

- comparecencia espontánea
- descargo
- constitución de domicilio
- presentación de documentación
- solicitudes (pago voluntario, prórroga, etc.)
- notas administrativas
- interacción del infractor o terceros con la causa

---

# REGLA CENTRAL

Debe existir una entidad base tipo:

👉 ActaPresentacion

NO queremos múltiples tablas separadas para cada tipo.

---

# OBJETIVO DE LA SESIÓN

Quiero que trabajemos en este orden:

## 1. Documento conceptual
capa-04-presentaciones.md

## 2. Diagrama estructural
capa-04-presentaciones.mermaid

## 3. DDL lógico
capa-04-ddl-logico.md

---

# ESTILO DE TRABAJO

- técnico, directo, sin vueltas
- sin sobre-diseño
- consistente con capas anteriores
- reutilizar lo ya existente (Documento, ActaDomicilio, etc.)
- separar bien responsabilidades
- no mezclar con económico ni resolución todavía

---

# MUY IMPORTANTE

Antes de empezar:

👉 Si detectás inconsistencias con Capa 01–03, marcarlas primero

---

# INSTRUCCIÓN FINAL

Arrancar directamente por:

👉 capa-04-presentaciones.md

(si es largo, dividir en partes)
