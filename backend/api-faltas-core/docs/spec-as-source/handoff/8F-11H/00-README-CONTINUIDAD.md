# Handoff 8F-11H - Pagos: obligacion, forma, plan, movimiento

## Identificacion

- **Slice:** 8F-11H
- **Fecha cierre:** 2026-07-06
- **Modulo:** backend/api-faltas-core
- **Branch/Build:** BUILD SUCCESS, 2107 tests, 0 failures, 0 errors

## Objetivo del slice

Implementar el modelo completo de pagos de actas de faltas sobre la infraestructura InMemory:
- Reconciliar fal_acta_obligacion_pago (SEMANTICA_INCOMPATIBLE -> ALINEADO)
- Implementar fal_acta_forma_pago, fal_acta_plan_pago_ref, fal_acta_pago_movimiento
- Servicios de orquestacion con idempotencia, OCC y reduccion deterministica de estado
- Adapters de compatibilidad para modelo viejo (FalPagoVoluntario, FalPagoCondena)
- 59 tests nuevos, seeder de pagos con 3 casos demo

## Estado al cierre

| Metrica | Valor |
|---------|-------|
| Tests totales | 2107 |
| Tests nuevos | +59 |
| Failures | 0 |
| Errors | 0 |
| Tablas ALINEADO | 52 (+4 nuevas en este slice) |
| Tablas FALTA_EN_INMEMORY | 4 |
| SEMANTICA_INCOMPATIBLE | 0 (resuelta fal_acta_obligacion_pago) |

## Archivos creados o modificados

### Enums nuevos (9)
- `domain/enums/TipoObligacionPago.java`
- `domain/enums/EstadoObligacionPago.java`
- `domain/enums/TipoFormaPago.java`
- `domain/enums/EstadoFormaPago.java`
- `domain/enums/EstadoPlanPago.java`
- `domain/enums/TipoMovimientoPago.java` (27 tipos)
- `domain/enums/MotivoAnulacionPago.java` (6 motivos)
- `domain/enums/MotivoAptitudIntimacion.java`
- `domain/enums/MotivoBajaFormaPago.java`

### Entidades nuevas (4)
- `domain/model/FalActaObligacionPago.java`
- `domain/model/FalActaFormaPago.java`
- `domain/model/FalActaPlanPagoRef.java`
- `domain/model/FalActaPagoMovimiento.java` (Builder pattern, append-only)

### Excepciones nuevas (4)
- `domain/exception/ObligacionPagoNoEncontradaException.java`
- `domain/exception/FormaPagoNoEncontradaException.java`
- `domain/exception/PlanPagoNoEncontradoException.java`
- `domain/exception/MovimientoPagoDuplicadoException.java`

### Repository interfaces nuevas (4)
- `repository/ObligacionPagoRepository.java`
- `repository/FormaPagoRepository.java`
- `repository/PlanPagoRefRepository.java`
- `repository/PagoMovimientoRepository.java`

### InMemory repositories nuevos (4)
- `repository/memory/InMemoryObligacionPagoRepository.java`
- `repository/memory/InMemoryFormaPagoRepository.java`
- `repository/memory/InMemoryPlanPagoRefRepository.java`
- `repository/memory/InMemoryPagoMovimientoRepository.java`

### Adapters de compatibilidad (2)
- `repository/memory/PagoVoluntarioAdapterRepository.java` (@Repository @Primary)
- `repository/memory/PagoCondenaAdapterRepository.java` (@Repository @Primary)

### Repositories viejos (2, modificados)
- `repository/memory/InMemoryPagoVoluntarioRepository.java` (removido @Repository, @Deprecated)
- `repository/memory/InMemoryPagoCondenaRepository.java` (removido @Repository, @Deprecated)

### Servicios nuevos (6)
- `application/service/PagoMovimientoReducer.java` (reductor deterministico)
- `application/service/ObligacionPagoService.java`
- `application/service/FormaPagoService.java`
- `application/service/PlanPagoService.java`
- `application/service/PagoMovimientoService.java`
- `application/service/PagoIntegracionService.java`

### Comandos nuevos (1)
- `application/command/NotificarMovimientoPagoCommand.java`

### Entidades modificadas (2)
- `domain/model/FalActaSnapshot.java` (+17 campos de pago)
- `snapshot/SnapshotRecalculador.java` (proyectarPagos() opcional, @Autowired(required=false))

### Enums modificados (2)
- `domain/enums/TipoEventoActa.java` (+12 eventos de pago: OBLDET, OBLAUL, DEBEMI, FPCGEN, FPPGEN, FPREFN, PAGPRC, PAGCFT, PAGANU, MOVPAG, PLNCAI, PLNCAN)

### Servicios modificados (1)
- `application/service/ObservacionService.java` (habilita tipos OBLIGACION_PAGO, FORMA_PAGO, PLAN_PAGO, MOVIMIENTO_PAGO)

### Seeder nuevo (1)
- `application/demo/PagoObligacionMockSeeder.java` (3 casos demo)

### Tests nuevos (4 archivos, 59 tests)
- `test/.../application/ObligacionPagoTest.java` (13 tests)
- `test/.../application/FormaPagoMovimientoTest.java` (16 tests)
- `test/.../application/PlanPagoMovimientoConcurrenciaTest.java` (15 tests)
- `test/.../application/PagoMovimientoReducerTest.java` (15 tests)

### Documentacion actualizada (2)
- `docs/spec-as-source/99-pendientes-siguientes-slices.md`
- `docs/spec-as-source/110-matriz-maestra-paridad-mariadb-inmemory.md`

## Invariantes clave implementados

1. **Unicidad vigente por obligacion**: Solo 1 obligacion/forma/plan vigente por acta/obligacion.
2. **OCC en todos los agregados con estado mutable**: versionRow en obligacion, forma y plan.
3. **Append-only en movimientos**: FalActaPagoMovimiento nunca se modifica ni borra.
4. **Idempotencia**: referenciaExterna unica por movimiento; mismo tipo + mismo obligId = devuelve existente.
5. **Triple EM y triple PG**: completos o todos NULL, tanto en forma como en movimiento.
6. **Reduccion deterministica**: PagoMovimientoReducer ordena por fhMovimiento+id; mismo resultado independiente del orden de llegada.
7. **Backcompat total**: 2048 tests existentes siguen pasando sin cambio.

## Tablas FALTA_EN_INMEMORY restantes (4)

1. `fal_acta_pago_movimiento_detalle` - detalle de cuotas individuales (futuro)
2. `fal_intimacion_pago` - intimaciones de pago por mora (futuro)
3. `fal_acta_concurso_infracciones` - concurso de infracciones (D8)
4. `fal_acta_medida_preventiva_historial` - historial medidas preventivas (D9)

## Proximo slice sugerido

**8F-11I - JDBC/MariaDB Migration**: comenzar la implementacion real con Spring JdbcClient
siguiendo el delta documentado en `109-delta-modelo-mariadb-inmemory.md`.