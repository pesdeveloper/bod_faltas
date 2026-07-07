# 06 — Inventario de Services

Servicios de aplicación de 8F-11D. Rutas relativas al repositorio.

---

## TarifarioService
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/application/service/TarifarioService.java`
- **Estado:** NUEVO (`??`)
- **Dependencias:** TarifarioUnidadFaltasRepository
- **Operaciones:**
  - `crear(tipoUnidad, valorUnidad, fhVigDesde, fhVigHasta, idUser)`: crea un tarifario verificando no-superposición.
  - `obtener(id)`: lanza TarifarioNoEncontradoException si no existe.
  - `listarTodos()`, `listarPorTipo(TipoUnidadFaltas)`
  - `resolverUltimoVigente(tipoUnidad, fecha)`: devuelve Optional.
  - `desactivar(id, idUser)`: baja lógica.
- **Lógica:** verificarSinSuperposicion() compara rangos activos; solapan() maneja fhVigHasta=null como LocalDate.MAX.

---

## ValorizacionService
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/application/service/ValorizacionService.java`
- **Estado:** NUEVO (`??`)
- **Dependencias:** ActaValorizacionRepository, ActaValorizacionItemRepository, ActaArticuloInfringidoRepository, NormativaRepository, TarifarioUnidadFaltasRepository, ActaRepository
- **Operaciones:**
  - `calcularBasePreliminar(actaId, idUser)`: calcula desde artículos activos; genera una valorizacion INFRACCION_BASE con items por artículo; usa findUltimoVigente para resolver tarifario.
  - `confirmar(valorizacionId, idUser)`: solo desde PRELIMINAR; garantía de una-vigente: desactiva la anterior del mismo tipo; marca nueva como CONFIRMADA + siVigente=true.
  - `anular(valorizacionId, idUser)`: cualquier estado no ANULADA.
  - `agregarItemManual(valorizacionId, actaArticuloId, monto, motivo, documentoId, idUser)`: solo a PRELIMINAR; OTRO_FUNDADO requiere documentoId.
  - `consultarVigente(actaId, tipo)`, `listarHistorial(actaId)`, `listarItems(valorizacionId)`
  - `seleccionarOperativa(actaId)`: prioridad CONDENA > PAGO_VOLUNTARIO > INFRACCION_BASE.
  - `mapTipoUnidad(TipoUnidad)`: convierte TipoUnidad → TipoUnidadFaltas.
- **Origen de verdad económica:** FalActaValorizacion.montoFinal (confirmada y vigente).
- **Actualización de snapshot:** a través de SnapshotRecalculador.java (extendido).

---

## MedidaPreventivaService
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/application/service/MedidaPreventivaService.java`
- **Estado:** NUEVO (`??`)
- **Dependencias:** MedidaPreventivaRepository, ArticuloMedidaPreventivaRepository
- **Operaciones:**
  - `crear(codigo, versionMedida, descripcion, idUser)`: alta de medida preventiva.
  - `obtener(id)`: lanza MedidaPreventivaNoEncontradaException si no existe.
  - `listarTodos()`, `listarActivos()`
  - `desactivar(id, idUser)`: baja lógica.
  - `asociarArticulo(articuloId, medidaPreventivaId, siObligatoria, idUser)`: crea FalArticuloMedidaPreventiva.
  - `desasociarArticulo(articuloId, medidaPreventivaId, idUser)`: baja lógica en la relación.

---

## ActaArticuloInfringidoService
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/application/service/ActaArticuloInfringidoService.java`
- **Estado:** NUEVO (`??`)
- **Dependencias:** ActaArticuloInfringidoRepository, NormativaRepository, ActaRepository
- **Operaciones:**
  - `imputar(actaId, normativaId, articuloId, idUser)`: alta de imputación.
  - `darDeBaja(id, motivo, idUser)`: baja lógica con MotivoBajaArticuloInfringido.
  - `listarPorActa(actaId)`, `listarActivosPorActa(actaId)`
  - `obtener(id)`: lanza ActaArticuloInfringidoNoEncontradoException si no existe.

---

## SnapshotRecalculador (modificado)
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/snapshot/SnapshotRecalculador.java`
- **Estado:** MODIFICADO (` M`)
- **Cambios:** Actualiza valorizacionOperativaId, estadoValorizacionOperativa, tipoValorizacionOperativa, montoOperativoVigente, siMontoConfirmado en FalActaSnapshot usando ValorizacionService.seleccionarOperativa().

---

## Servicios relacionados (persona — otros slices)

### PersonaService
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/application/service/PersonaService.java`
- **Estado:** NUEVO (`??`) — cerrado en 8F-11C

### PersonaDomicilioService
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/application/service/PersonaDomicilioService.java`
- **Estado:** NUEVO (`??`) — cerrado en 8F-11C
