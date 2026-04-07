# [CAPA 06] INTEGRACIÓN ECONÓMICA

## Finalidad de la capa

Esta capa modela la **integración entre el sistema de faltas y el sistema económico existente de cuenta corriente**.

Su objetivo no es reemplazar ni duplicar la lógica económica ya resuelta, sino permitir que cada `Acta` quede vinculada de forma estable con su cuenta económica para:

- generar deuda
- consultar deuda
- aplicar pagos
- financiar deuda mediante plan de pagos
- consultar estado económico
- mantener trazabilidad entre la causa y los comprobantes de cuenta corriente

---

## Qué representa esta capa

Esta capa representa el **ancla económica** de la causa dentro del sistema de faltas.

Es decir:

- cómo una `Acta` se vincula con una cuenta económica
- cómo el sistema de faltas identifica la cuenta corriente asociada
- qué procesos externos generan o consultan información económica

---

## Qué NO representa esta capa

Esta capa NO modela:

- comprobantes de cuenta corriente
- movimientos contables
- deuda calculada físicamente
- pagos
- cuotas
- planes de pago
- intereses
- refinanciaciones
- lógica interna de `cccmte`
- lógica interna de `ccmov`
- tablas económicas ya existentes

Todo eso ya vive en el sistema económico actual.

---

## Principio central de diseño

### 1. No duplicación

La capa económica de faltas **no crea un sistema económico paralelo**.

Se integra con el sistema actual de cuenta corriente, que ya resuelve:

- emisión de deuda
- cancelaciones
- pagos
- ajustes
- financiación
- cuotas
- relación entre comprobantes origen y movimientos

---

## Base de integración ya definida

La integración económica parte de esta relación:

- `Acta`
- `SujBieFaltas`
- `IdSuj`
- `IdBie`

y desde ahí se accede al sistema económico existente:

- `cccmte`
- `ccmov`
- `Acogimiento`
- `DocXCmte`
- y demás estructuras actuales

La relación conceptual base es:

`Acta` → `SujBieFaltas` → `(IdSuj, IdBie)` → cuenta corriente existente

---

## Idea central de la capa

Cada `Acta` debe poder tener una **cuenta económica persistente**.

Esa cuenta no se representa inventando una tabla de deuda propia, sino mediante una relación estable entre el acta y el par económico:

- `IdSuj`
- `IdBie`

Ese par es el que permite:

- leer deuda
- emitir comprobantes
- financiar
- registrar movimientos
- consultar saldo
- relacionar planes de pago

---

## Qué debe resolver esta capa

Como mínimo, la capa debe permitir:

- vincular una `Acta` a un `IdSuj`
- vincular una `Acta` a un `IdBie`
- asegurar persistencia de esa relación
- identificar la cuenta económica del acta
- documentar qué procesos externos se invocan para operar económicamente sobre la causa

---

## Enfoque de integración

La capa no resuelve el económico "por datos duplicados", sino por **procesos externos reutilizables**.

Ejemplos:

- generar deuda del acta
- consultar deuda vigente del acta
- generar plan de pagos
- consultar plan de pagos vigente
- consultar pagos imputados
- consultar estado económico general

La implementación real podrá ser:

- rutina
- función
- stored procedure
- subconsulta
- servicio de aplicación
- integración con SQL existente

Pero eso no se modela aquí como lógica persistida nueva.

---

## Entidad principal de la capa

La capa debería resolverse con una única entidad principal de vínculo económico.

Nombre sugerido:

- `SujBieFaltas`

---

## Qué es SujBieFaltas

`SujBieFaltas` representa la **vinculación persistente entre una causa (`Acta`) y su identidad económica** dentro del sistema de cuenta corriente.

No representa deuda.

No representa movimientos.

No representa plan de pagos.

Representa únicamente el punto de anclaje que permite que el sistema económico opere sobre la causa.

---

## Relación con Acta

Cada `Acta` debe poder vincularse con un único registro económico persistente.

Relación esperada:

- `Acta` 1 → 0..1 `SujBieFaltas`

En la práctica, una vez asignado, ese vínculo económico debería quedar estable.

---

## Relación con cuenta corriente

Desde `SujBieFaltas`, la cuenta se accede por:

- `IdSuj`
- `IdBie`

Ese par permite localizar en el sistema económico:

- comprobantes encabezado (`cccmte`)
- movimientos (`ccmov`)
- planes de pago
- acogimientos
- relaciones documentales/económicas existentes

---

## Regla clave

👉 La deuda del acta **no se guarda en Capa 06**.

Se obtiene consultando el sistema económico actual a través del vínculo `(IdSuj, IdBie)`.

---

## Ejemplo de operación económica

### Caso 1 — Emisión inicial de deuda

1. existe `Acta`
2. existe su vínculo `SujBieFaltas`
3. se invoca proceso económico de generación de deuda
4. ese proceso crea comprobantes y movimientos en el sistema económico existente
5. la deuda luego se consulta por `(IdSuj, IdBie)`

---

## Caso 2 — Plan de pagos

1. existe deuda previa del acta
2. se solicita plan de pagos
3. el sistema económico genera:
   - comprobante cancelatorio/ajuste
   - comprobantes de cuotas
   - registros de acogimiento y relaciones complementarias
4. Capa 06 no modela esas tablas; solo se integra con ellas

---

## Caso 3 — Consulta de deuda

1. se parte de `Acta`
2. se obtiene `IdSuj` + `IdBie`
3. se invoca una rutina o consulta de deuda
4. se devuelve deuda consolidada desde el sistema económico actual

---

## Responsabilidad de la capa

La responsabilidad real de esta capa es:

- **identificar**
- **anclar**
- **integrar**

No:

- calcular
- contabilizar
- emitir comprobantes por sí misma
- definir cuotas
- registrar pagos en tablas nuevas

---

## Procesos económicos esperados

A nivel conceptual, la capa debería contemplar procesos como:

- `CrearCuentaEconomicaActa`
- `GenerarDeudaActa`
- `ObtenerDeudaActa`
- `ObtenerComprobantesActa`
- `GenerarPlanPagosActa`
- `ObtenerPlanPagosActa`
- `ObtenerPagosActa`

Estos nombres son contractuales/conceptuales.

No implican todavía:

- nombres finales
- firma técnica definitiva
- SQL definitivo

## Importante sobre los procesos

Los procesos económicos no deben quedar modelados como nuevas tablas del sistema de faltas.

Deben considerarse:

- rutinas reutilizables
- funciones
- procedimientos
- consultas existentes
- servicios de integración

según la implementación real que ya existe o que se termine consolidando.

---

## Qué sí puede persistirse en esta capa

La capa solo necesita persistir el vínculo económico estable del acta.

Opcionalmente, y solo si aporta valor operativo real, podría admitir algún dato mínimo auxiliar como:

- fecha de alta del vínculo
- usuario de registro
- origen de registro

Pero no más que eso.

---

## Qué no conviene persistir acá

No conviene persistir en esta capa:

- saldo actual
- deuda total
- deuda vencida
- deuda financiada
- cantidad de cuotas
- monto del plan
- último pago
- fecha de último pago

Todo eso puede cambiar por procesos externos y debe leerse del sistema económico real, no duplicarse.

---

## Relación con Capa 05

Los actos administrativos pueden disparar o habilitar procesos económicos, pero Capa 06 no debe depender estructuralmente del acto.

Ejemplo:

- una resolución o fallo puede dar lugar a deuda
- pero la deuda se opera por el vínculo económico del acta
- no por una tabla económica colgada del acto

Esto mantiene el modelo simple.

---

## Relación con Capa 04

Las solicitudes de pago voluntario o de plan de pagos pueden ingresar por Capa 04, pero su tratamiento económico no pertenece a esa capa.

Capa 04 registra la presentación.  
Capa 06 integra la operación económica cuando corresponda.

---

## Relación con Capa 01

El historial relevante de operaciones económicas importantes debería reflejarse también en `ActaEvento`.

Ejemplos posibles:

- CUENTA_ECONOMICA_CREADA
- DEUDA_GENERADA
- PLAN_PAGOS_GENERADO
- PAGO_REGISTRADO
- DEUDA_CANCELADA

La capa no reemplaza el historial transversal.

---

## Qué debe responder la capa

Como mínimo, Capa 06 debe permitir responder:

- cuál es la identidad económica del acta
- con qué `IdSuj` opera
- con qué `IdBie` opera
- si la causa ya tiene cuenta económica asignada
- qué proceso económico debe invocarse para consultar deuda o generar operaciones

---

## Regla de arquitectura

👉 El sistema de faltas **usa** el sistema económico existente.  
👉 No lo reimplementa.

---

## Entidad conceptual de la capa

La capa puede resolverse con una única entidad principal:

- `SujBieFaltas`

sin crear nuevas tablas de deuda, pagos o cuotas.

---

## Cierre conceptual de la capa

Capa 06 incorpora al sistema de faltas el vínculo con el dominio económico sin duplicar su lógica interna.

Su función es concreta:

- asociar una cuenta económica persistente a cada acta
- habilitar la consulta y generación de operaciones económicas por integración
- mantener desacoplado el modelo de faltas del detalle interno de cuenta corriente

👉 En resumen: Capa 06 **ancla la causa al sistema económico existente**.
