# HANDOFF 8F-11J — RELACION CANONICA ACTA-DOCUMENTO

## Estado al cierre

- **Slice:** 8F-11J
- **Fecha:** 2026-07-06
- **Build:** 2280 tests / 0 failures / 0 errors / 0 skipped / BUILD SUCCESS
- **Paridad:** 56 ALINEADO / 1 FALTA_EN_INMEMORY / 2 PARCIAL / 0 SEMANTICA_INCOMPATIBLE / **0 RELACION_INCOMPLETA** / 3 NO_PERSISTIBLE / Total: 62
- **Baseline:** 2247 tests (8F-11I) ? 2280 tests (+33)
- **Sin commit**

## Objetivo cumplido

`fal_acta_documento`: `RELACION_INCOMPLETA` ? `ALINEADO` (GAP-12 cerrado)

## Archivos nuevos

### Dominio
- `FalActaDocumento.java` — entidad pivot; clave compuesta ActaDocumentoId; rolDocuActa, siPrincipal, auditoría
- `ActaDocumentoId.java` — record value object; (actaId, documentoId); inmutable; igualdad por valor
- `RolDocuActa.java` — enum 12 roles (ACTA_PRINCIPAL..OTRO); codigo short; tiposPermitidos(); admitePrincipal()
- `ActaDocumentoYaExisteException.java` — excepción de duplicado
- `ActaDocumentoNoEncontradaException.java` — excepción de not found

### Repository
- `ActaDocumentoRepository.java` — interfaz con 10 operaciones incl. atomicas
- `InMemoryActaDocumentoRepository.java` — ConcurrentHashMap + principalLock; operaciones atomicas

### Service
- `ActaDocumentoService.java` — validar acta/doc, compatibilidad rol/tipo, unicidad principal, resolverUltimoDocumentoOperativo()

### Tests
- `ActaDocumentoServiceTest.java` — 17 tests funcionales (T01-T10)
- `ActaDocumentoConcurrenciaTest.java` — 10 escenarios deterministas (C1-C10)
- `ActaDocumentoPivotIntegracionTest.java` — 5 tests de integración con productores y snapshot

## Archivos modificados

### Core
- `FalActaSnapshot.java` — campo `idDocuUlt: Long` + getter/setter
- `SnapshotRecalculador.java` — `actaDocumentoRepository @Autowired(required=false)`; `resolverIdDocuUlt()` computa idDocuUlt
- `DocumentoService.java` — `actaDocumentoService @Autowired(required=false)`; asocia pivot en generarDocumento, generarDesdePlantilla, incorporarDocumentoEscaneado; `rolDesdeAltaDocumento(TipoDocu)` helper
- `FalloActaService.java` — `actaDocumentoService @Autowired(required=false)`; asocia FALLO en dictarAbsolutorio y dictarCondenatorio
- `NotificacionService.java` — `actaDocumentoService @Autowired(required=false)`; valida y asocia NOTIFICACION en enviarNotificacion
- `DocumentoGraphDemoService.java` — `actaDocumentoService @Autowired(required=false)`; asocia documentos demo; `rolPorTipoDocu()` helper
- `LoteCorreoService.java` — race condition C6 corregida: `loteGeneracionMonitor` sincroniza existeCodigo+guardar en generarLote y generarLoteConIntentos

### Documentación
- `110-matriz-maestra-paridad-mariadb-inmemory.md` — ALINEADO: 55?56; RELACION_INCOMPLETA: 1?0; entry fal_acta_documento actualizado
- `99-pendientes-siguientes-slices.md` — estado 8F-11J agregado

## Decisión de fuente única

`FalActaDocumento` / `fal_acta_documento` es la fuente canónica de pertenencia funcional.
`FalDocumento.idActa` se preserva como campo de construcción (usado para queries legacy via `buscarPorActa`).
No se sincronizan bidireccionalmente; el pivot es la fuente operativa.

## Catálogo rol/tipo (RolDocuActa)

| Rol | Tipos permitidos | Principal |
|-----|-----------------|-----------|
| ACTA_PRINCIPAL | ACTA_INFRACCION | sí, unicidad |
| FALLO | ACTO_ADMINISTRATIVO | sí, unicidad |
| NOTIFICACION | NOTIFICACION_ACTA, NOTIFICACION_ACTO_ADMINISTRATIVO | sí, unicidad |
| MEDIDA_PREVENTIVA | MEDIDA_PREVENTIVA | sí |
| RESOLUCION | ACTO_ADMINISTRATIVO | sí, unicidad |
| NULIDAD | NULIDAD | sí, unicidad |
| RESOLUTORIO_BLOQUEANTE | RESOLUTORIO_BLOQUEANTE | sí |
| INTIMACION_PAGO | INTIMACION_PAGO | sí |
| INTIMACION_PLAN | INTIMACION_INCUMPLIMIENTO_PLAN | sí |
| CONSTANCIA | CONSTANCIA | sí |
| ANEXO | ANEXO, CONSTANCIA, OTRO | no |
| OTRO | todos | no |

## Concurrencia y rollback

10 escenarios deterministas cubiertos:
- C1: asociacion duplicada — solo una persiste, ActaDocumentoYaExisteException al segundo
- C2: dos ACTA_PRINCIPAL — exactamente uno principal al final
- C3: dos reemplazos del principal — exactamente uno principal
- C4: N asociaciones concurrentes distintas — todos persisten
- C5: acta inexistente — exception, 0 en store
- C6: fallo y notificacion independientes — ambos persisten sin interferencia
- C7: N hilos intentan ser principal — exactamente uno gana
- C8: consulta concurrente durante reemplazo — sin crash ni estado parcial
- C9: rol incompatible con tipo — exception, 0 en store
- C10: historia completa conservada en N reemplazos

## snapshot.idDocuUlt

Prioridad: FALLO > RESOLUCION > NOTIFICACION > ACTA_PRINCIPAL
Null si no hay documentos en el pivot.
Calculado en SnapshotRecalculador.resolverIdDocuUlt() con actaDocumentoRepository optional.

## Siguiente(s) slice sugerido(s)

- **8F-11K**: fal_acta_qr_acceso (FALTA_EN_INMEMORY) — único pendiente
- **8F-11L**: completar PARCIAL fal_acta y fal_acta_evento