# 02 — Reporte 8F-11D: Normativa, Tarifario, Medidas y Valorización

**Slice:** 8F-11D  
**Fecha:** 2026-07-05  
**Estado final:** CERRADO  
**Build:** BUILD SUCCESS — 1755 tests, 0 failures, 0 errors  
**Baseline:** 1660 tests (post 8F-11C)

---

## Auditoría inicial (estado antes de 8F-11D)

### Qué ya existía
- Normativa completa: FalNormativaFaltas, FalArticuloNormativaFaltas, FalDependenciaNormativa con NormativaRepository e InMemoryNormativaRepository — ALINEADO desde 8A-1.
- FalActaFallo.montoCondena: campo BigDecimal placeholder, sin vinculación a valorizacion_id.
- FalActaSnapshot: campos económicos ausentes.
- Enums TipoUnidad (SALARIO, UNIDAD_FIJA, MONTO) existentes en el artículo normativo.

### Qué estaba parcial
- NormativaTest: tenía guardrails `no_existe_tarifario` y `no_existe_tarifario_repository` para marcar ausencia de tarifario — se eliminaron al implementar 8F-11D.

### Qué faltaba (GAP-7 del 110)
- FalTarifarioUnidadFaltas — NO existía.
- FalMedidaPreventiva — NO existía.
- FalArticuloMedidaPreventiva — NO existía.
- FalActaArticuloInfringido — NO existía.
- FalActaValorizacion — NO existía.
- FalActaValorizacionItem — NO existía.
- Los seis repositories correspondientes — NO existían.
- TarifarioService, ValorizacionService, MedidaPreventivaService, ActaArticuloInfringidoService — NO existían.
- TarifarioMedidaMockSeeder — NO existía.

### Qué era incompatible
- FalArticuloNormativaFaltas.tipoUnidad usa enum TipoUnidad (SALARIO, UNIDAD_FIJA, MONTO).
  ValorizacionService.mapTipoUnidad() hace la conversión a TipoUnidadFaltas.
  Decisión: no renombrar TipoUnidad existente; crear TipoUnidadFaltas como enum nuevo con codigo().

---

## Cambios reales por tabla

### 1. fal_tarifario_unidad_faltas

| Componente | Estado |
|---|---|
| Entidad: FalTarifarioUnidadFaltas | ALINEADO |
| Campos: id Long, tipoUnidad TipoUnidadFaltas, valorUnidad BigDecimal(2dp), fhVigDesde LocalDate, fhVigHasta LocalDate NULL, siActiva boolean, fhAlta LocalDateTime, idUserAlta String | ALINEADO |
| Repository port: TarifarioUnidadFaltasRepository | ALINEADO |
| InMemory: InMemoryTarifarioUnidadFaltasRepository | ALINEADO |
| Service: TarifarioService | ALINEADO |
| Enums: TipoUnidadFaltas (SALARIO, UNIDAD_FIJA, MONTO) con codigo() | ALINEADO |
| Tests: TarifarioValorizacionTest (tarifarios) | ALINEADO |
| Seeder: TarifarioMedidaMockSeeder | ALINEADO |
| Docs: 109, 110 actualizados | ALINEADO |
| **Estado final** | **ALINEADO** |

**Invariantes implementadas:**
- Un tarifario referenciado no se modifica: se crea una nueva fila.
- No se permiten rangos activos superpuestos para el mismo tipo de unidad.
- esVigenteEn(fecha): siActiva + rango de fechas.
- TarifarioService.verificarSinSuperposicion: lanza PrecondicionVioladaException si hay solapamiento.

### 2. fal_medida_preventiva

| Componente | Estado |
|---|---|
| Entidad: FalMedidaPreventiva | ALINEADO |
| Campos: id Long, codigo String(max12), versionMedida short(>=1), descripcion String(max160), descripcionDetalle String(max255), idDep Long NULL, verDep Short NULL, siActiva boolean, siPuedeBloquearCierre boolean, tipoBloqueanteDefault OrigenBloqueanteMaterial NULL, fhAlta LocalDateTime, idUserAlta String | ALINEADO |
| Repository port: MedidaPreventivaRepository | ALINEADO |
| InMemory: InMemoryMedidaPreventivaRepository | ALINEADO |
| Service: MedidaPreventivaService | ALINEADO |
| Tests: TarifarioValorizacionTest (medidas) | ALINEADO |
| Seeder: TarifarioMedidaMockSeeder | ALINEADO |
| **Estado final** | **ALINEADO** |

**Invariantes implementadas:**
- Múltiples versiones históricas por código (código+versión como UK lógico).
- Solo una versión activa por código en un momento dado.
- tipoBloqueanteDefault requiere siPuedeBloquearCierre=true.
- Reutiliza OrigenBloqueanteMaterial para tipoBloqueanteDefault.

### 3. fal_articulo_medida_preventiva

| Componente | Estado |
|---|---|
| Entidad: FalArticuloMedidaPreventiva | ALINEADO |
| Clase auxiliar: ArticuloMedidaPreventivaId (record: articuloId, medidaPreventivaId) | ALINEADO |
| Campos: id ArticuloMedidaPreventivaId (PK compuesta), siObligatoria boolean, siActiva boolean, fhAlta LocalDateTime, idUserAlta String | ALINEADO |
| Repository port: ArticuloMedidaPreventivaRepository | ALINEADO |
| InMemory: InMemoryArticuloMedidaPreventivaRepository | ALINEADO |
| Tests: TarifarioValorizacionTest (relaciones artículo-medida) | ALINEADO |
| **Estado final** | **ALINEADO** |

**Invariantes implementadas:**
- PK compuesta (articuloId, medidaPreventivaId) — sin ID sintético.
- Baja lógica: siActiva=false. No se borra físicamente.
- Relaciones inactivas no habilitan nuevas aplicaciones.

### 4. fal_acta_articulo_infringido

| Componente | Estado |
|---|---|
| Entidad: FalActaArticuloInfringido | ALINEADO |
| Campos: id Long, actaId Long, normativaId Long, articuloId Long, siActivo boolean, motivoBaja MotivoBajaArticuloInfringido NULL, fhBaja LocalDateTime NULL, idUserBaja String NULL, fhAlta LocalDateTime, idUserAlta String | ALINEADO |
| Repository port: ActaArticuloInfringidoRepository | ALINEADO |
| InMemory: InMemoryActaArticuloInfringidoRepository | ALINEADO |
| Service: ActaArticuloInfringidoService | ALINEADO |
| Enum: MotivoBajaArticuloInfringido | ALINEADO |
| Tests: TarifarioValorizacionTest (imputaciones) | ALINEADO |
| **Estado final** | **ALINEADO** |

**Invariantes implementadas:**
- Solo registra qué se imputó. NO guarda montos, unidades ni tarifario.
- Baja lógica preserva la historia. Corrección crea nueva fila.
- darDeBaja() requiere motivo + fhBaja + idUserBaja.

### 5. fal_acta_valorizacion

| Componente | Estado |
|---|---|
| Entidad: FalActaValorizacion | ALINEADO |
| Campos: id Long, versionRow int (optimistic locking), actaId Long, estadoValorizacion EstadoValorizacion, tipoValorizacionActa TipoValorizacionActa, origenValorizacion OrigenValorizacion, criterioTarifario CriterioTarifario, montoBaseArticulos BigDecimal NULL, montoFinal BigDecimal(2dp), tipoUnidadFinal TipoUnidadFaltas NULL, cantidadUnidadesFinal BigDecimal(4dp) NULL, valorUnidadFinal BigDecimal(2dp) NULL, tarifarioUnidadId Long NULL, siSobrescribeTotal boolean, siCongelaValor boolean, fhCongelamiento LocalDateTime NULL, falloId Long NULL, documentoId Long NULL, fhValorizacion LocalDateTime, idUserValorizacion String, fhConfirmacion LocalDateTime NULL, idUserConfirmacion String NULL, siVigente boolean, fhAlta LocalDateTime, idUserAlta String | ALINEADO |
| Repository port: ActaValorizacionRepository (con optimistic locking) | ALINEADO |
| InMemory: InMemoryActaValorizacionRepository | ALINEADO |
| Service: ValorizacionService | ALINEADO |
| Enums: EstadoValorizacion, TipoValorizacionActa, OrigenValorizacion, CriterioTarifario | ALINEADO |
| Exception: ConcurrenciaConflictoException | ALINEADO |
| Tests: TarifarioValorizacionTest, OptimisticLockingTest | ALINEADO |
| **Estado final** | **ALINEADO** |

**Invariantes implementadas:**
- versionRow controla concurrencia optimista: save compara versionRow, incrementa si coincide, lanza ConcurrenciaConflictoException si no.
- Solo una vigente por acta+tipo en todo momento (garantía atómica en InMemory).
- No se borra ni se reactiva: historial inmutable.
- Una preliminar nunca desplaza la confirmada vigente.
- Ciclo: PRELIMINAR → CONFIRMADA (vigente) → REEMPLAZADA (al confirmar nueva) / ANULADA.
- Selección operativa: prioridad CONDENA > PAGO_VOLUNTARIO > INFRACCION_BASE.

### 6. fal_acta_valorizacion_item

| Componente | Estado |
|---|---|
| Entidad: FalActaValorizacionItem | ALINEADO |
| Campos: id Long, valorizacionId Long, actaArticuloId Long NULL, tipoValorizacionItem TipoValorizacionItem, tipoUnidadBase/Aplicada TipoUnidadFaltas NULL, cantidadUnidades BigDecimal(4dp) NULL, valorUnidadAplicado BigDecimal(2dp) NULL, montoAplicado BigDecimal(2dp), tarifarioUnidadId Long NULL, siManual boolean, motivoManual MotivoManualizacionValorizacion NULL, documentoId Long NULL, falloId Long NULL | ALINEADO |
| Repository port: ActaValorizacionItemRepository | ALINEADO |
| InMemory: InMemoryActaValorizacionItemRepository | ALINEADO |
| Enums: TipoValorizacionItem, MotivoManualizacionValorizacion | ALINEADO |
| Tests: TarifarioValorizacionTest (items) | ALINEADO |
| **Estado final** | **ALINEADO** |

**Invariantes implementadas:**
- Sin versionRow ni auditoría de usuario: inmutable una vez confirmada la valorización madre.
- actaArticuloId es obligatorio salvo ajuste total/manual global.
- Snapshot congelado del cálculo por artículo.

---

## FalActaSnapshot — extensión por 8F-11D

FalActaSnapshot fue extendido con campos de valorización operativa:
- `valorizacionOperativaId` Long NULL
- `estadoValorizacionOperativa` EstadoValorizacion NULL
- `tipoValorizacionOperativa` TipoValorizacionActa NULL
- `montoOperativoVigente` BigDecimal NULL
- `siMontoConfirmado` boolean

SnapshotRecalculador.java actualizado para proyectar la valorización operativa vigente al snapshot.

---

## Invariantes documentadas

| Invariante | Evidencia |
|---|---|
| Tarifarios por vigencia | TarifarioUnidadFaltasRepository.findVigentes + findUltimoVigente |
| No superposición de rangos activos | TarifarioService.verificarSinSuperposicion |
| Versiones de medidas (codigo+version) | FalMedidaPreventiva.codigo (max12) + versionMedida (short>=1) |
| PK compuesta artículo-medida | ArticuloMedidaPreventivaId record (articuloId, medidaPreventivaId) |
| Artículo imputado sin montos | FalActaArticuloInfringido sin BigDecimal ni tarifarioId |
| Baja lógica artículo | darDeBaja() con MotivoBajaArticuloInfringido |
| Historial de valorizaciones (inmutable) | save no borra; siVigente controla vigencia |
| Una vigente por acta+tipo | ActaValorizacionRepository.findVigenteByActaIdAndTipo + guardrail en confirmar() |
| Optimistic locking | versionRow en FalActaValorizacion; ConcurrenciaConflictoException |
| Ítems/snapshots congelados | FalActaValorizacionItem inmutable post-confirmación |
| Snapshot operativo | SnapshotRecalculador.java actualizado |

---

## Cierre

| Indicador | Valor |
|---|---|
| Tests post-8F-11D | 1755, BUILD SUCCESS |
| Build baseline post-8F-11C | 1660 |
| Tests nuevos | ~95 |
| Tablas cerradas | 6 (de 27 → 21 FALTA_EN_INMEMORY) |
| Gaps del slice | 0 (GAP-7 cerrado) |

**Recomendación: CERRADO — avanzar a 8F-11E (satélites) o 8F-11F (fallo/firmeza).**

---

## Gaps que persisten post-8F-11D

Los gaps restantes NO son del slice 8F-11D. Se documentan en `09-PARIDAD-Y-GAPS-RESTANTES.md`.

Los más relevantes para la continuación:
- **GAP-9:** FalActaFallo campos faltantes (valorizacionId ya necesario — Slice 8F-11F).
- **GAP-4/GAP-5:** Paralizacion/Archivo — Slice 8F-11G.
- **GAP-16:** Satélites de acta — Slice 8F-11E.
