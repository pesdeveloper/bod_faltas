# Versionado referencial

## Finalidad

Este archivo define cómo se persisten las entidades referenciales cuyo contexto debe quedar congelado para el expediente.

---

## Alcance

Aplica al menos a:

- `Dependencia`
- `Inspector`
- `Alcoholimetro`
- `RubroCom`

Y a otras referencias equivalentes si más adelante aparece la misma necesidad.

---

## Regla base

Cuando una referencia pueda cambiar en el tiempo y ese cambio no deba alterar indirectamente el contexto histórico del expediente, la persistencia debe separar:

- entidad principal
- versión aplicable

El expediente no debe depender solo de la fila actual.

---

## Estructura esperada

Para cada referencia versionada debe existir:

- una tabla principal de identidad estable
- una tabla de versiones
- referencia desde el expediente o satélite a la entidad principal
- referencia desde el expediente o satélite a la versión aplicada

Ejemplos esperados:

- `Dependencia`
- `DependenciaVersion`
- `Inspector`
- `InspectorVersion`
- `Alcoholimetro`
- `AlcoholimetroVersion`
- `RubroCom`
- `RubroComVersion`

---

## Reglas de persistencia

- La tabla principal conserva identidad estable.
- La tabla de versiones conserva el estado aplicable en un momento dado.
- El expediente o su satélite debe referenciar tanto la entidad como su versión aplicada.
- Cambios en la entidad principal no deben mutar indirectamente el contexto ya fijado en el expediente.
- No debe resolverse este problema embutiendo snapshots textuales completos dentro de `Acta`.

---

## Casos mínimos

### Dependencia

Debe poder cambiar sus datos sin alterar retroactivamente el contexto histórico de las actas ya registradas.

El expediente debe poder congelar:

- `IdDep`
- `VerDep`

---

### Inspector

Debe poder cambiar sus datos sin alterar retroactivamente el contexto histórico de las actas ya registradas.

El expediente debe poder congelar:

- `IdInsp`
- `VerInsp`

---

### Alcoholímetro

Debe poder cambiar:

- estado
- metadata técnica
- vigencia
- condición operativa

sin alterar retroactivamente el contexto histórico de un acta de tránsito ya registrada.

El satélite correspondiente debe poder congelar:

- `IdAlcoholimetro`
- `VerAlcoholimetro`

---

### Rubro

Debe poder cambiar:

- denominación
- habilitación o deshabilitación
- configuración referencial aplicable

sin alterar retroactivamente el contexto histórico del expediente que lo utilizó.

El satélite correspondiente debe poder congelar:

- `IdRub`
- `VerRub`

---

## Integridad

La persistencia debe asegurar, según corresponda:

- referencia válida a entidad principal
- referencia válida a versión aplicable
- coherencia entre entidad y versión
- imposibilidad de usar versiones ajenas a otra entidad principal

---

## Qué no debe hacerse

No debe resolverse el congelamiento histórico mediante:

- dependencia de la fila actual del maestro
- copia textual informal dentro del expediente
- campos redundantes sin estructura
- snapshots embebidos de referencia cuando el modelo ya admite versión

---

## Resultado esperado

Este bloque debe dejar resuelto que el expediente congela contexto histórico mediante referencias versionadas y no mediante copia textual embebida ni dependencia de filas actuales.