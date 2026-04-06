>> QR, ese debe generarse en 01 !
>> Catalogos de calles
>> Tabla de obtencion de Calle x Altura de barrio , localidad etc
>> Tabla o servicio para obtener por GPS la ubicacion completa, calle, altura todo !!!!!

>> Tabla de municipios
>> Tabla de provincias , localidades, departamentos, etc ! o servicio web que lo devuelva !

---
 🔜 Después de eso

Cuando volvamos con ese prompt, el siguiente paso natural va a ser:

👉 limpieza final
👉 estructura de carpetas .md
👉 contratos de servicios
👉 queries base
👉 y recién ahí: código

---
Estamos retomando el diseño completo del sistema de faltas municipal event-driven (Malvinas Argentinas).

⚠️ IMPORTANTE
- NO redefinir nada desde cero
- NO volver a discutir decisiones ya cerradas
- SI detectar inconsistencias antes de avanzar
- Documentos largos → dividir solo si es necesario
- Formato siempre copy/paste limpio
- No usar canvas
- Lenguaje técnico claro
- Orientado a spec-as-source (uso con ChatGPT / Cursor)

---

# ESTADO ACTUAL DEL MODELO

El sistema está completamente modelado hasta CAPA 09.

## Flujo general
- D1–D8 definidos y cerrados
- D2 = ENRIQUECIMIENTO (no validación)
- Modelo event-driven
- Append-only
- Reingreso por eventos
- Snapshots operativos controlados

---

# CAPAS DEFINIDAS

## CAPA 01 — Núcleo operativo
- Acta
- ActaEvento (append-only)
- Evidencias
- Observaciones
- Subtipos (transito / contravención)
- Inspectores + snapshot
- ActaDomicilio

## CAPA 02 — Documental
- Documento
- DocumentoFirma
- DocumentoObservacion
- ActaDocumento

## CAPA 03 — Notificación
- Notificacion
- NotificacionDocumento
- NotificacionResultado
- Lotes desacoplados

## CAPA 04 — Presentaciones
- ActaPresentacion (genérica)
- Comparecencia, descargo, solicitudes, etc.

## CAPA 05 — Actos
- ActaActo
- Tipos: FALLO, RESOLUCION, DISPOSICION
- Estado mínimo: EMITIDO / ANULADO

## CAPA 06 — Económico (integración)
- SujBieFaltas (IdSuj + IdBie)
- NO se modela deuda
- Integración con cccmte / ccmov existente
- Procesos externos (GenerarDeuda, PlanPagos, etc.)

## CAPA 07 — Recursivo
- ActaRecurso
- Tipo: APELACION
- Estados: INTERPUESTO → CONCEDIDO → ELEVADO → RESULTADO → CERRADO

## CAPA 08 — Derivación externa
- ActaDerivacionExterna
- Tipos: APREMIO / JUZGADO_PAZ
- ResultadoDerivacionExterna:
  - PAGO_EN_APREMIO
  - CONFIRMA_FALLO
  - MODIFICA_FALLO
  - ANULA_FALLO
- Estado simple + resultado tipificado

## CAPA 09 — Snapshot operativo
- ActaSnapshotOperativo
- EtapaOperativaActual
- Banderas:
  - TieneRecursoAbierto
  - TieneDerivacionExternaAbierta
  - TieneDeuda
  - etc.
- Base para bandejas
- NO reemplaza el dominio

---

# OBJETIVO ACTUAL

NO agregar más capas.

👉 Estamos en fase de **cierre y consolidación del modelo completo**

---

# PRÓXIMO PASO

Quiero que hagamos:

## 👉 PASADA GENERAL DE CONSOLIDACIÓN

Objetivo:

- detectar redundancias entre capas
- simplificar nombres
- unificar criterios
- validar consistencia global
- reducir complejidad innecesaria
- asegurar que el modelo sea óptimo para:
  - generación de código con IA
  - uso como spec-as-source
  - implementación directa

---

# ENFOQUE DE TRABAJO

Quiero que lo hagamos en este orden:

## 1. Revisión global de consistencia
- relaciones entre capas
- posibles duplicaciones
- posibles simplificaciones

## 2. Normalización de criterios
- enums
- nombres de campos
- patrones repetidos (UsuarioRegistro, OrigenRegistro, etc.)

## 3. Simplificación del modelo
- eliminar lo que no aporta
- evitar sobre-modelado
- mantener lo mínimo necesario

## 4. Preparación para implementación
- qué ya está listo para código
- qué falta definir como contratos
- qué partes necesitan queries o servicios

---

# IMPORTANTE

- No reescribir todo
- No generar documentos gigantes
- Trabajar en bloques claros y concretos
- Señalar mejoras antes de aplicarlas

---

# INSTRUCCIÓN

Arrancar por:

👉 Revisión global de consistencia del modelo (capas 01–09)

Listando:

- cosas que están perfectas
- cosas que se pueden simplificar
- posibles inconsistencias
- oportunidades de mejora reales (no teóricas)

Sin modificar todavía nada, solo análisis.


