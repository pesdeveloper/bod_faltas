# Handoff 8F-11F -- Continuidad

## Slice cerrado

**8F-11F -- Fallo, Firmeza y Apelacion**

Fecha: 2026-07-06
Build: 1976 tests, 0 failures, 0 errors, BUILD SUCCESS

## Proximo slice

**8F-11G -- Paralizacion, archivo, motivos, observaciones y gestion externa**

Pendientes mas urgentes:
1. `fal_acta_paralizacion` -- FalActaParalizacion completo con auditoria (actualmente solo campos basicos)
2. `fal_acta_archivo` -- FalActaArchivo nuevo
3. `fal_motivo_archivo` -- FalMotivoArchivo nuevo
4. `fal_observacion` -- FalObservacion nuevo (PAGAPR escribe aqui)
5. Completar gestion externa: FalActaGestionExterna auditoria

## Estado de la matriz de paridad

| Categoria | Cantidad |
|-----------|----------|
| Tablas MariaDB | 62 |
| ALINEADO | 44 |
| FALTA_EN_INMEMORY | 11 |
| PARCIAL | 2 |
| SEMANTICA_INCOMPATIBLE | 1 |
| RELACION_INCOMPLETA | 1 |
| NO_PERSISTIBLE | 3 |
| **Total** | **62** |

## Lo que entrega este slice

- `FalActaFallo` completo: firmeza inline, siVigente, falloReemplazadoId, fhVtoApelacion, multi-fallo atomico
- `FalActaApelacion` completo: canal, tipo, resultado, fhResolucion, 4 tipos de resolucion
- `FalActaApelacionDocumento` nuevo: entidad + repositorio InMemory
- `FalActaFirmezaCondena` transformado en DTO no persistible (P2 cerrada)
- `OrigenFirmezaCondena` enum con codigos numericos
- `ResultadoResolucionApelacion` enum nuevo: RECHAZADA, ACEPTADA_ABSUELVE, MODIFICA_CONDENA, NULIDAD
- `TipoPresentacion`, `CanalApelacion`, `TipoDocumentoApelacion`, `OrigenPresentacion` enums nuevos
- Tests nuevos: FalloInvariantesTest (13), ResolucionApelacionEfectosTest (11), ApelacionDocumentoTest (15), FalloApelacionConcurrenciaTest (10)
- Seeders demo: ACT-032 a ACT-037

## Archivos principales modificados

Ver 02-REPORTE-8F-11F.md para inventario completo.