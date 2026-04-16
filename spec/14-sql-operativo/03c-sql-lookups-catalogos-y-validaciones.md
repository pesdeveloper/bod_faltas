# [14-SQL-OPERATIVO] 03C - SQL LOOKUPS CATALOGOS Y VALIDACIONES

## Finalidad

Este archivo agrupa los lookups y validaciones UX que no pertenecen al bloque territorial principal.

Incluye:

- dependencias
- inspectores
- alcoholímetros
- medidas preventivas
- rubros
- numeración / talonarios
- storage
- normativa y artículos
- validaciones rápidas

---

## Grupo A - Catálogos operativos propios de faltas

### Dependencias vigentes
- `FalDependenciaVersion`

### Inspectores vigentes
- `FalInspectorVersion`

### Alcoholímetros vigentes
- `FalAlcoholimetroVersion`

### Medidas preventivas
- `FalMedidaPreventiva`

---

## Grupo B - Rubros comerciales compartidos

Tablas:
- `RubroCom`
- `RubroComVersion`

Regla:
- `RubroCom` conserva nombre real compartido
- `RubroComVersion` se usa para compatibilidad histórica/versionada si el flujo de faltas lo requiere

---

## Grupo C - Numeración / talonarios transversal

Tablas:
- `NumPolitica`
- `NumTalonario`
- `NumTalonarioDependencia`
- `NumTalonarioInspector`

Consultas típicas:
- talonarios vigentes por dependencia
- talonarios asignados a inspector
- políticas activas por sistema
- disponibilidad lógica de numeración

---

## Grupo D - Storage transversal

Tablas:
- `StorBackend`
- `StorPolitica`

Consultas típicas:
- políticas activas por sistema / familia / tipo
- backend default
- política de fallback

---

## Grupo E - Normativa y economía

Tablas:
- `FalNormativaFaltas`
- `FalArticuloNormativaFaltas`
- `FalTarifarioUnidadFaltas`

Consultas típicas:
- normativa vigente
- artículos vigentes por normativa
- valores vigentes por unidad o fecha

---

## Grupo F - Validaciones UX rápidas

Ejemplos:

- existencia de `NroActa`
- unicidad de `IdTecnico`
- disponibilidad de talonario
- vigencia de versión referencial
- existencia de documento asociable
- existencia de notificación activa incompatible

Estas validaciones deben ser:

- rápidas
- deterministas
- con resultado booleano o shape muy corto

---

## Reglas generales

- no usar estas consultas como reportes
- no devolver estructuras ricas
- limitar resultados
- ordenar útil para la UX
- apoyarse en índices adecuados
- separar lookups de validaciones complejas de negocio

---
