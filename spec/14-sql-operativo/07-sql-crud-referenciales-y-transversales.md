# [14-SQL-OPERATIVO] 07 - SQL CRUD REFERENCIALES Y TRANSVERSALES

## Finalidad

Este archivo agrupa los patrones SQL principales de:

- referenciales propios de faltas
- entidades versionadas
- numeración / talonarios transversal
- storage transversal
- entidades compartidas relevantes para faltas

---

## Grupo A - Referenciales propios de faltas

Tablas principales:

- `FalDependencia`
- `FalDependenciaVersion`
- `FalInspector`
- `FalInspectorVersion`
- `FalAlcoholimetro`
- `FalAlcoholimetroVersion`
- `FalMedidaPreventiva`

Patrón principal:
- alta raíz + primera versión
- cierre de versión vigente + nueva versión
- lectura de vigente
- lectura histórica por fecha

---

## Grupo B - Rubros comerciales compartidos

Tablas:
- `RubroCom`
- `RubroComVersion`

Regla:
- `RubroCom` conserva nombre real compartido
- `RubroComVersion` permite compatibilidad histórica/versionada para faltas

---

## Grupo C - Numeración / talonarios transversal

Tablas:
- `NumPolitica`
- `NumTalonario`
- `NumTalonarioDependencia`
- `NumTalonarioInspector`
- `NumTalonarioMovimiento`

Operaciones típicas:
- crear política
- crear talonario
- asignar talonario a dependencia
- asignar talonario a inspector
- registrar movimiento de número usado o reservado
- consultar talonarios vigentes
- consultar disponibilidad y próximo número lógico

---

## Grupo D - Storage transversal

Tablas:
- `StorBackend`
- `StorPolitica`
- `StorObjeto`

Operaciones típicas:
- alta de backend
- alta de política
- resolver política efectiva por sistema/familia/tipo
- registrar objeto
- obtener objeto por storage key

---

## Riesgos a controlar

- mezclar lógica de faltas con lógica transversal de numeración
- mezclar storage físico con documento lógico
- romper compatibilidad de `RubroCom` con el sistema actual
- no dejar claro qué parte del versionado cierra vigencia y cuál crea nueva versión

---
