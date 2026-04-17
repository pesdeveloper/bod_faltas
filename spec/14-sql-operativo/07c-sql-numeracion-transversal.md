# [14-SQL-OPERATIVO] 07C - SQL NUMERACION TRANSVERSAL

## Finalidad

Este archivo documenta las operaciones SQL principales de numeración y talonarios transversales.

Tablas:

- `NumPolitica`
- `NumTalonario`
- `NumTalonarioDependencia`
- `NumTalonarioInspector`
- `NumTalonarioMovimiento`

---

## Reglas generales

- La numeración es transversal y no exclusiva de faltas.
- Debe soportar políticas, talonarios, asignaciones, movimientos y consulta de disponibilidad.
- La vigencia de asignaciones no debe quedar ambigua.

---

## Operaciones principales

### 1. Crear política de numeración

#### Patrón transaccional aplicable
- patrón fundamental: **Alta simple**

#### Secuencia principal
- secuencia de `NumPolitica`

#### Shape mínimo requerido
Debe incluir, como mínimo:

- tipo o alcance de política
- reglas de numeración
- vigencia
- auditoría mínima

#### Orden base
1. obtener `Id`
2. insertar `NumPolitica`
3. confirmar transacción

---

### 2. Crear talonario

#### Patrón transaccional aplicable
- patrón fundamental: **Alta simple**

#### Secuencia principal
- secuencia de `NumTalonario`

#### Shape mínimo requerido
Debe incluir, como mínimo:

- referencia a política
- tipo de talonario
- rango o parámetros de operación
- vigencia
- auditoría mínima

#### Orden base
1. obtener `Id`
2. insertar `NumTalonario`
3. confirmar transacción

---

### 3. Asignar talonario a dependencia o inspector

#### Patrón transaccional aplicable
- patrón fundamental: **Asociación entre entidades existentes**

#### Shape mínimo requerido
Debe incluir, como mínimo:

- referencia al talonario
- referencia a dependencia o inspector
- vigencia de asignación
- auditoría mínima

#### Orden base
1. localizar entidades a vincular
2. insertar asignación
3. confirmar transacción

---

### 4. Registrar movimiento de numeración

#### Patrón transaccional aplicable
- patrón fundamental: **Alta simple**
- eventualmente asociado a operación externa o diferida

#### Secuencia principal
- secuencia de `NumTalonarioMovimiento`

#### Shape mínimo requerido
Debe incluir, como mínimo:

- referencia al talonario
- número afectado
- tipo de movimiento
- fecha/hora
- auditoría mínima

#### Casos típicos
- reservado
- emitido
- anulado
- invalidado
- usado por contingencia

---

### 5. Consultar disponibilidad y próximo número lógico

#### Regla de lectura
Debe poder resolverse:

- talonarios vigentes
- asignación vigente
- próximo número disponible o siguiente estado lógico
- historial reciente si el caso lo requiere

---
