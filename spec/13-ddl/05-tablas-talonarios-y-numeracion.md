# Tablas talonarios y numeración

## Finalidad

Este archivo define el bloque físico de talonarios y numeración del sistema.

Incluye:

- políticas de numeración
- talonarios electrónicos
- talonarios manuales físicos
- asignación opcional de talonarios a dependencias
- asignación opcional de talonarios manuales a inspectores
- control de uso y vigencia
- trazabilidad del estado de cada número en talonarios manuales físicos

Este bloque es transversal al dominio, pero impacta directamente en el alta y numeración del acta y de otros documentos del ecosistema.

---

## Criterios generales del bloque

- el número visible puede provenir de:
  - numeración electrónica
  - talonario manual físico preimpreso
- los talonarios pueden ser:
  - globales
  - asignados a una dependencia
- la asignación a inspector aplica solo a talonarios manuales físicos
- el estado de cada número manual se registra en `TalonarioMovimiento`
- no deben existir huecos sin trazabilidad en numeración manual física
- un talonario solo puede usarse si no está bloqueado

---

## Tabla: PoliticaNumeracion

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdPolNum` | `INT` | No | PK |
| `NomPolNum` | `VARCHAR(48)` | No | Nombre de política |
| `TipoTalonario` | `SMALLINT` | No | Electrónico o manual físico |
| `SiUsaPrefijo` | `SMALLINT` | No | 0/1 usa prefijo |
| `Prefijo` | `VARCHAR(10)` | Sí | Prefijo opcional |
| `SiUsaAnio` | `SMALLINT` | No | 0/1 incorpora año |
| `SiUsaSerie` | `SMALLINT` | No | 0/1 incorpora serie |
| `LongNro` | `SMALLINT` | No | Longitud del componente numérico |
| `SepPrefAnio` | `CHAR(1)` | Sí | Separador entre prefijo y año |
| `SepAnioSerie` | `CHAR(1)` | Sí | Separador entre año y serie |
| `SepSerieNro` | `CHAR(1)` | Sí | Separador entre serie y número |
| `ProxNro` | `INT` | Sí | Próximo número para numeración electrónica |
| `SiActiva` | `SMALLINT` | No | 0/1 |
| `FhVigDesde` | `DATETIME YEAR TO DAY` | No | Inicio vigencia |
| `FhVigHasta` | `DATETIME YEAR TO DAY` | Sí | Fin vigencia |

### Notas
- No se usa máscara libre.
- La política se compone por partes habilitadas y separadores específicos entre componentes.
- Los componentes posibles son:
  - prefijo
  - año
  - serie
  - número
- Cada separador puede ser distinto.
- Los separadores posibles pueden incluir:
  - `/`
  - `-`
  - `#`
  - `@`
  - espacio
  - sin separador (`NULL` o vacío)
- Si alguno de los componentes intermedios no se usa, el separador correspondiente debe quedar `NULL`.
- `ProxNro` aplica principalmente a políticas electrónicas.

---

## Tabla: Talonario

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdTalonario` | `INT` | No | PK |
| `IdPolNum` | `INT` | No | Política de numeración asociada |
| `CodTalonario` | `VARCHAR(4)` | No | Código interno corto |
| `TipoTalonario` | `SMALLINT` | No | Electrónico o manual físico |
| `AmbitoTalonario` | `SMALLINT` | No | Global o dependencia |
| `Serie` | `VARCHAR(10)` | Sí | Serie opcional |
| `NroDesde` | `INT` | Sí | Inicio de rango |
| `NroHasta` | `INT` | Sí | Fin de rango, solo para manual físico |
| `UltNroUsado` | `INT` | Sí | Último número utilizado |
| `SiBloqueado` | `SMALLINT` | No | 0/1 bloqueado |
| `CodDesbloqueo` | `CHAR(36)` | Sí | Código GUID de desbloqueo |
| `SiActiva` | `SMALLINT` | No | 0/1 |
| `ObsTalonario` | `VARCHAR(255)` | Sí | Observación breve |
| `FhAlta` | `DATETIME YEAR TO SECOND` | No | Alta técnica |
| `IdUserAlta` | `CHAR(36)` | No | Usuario alta |

### Notas
- `Talonario` representa una unidad administrable de numeración.
- `NroHasta` aplica solo a talonarios manuales físicos.
- Un talonario solo puede usarse si `SiBloqueado = 0`.
- `CodDesbloqueo` es generado por el sistema y el desbloqueo solo debe poder realizarse por API específica.
- Puede existir tanto para actas como para documentos transversales.

---

## Tabla: TalonarioDependencia

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdTalonario` | `INT` | No | Talonario |
| `IdDep` | `INT` | No | Dependencia asignada |
| `VerDep` | `SMALLINT` | No | Versión dependencia |
| `FhDesde` | `DATETIME YEAR TO DAY` | No | Inicio asignación |
| `FhHasta` | `DATETIME YEAR TO DAY` | Sí | Fin asignación |
| `SiActiva` | `SMALLINT` | No | 0/1 |

### Notas
- Esta relación es opcional.
- Aplica cuando el talonario está asignado a una dependencia.
- No todos los talonarios necesitan dependencia; puede haber talonarios globales.

---

## Tabla: TalonarioInsp

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdTalonario` | `INT` | No | Talonario |
| `IdInsp` | `INT` | No | Inspector asignado |
| `VerInsp` | `SMALLINT` | No | Versión inspector |
| `FhDesde` | `DATETIME YEAR TO DAY` | No | Inicio asignación |
| `FhHasta` | `DATETIME YEAR TO DAY` | Sí | Fin asignación |
| `SiActiva` | `SMALLINT` | No | 0/1 |

### Notas
- Esta asignación aplica solo a talonarios manuales físicos.
- La dependencia sigue siendo el dueño administrativo principal si el talonario no es global.

---

## Tabla: TalonarioMovimiento

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `Id` | `INT8` | No | PK |
| `IdTalonario` | `INT` | No | Talonario manual físico |
| `NroUsado` | `INT` | No | Número del talonario afectado |
| `EstadoNro` | `SMALLINT` | No | Estado del número |
| `MotivoMov` | `VARCHAR(80)` | Sí | Motivo corto si corresponde |
| `IdActa` | `INT8` | Sí | Acta vinculada si llegó a existir en central |
| `IdDep` | `INT` | Sí | Dependencia vinculada |
| `VerDep` | `SMALLINT` | Sí | Versión dependencia |
| `IdInsp` | `INT` | Sí | Inspector vinculado |
| `VerInsp` | `SMALLINT` | Sí | Versión inspector |
| `FhMov` | `DATETIME YEAR TO SECOND` | No | Fecha/hora movimiento |
| `IdUserMov` | `CHAR(36)` | No | Usuario que registra |

### Notas
- Esta tabla aplica solo a talonarios manuales físicos.
- Registra el estado de cada número:
  - usado
  - anulado
- `MotivoMov` se usa especialmente cuando el número queda anulado.
- No hace falta una tabla separada para anulados si el estado del número ya queda trazado acá.
- Para anulaciones, motivos esperables pueden ser:
  - `ERROR`
  - `ACTA_NULA`

---

## Reglas generales del bloque

- No debe haber huecos en numeración manual física sin estado registrable.
- El estado de cada número manual debe poder conocerse desde `TalonarioMovimiento`.
- Los talonarios pueden ser globales o asignados a dependencias.
- La asignación a inspector se reserva a talonarios manuales físicos.
- Si el número proviene de papel preimpreso, ese número es el número administrativo visible al ingresar el acta.
- Los documentos transversales también pueden usar talonarios, incluso sin dependencia.

---

## Enumeraciones del bloque

### TipoTalonario
- `1 = ELECTRONICO`
- `2 = MANUAL_FISICO`

### AmbitoTalonario
- `1 = GLOBAL`
- `2 = DEPENDENCIA`

### EstadoNro
- `1 = USADO`
- `2 = ANULADO`

### SiActiva
- `0 = NO`
- `1 = SI`

### SiBloqueado
- `0 = NO`
- `1 = SI`