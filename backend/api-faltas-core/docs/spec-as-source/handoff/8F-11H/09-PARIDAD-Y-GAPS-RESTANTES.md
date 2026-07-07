# 09 - Paridad y Gaps Restantes

## Estado de paridad al cierre de 8F-11H

**62 tablas MariaDB. 52 ALINEADO. 4 FALTA_EN_INMEMORY. 0 SEMANTICA_INCOMPATIBLE.**

## Tablas ALINEADAS en este slice (+4)

| Tabla | Entidad Java | Slice |
|-------|-------------|-------|
| fal_acta_obligacion_pago | FalActaObligacionPago | 8F-11H |
| fal_acta_forma_pago | FalActaFormaPago | 8F-11H |
| fal_acta_plan_pago_ref | FalActaPlanPagoRef | 8F-11H |
| fal_acta_pago_movimiento | FalActaPagoMovimiento | 8F-11H |

## Tablas FALTA_EN_INMEMORY restantes (4)

| Tabla | Descripcion | Slice sugerido |
|-------|-------------|---------------|
| fal_acta_pago_movimiento_detalle | Detalle de cuotas individuales (cache sincronizado con Ingresos) | 8F-11I o post-JDBC |
| fal_intimacion_pago | Intimaciones por mora de obligaciones o planes | 8F-11I o post-JDBC |
| fal_acta_concurso_infracciones | Concurso de infracciones (D8) | 8F-11I |
| fal_acta_medida_preventiva_historial | Historial de medidas preventivas levantadas | D9 |

## Tablas PARCIAL (2)

| Tabla | Campos faltantes | Impacto |
|-------|-----------------|---------|
| fal_acta | Campos de pago no mapeados directamente (proyectados via snapshot) | Bajo |
| fal_acta_evento | Nuevos tipos de pago en TipoEventoActa pero sin emisor automatico desde servicios | Bajo |

## Paridad acumulada por slice

| Slice | ALINEADO | FALTA_EN_INMEMORY | Tests |
|-------|----------|-------------------|-------|
| 8F-11A | 32 | 29 | ~1600 |
| 8F-11B | 32 | 29 | ~1650 |
| 8F-11C | 32 | 27 | ~1720 |
| 8F-11D | 38 | 21 | 1785 |
| 8F-11D-R1 | 38 | 21 | 1815 |
| 8F-11E | 41 | 12 | 1901 |
| 8F-11F | 44 | 11 | 1976 |
| 8F-11G | 48 | 7 | 2048 |
| **8F-11H** | **52** | **4** | **2107** |

## Recomendaciones para siguientes slices

### Opcion A: 8F-11I - JDBC/MariaDB Migration
Comenzar la implementacion productiva usando Spring JdbcClient siguiendo el delta
documentado en `109-delta-modelo-mariadb-inmemory.md`.

### Opcion B: 8F-11I - Tablas restantes (4)
Implementar las 4 tablas FALTA_EN_INMEMORY: detalle cuotas, intimaciones,
concurso infracciones, historial medidas preventivas.

### Opcion C: 8F-11I - Endpoints de pago
Exponer endpoints REST para consulta y gestion del ciclo de pago via
PagoIntegracionService.