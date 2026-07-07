# 02 - Reporte 8F-11G

## Slice cerrado: Observaciones, Paralización y Archivo

**Fecha:** 2026-07-06  
**Build:** 2048 tests, BUILD SUCCESS  

## Paridad antes/despues

| Tabla | Antes | Despues |
|---|---|---|
| fal_observacion | FALTA_EN_INMEMORY | ALINEADO |
| fal_acta_paralizacion | FALTA_EN_INMEMORY | ALINEADO |
| fal_acta_archivo | FALTA_EN_INMEMORY | ALINEADO |
| fal_motivo_archivo | FALTA_EN_INMEMORY | ALINEADO |

## Archivos nuevos (src/main)

- domain/enums/EntidadTipoObservada.java
- domain/enums/OrigenObservacion.java
- domain/enums/MotivoParalizacion.java
- domain/model/FalObservacion.java
- domain/model/FalActaParalizacion.java
- domain/model/FalMotivoArchivo.java
- domain/model/FalActaArchivo.java
- domain/exception/MotivoArchivoNoEncontradoException.java
- repository/ObservacionRepository.java
- repository/ActaParalizacionRepository.java
- repository/MotivoArchivoRepository.java
- repository/ActaArchivoRepository.java
- repository/memory/InMemoryObservacionRepository.java
- repository/memory/InMemoryActaParalizacionRepository.java
- repository/memory/InMemoryMotivoArchivoRepository.java
- repository/memory/InMemoryActaArchivoRepository.java
- application/service/ObservacionService.java
- application/service/ParalizacionActaService.java
- application/service/MotivoArchivoService.java
- application/service/ArchivoActaService.java
- application/command/ArchivarActaCommand.java
- application/command/ReingresarDesdeArchivoCommand.java
- application/demo/MotivoArchivoMockSeeder.java

## Archivos modificados (src/main)

- application/command/ParalizarActaCommand.java (enum MotivoParalizacion, idUserOperacion)
- application/command/ReactivarActaCommand.java (versionParalizacion, idUserOperacion)
- application/service/ActaParalizacionService.java (delegador a ParalizacionActaService)
- application/demo/CasoUsoFuncionalRunner.java (wiring nuevos repos + servicio + MotivoParalizacion enum)
- snapshot/SnapshotRecalculador.java (proyectarParalizacion, optional injection)
- domain/model/FalActaSnapshot.java (campo motivoParalizacionAct)

## Archivos nuevos (src/test)

- application/ObservacionTest.java (18 tests)
- application/ParalizacionActaInvariantesTest.java (18 tests)
- application/MotivoArchivoTest.java (16 tests)
- application/ArchivoActaTest.java (20 tests)

## Decisiones de diseno

1. ParalizarActaCommand usa enum MotivoParalizacion (no texto libre)
2. FalActaParalizacion: ciclos historicos per actaId, sincronia per-actaId
3. FalActaArchivo: captura snapshot de origen completo (estado, bloque, bandeja)
4. ArchivoActaService cierra paraliz activa si acta archivada estaba paralizada
5. FalActaSnapshot.motivoParalizacionAct proyectado opcionalmente en SnapshotRecalculador
6. ActaParalizacionService conservado como delegador para back-compat con CasoUsoFuncionalRunner