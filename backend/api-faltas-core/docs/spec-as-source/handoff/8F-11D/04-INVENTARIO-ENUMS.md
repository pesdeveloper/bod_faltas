# 04 — Inventario de Enums

Enums usados por 8F-11D. Rutas relativas al repositorio.

Prefijo de paquete: `ar.gob.malvinas.faltas.core.domain.enums`

---

## Enums nuevos en 8F-11D

### CriterioTarifario
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/enums/CriterioTarifario.java`
- **Estado:** NUEVO (untracked `??`)
- **Uso:** Criterio de cálculo aplicado en FalActaValorizacion.
- **Valores esperados:** ULTIMO_VIGENTE, FECHA_INFRACCION, MANUAL, etc.
- **fromCodigo:** verificar en la clase

### EstadoValorizacion
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/enums/EstadoValorizacion.java`
- **Estado:** NUEVO (untracked `??`)
- **Tabla código/valor:** PRELIMINAR, CONFIRMADA, REEMPLAZADA, ANULADA
- **Uso:** Estado en FalActaValorizacion.estadoValorizacion

### MotivoBajaArticuloInfringido
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/enums/MotivoBajaArticuloInfringido.java`
- **Estado:** NUEVO (untracked `??`)
- **Uso:** Motivo de baja lógica en FalActaArticuloInfringido.motivoBaja

### MotivoManualizacionValorizacion
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/enums/MotivoManualizacionValorizacion.java`
- **Estado:** NUEVO (untracked `??`)
- **Uso:** Motivo de item manual en FalActaValorizacionItem.motivoManual
- **Nota:** OTRO_FUNDADO requiere documentoId hasta que 8F-11G implemente observaciones.

### OrigenValorizacion
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/enums/OrigenValorizacion.java`
- **Estado:** NUEVO (untracked `??`)
- **Uso:** Origen en FalActaValorizacion (SISTEMA, MANUAL, IMPORTADO, etc.)

### TipoUnidadFaltas
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/enums/TipoUnidadFaltas.java`
- **Estado:** NUEVO (untracked `??`)
- **Tabla código/valor:** SALARIO, UNIDAD_FIJA, MONTO — con codigo()
- **Decisión de diseño:** No se renombró TipoUnidad existente. Se creó TipoUnidadFaltas nuevo con codigo(). ValorizacionService.mapTipoUnidad() convierte TipoUnidad -> TipoUnidadFaltas.
- **Uso:** FalTarifarioUnidadFaltas, FalActaValorizacion, FalActaValorizacionItem

### TipoValorizacionActa
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/enums/TipoValorizacionActa.java`
- **Estado:** NUEVO (untracked `??`)
- **Tabla código/valor:** INFRACCION_BASE, PAGO_VOLUNTARIO, CONDENA, AJUSTE_TOTAL, etc.
- **Uso:** Tipo en FalActaValorizacion; prioridad operativa: CONDENA > PAGO_VOLUNTARIO > INFRACCION_BASE

### TipoValorizacionItem
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/enums/TipoValorizacionItem.java`
- **Estado:** NUEVO (untracked `??`)
- **Tabla código/valor:** AUTOMATICA, MANUAL, AJUSTE, etc.
- **Uso:** FalActaValorizacionItem.tipoValorizacionItem

---

## Enums reutilizados (preexistentes, relevantes para 8F-11D)

### OrigenBloqueanteMaterial (reutilizado)
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/enums/OrigenBloqueanteMaterial.java`
- **Estado:** PREEXISTENTE
- **Uso en 8F-11D:** FalMedidaPreventiva.tipoBloqueanteDefault reutiliza este enum en lugar de crear uno nuevo.

### TipoUnidad (preexistente — NO es TipoUnidadFaltas)
- **Ruta:** `backend/api-faltas-core/src/main/java/ar/gob/malvinas/faltas/core/domain/enums/TipoUnidad.java`
- **Estado:** PREEXISTENTE
- **Uso:** FalArticuloNormativaFaltas.tipoUnidad — convertido a TipoUnidadFaltas en ValorizacionService.

---

## Enums nuevos en otros slices (presentes en el working tree)

### ModoDomicilio, OrigenDomicilio, TipoDomicilio, TipoPersona, TipoDocumentoPersona
- Slices 8F-11B/C — cerrados antes de 8F-11D.

### SujBieEstado, OrigenUbicacion, UnidadTerritorialTipo
- Slices 8F-11B/C — cerrados antes de 8F-11D.

---

## Enum eliminado (relevante como referencia)

### FormatoPlantillaContenido
- **Estado:** ELIMINADO en 8F-10-R1 (`D`en git status)
- **Motivo:** Solo Markdown — el campo formato fue eliminado del modelo MariaDB y del código Java.
