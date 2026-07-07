# Inventario de Repositories â€” 8F-11E

## Interfaces nuevas

| Interface | Operaciones clave |
|-----------|-------------------|
| VehiculoMarcaRepository | nextId, guardar, findById, findByCodigo, findByNombre, findAllActivas |
| VehiculoModeloRepository | nextId, guardar, findById, findByMarcaAndCodigo, findByMarcaAndNombre, findActivasByMarca |
| RubroVersionRepository | nextId, sincronizarAtomicamente, findByRubroId, findActualByIdRub, findAllActualesActivas, findByIdRub |
| ActaTransitoRepository | nextId, guardar, findByActaId, existsByActaId |
| ActaTransito 
            $m = # Inventario de Repositories â€” 8F-11E

## Interfaces nuevas

| Interface | Operaciones clave |
|-----------|-------------------|
| VehiculoMarcaRepository | nextId, guardar, findById, findByCodigo, findByNombre, findAllActivas |
| VehiculoModeloRepository | nextId, guardar, findById, findByMarcaAndCodigo, findByMarcaAndNombre, findActivasByMarca |
| RubroVersionRepository | nextId, sincronizarAtomicamente, findByRubroId, findActualByIdRub, findAllActualesActivas, findByIdRub |
| ActaTransitoRepository | nextId, guardar, findByActaId, existsByActaId |
| ActaTransitoAlcoholemiaRepository | nextId, guardar, findById, findByActaId, findResultadoFinalByActaId, existsOrdenByActaId, marcarResultadoFinalAtomicamente |
| ActaVehiculoRepository | nextId, guardar, findByActaId, existsByActaId |
| ActaContravencionRepository | nextId, guardar, findByActaId, existsByActaId |
| ActaSustanciasAlimenticiasRepository | nextId, guardar, findByActaId, existsByActaId |
| ActaMedidaPreventivaRepository | nextId, guardar, findById, findByActaId, findActivasByActaId |

## Operaciones atÃ³micas

| Repo | MÃ©todo atÃ³mico | Mecanismo |
|------|----------------|-----------|
| InMemoryRubroVersionRepository | sincronizarAtomicamente() | synchronized block en store |
| InMemoryActaTransitoAlcoholemiaRepository | marcarResultadoFinalAtomicamente() | lockFinal object synchronized |
| InMemoryVehiculoMarcaRepository | (via VehiculoMarcaService.altaMarca) | synchronized method |
| InMemoryVehiculoModeloRepository | (via VehiculoModeloService.altaModelo) | synchronized method |.Value
            if ($m -ceq $m.ToUpper()) { 'ALCOHOLEMIA' }
            elseif ($m -ceq ($m.Substring(0,1).ToUpper() + $m.Substring(1).ToLower())) { 'Alcoholemia' }
            elseif ($m -ceq $m.ToLower()) { 'alcoholemia' }
            else { 'alcoholemia' }
        Repository | nextId, guardar, findById, findByActaId, findResultadoFinalByActaId, existsOrdenByActaId, marcarResultadoFinalAtomicamente |
| ActaVehiculoRepository | nextId, guardar, findByActaId, existsByActaId |
| ActaContravencionRepository | nextId, guardar, findByActaId, existsByActaId |
| ActaSustanciasAlimenticiasRepository | nextId, guardar, findByActaId, existsByActaId |
| ActaMedidaPreventivaRepository | nextId, guardar, findById, findByActaId, findActivasByActaId |

## Operaciones atÃ³micas

| Repo | MÃ©todo atÃ³mico | Mecanismo |
|------|----------------|-----------|
| InMemoryRubroVersionRepository | sincronizarAtomicamente() | synchronized block en store |
| InMemoryActaTransito 
            $m = # Inventario de Repositories â€” 8F-11E

## Interfaces nuevas

| Interface | Operaciones clave |
|-----------|-------------------|
| VehiculoMarcaRepository | nextId, guardar, findById, findByCodigo, findByNombre, findAllActivas |
| VehiculoModeloRepository | nextId, guardar, findById, findByMarcaAndCodigo, findByMarcaAndNombre, findActivasByMarca |
| RubroVersionRepository | nextId, sincronizarAtomicamente, findByRubroId, findActualByIdRub, findAllActualesActivas, findByIdRub |
| ActaTransitoRepository | nextId, guardar, findByActaId, existsByActaId |
| ActaTransitoAlcoholemiaRepository | nextId, guardar, findById, findByActaId, findResultadoFinalByActaId, existsOrdenByActaId, marcarResultadoFinalAtomicamente |
| ActaVehiculoRepository | nextId, guardar, findByActaId, existsByActaId |
| ActaContravencionRepository | nextId, guardar, findByActaId, existsByActaId |
| ActaSustanciasAlimenticiasRepository | nextId, guardar, findByActaId, existsByActaId |
| ActaMedidaPreventivaRepository | nextId, guardar, findById, findByActaId, findActivasByActaId |

## Operaciones atÃ³micas

| Repo | MÃ©todo atÃ³mico | Mecanismo |
|------|----------------|-----------|
| InMemoryRubroVersionRepository | sincronizarAtomicamente() | synchronized block en store |
| InMemoryActaTransitoAlcoholemiaRepository | marcarResultadoFinalAtomicamente() | lockFinal object synchronized |
| InMemoryVehiculoMarcaRepository | (via VehiculoMarcaService.altaMarca) | synchronized method |
| InMemoryVehiculoModeloRepository | (via VehiculoModeloService.altaModelo) | synchronized method |.Value
            if ($m -ceq $m.ToUpper()) { 'ALCOHOLEMIA' }
            elseif ($m -ceq ($m.Substring(0,1).ToUpper() + $m.Substring(1).ToLower())) { 'Alcoholemia' }
            elseif ($m -ceq $m.ToLower()) { 'alcoholemia' }
            else { 'alcoholemia' }
        Repository | marcarResultadoFinalAtomicamente() | lockFinal object synchronized |
| InMemoryVehiculoMarcaRepository | (via VehiculoMarcaService.altaMarca) | synchronized method |
| InMemoryVehiculoModeloRepository | (via VehiculoModeloService.altaModelo) | synchronized method |