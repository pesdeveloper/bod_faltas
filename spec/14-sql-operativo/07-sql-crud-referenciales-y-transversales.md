# [14-SQL-OPERATIVO] 07 - SQL CRUD REFERENCIALES Y TRANSVERSALES

## Finalidad

Este archivo funciona como puerta de entrada para el bloque SQL de:

- referenciales propios de faltas
- entidades versionadas
- rubros compartidos
- numeración / talonarios transversal
- storage transversal

No concentra todo el detalle.  
El detalle se reparte en archivos satélite para mantener el bloque compacto, navegable y usable como `spec-as-source`.

---

## Cómo leer este bloque

- referenciales propios y entidades versionadas → [`07a-sql-referenciales-versionados.md`](./07a-sql-referenciales-versionados.md)
- rubros compartidos → [`07b-sql-rubros-compartidos.md`](./07b-sql-rubros-compartidos.md)
- numeración / talonarios transversal → [`07c-sql-numeracion-transversal.md`](./07c-sql-numeracion-transversal.md)
- storage transversal → [`07d-sql-storage-transversal.md`](./07d-sql-storage-transversal.md)

---

## Reglas generales

- Las entidades versionadas deben respetar el patrón de cierre de vigencia más alta de nueva versión.
- Las entidades transversales (`Num*`, `Stor*`) no deben contaminarse con lógica específica de faltas.
- Las entidades compartidas (`RubroCom`, `RubroComVersion`) deben conservar compatibilidad con el sistema actual.
- Las operaciones de lectura operativa deben privilegiar la versión vigente, salvo que el caso de uso requiera lectura histórica.
- Cuando una asignación o vinculación tenga efecto operativo inmediato, la persistencia debe dejarlo resuelto en la misma transacción.

---

## Grupos incluidos

### A. Referenciales propios de faltas
- `FalDependencia`
- `FalDependenciaVersion`
- `FalInspector`
- `FalInspectorVersion`
- `FalAlcoholimetro`
- `FalAlcoholimetroVersion`
- `FalMedidaPreventiva`

### B. Rubros comerciales compartidos
- `RubroCom`
- `RubroComVersion`

### C. Numeración / talonarios transversal
- `NumPolitica`
- `NumTalonario`
- `NumTalonarioDependencia`
- `NumTalonarioInspector`
- `NumTalonarioMovimiento`

### D. Storage transversal
- `StorBackend`
- `StorPolitica`
- `StorObjeto`

---

## Riesgos a controlar

- mezclar lógica de faltas con lógica transversal de numeración
- mezclar storage físico con documento lógico
- romper compatibilidad de `RubroCom` con el sistema actual
- no dejar claro qué parte del versionado cierra vigencia y cuál crea nueva versión
- dejar asignaciones de talonario o políticas de storage en estado ambiguo

---
