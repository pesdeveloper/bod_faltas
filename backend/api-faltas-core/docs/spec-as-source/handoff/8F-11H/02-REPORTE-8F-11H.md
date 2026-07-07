# 02 - Reporte 8F-11H

## Identificacion

- Slice: 8F-11H
- Fecha: 2026-07-06
- Modulo: backend/api-faltas-core

## Objetivo

Implementar el modelo completo de obligaciones, formas, planes y movimientos de pago para actas de faltas.

## Decisiones de diseno

### D1: Mantener compatibilidad con modelo viejo (FalPagoVoluntario, FalPagoCondena)
- Se crearon `PagoVoluntarioAdapterRepository` y `PagoCondenaAdapterRepository` como `@Repository @Primary`
- Los repos viejos `InMemoryPagoVoluntario/CondenaRepository` quedan sin `@Repository`, usados directamente en CasoUsoFuncionalRunner
- Esto garantiza que los 2048 tests existentes sigan pasando sin cambio alguno

### D2: PagoMovimientoReducer es stateless (proyecta, no muta)
- El reducer solo recibe listas y devuelve estados; no modifica los objetos
- PagoIntegracionService aplica los estados calculados y persiste los cambios
- Esto permite recalcular desde cualquier historial sin efectos secundarios

### D3: FalActaPagoMovimiento es append-only con Builder
- No tiene versionRow (no se modifica ni borra)
- El patron Builder permite campos opcionales sin constructores gigantes
- Idempotencia por `referenciaExterna`: mismo tipo + mismo obligId devuelve el existente; tipo diferente lanza MovimientoPagoDuplicadoException

### D4: FalActaObligacionPago.cancelar() vs setEstadoObligacion()
- `cancelar()` es la transicion de dominio real: valida estado previo, setea CANCELADA y registra fhCancelacion
- `setEstadoObligacion()` es un setter para proyeccion (SnapshotRecalculador, seeders)
- No se pueden llamar ambos secuencialmente para el mismo objeto

### D5: SnapshotRecalculador con @Autowired(required=false)
- Los 4 repos nuevos se inyectan como Optional via @Autowired(required=false)
- Si no estan disponibles (tests viejos sin contexto Spring), proyectarPagos() es no-op
- No se rompe ningun test existente

### D6: 12 eventos nuevos en TipoEventoActa
- OBLDET, OBLAUL, DEBEMI, FPCGEN, FPPGEN, FPREFN, PAGPRC, PAGCFT, PAGANU, MOVPAG, PLNCAI, PLNCAN
- Convención: 6 caracteres, mayusculas, descriptivos del evento real

## Tablas MariaDB reconciliadas en este slice

| Tabla | Estado anterior | Estado nuevo |
|-------|-----------------|--------------|
| fal_acta_obligacion_pago | SEMANTICA_INCOMPATIBLE | ALINEADO |
| fal_acta_forma_pago | FALTA_EN_INMEMORY | ALINEADO |
| fal_acta_plan_pago_ref | FALTA_EN_INMEMORY | ALINEADO |
| fal_acta_pago_movimiento | FALTA_EN_INMEMORY | ALINEADO |

## Tests por clase

| Clase | Tests |
|-------|-------|
| ObligacionPagoTest | 13 |
| FormaPagoMovimientoTest | 16 |
| PlanPagoMovimientoConcurrenciaTest | 15 |
| PagoMovimientoReducerTest | 15 |
| **Total nuevos** | **59** |

## Cobertura de invariantes

- [x] Unicidad vigente por acta/obligacion (OCC + repo)
- [x] Monto con escala 2, no negativo
- [x] Triple EM y triple PG: completos o todos NULL
- [x] nroForma positivo y unico por obligacion
- [x] Par (idTdocPlan, idDocPlan) unico globalmente
- [x] Movimientos append-only (no update, no delete)
- [x] Idempotencia por referenciaExterna
- [x] OCC en obligacion, forma y plan (concurrencia 10-hilos)
- [x] Reduccion deterministica fuera de orden
- [x] Seeder 3 casos demo (contado confirmado, plan en mora, refinanciacion)

## Gap con MariaDB productivo

Las siguientes columnas del modelo productivo no tienen campo en InMemory aun:
- `fal_acta_obligacion_pago.si_apt_intimacion` -> campo en FalActaObligacionPago ausente
- `fal_acta_plan_pago_ref.nro_plan` -> campo en FalActaPlanPagoRef ausente (usa par idTdoc/idDoc como clave)
- Detalle de cuotas individuales -> sin entidad
- Intimaciones de pago -> sin entidad

Estos gaps quedan documentados para 8F-11I (JDBC/MariaDB migration).