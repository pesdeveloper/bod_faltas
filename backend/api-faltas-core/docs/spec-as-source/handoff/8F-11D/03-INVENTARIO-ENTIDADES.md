# 03 — Inventario de Entidades

Rutas relativas al repositorio `S:\Source\Repos\Bod-Faltas`.

---

## Entidades de las seis tablas de 8F-11D

### FalTarifarioUnidadFaltas
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalTarifarioUnidadFaltas.java`
- **Tabla:** `fal_tarifario_unidad_faltas`
- **Finalidad:** Valor monetario de una unidad de faltas por tipo y vigencia temporal.
- **Estado:** NUEVO (untracked `??`)
- **Campos clave:** id Long (PK), tipoUnidad TipoUnidadFaltas, valorUnidad BigDecimal(2dp), fhVigDesde LocalDate, fhVigHasta LocalDate NULL, siActiva boolean

### FalMedidaPreventiva
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalMedidaPreventiva.java`
- **Tabla:** `fal_medida_preventiva`
- **Finalidad:** Catálogo de medidas preventivas disponibles con soporte de versiones históricas.
- **Estado:** NUEVO (untracked `??`)
- **Campos clave:** id Long (PK), codigo String(max12), versionMedida short(>=1), descripcion String(max160), idDep Long NULL, verDep Short NULL, siActiva boolean, siPuedeBloquearCierre boolean, tipoBloqueanteDefault OrigenBloqueanteMaterial NULL

### FalArticuloMedidaPreventiva
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalArticuloMedidaPreventiva.java`
- **Tabla:** `fal_articulo_medida_preventiva`
- **Finalidad:** Relación entre un artículo normativo y una medida preventiva.
- **Estado:** NUEVO (untracked `??`)
- **Campos clave:** id ArticuloMedidaPreventivaId (PK compuesta), siObligatoria boolean, siActiva boolean

### ArticuloMedidaPreventivaId
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/model/ArticuloMedidaPreventivaId.java`
- **Tabla:** N/A (value object de PK compuesta)
- **Finalidad:** PK compuesta (articuloId Long, medidaPreventivaId Long).
- **Estado:** NUEVO (untracked `??`)

### FalActaArticuloInfringido
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalActaArticuloInfringido.java`
- **Tabla:** `fal_acta_articulo_infringido`
- **Finalidad:** Imputación de un artículo normativo a un acta de faltas. Solo registra qué se imputó.
- **Estado:** NUEVO (untracked `??`)
- **Campos clave:** id Long (PK), actaId Long, normativaId Long, articuloId Long, siActivo boolean, motivoBaja MotivoBajaArticuloInfringido NULL, fhBaja LocalDateTime NULL

### FalActaValorizacion
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalActaValorizacion.java`
- **Tabla:** `fal_acta_valorizacion`
- **Finalidad:** Decisión económica global de un acta en un momento dado. Con optimistic locking.
- **Estado:** NUEVO (untracked `??`)
- **Campos clave:** id Long (PK), versionRow int (optimistic locking), actaId Long, estadoValorizacion EstadoValorizacion, tipoValorizacionActa TipoValorizacionActa, montoFinal BigDecimal, siVigente boolean

### FalActaValorizacionItem
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalActaValorizacionItem.java`
- **Tabla:** `fal_acta_valorizacion_item`
- **Finalidad:** Cálculo congelado de un artículo imputado dentro de una valorización.
- **Estado:** NUEVO (untracked `??`)
- **Campos clave:** id Long (PK), valorizacionId Long, actaArticuloId Long NULL, tipoValorizacionItem TipoValorizacionItem, montoAplicado BigDecimal, siManual boolean

---

## Entidades relacionadas (modificadas en 8F-11D)

### FalActaSnapshot
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalActaSnapshot.java`
- **Tabla:** `fal_acta_snapshot`
- **Estado:** MODIFICADO (` M`)
- **Cambios:** Agregados campos de valorización operativa: valorizacionOperativaId Long NULL, estadoValorizacionOperativa EstadoValorizacion NULL, tipoValorizacionOperativa TipoValorizacionActa NULL, montoOperativoVigente BigDecimal NULL, siMontoConfirmado boolean.

### FalActa
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalActa.java`
- **Tabla:** `fal_acta`
- **Estado:** MODIFICADO (` M`)
- **Cambios:** Ajustes por integración con personas y valorización (slices 8F-11B/C/D).

---

## Entidades de normativa/artículo (reutilizadas en 8F-11D)

### FalNormativaFaltas
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalNormativaFaltas.java`
- **Estado:** PREEXISTENTE — ALINEADO

### FalArticuloNormativaFaltas
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalArticuloNormativaFaltas.java`
- **Estado:** PREEXISTENTE — ALINEADO

---

## Entidades de persona (relacionadas, cerradas en 8F-11C)

### FalPersona
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalPersona.java`
- **Estado:** NUEVO (`??`) — cerrado en 8F-11C

### FalPersonaDomicilio
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalPersonaDomicilio.java`
- **Estado:** NUEVO (`??`) — cerrado en 8F-11C

---

## Pagos transitorios relacionados

### FalPagoVoluntario
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalPagoVoluntario.java`
- **Estado:** PREEXISTENTE — SEMANTICA_INCOMPATIBLE (D2 cerrada) — Slice 8F-11H

### FalPagoCondena
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalPagoCondena.java`
- **Estado:** PREEXISTENTE — SEMANTICA_INCOMPATIBLE (D2 cerrada) — Slice 8F-11H
