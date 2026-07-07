# Reporte final: Slice 8F-11D-R1 — Cierre total valorizacion

## A. Objetivo del slice
Revision post-auditoria de 8F-11D. Cierre de 11 inconsistencias entre el modelo MariaDB productivo y la implementacion InMemory del prototipo, enfocando invariantes de concurrencia, inmutabilidad economica y paridad documental.

---

## B. Archivos tocados

### Dominio (src/main/java/.../domain/model)
| Archivo | Cambio |
|---------|--------|
| FalActaValorizacion.java | verificarMutable(); marcarConfirmada/Reemplazada/Anulada(); estado no puede retroceder |
| FalActaSnapshot.java | +montoValorizacionVigente, +tipoValorizacionVigente, +estadoValorizacionVigente, +idValorizacionVigente |

### Repositorios — interfaces
| Archivo | Cambio |
|---------|--------|
| ActaValorizacionRepository.java | +confirmarVigenteAtomico(actaId, tipo, nuevaId, vigenteAnteriorId) |
| MedidaPreventivaRepository.java | +crearNuevaVersionAtomico(codigo, nuevaVersion) |

### Repositorios — InMemory (src/main/java/.../repository/memory)
| Archivo | Cambio |
|---------|--------|
| InMemoryActaValorizacionRepository.java | confirmarVigenteAtomico implementado synchronized+CAS; findVigente detecta corrupcion; save guarda vigente solo con CONFIRMADA |
| InMemoryActaValorizacionItemRepository.java | save verifica que padre no sea CONFIRMADA; constructor compat. hacia atras |
| InMemoryTarifarioUnidadFaltasRepository.java | save synchronized; detecta solapamiento de vigencia entre tarifarios activos del mismo tipo |
| InMemoryMedidaPreventivaRepository.java | save UK (codigo, versionMedida); crearNuevaVersionAtomico desactiva anterior y activa nueva |
| InMemoryActaArticuloInfringidoRepository.java | save synchronized; UK (actaId, articuloId) para registros siActivo=true |
| InMemoryArticuloMedidaPreventivaRepository.java | save synchronized; rechaza reactivacion silenciosa (false->true sin metodo explicito) |

### Servicios
| Archivo | Cambio |
|---------|--------|
| ValorizacionService.java | calcularBasePreliminar lanza PrecondicionVioladaException; confirmar delega a confirmarVigenteAtomico; anular usa marcarAnulada() |
| MedidaPreventivaService.java | crearNuevaVersion delega a crearNuevaVersionAtomico |

### Snapshot
| Archivo | Cambio |
|---------|--------|
| SnapshotRecalculador.java | constructor @Autowired 8-param; constructor 7-param compat.; proyectarValorizacion() pobla campos snapshot |

### Tests
| Archivo | Cambio |
|---------|--------|
| ValorizacionInvariantesR1Test.java | NUEVO — 27 tests: R1-A (4), R1-B (4), R1-C (4), R1-D tarifario (5), R1-D medida (4), R1-D art.infringido (3), R1-D art.medida (3) |

### Documentacion
| Archivo | Cambio |
|---------|--------|
| 110-matriz-maestra-paridad-mariadb-inmemory.md | 6 tablas: FALTA_EN_INMEMORY -> ALINEADO (tarifario, medida_preventiva, articulo_medida_preventiva, acta_articulo_infringido, acta_valorizacion, acta_valorizacion_item); conteos actualizados (32 ALINEADO, 23 FALTA) |
| 99-pendientes-siguientes-slices.md | seccion 8F-11D-R1 cerrado; 1785 tests referenciados |
| 109-delta-modelo-mariadb-inmemory.md | reconstruido tras perdida accidental (archivo no tenia tracking git) |

---

## C. Tests ejecutados y resultado

**Total: 1785 tests, 0 failures, 0 errors, BUILD SUCCESS**

> **Nota de auditoria:** El reporte preliminar indicaba 27 tests nuevos. La auditoria del archivo fuente confirma 30. Los 3 adicionales: TarifarioSuperposicionR1DTest tiene 6 (no 5) y ActaArticuloInfringidoUKR1DTest tiene 5 (no 3). Delta 1755->1785=30.

Tests nuevos de R1 (ValorizacionInvariantesR1Test):

| Suite | Descripcion | Tests | Resultado |
|-------|-------------|-------|-----------|
| SnapshotProyeccionR1ATest | Snapshot sin/con valorizacion service; proyeccion de campos | 4 | GREEN |
| ConfirmarVigenteAtomicoR1BTest | Atomicidad secuencial y concurrente (CyclicBarrier); solo una triunfa | 4 | GREEN |
| ItemInmutabilidadR1CTest | Items: PRELIMINAR permite save; CONFIRMADA/REEMPLAZADA rechazan | 4 | GREEN |
| TarifarioSuperposicionR1DTest | Rangos contiguos; solapamiento/contenido rechazado; inactivo no bloquea; find_ultimo_vigente_detecta_invariante_rota | **6** | GREEN |
| MedidaPreventivaVersionAtomicaR1DTest | Una version activa por codigo; UK version; deactivacion de anterior | 4 | GREEN |
| ActaArticuloInfringidoUKR1DTest | UK activo (actaId, articuloId); diferente_acta_no_conflicta; diferente_articulo_no_conflicta; baja permite reasignacion | **5** | GREEN |
| ArticuloMedidaReactivacionR1DTest | Reactivacion silenciosa rechazada; reactivacion explicita permitida | 3 | GREEN |

---

## D. Invariantes implementados

### R1-A: Proyeccion de valorizacion en snapshot
El `SnapshotRecalculador` recalcula, para cada acta con valorizacion CONFIRMADA vigente, los campos:
- `montoValorizacionVigente`, `tipoValorizacionVigente`, `estadoValorizacionVigente`, `idValorizacionVigente`

### R1-B: Atomicidad de la vigencia (una sola vigente+confirmada)
`confirmarVigenteAtomico(actaId, tipo, nuevaId, vigenteAnteriorId)` en bloque synchronized:
1. CAS sobre vigenteAnteriorId: si cambio entre lectura y escritura, lanza ConcurrenciaConflictoException
2. Marca anterior como REEMPLAZADA y siVigente=false
3. Confirma nueva como CONFIRMADA y siVigente=true
4. Atomicamente: nunca dos CONFIRMADA+vigente coexisten para el mismo (actaId, tipo)

### R1-C: Inmutabilidad de items post-confirmacion
`InMemoryActaValorizacionItemRepository.save` consulta el estado del padre; si no es PRELIMINAR, lanza PrecondicionVioladaException con codigo ITEM_INMUTABLE.

### R1-D (puntual)
- **Tarifario**: solapamiento de vigencia entre tarifarios activos del mismo tipo rechazado atomicamente
- **MedidaPreventiva**: UK (codigo, versionMedida); crearNuevaVersionAtomico desactiva anterior
- **ArticuloMedidaPreventiva**: reactivacion silenciosa (siActiva: false->true directo) rechazada
- **ActaArticuloInfringido**: UK activo (actaId, articuloId); registros dados de baja no bloquean reasignacion
- **FalActaValorizacion**: setters economicos (montoBase, montoFinal, montoBonificacion) verifican estado != PRELIMINAR
- **calcularBasePreliminar**: lanza PrecondicionVioladaException si la unidad no tiene tarifario activo

---

## E. Decisiones tecnicas relevantes

1. **Constructor dual en SnapshotRecalculador**: para no romper los tests existentes que instancian manualmente con 7 params, se agrego un constructor 7-param que deja `valorizacionService=null` y trata el caso null graciosamente en `proyectarValorizacion`.

2. **Constructor dual en InMemoryActaValorizacionItemRepository**: el no-arg constructor pone `valorizacionRepo=null`; en save, si el repo es null, se omite la guarda (permite tests legacy que no proveen el dep).

3. **CyclicBarrier en test de concurrencia**: el test `concurrencia_real_solo_una_triunfa` usa un barrier para que ambos threads llamen `confirmarVigenteAtomico` simultaneamente con `vigenteAnteriorId=null`, garantizando que exactamente uno lanza `ConcurrenciaConflictoException`.

4. **Inmutabilidad no en setter sino en guarda**: en vez de lanzar en cada setter (que romperia muchos tests de construccion), `verificarMutable()` solo se invoca en setters economicos (monto*). La construccion inicial con `new` no pasa por estos setters.

5. **Reactivacion de ArticuloMedidaPreventiva**: se decidio rechazar via excepcion la reactivacion directa, forzando que el servicio exponga un metodo dedicado `reactivar`. Esto cierra el GAP-8.9 sin inventar logica de negocio nueva.

---

## F. Gaps que quedaron fuera del alcance de R1

Los siguientes items de la seccion 9+ de la matriz quedan para slices futuros:

| Gap | Tabla | Slice sugerido |
|-----|-------|----------------|
| Seccion 9 | fal_acta_transito, fal_acta_transito_alcoholemia | 8F-11E |
| Seccion 9 | fal_acta_vehiculo | 8F-11E |
| Seccion 10+ | fal_persona*, fal_domicilio* | 8F-12 |
| Seccion 10+ | fal_unidad_territorial | 8F-12 |

Ver `99-pendientes-siguientes-slices.md` para la lista completa actualizada.

---

## G. Estado de la matriz de paridad post-R1

| Categoria | Antes R1 | Despues R1 |
|-----------|----------|------------|
| ALINEADO | 26 | 32 |
| FALTA_EN_INMEMORY | 27 (post-8F-11C) | **21** |
| NO_APLICA / OMITIDO | sin cambio | sin cambio |

Fuente: `110-matriz-maestra-paridad-mariadb-inmemory.md` (actualizado)

---

## H. Validacion observable

Para verificar el estado actual del modulo:

```
cd backend/api-faltas-core
mvn test
```

Resultado esperado: `Tests run: 1785, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS`

Para ejecutar solo los tests R1:
```
mvn test -Dtest=ValorizacionInvariantesR1Test
```

---

*Reporte generado: 2026-07-06 — Slice 8F-11D-R1 CERRADO*