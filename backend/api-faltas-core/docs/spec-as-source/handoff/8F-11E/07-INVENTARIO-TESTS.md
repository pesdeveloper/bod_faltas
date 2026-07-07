# Inventario de Tests â€” 8F-11E

## Archivo principal

SatelitesCatalogosTest.java â€” 116 tests nuevos

## Nested Classes

| Clase | DescripciÃ³n |
|-------|-------------|
| TipoPruebaAlcoholemiaTest | 5 tests: tamaÃ±o, codigos, fromCodigo, desconocido, no-ordinal |
| ResultadoCualitativoAlcoholemiaTest | 4 tests |
| UnidadMedidaAlcoholemiaTest | 4 tests |
| TipoVehiculoTest | 5 tests |
| EstadoGeneralVehiculoTest | 4 tests |
| OrigenNomenclaturaTest | 4 tests |
| MotivoNomenclaturaManualTest | 4 tests |
| AmbitoCtvTest | 4 tests |
| EstadoMedidaAplicadaTest | 4 tests |
| VehiculoMarcaTests | 11 tests: alta, codigo-max, nombre-max, codigo-dup, nombre-dup, baja, findById-inexistente, copia, secuencia, concurrencia-CyclicBarrier |
| VehiculoModeloTests | 8 tests: alta, codigo-dup, nombre-dup, baja, inexistente, marca-inexistente, findActivas, historico |
| RubroVersionTests | 9 tests: alta, unchanged, cambio-version, deshabilitado, findActual, findAllActualesActivas, una-actual, coherencia-ok, coherencia-incoherente, concurrencia-CyclicBarrier |
| ActaTransitoTests | 5 tests: 1:1, duplicado, inexistente, flags-default, copia |
| ActaTransito 
            $m = # Inventario de Tests â€” 8F-11E

## Archivo principal

SatelitesCatalogosTest.java â€” 116 tests nuevos

## Nested Classes

| Clase | DescripciÃ³n |
|-------|-------------|
| TipoPruebaAlcoholemiaTest | 5 tests: tamaÃ±o, codigos, fromCodigo, desconocido, no-ordinal |
| ResultadoCualitativoAlcoholemiaTest | 4 tests |
| UnidadMedidaAlcoholemiaTest | 4 tests |
| TipoVehiculoTest | 5 tests |
| EstadoGeneralVehiculoTest | 4 tests |
| OrigenNomenclaturaTest | 4 tests |
| MotivoNomenclaturaManualTest | 4 tests |
| AmbitoCtvTest | 4 tests |
| EstadoMedidaAplicadaTest | 4 tests |
| VehiculoMarcaTests | 11 tests: alta, codigo-max, nombre-max, codigo-dup, nombre-dup, baja, findById-inexistente, copia, secuencia, concurrencia-CyclicBarrier |
| VehiculoModeloTests | 8 tests: alta, codigo-dup, nombre-dup, baja, inexistente, marca-inexistente, findActivas, historico |
| RubroVersionTests | 9 tests: alta, unchanged, cambio-version, deshabilitado, findActual, findAllActualesActivas, una-actual, coherencia-ok, coherencia-incoherente, concurrencia-CyclicBarrier |
| ActaTransitoTests | 5 tests: 1:1, duplicado, inexistente, flags-default, copia |
| ActaTransitoAlcoholemiaTests | 6 tests: medicion-valida, orden-unico, numerico-sin-unidad, escala-max-2, una-final, reemplazo-final, concurrencia-CyclicBarrier |
| ActaVehiculoTests | 7 tests: fallback-textual, modelo-otra-marca, marca-modelo-correctos, duplicado, inexistente, dominio-max-10, marca-inactiva-historica |
| ActaContravencionTests | 9 tests: valido, manual-excepcional, manual-origen-incorrecto, pares-suj-bie-c, ambito-otro, ambito-no-otro, ambito-txt-max-80, secc-max-2, rubro-coherente, rubro-incoherente, duplicado, nomenclatura-resumen |
| ActaSustanciasTests | 5 tests: valido, rubro-coherente, ambito-otro, descripcion-larga, duplicado, historico-rubro |
| ActaMedidaPreventivaTests | 8 tests: aplicar, otro-acta, no-aplicable, crea-bloqueante, sin-bloqueante, levantar-resuelve, cumplida-resuelve, historial, articulo-inactivo |
| SnapshotProyeccionTests | 5 tests: proyecta-licencia, sin-transito-null, proyecta-idBie, proyecta-nomenclatura, sin-contravencion-null |

## Total

- **1901 tests** total en el build
- **+116** tests respecto al baseline (1785).Value
            if ($m -ceq $m.ToUpper()) { 'ALCOHOLEMIA' }
            elseif ($m -ceq ($m.Substring(0,1).ToUpper() + $m.Substring(1).ToLower())) { 'Alcoholemia' }
            elseif ($m -ceq $m.ToLower()) { 'alcoholemia' }
            else { 'alcoholemia' }
        Tests | 6 tests: medicion-valida, orden-unico, numerico-sin-unidad, escala-max-2, una-final, reemplazo-final, concurrencia-CyclicBarrier |
| ActaVehiculoTests | 7 tests: fallback-textual, modelo-otra-marca, marca-modelo-correctos, duplicado, inexistente, dominio-max-10, marca-inactiva-historica |
| ActaContravencionTests | 9 tests: valido, manual-excepcional, manual-origen-incorrecto, pares-suj-bie-c, ambito-otro, ambito-no-otro, ambito-txt-max-80, secc-max-2, rubro-coherente, rubro-incoherente, duplicado, nomenclatura-resumen |
| ActaSustanciasTests | 5 tests: valido, rubro-coherente, ambito-otro, descripcion-larga, duplicado, historico-rubro |
| ActaMedidaPreventivaTests | 8 tests: aplicar, otro-acta, no-aplicable, crea-bloqueante, sin-bloqueante, levantar-resuelve, cumplida-resuelve, historial, articulo-inactivo |
| SnapshotProyeccionTests | 5 tests: proyecta-licencia, sin-transito-null, proyecta-idBie, proyecta-nomenclatura, sin-contravencion-null |

## Total

- **1901 tests** total en el build
- **+116** tests respecto al baseline (1785)