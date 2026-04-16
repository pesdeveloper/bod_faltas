# Tablas snapshot auxiliares y proyecciones

## Finalidad

Este archivo define la tabla snapshot operativa principal del acta y una tabla técnica opcional de control de regeneración.

La fuente primaria de verdad sigue siendo:

- `Acta`
- `ActaEvento`
- tablas satélite
- tablas documentales
- tablas de notificación
- tablas normativas y referenciales
- integraciones económicas cuando corresponda

Este bloque existe para:

- acelerar lecturas operativas
- simplificar consultas frecuentes
- sostener bandejas y estadísticas
- evitar joins costosos para información crítica de decisión

---

## Criterios generales del bloque

- el snapshot es derivado
- el snapshot es regenerable
- no introduce verdad alternativa
- debe contener solo información útil para operación, bandejas, plazos y estadísticas
- debe evitar joins costosos cuando el dato es de consulta permanente

---

## Tabla: ActaSnapshot

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdActa` | `INT8` | No | PK/FK a acta |
| `FhActa` | `DATETIME YEAR TO SECOND` | No | Fecha/hora del acta |
| `IdDep` | `INT` | No | Dependencia proyectada |
| `VerDep` | `SMALLINT` | No | Versión dependencia |
| `IdInsp` | `INT` | No | Inspector proyectado |
| `VerInsp` | `SMALLINT` | No | Versión inspector |
| `BloqueActual` | `SMALLINT` | No | Bloque operativo actual |
| `EstProcAct` | `SMALLINT` | No | Estado de proceso actual |
| `SitAdmAct` | `SMALLINT` | No | Situación administrativa actual |
| `CodBandeja` | `VARCHAR(40)` | No | Código lógico de bandeja |
| `SiVisibleBandeja` | `SMALLINT` | No | 0/1 visible en bandeja |
| `Prioridad` | `SMALLINT` | Sí | Prioridad operativa opcional |
| `SiNotifActa` | `SMALLINT` | No | 0/1 tiene notificación de acta |
| `SiNotifActaProc` | `SMALLINT` | No | 0/1 notificación de acta en proceso |
| `SiNotifActaAcusePend` | `SMALLINT` | No | 0/1 notificación de acta pendiente de acuse |
| `SiNotifMedPrev` | `SMALLINT` | No | 0/1 tiene notificación de medida preventiva |
| `SiNotifMedPrevProc` | `SMALLINT` | No | 0/1 notificación de medida preventiva en proceso |
| `SiNotifMedPrevAcusePend` | `SMALLINT` | No | 0/1 notificación de medida preventiva pendiente de acuse |
| `SiNotifFallo` | `SMALLINT` | No | 0/1 tiene notificación de fallo/acto |
| `SiNotifFalloProc` | `SMALLINT` | No | 0/1 notificación de fallo/acto en proceso |
| `SiNotifFalloAcusePend` | `SMALLINT` | No | 0/1 notificación de fallo/acto pendiente de acuse |
| `CantReintNotif` | `SMALLINT` | No | Cantidad de reintentos de notificación |
| `SiPagoVolunt` | `SMALLINT` | No | 0/1 tiene solicitud/circuito de pago voluntario |
| `FhPagoVolunt` | `DATETIME YEAR TO SECOND` | Sí | Fecha de solicitud/registro pago voluntario |
| `MontoActa` | `DECIMAL(16,2)` | Sí | Monto total exigible actual |
| `SiPagoTotal` | `SMALLINT` | No | 0/1 pago total |
| `SiPlanPago` | `SMALLINT` | No | 0/1 tiene plan de pagos |
| `CantCuotasPlan` | `SMALLINT` | Sí | Cantidad de cuotas del plan |
| `ValorCuotaPlan` | `DECIMAL(16,2)` | Sí | Valor de cuota del plan |
| `CantCaidasPlan` | `SMALLINT` | No | Cantidad de caídas/refinanciaciones |
| `SiGestionExt` | `SMALLINT` | No | 0/1 está en gestión externa |
| `TipoGestionExt` | `SMALLINT` | Sí | Tipo de gestión externa |
| `SiReingresoGestionExt` | `SMALLINT` | No | 0/1 reingresó desde gestión externa |
| `ResultadoGestionExt` | `SMALLINT` | Sí | Resultado resumido de gestión externa |
| `FhVtoPresentacion` | `DATETIME YEAR TO SECOND` | Sí | Vencimiento para presentación/comparecencia |
| `FhVtoApelacion` | `DATETIME YEAR TO SECOND` | Sí | Vencimiento para apelación |
| `FhVtoApremio` | `DATETIME YEAR TO SECOND` | Sí | Vencimiento para derivación/apremio |
| `IdEvtUlt` | `INT8` | Sí | Último evento |
| `IdDocuUlt` | `INT8` | Sí | Último documento |
| `IdNotifUlt` | `INT8` | Sí | Última notificación |
| `FhUltMod` | `DATETIME YEAR TO SECOND` | Sí | Última modificación proyectada |
| `IdUserUltMod` | `CHAR(36)` | Sí | Usuario última modificación |
| `FhSnapshot` | `DATETIME YEAR TO SECOND` | No | Momento de proyección |

### Notas
- Esta tabla reemplaza el enfoque de múltiples snapshots auxiliares.
- Su objetivo es lectura rápida de decisión, bandejas y estadísticas.
- Debe permitir responder rápido:
  - dónde está el acta
  - qué notificaciones tiene
  - qué está pendiente
  - si tiene gestión externa
  - si tiene pago o plan
  - qué plazo corre
- Debe poder regenerarse desde la fuente primaria.
- No reemplaza a `Acta`, `ActaEvento`, `Notificacion`, `Documento`, ni a tablas económicas o externas.

---

## Reglas generales del bloque

- `ActaSnapshot` debe poder regenerarse completamente desde la fuente primaria.
- Si una proyección queda inconsistente, debe priorizarse siempre la fuente primaria.
- Este bloque existe por razones operativas y de performance.
- No se incorporan tablas técnicas adicionales de control si no aportan valor operativo directo al dominio.

---

## Enumeraciones del bloque

### TipoGestionExt
- `1 = APREMIO`
- `2 = DERIVACION_ORGANISMO_EXTERNO`
- `3 = JUZGADO`
- `4 = OTRA`

### ResultadoGestionExt
- `1 = PENDIENTE`
- `2 = EN_TRAMITE`
- `3 = FINALIZADA_OK`
- `4 = FINALIZADA_SIN_RESULTADO`
- `5 = REINGRESADA`
- `6 = CERRADA`

### SiVisibleBandeja
- `0 = NO`
- `1 = SI`