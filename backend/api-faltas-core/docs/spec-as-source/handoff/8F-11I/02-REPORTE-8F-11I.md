# Reporte de Cierre 8F-11I

**Fecha:** 2026-07-06  
**Resultado:** CERRADO / BUILD SUCCESS  

## Tablas implementadas

| Tabla MariaDB | Entidad Java | Repositorio | Estado |
|---------------|--------------|-------------|--------|
| fal_notificacion_intento | FalNotificacionIntento | InMemoryNotificacionIntentoRepository | ALINEADO |
| fal_notificacion_acuse | FalNotificacionAcuse | InMemoryNotificacionAcuseRepository | ALINEADO |
| fal_lote_correo | FalLoteCorreo | InMemoryLoteCorreoRepository | ALINEADO |

## Conteos de paridad post-8F-11I

| Categoria | Cantidad |
|-----------|----------|
| Tablas MariaDB auditadas | 62 |
| ALINEADO | **55** (era 52 en 8F-11H) |
| FALTA_EN_INMEMORY | **1** (fal_acta_qr_acceso, diferible 8F-11K) |
| PARCIAL | 2 |
| RELACION_INCOMPLETA | 1 |
| NO_PERSISTIBLE | 3 |

## Decisiones clave

1. **FalNotificacion no redisenhada**: La tabla fal_notificacion ya era ALINEADO. Solo se extendieron los enums necesarios.
2. **Correlativo atomico por notificacion**: ConcurrentHashMap<Long, AtomicInteger> en el repo, sincronizado en guardar().
3. **Referencia externa atomica**: claimReferenciaExterna() con putIfAbsent evita duplicados bajo concurrencia.
4. **Acuse idempotente atomico**: synchronized(acuseRepository) en registrarAcuse().
5. **Retry en notif update**: Loop de hasta 10 retries en ConcurrenciaConflictoException al actualizar FalNotificacion.
6. **LOTEM codigo 6 chars**: Corregido a 'LOTEMI' para cumplir invariante de 6 chars en TipoEventoActa.codigo().

## Proximo slice: 8F-11J

- FalActaDocumento (tabla pivot documento-acta con orden)
- Campos faltantes de FalDocumento
- RELACION_INCOMPLETA -> ALINEADO para fal_acta_documento