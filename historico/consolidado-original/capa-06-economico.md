# [CAPA 06] — INTEGRACIÓN ECONÓMICA CONSOLIDADA

---

# 1. FINALIDAD DE LA CAPA

Esta capa define la **integración del Acta con el sistema económico**.

No modela deuda, pagos ni cuentas corrientes.

Su objetivo es:

* vincular el Acta con el sistema económico existente
* permitir generación de deuda externa
* permitir registro de pagos provenientes de otros sistemas

---

# 2. PRINCIPIO CENTRAL

## Lo económico no pertenece a este sistema

Este sistema:

* no calcula deuda
* no gestiona cuentas corrientes
* no registra movimientos económicos internos

---

## Este sistema solo:

* dispara procesos económicos
* recibe resultados económicos

---

# 3. MODELO DE INTEGRACIÓN

## 3.1 Identificación económica del Acta

Se utiliza una referencia externa:

## SujBieFaltas

---

### Descripción

Identificador que permite vincular el Acta con el sistema económico.

---

### Contiene

* IdActa
* IdSuj (ej: 99 — varios)
* IdBie

---

### Función

Permitir que el sistema externo:

* genere deuda
* registre pagos
* vincule comprobantes

---

# 4. INTERACCIÓN CON EL SISTEMA ECONÓMICO

## 4.1 Salidas del sistema

El sistema puede generar eventos que disparan acciones externas:

---

### Ejemplos

* DEUDA_GENERADA
* PLAN_DE_PAGO_SOLICITADO

---

## Importante

Estos eventos:

* representan intención o acción lógica
* no contienen detalle económico

---

## 4.2 Entradas al sistema

El sistema puede recibir información externa:

---

### Ejemplos

* PAGO_REGISTRADO
* PAGO_CONFIRMADO
* PLAN_APROBADO

---

## Regla

Estos hechos se registran como eventos en ActaEvento.

---

# 5. DOCUMENTOS RELACIONADOS

Si existen documentos económicos:

* comprobantes
* recibos
* constancias

Se modelan mediante:

* Documento
* ActaDocumento

---

# 6. RELACIÓN CON SNAPSHOT

El sistema puede proyectar:

* TieneDeuda
* PagoRegistrado
* TienePlanDePago

---

## Regla

El snapshot refleja estado operativo, no lógica económica.

---

# 7. REGLAS DE DISEÑO

## 7.1 No modelar lógica económica

No crear:

* tablas de deuda
* movimientos
* cuotas
* cálculos

---

## 7.2 No duplicar información

Los datos económicos viven en el sistema externo.

---

## 7.3 Modelo desacoplado

La integración debe permitir:

* cambios en el sistema económico
* reemplazo del sistema externo
* independencia del dominio principal

---

# 8. RESULTADO DE LA CAPA

Esta capa permite:

* integrar el Acta con el sistema económico
* mantener el dominio limpio
* evitar duplicación de lógica
* soportar pagos y deuda sin complejidad interna

---

## Resumen

Esta capa define:

## cómo el Acta interactúa con el sistema económico

sin absorber su complejidad.

---

