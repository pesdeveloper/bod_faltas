# Handoff 8F-11I - Notificaciones: Intentos, Acuses y Lotes

**Slice:** 8F-11I  
**Fecha:** 2026-07-06  
**Estado:** CERRADO  
**Build:** 2247 tests, 0 failures, BUILD SUCCESS  

## Objetivo del slice

Implementar InMemory para las 3 tablas pendientes de notificaciones:
- `fal_notificacion_intento` -> FalNotificacionIntento
- `fal_notificacion_acuse` -> FalNotificacionAcuse
- `fal_lote_correo` -> FalLoteCorreo

## Resultado

- 3 tablas pasan de FALTA_EN_INMEMORY a ALINEADO
- ALINEADO total: 55 tablas (era 52 en 8F-11H)
- FALTA_EN_INMEMORY restante: 1 tabla (fal_acta_qr_acceso, diferible - Slice 8F-11K)
- Tests: 2247 (+140 vs baseline 2107)

## Componentes implementados

### Enums nuevos
- `CanalNotificacion`: CORREO_POSTAL, NOTIFICADOR_MUNICIPAL, PRESENCIAL, PORTAL_INFRACTOR, EMAIL
- `TipoNotificacion`: ACTA_INFRACCION, FALLO_ABSOLUTORIO, FALLO_CONDENATORIO
- `TipoAcuse`: ACUSE_RECEPCION, ACUSE_RECHAZO, ACUSE_DOMICILIO_INEXISTENTE, ACUSE_PERSONA_DESCONOCIDA, ACUSE_AUSENTE, ACUSE_OTRO
- `EstadoAcuse`: PENDIENTE, RECIBIDO, VALIDADO, OBSERVADO, ANULADO
- `EstadoLote`: GENERADO, EMITIDO, PROCESADO, ANULADO, CON_ERROR

### Enums extendidos
- `EstadoNotificacion`: +SIN_EFECTO
- `ResultadoNotificacion`: +SUPERADA_POR_PORTAL
- `TipoEventoActa`: +NOTINT, +NOTREI, +NOTRVE, +ACUGEN, +ACUVAL, +LOTGEN, +LOTEMI, +LOTPRC, +LOTANU, +PORPOS, +NOTSUP

### Entidades
- `FalNotificacionIntento`: correlativo atomico por notificacion, canal, domicilio/destino digital, lote, referencia externa
- `FalNotificacionAcuse`: tipo acuse, estado acuse, storage key, fecha
- `FalLoteCorreo`: codigo unico, estado, referencia externa, guid ext

### Excepciones
- `NotificacionIntentoNoEncontradoException`
- `NotificacionAcuseNoEncontradoException`
- `LoteCorreoNoEncontradoException`
- `AcuseDuplicadoException`
- `LoteCodigoDuplicadoException`

### Repositorios
- `NotificacionIntentoRepository` + `InMemoryNotificacionIntentoRepository`
  - Correlativo atomico por notificacion (ConcurrentHashMap<Long, AtomicInteger>)
  - claimReferenciaExterna() para unicidad atomica de referencia externa
- `NotificacionAcuseRepository` + `InMemoryNotificacionAcuseRepository`
  - buscarPorIdempotencia() para check atomico
- `LoteCorreoRepository` + `InMemoryLoteCorreoRepository`
  - existeCodigo() para unicidad de loteCodigo

### Servicios
- `NotificacionIntentoService`: registrarIntento, registrarReintentoPorVencimiento, registrarResultadoIntento, registrarPortalPositivo, obtenerIntentos
- `NotificacionAcuseService`: registrarAcuse (synchronized idempotency), validarAcuse, observarAcuse, anularAcuse
- `LoteCorreoService`: generarLote, generarLoteConIntentos, emitirLote, procesarLote, marcarConError, anularLote

## Circuitos concurrentes resueltos

- C2: registrarIntento concurrente -> retry loop sobre ConcurrenciaConflictoException al actualizar FalNotificacion
- C3: referenciaExterna unica atomica -> claimReferenciaExterna() via putIfAbsent en ConcurrentHashMap
- C4: acuse idempotente atomico -> synchronized(acuseRepository) en registrarAcuse

## Proximo slice

**8F-11J** - Documentos y relaciones restantes (FalActaDocumento pivot, campos faltantes FalDocumento)