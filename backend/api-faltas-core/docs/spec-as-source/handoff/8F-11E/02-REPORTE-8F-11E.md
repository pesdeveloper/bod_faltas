# Reporte 8F-11E â€” SatÃ©lites del Acta y CatÃ¡logos VehÃ­culo/Rubro

## 1. AuditorÃ­a inicial

| Tabla | Entidad preexistente | Estado previo |
|-------|---------------------|---------------|
| fal_acta_transito | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_transito_alcoholemia | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_vehiculo | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_contravencion | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_sustancias_alimenticias | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_medida_preventiva | NO EXISTE | FALTA_EN_INMEMORY |
| fal_vehiculo_marca | NO EXISTE | FALTA_EN_INMEMORY |
| fal_vehiculo_modelo | NO EXISTE | FALTA_EN_INMEMORY |
| fal_rubro_version | NO EXISTE | FALTA_EN_INMEMORY |

## 2. Archivos creados

### Enums (9)
- TipoPruebaAlcoholemia.java
- ResultadoCualitativoAlcoholemia.java
- UnidadMedidaAlcoholemia.java
- TipoVehiculo.java
- EstadoGeneralVehiculo.java
- OrigenNomenclatura.java
- MotivoNomenclaturaManual.java
- AmbitoCtv.java
- EstadoMedidaAplicada.java

### Excepciones (9)
- ActaContravencionNoEncontradaException.java
- ActaMedidaPreventivaAplicadaNoEncontradaException.java
- ActaSustanciasNoEncontradaException.java
- ActaTransito 
            $m = # Reporte 8F-11E â€” SatÃ©lites del Acta y CatÃ¡logos VehÃ­culo/Rubro

## 1. AuditorÃ­a inicial

| Tabla | Entidad preexistente | Estado previo |
|-------|---------------------|---------------|
| fal_acta_transito | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_transito_alcoholemia | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_vehiculo | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_contravencion | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_sustancias_alimenticias | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_medida_preventiva | NO EXISTE | FALTA_EN_INMEMORY |
| fal_vehiculo_marca | NO EXISTE | FALTA_EN_INMEMORY |
| fal_vehiculo_modelo | NO EXISTE | FALTA_EN_INMEMORY |
| fal_rubro_version | NO EXISTE | FALTA_EN_INMEMORY |

## 2. Archivos creados

### Enums (9)
- TipoPruebaAlcoholemia.java
- ResultadoCualitativoAlcoholemia.java
- UnidadMedidaAlcoholemia.java
- TipoVehiculo.java
- EstadoGeneralVehiculo.java
- OrigenNomenclatura.java
- MotivoNomenclaturaManual.java
- AmbitoCtv.java
- EstadoMedidaAplicada.java

### Excepciones (9)
- ActaContravencionNoEncontradaException.java
- ActaMedidaPreventivaAplicadaNoEncontradaException.java
- ActaSustanciasNoEncontradaException.java
- ActaTransitoAlcoholemiaNoEncontradaException.java
- ActaTransitoNoEncontradaException.java
- ActaVehiculoNoEncontradoException.java
- RubroVersionNoEncontradoException.java
- VehiculoMarcaNoEncontradaException.java
- VehiculoModeloNoEncontradoException.java

### Entidades (9)
- FalActaContravencion.java
- FalActaMedidaPreventiva.java
- FalActaSustanciasAlimenticias.java
- FalActaTransito.java
- FalActaTransitoAlcoholemia.java
- FalActaVehiculo.java
- FalRubroVersion.java
- FalVehiculoMarca.java
- FalVehiculoModelo.java

### Repository interfaces (9)
- ActaContravencionRepository.java
- ActaMedidaPreventivaRepository.java
- ActaSustanciasAlimenticiasRepository.java
- ActaTransitoAlcoholemiaRepository.java
- ActaTransitoRepository.java
- ActaVehiculoRepository.java
- RubroVersionRepository.java
- VehiculoMarcaRepository.java
- VehiculoModeloRepository.java

### InMemory repositories (9)
- InMemoryActaContravencionRepository.java
- InMemoryActaMedidaPreventivaRepository.java
- InMemoryActaSustanciasAlimenticiasRepository.java
- InMemoryActaTransitoAlcoholemiaRepository.java
- InMemoryActaTransitoRepository.java
- InMemoryActaVehiculoRepository.java
- InMemoryRubroVersionRepository.java
- InMemoryVehiculoMarcaRepository.java
- InMemoryVehiculoModeloRepository.java

### Services (8)
- ActaContravencionService.java
- ActaMedidaPreventivaAplicadaService.java
- ActaSustanciasAlimenticiasService.java
- ActaTransitoService.java
- ActaVehiculoService.java
- RubroVersionService.java
- VehiculoMarcaService.java
- VehiculoModeloService.java

### Seeder (1)
- SatelitesCatalogosSeeder.java

### Tests (1)
- SatelitesCatalogosTest.java (116 tests)

## 3. Archivos modificados

- FalActaSnapshot.java: +5 campos (licenciaProvinciaTxt, licenciaUnidadTxt, nomenclaturaResumen, idBieI, idBieC)
- SnapshotRecalculador.java: +imports, +actaTransitoRepository, +actaContravencionRepository, +proyectarSatelites()

## 4. Enums creados/reutilizados y cÃ³digos

| Enum | Valores | CÃ³digos |
|------|---------|---------|
| TipoPruebaAlcoholemia | ALOMETRO, ALCOHOLIMETRO | 1, 2 |
| ResultadoCualitativoAlcoholemia | NEGATIVO, POSITIVO, INVALIDO, NO_REALIZADO | 1, 2, 3, 4 |
| UnidadMedidaAlcoholemia | G_L, MG_L_AIRE | 1, 2 |
| TipoVehiculo | AUTO...OTRO | 1-10 |
| EstadoGeneralVehiculo | BUENO...NO_APLICA | 1-5 |
| OrigenNomenclatura | CATASTRO...MANUAL_EXCEPCIONAL | 1-6 |
| MotivoNomenclaturaManual | SIN_DATOS_CATASTRO...OTRO | 1-6 |
| AmbitoCtv | BALDIO...OTRO | 1-6 |
| EstadoMedidaAplicada | APLICADA, LEVANTADA, ANULADA, CUMPLIDA | 1, 2, 3, 4 |

Todos: codigo() explÃ­cito, fromCodigo(), rechazo de desconocidos. Sin uso de ordinal.

## 5. Repositories y operaciones atÃ³micas

- InMemoryRubroVersionRepository.sincronizarAtomicamente(): synchronized, cierra versiÃ³n anterior, crea nueva
- InMemoryActaTransitoAlcoholemiaRepository.marcarResultadoFinalAtomicamente(): lockFinal synchronized, desmarca anterior, marca nuevo
- InMemoryVehiculoMarcaRepository/VehiculoMarcaService.altaMarca(): synchronized, unicidad codigo+nombre
- InMemoryVehiculoModeloRepository/VehiculoModeloService.altaModelo(): unicidad por marca

## 6. Servicios y transacciones lÃ³gicas

- ActaTransitoService: 1:1 transito, N alcoholemias, marcarResultadoFinal delegando a repo atÃ³mico
- ActaVehiculoService: 1:1 vehiculo, validacion marca-modelo
- VehiculoMarcaService/VehiculoModeloService: synchronized para unicidad, baja logica
- RubroVersionService: sincronizarAtomicamente, versionado con SHA-256
- ActaContravencionService/ActaSustanciasService: validacion rubro coherente, 1:1
- ActaMedidaPreventivaAplicadaService: synchronized, relacion articulo-medida via catalogo, bloqueante atomico, transiciones+rollback logico

## 7. IntegraciÃ³n con snapshot

- SnapshotRecalculador.proyectarSatelites():
  - actaTransitoRepository: licenciaProvinciaTxt="Prov-{id}", licenciaUnidadTxt=tipo.name()
  - actaContravencionRepository: nomenclaturaResumen, idBieI, idBieC
- Repos inyectados con @Autowired(required=false): compatibilidad hacia atrÃ¡s garantizada
- Snapshot no es fuente primaria; regenerable

## 8. Tests nuevos

- SatelitesCatalogosTest.java: 116 tests
- 11 nested classes:
  - TipoPruebaAlcoholemiaTest, ResultadoCualitativoAlcoholemiaTest, UnidadMedidaAlcoholemiaTest (enums alcoholemia)
  - TipoVehiculoTest, EstadoGeneralVehiculoTest (enums vehiculo)
  - OrigenNomenclaturaTest, MotivoNomenclaturaManualTest, AmbitoCtvTest (enums contravencion)
  - EstadoMedidaAplicadaTest (enum medida)
  - VehiculoMarcaTests (11 tests: CRUD, unicidad, baja, copia, secuencia, concurrencia CyclicBarrier)
  - VehiculoModeloTests (8 tests: CRUD, unicidad, baja, historico)
  - RubroVersionTests (9 tests: alta, unchanged, cambio, deshabilitado, consulta, concurrencia CyclicBarrier)
  - ActaTransitoTests (5 tests: 1:1, duplicado, inexistente, flags, copia)
  - ActaTransitoAlcoholemiaTests (6 tests: medicion, orden, numerico, final, reemplazo, concurrencia)
  - ActaVehiculoTests (7 tests: fallback, marca-modelo, duplicado, inexistente, dominio, historico)
  - ActaContravencionTests (9 tests: controlado, manual, pares, ambito, longitudes, rubro, snapshot)
  - ActaSustanciasTests (5 tests: valido, rubro, ambito, descripcion, historico)
  - ActaMedidaPreventivaTests (8 tests: aplicar, otro-acta, no-aplicable, bloqueante, transiciones, historial)
  - SnapshotProyeccionTests (5 tests: licencia, sin-transito, idBie, nomenclatura, sin-contravencion)

## 9. Build completo

1901 tests â€” 0 failures â€” 0 errors â€” BUILD SUCCESS (2026-07-06T15:08:55-03:00)

## 10. Conteo antes/despuÃ©s

| CategorÃ­a | Pre-8F-11E | Post-8F-11E |
|-----------|-----------|------------|
| ALINEADO | 32 | **41** |
| FALTA_EN_INMEMORY | 21 | **12** |
| PARCIAL | 4 | 4 |
| SEMANTICA_INCOMPATIBLE | 1 | 1 |
| RELACION_INCOMPLETA | 1 | 1 |
| NO_PERSISTIBLE | 3 | 3 |
| Tests | 1785 | **1901** |

## 11. Lista exacta de 12 faltantes posteriores

1. fal_observacion
2. fal_notificacion_intento
3. fal_notificacion_acuse
4. fal_lote_correo
5. fal_acta_apelacion_documento
6. fal_acta_paralizacion
7. fal_acta_archivo
8. fal_acta_forma_pago
9. fal_acta_plan_pago_ref
10. fal_acta_pago_movimiento
11. fal_acta_qr_acceso
12. fal_motivo_archivo

## 12. Gaps no pertenecientes al slice

- fal_acta_fallo: campos PARCIALES (resultado_fallo, valorizacion_id, fh_firma, si_firme, fh_firmeza, origen_firmeza) â†’ 8F-11F
- fal_acta_apelacion: campos PARCIALES (fh_vto_apelacion, version_row) â†’ 8F-11F
- Observaciones administrativas: fal_observacion â†’ 8F-11G o posterior
- Pagos: fal_acta_forma_pago, fal_acta_plan_pago_ref, fal_acta_pago_movimiento â†’ 8F-11H
- QR: fal_acta_qr_acceso â†’ 8F-11I o posterior

## 13. Sin commit

No se realizÃ³ commit. Los cambios estÃ¡n en el working tree listo para revisiÃ³n..Value
            if ($m -ceq $m.ToUpper()) { 'ALCOHOLEMIA' }
            elseif ($m -ceq ($m.Substring(0,1).ToUpper() + $m.Substring(1).ToLower())) { 'Alcoholemia' }
            elseif ($m -ceq $m.ToLower()) { 'alcoholemia' }
            else { 'alcoholemia' }
        NoEncontradaException.java
- ActaTransitoNoEncontradaException.java
- ActaVehiculoNoEncontradoException.java
- RubroVersionNoEncontradoException.java
- VehiculoMarcaNoEncontradaException.java
- VehiculoModeloNoEncontradoException.java

### Entidades (9)
- FalActaContravencion.java
- FalActaMedidaPreventiva.java
- FalActaSustanciasAlimenticias.java
- FalActaTransito.java
- FalActaTransitoAlcoholemia.java
- FalActaVehiculo.java
- FalRubroVersion.java
- FalVehiculoMarca.java
- FalVehiculoModelo.java

### Repository interfaces (9)
- ActaContravencionRepository.java
- ActaMedidaPreventivaRepository.java
- ActaSustanciasAlimenticiasRepository.java
- ActaTransito 
            $m = # Reporte 8F-11E â€” SatÃ©lites del Acta y CatÃ¡logos VehÃ­culo/Rubro

## 1. AuditorÃ­a inicial

| Tabla | Entidad preexistente | Estado previo |
|-------|---------------------|---------------|
| fal_acta_transito | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_transito_alcoholemia | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_vehiculo | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_contravencion | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_sustancias_alimenticias | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_medida_preventiva | NO EXISTE | FALTA_EN_INMEMORY |
| fal_vehiculo_marca | NO EXISTE | FALTA_EN_INMEMORY |
| fal_vehiculo_modelo | NO EXISTE | FALTA_EN_INMEMORY |
| fal_rubro_version | NO EXISTE | FALTA_EN_INMEMORY |

## 2. Archivos creados

### Enums (9)
- TipoPruebaAlcoholemia.java
- ResultadoCualitativoAlcoholemia.java
- UnidadMedidaAlcoholemia.java
- TipoVehiculo.java
- EstadoGeneralVehiculo.java
- OrigenNomenclatura.java
- MotivoNomenclaturaManual.java
- AmbitoCtv.java
- EstadoMedidaAplicada.java

### Excepciones (9)
- ActaContravencionNoEncontradaException.java
- ActaMedidaPreventivaAplicadaNoEncontradaException.java
- ActaSustanciasNoEncontradaException.java
- ActaTransitoAlcoholemiaNoEncontradaException.java
- ActaTransitoNoEncontradaException.java
- ActaVehiculoNoEncontradoException.java
- RubroVersionNoEncontradoException.java
- VehiculoMarcaNoEncontradaException.java
- VehiculoModeloNoEncontradoException.java

### Entidades (9)
- FalActaContravencion.java
- FalActaMedidaPreventiva.java
- FalActaSustanciasAlimenticias.java
- FalActaTransito.java
- FalActaTransitoAlcoholemia.java
- FalActaVehiculo.java
- FalRubroVersion.java
- FalVehiculoMarca.java
- FalVehiculoModelo.java

### Repository interfaces (9)
- ActaContravencionRepository.java
- ActaMedidaPreventivaRepository.java
- ActaSustanciasAlimenticiasRepository.java
- ActaTransitoAlcoholemiaRepository.java
- ActaTransitoRepository.java
- ActaVehiculoRepository.java
- RubroVersionRepository.java
- VehiculoMarcaRepository.java
- VehiculoModeloRepository.java

### InMemory repositories (9)
- InMemoryActaContravencionRepository.java
- InMemoryActaMedidaPreventivaRepository.java
- InMemoryActaSustanciasAlimenticiasRepository.java
- InMemoryActaTransitoAlcoholemiaRepository.java
- InMemoryActaTransitoRepository.java
- InMemoryActaVehiculoRepository.java
- InMemoryRubroVersionRepository.java
- InMemoryVehiculoMarcaRepository.java
- InMemoryVehiculoModeloRepository.java

### Services (8)
- ActaContravencionService.java
- ActaMedidaPreventivaAplicadaService.java
- ActaSustanciasAlimenticiasService.java
- ActaTransitoService.java
- ActaVehiculoService.java
- RubroVersionService.java
- VehiculoMarcaService.java
- VehiculoModeloService.java

### Seeder (1)
- SatelitesCatalogosSeeder.java

### Tests (1)
- SatelitesCatalogosTest.java (116 tests)

## 3. Archivos modificados

- FalActaSnapshot.java: +5 campos (licenciaProvinciaTxt, licenciaUnidadTxt, nomenclaturaResumen, idBieI, idBieC)
- SnapshotRecalculador.java: +imports, +actaTransitoRepository, +actaContravencionRepository, +proyectarSatelites()

## 4. Enums creados/reutilizados y cÃ³digos

| Enum | Valores | CÃ³digos |
|------|---------|---------|
| TipoPruebaAlcoholemia | ALOMETRO, ALCOHOLIMETRO | 1, 2 |
| ResultadoCualitativoAlcoholemia | NEGATIVO, POSITIVO, INVALIDO, NO_REALIZADO | 1, 2, 3, 4 |
| UnidadMedidaAlcoholemia | G_L, MG_L_AIRE | 1, 2 |
| TipoVehiculo | AUTO...OTRO | 1-10 |
| EstadoGeneralVehiculo | BUENO...NO_APLICA | 1-5 |
| OrigenNomenclatura | CATASTRO...MANUAL_EXCEPCIONAL | 1-6 |
| MotivoNomenclaturaManual | SIN_DATOS_CATASTRO...OTRO | 1-6 |
| AmbitoCtv | BALDIO...OTRO | 1-6 |
| EstadoMedidaAplicada | APLICADA, LEVANTADA, ANULADA, CUMPLIDA | 1, 2, 3, 4 |

Todos: codigo() explÃ­cito, fromCodigo(), rechazo de desconocidos. Sin uso de ordinal.

## 5. Repositories y operaciones atÃ³micas

- InMemoryRubroVersionRepository.sincronizarAtomicamente(): synchronized, cierra versiÃ³n anterior, crea nueva
- InMemoryActaTransitoAlcoholemiaRepository.marcarResultadoFinalAtomicamente(): lockFinal synchronized, desmarca anterior, marca nuevo
- InMemoryVehiculoMarcaRepository/VehiculoMarcaService.altaMarca(): synchronized, unicidad codigo+nombre
- InMemoryVehiculoModeloRepository/VehiculoModeloService.altaModelo(): unicidad por marca

## 6. Servicios y transacciones lÃ³gicas

- ActaTransitoService: 1:1 transito, N alcoholemias, marcarResultadoFinal delegando a repo atÃ³mico
- ActaVehiculoService: 1:1 vehiculo, validacion marca-modelo
- VehiculoMarcaService/VehiculoModeloService: synchronized para unicidad, baja logica
- RubroVersionService: sincronizarAtomicamente, versionado con SHA-256
- ActaContravencionService/ActaSustanciasService: validacion rubro coherente, 1:1
- ActaMedidaPreventivaAplicadaService: synchronized, relacion articulo-medida via catalogo, bloqueante atomico, transiciones+rollback logico

## 7. IntegraciÃ³n con snapshot

- SnapshotRecalculador.proyectarSatelites():
  - actaTransitoRepository: licenciaProvinciaTxt="Prov-{id}", licenciaUnidadTxt=tipo.name()
  - actaContravencionRepository: nomenclaturaResumen, idBieI, idBieC
- Repos inyectados con @Autowired(required=false): compatibilidad hacia atrÃ¡s garantizada
- Snapshot no es fuente primaria; regenerable

## 8. Tests nuevos

- SatelitesCatalogosTest.java: 116 tests
- 11 nested classes:
  - TipoPruebaAlcoholemiaTest, ResultadoCualitativoAlcoholemiaTest, UnidadMedidaAlcoholemiaTest (enums alcoholemia)
  - TipoVehiculoTest, EstadoGeneralVehiculoTest (enums vehiculo)
  - OrigenNomenclaturaTest, MotivoNomenclaturaManualTest, AmbitoCtvTest (enums contravencion)
  - EstadoMedidaAplicadaTest (enum medida)
  - VehiculoMarcaTests (11 tests: CRUD, unicidad, baja, copia, secuencia, concurrencia CyclicBarrier)
  - VehiculoModeloTests (8 tests: CRUD, unicidad, baja, historico)
  - RubroVersionTests (9 tests: alta, unchanged, cambio, deshabilitado, consulta, concurrencia CyclicBarrier)
  - ActaTransitoTests (5 tests: 1:1, duplicado, inexistente, flags, copia)
  - ActaTransitoAlcoholemiaTests (6 tests: medicion, orden, numerico, final, reemplazo, concurrencia)
  - ActaVehiculoTests (7 tests: fallback, marca-modelo, duplicado, inexistente, dominio, historico)
  - ActaContravencionTests (9 tests: controlado, manual, pares, ambito, longitudes, rubro, snapshot)
  - ActaSustanciasTests (5 tests: valido, rubro, ambito, descripcion, historico)
  - ActaMedidaPreventivaTests (8 tests: aplicar, otro-acta, no-aplicable, bloqueante, transiciones, historial)
  - SnapshotProyeccionTests (5 tests: licencia, sin-transito, idBie, nomenclatura, sin-contravencion)

## 9. Build completo

1901 tests â€” 0 failures â€” 0 errors â€” BUILD SUCCESS (2026-07-06T15:08:55-03:00)

## 10. Conteo antes/despuÃ©s

| CategorÃ­a | Pre-8F-11E | Post-8F-11E |
|-----------|-----------|------------|
| ALINEADO | 32 | **41** |
| FALTA_EN_INMEMORY | 21 | **12** |
| PARCIAL | 4 | 4 |
| SEMANTICA_INCOMPATIBLE | 1 | 1 |
| RELACION_INCOMPLETA | 1 | 1 |
| NO_PERSISTIBLE | 3 | 3 |
| Tests | 1785 | **1901** |

## 11. Lista exacta de 12 faltantes posteriores

1. fal_observacion
2. fal_notificacion_intento
3. fal_notificacion_acuse
4. fal_lote_correo
5. fal_acta_apelacion_documento
6. fal_acta_paralizacion
7. fal_acta_archivo
8. fal_acta_forma_pago
9. fal_acta_plan_pago_ref
10. fal_acta_pago_movimiento
11. fal_acta_qr_acceso
12. fal_motivo_archivo

## 12. Gaps no pertenecientes al slice

- fal_acta_fallo: campos PARCIALES (resultado_fallo, valorizacion_id, fh_firma, si_firme, fh_firmeza, origen_firmeza) â†’ 8F-11F
- fal_acta_apelacion: campos PARCIALES (fh_vto_apelacion, version_row) â†’ 8F-11F
- Observaciones administrativas: fal_observacion â†’ 8F-11G o posterior
- Pagos: fal_acta_forma_pago, fal_acta_plan_pago_ref, fal_acta_pago_movimiento â†’ 8F-11H
- QR: fal_acta_qr_acceso â†’ 8F-11I o posterior

## 13. Sin commit

No se realizÃ³ commit. Los cambios estÃ¡n en el working tree listo para revisiÃ³n..Value
            if ($m -ceq $m.ToUpper()) { 'ALCOHOLEMIA' }
            elseif ($m -ceq ($m.Substring(0,1).ToUpper() + $m.Substring(1).ToLower())) { 'Alcoholemia' }
            elseif ($m -ceq $m.ToLower()) { 'alcoholemia' }
            else { 'alcoholemia' }
        Repository.java
- ActaTransitoRepository.java
- ActaVehiculoRepository.java
- RubroVersionRepository.java
- VehiculoMarcaRepository.java
- VehiculoModeloRepository.java

### InMemory repositories (9)
- InMemoryActaContravencionRepository.java
- InMemoryActaMedidaPreventivaRepository.java
- InMemoryActaSustanciasAlimenticiasRepository.java
- InMemoryActaTransito 
            $m = # Reporte 8F-11E â€” SatÃ©lites del Acta y CatÃ¡logos VehÃ­culo/Rubro

## 1. AuditorÃ­a inicial

| Tabla | Entidad preexistente | Estado previo |
|-------|---------------------|---------------|
| fal_acta_transito | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_transito_alcoholemia | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_vehiculo | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_contravencion | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_sustancias_alimenticias | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_medida_preventiva | NO EXISTE | FALTA_EN_INMEMORY |
| fal_vehiculo_marca | NO EXISTE | FALTA_EN_INMEMORY |
| fal_vehiculo_modelo | NO EXISTE | FALTA_EN_INMEMORY |
| fal_rubro_version | NO EXISTE | FALTA_EN_INMEMORY |

## 2. Archivos creados

### Enums (9)
- TipoPruebaAlcoholemia.java
- ResultadoCualitativoAlcoholemia.java
- UnidadMedidaAlcoholemia.java
- TipoVehiculo.java
- EstadoGeneralVehiculo.java
- OrigenNomenclatura.java
- MotivoNomenclaturaManual.java
- AmbitoCtv.java
- EstadoMedidaAplicada.java

### Excepciones (9)
- ActaContravencionNoEncontradaException.java
- ActaMedidaPreventivaAplicadaNoEncontradaException.java
- ActaSustanciasNoEncontradaException.java
- ActaTransitoAlcoholemiaNoEncontradaException.java
- ActaTransitoNoEncontradaException.java
- ActaVehiculoNoEncontradoException.java
- RubroVersionNoEncontradoException.java
- VehiculoMarcaNoEncontradaException.java
- VehiculoModeloNoEncontradoException.java

### Entidades (9)
- FalActaContravencion.java
- FalActaMedidaPreventiva.java
- FalActaSustanciasAlimenticias.java
- FalActaTransito.java
- FalActaTransitoAlcoholemia.java
- FalActaVehiculo.java
- FalRubroVersion.java
- FalVehiculoMarca.java
- FalVehiculoModelo.java

### Repository interfaces (9)
- ActaContravencionRepository.java
- ActaMedidaPreventivaRepository.java
- ActaSustanciasAlimenticiasRepository.java
- ActaTransitoAlcoholemiaRepository.java
- ActaTransitoRepository.java
- ActaVehiculoRepository.java
- RubroVersionRepository.java
- VehiculoMarcaRepository.java
- VehiculoModeloRepository.java

### InMemory repositories (9)
- InMemoryActaContravencionRepository.java
- InMemoryActaMedidaPreventivaRepository.java
- InMemoryActaSustanciasAlimenticiasRepository.java
- InMemoryActaTransitoAlcoholemiaRepository.java
- InMemoryActaTransitoRepository.java
- InMemoryActaVehiculoRepository.java
- InMemoryRubroVersionRepository.java
- InMemoryVehiculoMarcaRepository.java
- InMemoryVehiculoModeloRepository.java

### Services (8)
- ActaContravencionService.java
- ActaMedidaPreventivaAplicadaService.java
- ActaSustanciasAlimenticiasService.java
- ActaTransitoService.java
- ActaVehiculoService.java
- RubroVersionService.java
- VehiculoMarcaService.java
- VehiculoModeloService.java

### Seeder (1)
- SatelitesCatalogosSeeder.java

### Tests (1)
- SatelitesCatalogosTest.java (116 tests)

## 3. Archivos modificados

- FalActaSnapshot.java: +5 campos (licenciaProvinciaTxt, licenciaUnidadTxt, nomenclaturaResumen, idBieI, idBieC)
- SnapshotRecalculador.java: +imports, +actaTransitoRepository, +actaContravencionRepository, +proyectarSatelites()

## 4. Enums creados/reutilizados y cÃ³digos

| Enum | Valores | CÃ³digos |
|------|---------|---------|
| TipoPruebaAlcoholemia | ALOMETRO, ALCOHOLIMETRO | 1, 2 |
| ResultadoCualitativoAlcoholemia | NEGATIVO, POSITIVO, INVALIDO, NO_REALIZADO | 1, 2, 3, 4 |
| UnidadMedidaAlcoholemia | G_L, MG_L_AIRE | 1, 2 |
| TipoVehiculo | AUTO...OTRO | 1-10 |
| EstadoGeneralVehiculo | BUENO...NO_APLICA | 1-5 |
| OrigenNomenclatura | CATASTRO...MANUAL_EXCEPCIONAL | 1-6 |
| MotivoNomenclaturaManual | SIN_DATOS_CATASTRO...OTRO | 1-6 |
| AmbitoCtv | BALDIO...OTRO | 1-6 |
| EstadoMedidaAplicada | APLICADA, LEVANTADA, ANULADA, CUMPLIDA | 1, 2, 3, 4 |

Todos: codigo() explÃ­cito, fromCodigo(), rechazo de desconocidos. Sin uso de ordinal.

## 5. Repositories y operaciones atÃ³micas

- InMemoryRubroVersionRepository.sincronizarAtomicamente(): synchronized, cierra versiÃ³n anterior, crea nueva
- InMemoryActaTransitoAlcoholemiaRepository.marcarResultadoFinalAtomicamente(): lockFinal synchronized, desmarca anterior, marca nuevo
- InMemoryVehiculoMarcaRepository/VehiculoMarcaService.altaMarca(): synchronized, unicidad codigo+nombre
- InMemoryVehiculoModeloRepository/VehiculoModeloService.altaModelo(): unicidad por marca

## 6. Servicios y transacciones lÃ³gicas

- ActaTransitoService: 1:1 transito, N alcoholemias, marcarResultadoFinal delegando a repo atÃ³mico
- ActaVehiculoService: 1:1 vehiculo, validacion marca-modelo
- VehiculoMarcaService/VehiculoModeloService: synchronized para unicidad, baja logica
- RubroVersionService: sincronizarAtomicamente, versionado con SHA-256
- ActaContravencionService/ActaSustanciasService: validacion rubro coherente, 1:1
- ActaMedidaPreventivaAplicadaService: synchronized, relacion articulo-medida via catalogo, bloqueante atomico, transiciones+rollback logico

## 7. IntegraciÃ³n con snapshot

- SnapshotRecalculador.proyectarSatelites():
  - actaTransitoRepository: licenciaProvinciaTxt="Prov-{id}", licenciaUnidadTxt=tipo.name()
  - actaContravencionRepository: nomenclaturaResumen, idBieI, idBieC
- Repos inyectados con @Autowired(required=false): compatibilidad hacia atrÃ¡s garantizada
- Snapshot no es fuente primaria; regenerable

## 8. Tests nuevos

- SatelitesCatalogosTest.java: 116 tests
- 11 nested classes:
  - TipoPruebaAlcoholemiaTest, ResultadoCualitativoAlcoholemiaTest, UnidadMedidaAlcoholemiaTest (enums alcoholemia)
  - TipoVehiculoTest, EstadoGeneralVehiculoTest (enums vehiculo)
  - OrigenNomenclaturaTest, MotivoNomenclaturaManualTest, AmbitoCtvTest (enums contravencion)
  - EstadoMedidaAplicadaTest (enum medida)
  - VehiculoMarcaTests (11 tests: CRUD, unicidad, baja, copia, secuencia, concurrencia CyclicBarrier)
  - VehiculoModeloTests (8 tests: CRUD, unicidad, baja, historico)
  - RubroVersionTests (9 tests: alta, unchanged, cambio, deshabilitado, consulta, concurrencia CyclicBarrier)
  - ActaTransitoTests (5 tests: 1:1, duplicado, inexistente, flags, copia)
  - ActaTransitoAlcoholemiaTests (6 tests: medicion, orden, numerico, final, reemplazo, concurrencia)
  - ActaVehiculoTests (7 tests: fallback, marca-modelo, duplicado, inexistente, dominio, historico)
  - ActaContravencionTests (9 tests: controlado, manual, pares, ambito, longitudes, rubro, snapshot)
  - ActaSustanciasTests (5 tests: valido, rubro, ambito, descripcion, historico)
  - ActaMedidaPreventivaTests (8 tests: aplicar, otro-acta, no-aplicable, bloqueante, transiciones, historial)
  - SnapshotProyeccionTests (5 tests: licencia, sin-transito, idBie, nomenclatura, sin-contravencion)

## 9. Build completo

1901 tests â€” 0 failures â€” 0 errors â€” BUILD SUCCESS (2026-07-06T15:08:55-03:00)

## 10. Conteo antes/despuÃ©s

| CategorÃ­a | Pre-8F-11E | Post-8F-11E |
|-----------|-----------|------------|
| ALINEADO | 32 | **41** |
| FALTA_EN_INMEMORY | 21 | **12** |
| PARCIAL | 4 | 4 |
| SEMANTICA_INCOMPATIBLE | 1 | 1 |
| RELACION_INCOMPLETA | 1 | 1 |
| NO_PERSISTIBLE | 3 | 3 |
| Tests | 1785 | **1901** |

## 11. Lista exacta de 12 faltantes posteriores

1. fal_observacion
2. fal_notificacion_intento
3. fal_notificacion_acuse
4. fal_lote_correo
5. fal_acta_apelacion_documento
6. fal_acta_paralizacion
7. fal_acta_archivo
8. fal_acta_forma_pago
9. fal_acta_plan_pago_ref
10. fal_acta_pago_movimiento
11. fal_acta_qr_acceso
12. fal_motivo_archivo

## 12. Gaps no pertenecientes al slice

- fal_acta_fallo: campos PARCIALES (resultado_fallo, valorizacion_id, fh_firma, si_firme, fh_firmeza, origen_firmeza) â†’ 8F-11F
- fal_acta_apelacion: campos PARCIALES (fh_vto_apelacion, version_row) â†’ 8F-11F
- Observaciones administrativas: fal_observacion â†’ 8F-11G o posterior
- Pagos: fal_acta_forma_pago, fal_acta_plan_pago_ref, fal_acta_pago_movimiento â†’ 8F-11H
- QR: fal_acta_qr_acceso â†’ 8F-11I o posterior

## 13. Sin commit

No se realizÃ³ commit. Los cambios estÃ¡n en el working tree listo para revisiÃ³n..Value
            if ($m -ceq $m.ToUpper()) { 'ALCOHOLEMIA' }
            elseif ($m -ceq ($m.Substring(0,1).ToUpper() + $m.Substring(1).ToLower())) { 'Alcoholemia' }
            elseif ($m -ceq $m.ToLower()) { 'alcoholemia' }
            else { 'alcoholemia' }
        Repository.java
- InMemoryActaTransitoRepository.java
- InMemoryActaVehiculoRepository.java
- InMemoryRubroVersionRepository.java
- InMemoryVehiculoMarcaRepository.java
- InMemoryVehiculoModeloRepository.java

### Services (8)
- ActaContravencionService.java
- ActaMedidaPreventivaAplicadaService.java
- ActaSustanciasAlimenticiasService.java
- ActaTransitoService.java
- ActaVehiculoService.java
- RubroVersionService.java
- VehiculoMarcaService.java
- VehiculoModeloService.java

### Seeder (1)
- SatelitesCatalogosSeeder.java

### Tests (1)
- SatelitesCatalogosTest.java (116 tests)

## 3. Archivos modificados

- FalActaSnapshot.java: +5 campos (licenciaProvinciaTxt, licenciaUnidadTxt, nomenclaturaResumen, idBieI, idBieC)
- SnapshotRecalculador.java: +imports, +actaTransitoRepository, +actaContravencionRepository, +proyectarSatelites()

## 4. Enums creados/reutilizados y cÃ³digos

| Enum | Valores | CÃ³digos |
|------|---------|---------|
| TipoPruebaAlcoholemia | ALOMETRO, ALCOHOLIMETRO | 1, 2 |
| ResultadoCualitativoAlcoholemia | NEGATIVO, POSITIVO, INVALIDO, NO_REALIZADO | 1, 2, 3, 4 |
| UnidadMedidaAlcoholemia | G_L, MG_L_AIRE | 1, 2 |
| TipoVehiculo | AUTO...OTRO | 1-10 |
| EstadoGeneralVehiculo | BUENO...NO_APLICA | 1-5 |
| OrigenNomenclatura | CATASTRO...MANUAL_EXCEPCIONAL | 1-6 |
| MotivoNomenclaturaManual | SIN_DATOS_CATASTRO...OTRO | 1-6 |
| AmbitoCtv | BALDIO...OTRO | 1-6 |
| EstadoMedidaAplicada | APLICADA, LEVANTADA, ANULADA, CUMPLIDA | 1, 2, 3, 4 |

Todos: codigo() explÃ­cito, fromCodigo(), rechazo de desconocidos. Sin uso de ordinal.

## 5. Repositories y operaciones atÃ³micas

- InMemoryRubroVersionRepository.sincronizarAtomicamente(): synchronized, cierra versiÃ³n anterior, crea nueva
- InMemoryActaTransito 
            $m = # Reporte 8F-11E â€” SatÃ©lites del Acta y CatÃ¡logos VehÃ­culo/Rubro

## 1. AuditorÃ­a inicial

| Tabla | Entidad preexistente | Estado previo |
|-------|---------------------|---------------|
| fal_acta_transito | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_transito_alcoholemia | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_vehiculo | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_contravencion | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_sustancias_alimenticias | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_medida_preventiva | NO EXISTE | FALTA_EN_INMEMORY |
| fal_vehiculo_marca | NO EXISTE | FALTA_EN_INMEMORY |
| fal_vehiculo_modelo | NO EXISTE | FALTA_EN_INMEMORY |
| fal_rubro_version | NO EXISTE | FALTA_EN_INMEMORY |

## 2. Archivos creados

### Enums (9)
- TipoPruebaAlcoholemia.java
- ResultadoCualitativoAlcoholemia.java
- UnidadMedidaAlcoholemia.java
- TipoVehiculo.java
- EstadoGeneralVehiculo.java
- OrigenNomenclatura.java
- MotivoNomenclaturaManual.java
- AmbitoCtv.java
- EstadoMedidaAplicada.java

### Excepciones (9)
- ActaContravencionNoEncontradaException.java
- ActaMedidaPreventivaAplicadaNoEncontradaException.java
- ActaSustanciasNoEncontradaException.java
- ActaTransitoAlcoholemiaNoEncontradaException.java
- ActaTransitoNoEncontradaException.java
- ActaVehiculoNoEncontradoException.java
- RubroVersionNoEncontradoException.java
- VehiculoMarcaNoEncontradaException.java
- VehiculoModeloNoEncontradoException.java

### Entidades (9)
- FalActaContravencion.java
- FalActaMedidaPreventiva.java
- FalActaSustanciasAlimenticias.java
- FalActaTransito.java
- FalActaTransitoAlcoholemia.java
- FalActaVehiculo.java
- FalRubroVersion.java
- FalVehiculoMarca.java
- FalVehiculoModelo.java

### Repository interfaces (9)
- ActaContravencionRepository.java
- ActaMedidaPreventivaRepository.java
- ActaSustanciasAlimenticiasRepository.java
- ActaTransitoAlcoholemiaRepository.java
- ActaTransitoRepository.java
- ActaVehiculoRepository.java
- RubroVersionRepository.java
- VehiculoMarcaRepository.java
- VehiculoModeloRepository.java

### InMemory repositories (9)
- InMemoryActaContravencionRepository.java
- InMemoryActaMedidaPreventivaRepository.java
- InMemoryActaSustanciasAlimenticiasRepository.java
- InMemoryActaTransitoAlcoholemiaRepository.java
- InMemoryActaTransitoRepository.java
- InMemoryActaVehiculoRepository.java
- InMemoryRubroVersionRepository.java
- InMemoryVehiculoMarcaRepository.java
- InMemoryVehiculoModeloRepository.java

### Services (8)
- ActaContravencionService.java
- ActaMedidaPreventivaAplicadaService.java
- ActaSustanciasAlimenticiasService.java
- ActaTransitoService.java
- ActaVehiculoService.java
- RubroVersionService.java
- VehiculoMarcaService.java
- VehiculoModeloService.java

### Seeder (1)
- SatelitesCatalogosSeeder.java

### Tests (1)
- SatelitesCatalogosTest.java (116 tests)

## 3. Archivos modificados

- FalActaSnapshot.java: +5 campos (licenciaProvinciaTxt, licenciaUnidadTxt, nomenclaturaResumen, idBieI, idBieC)
- SnapshotRecalculador.java: +imports, +actaTransitoRepository, +actaContravencionRepository, +proyectarSatelites()

## 4. Enums creados/reutilizados y cÃ³digos

| Enum | Valores | CÃ³digos |
|------|---------|---------|
| TipoPruebaAlcoholemia | ALOMETRO, ALCOHOLIMETRO | 1, 2 |
| ResultadoCualitativoAlcoholemia | NEGATIVO, POSITIVO, INVALIDO, NO_REALIZADO | 1, 2, 3, 4 |
| UnidadMedidaAlcoholemia | G_L, MG_L_AIRE | 1, 2 |
| TipoVehiculo | AUTO...OTRO | 1-10 |
| EstadoGeneralVehiculo | BUENO...NO_APLICA | 1-5 |
| OrigenNomenclatura | CATASTRO...MANUAL_EXCEPCIONAL | 1-6 |
| MotivoNomenclaturaManual | SIN_DATOS_CATASTRO...OTRO | 1-6 |
| AmbitoCtv | BALDIO...OTRO | 1-6 |
| EstadoMedidaAplicada | APLICADA, LEVANTADA, ANULADA, CUMPLIDA | 1, 2, 3, 4 |

Todos: codigo() explÃ­cito, fromCodigo(), rechazo de desconocidos. Sin uso de ordinal.

## 5. Repositories y operaciones atÃ³micas

- InMemoryRubroVersionRepository.sincronizarAtomicamente(): synchronized, cierra versiÃ³n anterior, crea nueva
- InMemoryActaTransitoAlcoholemiaRepository.marcarResultadoFinalAtomicamente(): lockFinal synchronized, desmarca anterior, marca nuevo
- InMemoryVehiculoMarcaRepository/VehiculoMarcaService.altaMarca(): synchronized, unicidad codigo+nombre
- InMemoryVehiculoModeloRepository/VehiculoModeloService.altaModelo(): unicidad por marca

## 6. Servicios y transacciones lÃ³gicas

- ActaTransitoService: 1:1 transito, N alcoholemias, marcarResultadoFinal delegando a repo atÃ³mico
- ActaVehiculoService: 1:1 vehiculo, validacion marca-modelo
- VehiculoMarcaService/VehiculoModeloService: synchronized para unicidad, baja logica
- RubroVersionService: sincronizarAtomicamente, versionado con SHA-256
- ActaContravencionService/ActaSustanciasService: validacion rubro coherente, 1:1
- ActaMedidaPreventivaAplicadaService: synchronized, relacion articulo-medida via catalogo, bloqueante atomico, transiciones+rollback logico

## 7. IntegraciÃ³n con snapshot

- SnapshotRecalculador.proyectarSatelites():
  - actaTransitoRepository: licenciaProvinciaTxt="Prov-{id}", licenciaUnidadTxt=tipo.name()
  - actaContravencionRepository: nomenclaturaResumen, idBieI, idBieC
- Repos inyectados con @Autowired(required=false): compatibilidad hacia atrÃ¡s garantizada
- Snapshot no es fuente primaria; regenerable

## 8. Tests nuevos

- SatelitesCatalogosTest.java: 116 tests
- 11 nested classes:
  - TipoPruebaAlcoholemiaTest, ResultadoCualitativoAlcoholemiaTest, UnidadMedidaAlcoholemiaTest (enums alcoholemia)
  - TipoVehiculoTest, EstadoGeneralVehiculoTest (enums vehiculo)
  - OrigenNomenclaturaTest, MotivoNomenclaturaManualTest, AmbitoCtvTest (enums contravencion)
  - EstadoMedidaAplicadaTest (enum medida)
  - VehiculoMarcaTests (11 tests: CRUD, unicidad, baja, copia, secuencia, concurrencia CyclicBarrier)
  - VehiculoModeloTests (8 tests: CRUD, unicidad, baja, historico)
  - RubroVersionTests (9 tests: alta, unchanged, cambio, deshabilitado, consulta, concurrencia CyclicBarrier)
  - ActaTransitoTests (5 tests: 1:1, duplicado, inexistente, flags, copia)
  - ActaTransitoAlcoholemiaTests (6 tests: medicion, orden, numerico, final, reemplazo, concurrencia)
  - ActaVehiculoTests (7 tests: fallback, marca-modelo, duplicado, inexistente, dominio, historico)
  - ActaContravencionTests (9 tests: controlado, manual, pares, ambito, longitudes, rubro, snapshot)
  - ActaSustanciasTests (5 tests: valido, rubro, ambito, descripcion, historico)
  - ActaMedidaPreventivaTests (8 tests: aplicar, otro-acta, no-aplicable, bloqueante, transiciones, historial)
  - SnapshotProyeccionTests (5 tests: licencia, sin-transito, idBie, nomenclatura, sin-contravencion)

## 9. Build completo

1901 tests â€” 0 failures â€” 0 errors â€” BUILD SUCCESS (2026-07-06T15:08:55-03:00)

## 10. Conteo antes/despuÃ©s

| CategorÃ­a | Pre-8F-11E | Post-8F-11E |
|-----------|-----------|------------|
| ALINEADO | 32 | **41** |
| FALTA_EN_INMEMORY | 21 | **12** |
| PARCIAL | 4 | 4 |
| SEMANTICA_INCOMPATIBLE | 1 | 1 |
| RELACION_INCOMPLETA | 1 | 1 |
| NO_PERSISTIBLE | 3 | 3 |
| Tests | 1785 | **1901** |

## 11. Lista exacta de 12 faltantes posteriores

1. fal_observacion
2. fal_notificacion_intento
3. fal_notificacion_acuse
4. fal_lote_correo
5. fal_acta_apelacion_documento
6. fal_acta_paralizacion
7. fal_acta_archivo
8. fal_acta_forma_pago
9. fal_acta_plan_pago_ref
10. fal_acta_pago_movimiento
11. fal_acta_qr_acceso
12. fal_motivo_archivo

## 12. Gaps no pertenecientes al slice

- fal_acta_fallo: campos PARCIALES (resultado_fallo, valorizacion_id, fh_firma, si_firme, fh_firmeza, origen_firmeza) â†’ 8F-11F
- fal_acta_apelacion: campos PARCIALES (fh_vto_apelacion, version_row) â†’ 8F-11F
- Observaciones administrativas: fal_observacion â†’ 8F-11G o posterior
- Pagos: fal_acta_forma_pago, fal_acta_plan_pago_ref, fal_acta_pago_movimiento â†’ 8F-11H
- QR: fal_acta_qr_acceso â†’ 8F-11I o posterior

## 13. Sin commit

No se realizÃ³ commit. Los cambios estÃ¡n en el working tree listo para revisiÃ³n..Value
            if ($m -ceq $m.ToUpper()) { 'ALCOHOLEMIA' }
            elseif ($m -ceq ($m.Substring(0,1).ToUpper() + $m.Substring(1).ToLower())) { 'Alcoholemia' }
            elseif ($m -ceq $m.ToLower()) { 'alcoholemia' }
            else { 'alcoholemia' }
        Repository.marcarResultadoFinalAtomicamente(): lockFinal synchronized, desmarca anterior, marca nuevo
- InMemoryVehiculoMarcaRepository/VehiculoMarcaService.altaMarca(): synchronized, unicidad codigo+nombre
- InMemoryVehiculoModeloRepository/VehiculoModeloService.altaModelo(): unicidad por marca

## 6. Servicios y transacciones lÃ³gicas

- ActaTransitoService: 1:1 transito, N alcoholemias, marcarResultadoFinal delegando a repo atÃ³mico
- ActaVehiculoService: 1:1 vehiculo, validacion marca-modelo
- VehiculoMarcaService/VehiculoModeloService: synchronized para unicidad, baja logica
- RubroVersionService: sincronizarAtomicamente, versionado con SHA-256
- ActaContravencionService/ActaSustanciasService: validacion rubro coherente, 1:1
- ActaMedidaPreventivaAplicadaService: synchronized, relacion articulo-medida via catalogo, bloqueante atomico, transiciones+rollback logico

## 7. IntegraciÃ³n con snapshot

- SnapshotRecalculador.proyectarSatelites():
  - actaTransitoRepository: licenciaProvinciaTxt="Prov-{id}", licenciaUnidadTxt=tipo.name()
  - actaContravencionRepository: nomenclaturaResumen, idBieI, idBieC
- Repos inyectados con @Autowired(required=false): compatibilidad hacia atrÃ¡s garantizada
- Snapshot no es fuente primaria; regenerable

## 8. Tests nuevos

- SatelitesCatalogosTest.java: 116 tests
- 11 nested classes:
  - TipoPruebaAlcoholemiaTest, ResultadoCualitativoAlcoholemiaTest, UnidadMedidaAlcoholemiaTest (enums alcoholemia)
  - TipoVehiculoTest, EstadoGeneralVehiculoTest (enums vehiculo)
  - OrigenNomenclaturaTest, MotivoNomenclaturaManualTest, AmbitoCtvTest (enums contravencion)
  - EstadoMedidaAplicadaTest (enum medida)
  - VehiculoMarcaTests (11 tests: CRUD, unicidad, baja, copia, secuencia, concurrencia CyclicBarrier)
  - VehiculoModeloTests (8 tests: CRUD, unicidad, baja, historico)
  - RubroVersionTests (9 tests: alta, unchanged, cambio, deshabilitado, consulta, concurrencia CyclicBarrier)
  - ActaTransitoTests (5 tests: 1:1, duplicado, inexistente, flags, copia)
  - ActaTransito 
            $m = # Reporte 8F-11E â€” SatÃ©lites del Acta y CatÃ¡logos VehÃ­culo/Rubro

## 1. AuditorÃ­a inicial

| Tabla | Entidad preexistente | Estado previo |
|-------|---------------------|---------------|
| fal_acta_transito | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_transito_alcoholemia | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_vehiculo | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_contravencion | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_sustancias_alimenticias | NO EXISTE | FALTA_EN_INMEMORY |
| fal_acta_medida_preventiva | NO EXISTE | FALTA_EN_INMEMORY |
| fal_vehiculo_marca | NO EXISTE | FALTA_EN_INMEMORY |
| fal_vehiculo_modelo | NO EXISTE | FALTA_EN_INMEMORY |
| fal_rubro_version | NO EXISTE | FALTA_EN_INMEMORY |

## 2. Archivos creados

### Enums (9)
- TipoPruebaAlcoholemia.java
- ResultadoCualitativoAlcoholemia.java
- UnidadMedidaAlcoholemia.java
- TipoVehiculo.java
- EstadoGeneralVehiculo.java
- OrigenNomenclatura.java
- MotivoNomenclaturaManual.java
- AmbitoCtv.java
- EstadoMedidaAplicada.java

### Excepciones (9)
- ActaContravencionNoEncontradaException.java
- ActaMedidaPreventivaAplicadaNoEncontradaException.java
- ActaSustanciasNoEncontradaException.java
- ActaTransitoAlcoholemiaNoEncontradaException.java
- ActaTransitoNoEncontradaException.java
- ActaVehiculoNoEncontradoException.java
- RubroVersionNoEncontradoException.java
- VehiculoMarcaNoEncontradaException.java
- VehiculoModeloNoEncontradoException.java

### Entidades (9)
- FalActaContravencion.java
- FalActaMedidaPreventiva.java
- FalActaSustanciasAlimenticias.java
- FalActaTransito.java
- FalActaTransitoAlcoholemia.java
- FalActaVehiculo.java
- FalRubroVersion.java
- FalVehiculoMarca.java
- FalVehiculoModelo.java

### Repository interfaces (9)
- ActaContravencionRepository.java
- ActaMedidaPreventivaRepository.java
- ActaSustanciasAlimenticiasRepository.java
- ActaTransitoAlcoholemiaRepository.java
- ActaTransitoRepository.java
- ActaVehiculoRepository.java
- RubroVersionRepository.java
- VehiculoMarcaRepository.java
- VehiculoModeloRepository.java

### InMemory repositories (9)
- InMemoryActaContravencionRepository.java
- InMemoryActaMedidaPreventivaRepository.java
- InMemoryActaSustanciasAlimenticiasRepository.java
- InMemoryActaTransitoAlcoholemiaRepository.java
- InMemoryActaTransitoRepository.java
- InMemoryActaVehiculoRepository.java
- InMemoryRubroVersionRepository.java
- InMemoryVehiculoMarcaRepository.java
- InMemoryVehiculoModeloRepository.java

### Services (8)
- ActaContravencionService.java
- ActaMedidaPreventivaAplicadaService.java
- ActaSustanciasAlimenticiasService.java
- ActaTransitoService.java
- ActaVehiculoService.java
- RubroVersionService.java
- VehiculoMarcaService.java
- VehiculoModeloService.java

### Seeder (1)
- SatelitesCatalogosSeeder.java

### Tests (1)
- SatelitesCatalogosTest.java (116 tests)

## 3. Archivos modificados

- FalActaSnapshot.java: +5 campos (licenciaProvinciaTxt, licenciaUnidadTxt, nomenclaturaResumen, idBieI, idBieC)
- SnapshotRecalculador.java: +imports, +actaTransitoRepository, +actaContravencionRepository, +proyectarSatelites()

## 4. Enums creados/reutilizados y cÃ³digos

| Enum | Valores | CÃ³digos |
|------|---------|---------|
| TipoPruebaAlcoholemia | ALOMETRO, ALCOHOLIMETRO | 1, 2 |
| ResultadoCualitativoAlcoholemia | NEGATIVO, POSITIVO, INVALIDO, NO_REALIZADO | 1, 2, 3, 4 |
| UnidadMedidaAlcoholemia | G_L, MG_L_AIRE | 1, 2 |
| TipoVehiculo | AUTO...OTRO | 1-10 |
| EstadoGeneralVehiculo | BUENO...NO_APLICA | 1-5 |
| OrigenNomenclatura | CATASTRO...MANUAL_EXCEPCIONAL | 1-6 |
| MotivoNomenclaturaManual | SIN_DATOS_CATASTRO...OTRO | 1-6 |
| AmbitoCtv | BALDIO...OTRO | 1-6 |
| EstadoMedidaAplicada | APLICADA, LEVANTADA, ANULADA, CUMPLIDA | 1, 2, 3, 4 |

Todos: codigo() explÃ­cito, fromCodigo(), rechazo de desconocidos. Sin uso de ordinal.

## 5. Repositories y operaciones atÃ³micas

- InMemoryRubroVersionRepository.sincronizarAtomicamente(): synchronized, cierra versiÃ³n anterior, crea nueva
- InMemoryActaTransitoAlcoholemiaRepository.marcarResultadoFinalAtomicamente(): lockFinal synchronized, desmarca anterior, marca nuevo
- InMemoryVehiculoMarcaRepository/VehiculoMarcaService.altaMarca(): synchronized, unicidad codigo+nombre
- InMemoryVehiculoModeloRepository/VehiculoModeloService.altaModelo(): unicidad por marca

## 6. Servicios y transacciones lÃ³gicas

- ActaTransitoService: 1:1 transito, N alcoholemias, marcarResultadoFinal delegando a repo atÃ³mico
- ActaVehiculoService: 1:1 vehiculo, validacion marca-modelo
- VehiculoMarcaService/VehiculoModeloService: synchronized para unicidad, baja logica
- RubroVersionService: sincronizarAtomicamente, versionado con SHA-256
- ActaContravencionService/ActaSustanciasService: validacion rubro coherente, 1:1
- ActaMedidaPreventivaAplicadaService: synchronized, relacion articulo-medida via catalogo, bloqueante atomico, transiciones+rollback logico

## 7. IntegraciÃ³n con snapshot

- SnapshotRecalculador.proyectarSatelites():
  - actaTransitoRepository: licenciaProvinciaTxt="Prov-{id}", licenciaUnidadTxt=tipo.name()
  - actaContravencionRepository: nomenclaturaResumen, idBieI, idBieC
- Repos inyectados con @Autowired(required=false): compatibilidad hacia atrÃ¡s garantizada
- Snapshot no es fuente primaria; regenerable

## 8. Tests nuevos

- SatelitesCatalogosTest.java: 116 tests
- 11 nested classes:
  - TipoPruebaAlcoholemiaTest, ResultadoCualitativoAlcoholemiaTest, UnidadMedidaAlcoholemiaTest (enums alcoholemia)
  - TipoVehiculoTest, EstadoGeneralVehiculoTest (enums vehiculo)
  - OrigenNomenclaturaTest, MotivoNomenclaturaManualTest, AmbitoCtvTest (enums contravencion)
  - EstadoMedidaAplicadaTest (enum medida)
  - VehiculoMarcaTests (11 tests: CRUD, unicidad, baja, copia, secuencia, concurrencia CyclicBarrier)
  - VehiculoModeloTests (8 tests: CRUD, unicidad, baja, historico)
  - RubroVersionTests (9 tests: alta, unchanged, cambio, deshabilitado, consulta, concurrencia CyclicBarrier)
  - ActaTransitoTests (5 tests: 1:1, duplicado, inexistente, flags, copia)
  - ActaTransitoAlcoholemiaTests (6 tests: medicion, orden, numerico, final, reemplazo, concurrencia)
  - ActaVehiculoTests (7 tests: fallback, marca-modelo, duplicado, inexistente, dominio, historico)
  - ActaContravencionTests (9 tests: controlado, manual, pares, ambito, longitudes, rubro, snapshot)
  - ActaSustanciasTests (5 tests: valido, rubro, ambito, descripcion, historico)
  - ActaMedidaPreventivaTests (8 tests: aplicar, otro-acta, no-aplicable, bloqueante, transiciones, historial)
  - SnapshotProyeccionTests (5 tests: licencia, sin-transito, idBie, nomenclatura, sin-contravencion)

## 9. Build completo

1901 tests â€” 0 failures â€” 0 errors â€” BUILD SUCCESS (2026-07-06T15:08:55-03:00)

## 10. Conteo antes/despuÃ©s

| CategorÃ­a | Pre-8F-11E | Post-8F-11E |
|-----------|-----------|------------|
| ALINEADO | 32 | **41** |
| FALTA_EN_INMEMORY | 21 | **12** |
| PARCIAL | 4 | 4 |
| SEMANTICA_INCOMPATIBLE | 1 | 1 |
| RELACION_INCOMPLETA | 1 | 1 |
| NO_PERSISTIBLE | 3 | 3 |
| Tests | 1785 | **1901** |

## 11. Lista exacta de 12 faltantes posteriores

1. fal_observacion
2. fal_notificacion_intento
3. fal_notificacion_acuse
4. fal_lote_correo
5. fal_acta_apelacion_documento
6. fal_acta_paralizacion
7. fal_acta_archivo
8. fal_acta_forma_pago
9. fal_acta_plan_pago_ref
10. fal_acta_pago_movimiento
11. fal_acta_qr_acceso
12. fal_motivo_archivo

## 12. Gaps no pertenecientes al slice

- fal_acta_fallo: campos PARCIALES (resultado_fallo, valorizacion_id, fh_firma, si_firme, fh_firmeza, origen_firmeza) â†’ 8F-11F
- fal_acta_apelacion: campos PARCIALES (fh_vto_apelacion, version_row) â†’ 8F-11F
- Observaciones administrativas: fal_observacion â†’ 8F-11G o posterior
- Pagos: fal_acta_forma_pago, fal_acta_plan_pago_ref, fal_acta_pago_movimiento â†’ 8F-11H
- QR: fal_acta_qr_acceso â†’ 8F-11I o posterior

## 13. Sin commit

No se realizÃ³ commit. Los cambios estÃ¡n en el working tree listo para revisiÃ³n..Value
            if ($m -ceq $m.ToUpper()) { 'ALCOHOLEMIA' }
            elseif ($m -ceq ($m.Substring(0,1).ToUpper() + $m.Substring(1).ToLower())) { 'Alcoholemia' }
            elseif ($m -ceq $m.ToLower()) { 'alcoholemia' }
            else { 'alcoholemia' }
        Tests (6 tests: medicion, orden, numerico, final, reemplazo, concurrencia)
  - ActaVehiculoTests (7 tests: fallback, marca-modelo, duplicado, inexistente, dominio, historico)
  - ActaContravencionTests (9 tests: controlado, manual, pares, ambito, longitudes, rubro, snapshot)
  - ActaSustanciasTests (5 tests: valido, rubro, ambito, descripcion, historico)
  - ActaMedidaPreventivaTests (8 tests: aplicar, otro-acta, no-aplicable, bloqueante, transiciones, historial)
  - SnapshotProyeccionTests (5 tests: licencia, sin-transito, idBie, nomenclatura, sin-contravencion)

## 9. Build completo

1901 tests â€” 0 failures â€” 0 errors â€” BUILD SUCCESS (2026-07-06T15:08:55-03:00)

## 10. Conteo antes/despuÃ©s

| CategorÃ­a | Pre-8F-11E | Post-8F-11E |
|-----------|-----------|------------|
| ALINEADO | 32 | **41** |
| FALTA_EN_INMEMORY | 21 | **12** |
| PARCIAL | 4 | 4 |
| SEMANTICA_INCOMPATIBLE | 1 | 1 |
| RELACION_INCOMPLETA | 1 | 1 |
| NO_PERSISTIBLE | 3 | 3 |
| Tests | 1785 | **1901** |

## 11. Lista exacta de 12 faltantes posteriores

1. fal_observacion
2. fal_notificacion_intento
3. fal_notificacion_acuse
4. fal_lote_correo
5. fal_acta_apelacion_documento
6. fal_acta_paralizacion
7. fal_acta_archivo
8. fal_acta_forma_pago
9. fal_acta_plan_pago_ref
10. fal_acta_pago_movimiento
11. fal_acta_qr_acceso
12. fal_motivo_archivo

## 12. Gaps no pertenecientes al slice

- fal_acta_fallo: campos PARCIALES (resultado_fallo, valorizacion_id, fh_firma, si_firme, fh_firmeza, origen_firmeza) â†’ 8F-11F
- fal_acta_apelacion: campos PARCIALES (fh_vto_apelacion, version_row) â†’ 8F-11F
- Observaciones administrativas: fal_observacion â†’ 8F-11G o posterior
- Pagos: fal_acta_forma_pago, fal_acta_plan_pago_ref, fal_acta_pago_movimiento â†’ 8F-11H
- QR: fal_acta_qr_acceso â†’ 8F-11I o posterior

## 13. Sin commit

No se realizÃ³ commit. Los cambios estÃ¡n en el working tree listo para revisiÃ³n.