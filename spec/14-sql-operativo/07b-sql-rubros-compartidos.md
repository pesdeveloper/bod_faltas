# [14-SQL-OPERATIVO] 07B - SQL RUBROS COMPARTIDOS

## Finalidad

Este archivo documenta el tratamiento SQL de los rubros comerciales compartidos relevantes para faltas.

Tablas:

- `RubroCom`
- `RubroComVersion`

---

## Regla general

- `RubroCom` conserva nombre real compartido.
- `RubroComVersion` permite compatibilidad histórica/versionada para faltas.
- La operatoria no debe romper compatibilidad con el sistema actual de ingresos.

---

## Operaciones principales

### 1. Lectura de rubro vigente

#### Patrón operativo
Lectura corta sobre:

- rubro base
- versión vigente si aplica

#### Objetivo
Permitir uso de faltas sin reescribir la lógica propia del sistema de ingresos.

---

### 2. Lectura histórica o por versión

#### Regla de lectura
Cuando el caso de uso requiera trazabilidad, debe poder resolverse:

- versión por fecha
- versión por identificador
- historial de versiones por rubro

---

### 3. Alta o nueva versión en contexto compartido

#### Regla
Solo debe documentarse o implementarse aquí si realmente forma parte de las operaciones autorizadas del ecosistema compartido.

Si la modificación de `RubroCom` sigue perteneciendo al sistema actual, faltas debe consumirlo como dependencia compartida y no asumir control total del CRUD.

---
