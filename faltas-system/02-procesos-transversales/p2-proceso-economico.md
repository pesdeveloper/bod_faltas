# [P2] PROCESO ECONÓMICO — COBRO / RECIBO / LIBERACIÓN

## Naturaleza

Proceso transversal reutilizable.

Puede invocarse desde:

- D1
- D3
- D4
- D5
- o cualquier punto autorizado

---

## Finalidad

Gestionar:

- liquidación
- pago
- recibo
- liberación

---

## Objeto

ACTA / OBLIGACIÓN ECONÓMICA

---

## Principios

### 1. Independiente del flujo

No depende de D2 ni D3

### 2. Pagador ≠ titular

- imputación → ID_SUJ_FALTAS
- recibo → pagador real

### 3. Pago puede ocurrir en cualquier momento

si la autoridad lo permite

---

## P2.1 Liquidación

- cálculo automático o manual
- conceptos
- importe

Evento:
- LIQUIDACION_GENERADA

---

## P2.2 Registro de intención

Evento:
- INTENCION_PAGO_REGISTRADA

No produce efectos jurídicos

---

## P2.3 Pago preconfirmado

Evento:
- PAGO_PRECONFIRMADO

- pendiente de validación
- no libera
- no emite recibo definitivo

---

## P2.4 Pago confirmado

Evento:
- PAGO_CONFIRMADO

- impacta cuenta económica
- habilita recibo

---

## P2.5 Emisión de recibo

Evento:
- RECIBO_EMITIDO

Incluye:

- pagador real
- importe
- medio

---

## P2.6 Evaluar liberación

Decisión:

¿Corresponde liberar?

---

## P2.7 Liberación

### Sin documento

Evento:
- LIBERACION_EJECUTADA

---

### Con documento

Invoca:

→ P1 PROCESO DOCUMENTAL

Evento:
- DOCUMENTO_LIBERACION_GENERADO

---

## Salidas

Retorna al bloque origen con:

- estado de pago
- estado de liberación
- eventos generados

---

## Eventos mínimos

- LIQUIDACION_GENERADA
- INTENCION_PAGO_REGISTRADA
- PAGO_PRECONFIRMADO
- PAGO_CONFIRMADO
- RECIBO_EMITIDO
- LIBERACION_EJECUTADA
- DOCUMENTO_LIBERACION_GENERADO