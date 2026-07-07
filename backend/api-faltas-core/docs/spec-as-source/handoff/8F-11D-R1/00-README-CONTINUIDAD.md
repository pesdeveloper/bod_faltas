# Paquete de continuidad: Slice 8F-11D-R1

## Slice
`8F-11D-R1` — Cierre total valorizacion: invariantes de concurrencia, inmutabilidad y paridad

## Fecha
2026-07-06

## Build base
- Slice anterior: `8F-11D` — 1785 tests, 0 failures
- Este R1: 1785 tests, 0 failures — BUILD SUCCESS

## Motivacion
Revision post-auditoria del slice 8F-11D. 11 inconsistencias entre el modelo MariaDB objetivo y la implementacion InMemory. Este paquete cierra todas.

## Objetivos cerrados

| ID | Descripcion | Estado |
|----|-------------|--------|
| R1-A | SnapshotRecalculador proyecta valorizacion vigente CONFIRMADA | CERRADO |
| R1-B | confirmarVigenteAtomico: solo una valorizacion vigente+confirmada por acta+tipo | CERRADO |
| R1-C | FalActaValorizacionItem inmutable post-confirmacion | CERRADO |
| R1-D.8.5 | FalActaValorizacion: inmutabilidad economica via verificarMutable() | CERRADO |
| R1-D.8.6 | Transiciones de estado via metodos de dominio (marcarConfirmada/Reemplazada/Anulada) | CERRADO |
| R1-D.8.7 | FalTarifarioUnidadFaltas: guarda atomica solapamiento de vigencia | CERRADO |
| R1-D.8.8 | FalMedidaPreventiva: crearNuevaVersionAtomico | CERRADO |
| R1-D.8.9 | FalArticuloMedidaPreventiva: guarda contra reactivacion silenciosa | CERRADO |
| R1-D.8.10 | FalActaArticuloInfringido: UK activo (actaId, articuloId) | CERRADO |
| R1-D.8.11 | calcularBasePreliminar: manejo de error explicito | CERRADO |
| R1-E | Documentacion: 110, 99, 109 actualizados | CERRADO |

## Tests nuevos (30 en ValorizacionInvariantesR1Test - auditado)
- SnapshotProyeccionR1ATest (4 tests)
- ConfirmarVigenteAtomicoR1BTest (4 tests, concurrencia real CyclicBarrier)
- ItemInmutabilidadR1CTest (4 tests)
- TarifarioSuperposicionR1DTest (**6 tests** - no 5; incluye find_ultimo_vigente_detecta_invariante_rota)
- MedidaPreventivaVersionAtomicaR1DTest (4 tests)
- ActaArticuloInfringidoUKR1DTest (**5 tests** - no 3; incluye diferente_acta/articulo_no_conflicta)
- ArticuloMedidaReactivacionR1DTest (3 tests)
- **Total auditado: 30 tests (delta real 1755->1785)**

## Archivos modificados (resumen)
- FalActaValorizacion.java (verificarMutable, marcarConfirmada/Reemplazada/Anulada)
- FalActaSnapshot.java (campos valorizacion)
- ActaValorizacionRepository.java, MedidaPreventivaRepository.java (nuevos metodos atomicos)
- InMemoryActaValorizacionRepository.java (confirmarVigenteAtomico)
- InMemoryActaValorizacionItemRepository.java (guarda inmutabilidad)
- InMemoryTarifarioUnidadFaltasRepository.java (guarda solapamiento)
- InMemoryMedidaPreventivaRepository.java (crearNuevaVersionAtomico)
- InMemoryActaArticuloInfringidoRepository.java (UK activo)
- InMemoryArticuloMedidaPreventivaRepository.java (guarda reactivacion)
- ValorizacionService.java, MedidaPreventivaService.java (usan nuevos atomicos)
- SnapshotRecalculador.java (proyectarValorizacion, compat. hacia atras)
- ValorizacionInvariantesR1Test.java (27 tests nuevos)
- 110-matriz-maestra-paridad-mariadb-inmemory.md (6 tablas FALTA -> ALINEADO)
- 99-pendientes-siguientes-slices.md (8F-11D-R1 cerrado)
- 109-delta-modelo-mariadb-inmemory.md (reconstruido)

## Proximo slice sugerido
Ver `99-pendientes-siguientes-slices.md` seccion "Siguiente: 8F-11E"
Candidatos: 8F-11E (fal_acta_transito/alcoholemia), 8F-12 (PersonaService)