# [ANEXO] DDL LÓGICO — CAPA 06 — INTEGRACIÓN ECONÓMICA

> Este anexo resume la definición lógica de tablas, campos y tipos de datos de la Capa 06.  
> No representa aún el SQL físico final ni incluye la totalidad de índices, constraints ni detalles de implementación.

---

# 1) FINALIDAD DE LA CAPA

La Capa 06 modela la integración entre una `Acta` y el sistema económico existente de cuenta corriente.

No crea un subsistema económico nuevo.

Su objetivo es persistir la identidad económica de la causa y permitir la integración con procesos externos que ya resuelven:

- emisión de deuda
- movimientos
- pagos
- financiación
- planes de pago
- cancelaciones
- lectura de saldo o deuda

---

# 2) CRITERIOS DE DISEÑO CONSOLIDADOS

## 2.1 No duplicación

La capa no crea tablas para:

- deuda
- pagos
- cuotas
- planes
- saldos
- intereses
- comprobantes
- movimientos

Todo eso ya existe en el sistema económico actual.

---

## 2.2 Integración por vínculo persistente

La capa persiste únicamente el vínculo económico estable entre `Acta` y el par:

- `IdSuj`
- `IdBie`

Ese par permite que el sistema económico existente opere sobre la causa.

---

## 2.3 Procesos externos reutilizables

La generación y lectura de información económica se resuelve por integración con rutinas, funciones, procedimientos o consultas existentes.

La capa documenta el punto de integración, pero no persiste esa lógica como nuevas estructuras.

---

## 2.4 Historial transversal no se reemplaza

Los hechos económicos relevantes deben reflejarse también en:

- `ActaEvento`

La capa no reemplaza el historial general.

---

# 3) ENTIDAD DE LA CAPA

La Capa 06 queda compuesta, en principio, por una única entidad principal:

- `SujBieFaltas`

---

# 4) TABLA: SujBieFaltas

## 4.1 Finalidad

Representa la vinculación persistente entre una `Acta` y su identidad económica en cuenta corriente.

No representa deuda ni movimientos.

Representa únicamente el ancla económica del acta.

---

## 4.2 PK

- `PK_SujBieFaltas (Id)`

---

## 4.3 FK

- `FK_SujBieFaltas_Acta (IdActa -> Acta.Id)`

---

## 4.4 Uniques sugeridas

- `UQ_SujBieFaltas_IdActa`
- `UQ_SujBieFaltas_IdSuj_IdBie`

Estas restricciones buscan asegurar:

- un vínculo económico principal por acta
- unicidad del par económico dentro del dominio de faltas

---

## 4.5 Índices sugeridos

- `IX_SujBieFaltas_IdActa`
- `IX_SujBieFaltas_IdSuj`
- `IX_SujBieFaltas_IdBie`
- `IX_SujBieFaltas_IdSuj_IdBie`
- `IX_SujBieFaltas_FechaRegistro`

---

## 4.6 Campos

### 4.6.1 Identidad y relación base

- `Id` → INT8 NOT NULL  
  Identificador único del vínculo económico.

- `IdActa` → INT8 NOT NULL  
  Acta a la que pertenece la cuenta económica.

---

### 4.6.2 Identidad económica

- `IdSuj` → SMALLINT NOT NULL  
  Identificador de sujeto económico utilizado por cuenta corriente.

- `IdBie` → INT NOT NULL  
  Identificador de bien/cuenta económica persistente asociado al acta.

> Observación:
> El par `IdSuj + IdBie` es el ancla que permite ubicar comprobantes y movimientos del sistema económico actual.

---

### 4.6.3 Fecha

- `FechaRegistro` → DATETIME YEAR TO SECOND NOT NULL  
  Fecha/hora en que se registró el vínculo económico del acta.

---

### 4.6.4 Trazabilidad operativa mínima

- `UsuarioRegistro` → VARCHAR(36) NOT NULL  
  Subject del usuario o identidad operativa que registró el vínculo.

- `OrigenRegistro` → SMALLINT NOT NULL  
  Origen técnico/operativo del alta.

  Valores previstos:

  - 1 = MANUAL
  - 2 = SISTEMA
  - 3 = PROCESO
  - 4 = INTEGRACION

---

## 4.7 Reglas lógicas

### Regla 1
Toda fila de `SujBieFaltas` pertenece a una única `Acta`.

### Regla 2
Cada `Acta` debe tener como máximo un vínculo económico persistente.

### Regla 3
El par `IdSuj + IdBie` identifica la cuenta económica utilizada por la causa.

### Regla 4
La capa no persiste deuda, saldo, cuotas, pagos ni planes.

### Regla 5
Toda operación económica posterior se obtiene o ejecuta por integración con el sistema económico existente.

---

## 4.8 Observaciones de diseño

### Observación A
Se adopta una única entidad para mantener la capa liviana.

### Observación B
La capa no replica tablas económicas existentes como `cccmte` o `ccmov`.

### Observación C
La relación con `cccmte`, `ccmov`, `Acogimiento` y `DocXCmte` es funcional/integrativa, no física dentro del modelo de faltas.

---
# 5) INTEGRIDAD CRUZADA DE LA CAPA

## 5.1 Coherencia con Acta

- `SujBieFaltas.IdActa` debe referir a una `Acta` existente.
- una `Acta` no debería tener más de un vínculo económico persistente.

---

## 5.2 Coherencia con sistema económico existente

El par:

- `IdSuj`
- `IdBie`

debe ser coherente con las reglas del sistema económico actual.

La capa asume que ese par es el mecanismo válido para operar con:

- `cccmte`
- `ccmov`
- `Acogimiento`
- `DocXCmte`
- y demás tablas económicas relacionadas

pero no define sus constraints internos.

---

## 5.3 Coherencia con ActaEvento

La capa no obliga físicamente la creación de `ActaEvento`, pero funcionalmente debe integrarse con el historial general.

Eventos típicos que podrían reflejarse desde lógica de negocio:

- CUENTA_ECONOMICA_CREADA
- DEUDA_GENERADA
- PLAN_PAGOS_GENERADO
- PAGO_REGISTRADO
- DEUDA_CANCELADA

---

# 6) PROCESOS DE INTEGRACIÓN ESPERADOS

> Esta sección no define tablas nuevas.  
> Define los procesos conceptuales que Capa 06 necesita para operar.

## 6.1 CrearCuentaEconomicaActa

Finalidad:

- crear o asignar el vínculo económico persistente del acta

Entrada esperada:

- `IdActa`

Salida esperada:

- `IdSuj`
- `IdBie`

---

## 6.2 GenerarDeudaActa

Finalidad:

- generar la deuda inicial del acta en el sistema económico existente

Entrada esperada:

- `IdActa`

Salida esperada:

- identificadores de comprobantes generados o resultado equivalente según implementación

---

## 6.3 ObtenerDeudaActa

Finalidad:

- consultar la deuda vigente de la causa

Entrada esperada:

- `IdActa`

Salida esperada:

- saldo/deuda consolidada, según implementación real

---

## 6.4 ObtenerComprobantesActa

Finalidad:

- recuperar comprobantes y/o relaciones económicas asociadas al acta

Entrada esperada:

- `IdActa`

---

## 6.5 GenerarPlanPagosActa

Finalidad:

- generar plan de pagos sobre deuda existente del acta

Entrada esperada:

- `IdActa`
- parámetros del plan, según implementación

---

## 6.6 ObtenerPlanPagosActa

Finalidad:

- consultar plan o acogimiento vigente del acta

Entrada esperada:

- `IdActa`

---

## 6.7 ObtenerPagosActa

Finalidad:

- consultar pagos o cancelaciones imputadas a la deuda del acta

Entrada esperada:

- `IdActa`

---

# 7) CATÁLOGOS / ENUMS LÓGICOS DE LA CAPA

## 7.1 OrigenRegistro

- 1 = MANUAL
- 2 = SISTEMA
- 3 = PROCESO
- 4 = INTEGRACION

---

# 8) REGLAS DE NEGOCIO TRANSVERSALES SUGERIDAS

## Regla A
Toda `Acta` que opere económicamente debe tener vínculo en `SujBieFaltas`.

## Regla B
La deuda del acta se consulta desde el sistema económico existente, no desde tablas duplicadas del modelo de faltas.

## Regla C
La financiación o plan de pagos se resuelve en el sistema económico actual y no genera tablas nuevas en Capa 06.

## Regla D
Los comprobantes económicos del acta se determinan por el vínculo `IdSuj + IdBie`.

## Regla E
La capa 06 no reemplaza la lógica de cuenta corriente; solo la integra.

---

# 9) CONSULTAS OPERATIVAS QUE LA CAPA DEBE HABILITAR

La capa debe permitir consultas como:

- qué `IdSuj` tiene un acta
- qué `IdBie` tiene un acta
- si un acta ya tiene cuenta económica asignada
- deuda del acta por integración
- comprobantes del acta por integración
- plan de pagos del acta por integración
- pagos asociados al acta por integración

---

# 10) CIERRE DEL ANEXO

La Capa 06 queda lógicamente definida como una capa simple de integración económica, basada en una única entidad:

- `SujBieFaltas`

Su función es concreta:

- anclar la causa al sistema económico existente
- habilitar procesos de consulta y generación económica
- evitar duplicación de la lógica ya resuelta en cuenta corriente

👉 Capa 06 no modela deuda; **modela integración económica**.
