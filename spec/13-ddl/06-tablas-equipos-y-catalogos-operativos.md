# Tablas equipos y catálogos operativos

## Finalidad

Este archivo define tablas de soporte operativo que forman parte del ecosistema del acta y que requieren referencia histórica o versionada.

Incluye:

- `Alcoholimetro`
- `AlcoholimetroVersion`
- `RubroCom`
- `RubroComVersion`

Estas tablas no reemplazan el núcleo del acta, pero sí proveen catálogos y equipos cuya referencia debe quedar congelada cuando el acta los utiliza.

---

## Tabla: Alcoholimetro

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdAlcoholimetro` | `INT` | No | PK |
| `CodAlcoholimetro` | `VARCHAR(20)` | No | Código interno visible |
| `NroSerie` | `VARCHAR(64)` | No | Número de serie |
| `Marca` | `VARCHAR(64)` | Sí | Marca |
| `Modelo` | `VARCHAR(64)` | Sí | Modelo |
| `SiDeshabilitado` | `SMALLINT` | No | 0/1 |
| `ObsDeshabilitado` | `VARCHAR(255)` | Sí | Motivo o contexto de deshabilitación |
| `SiActivo` | `SMALLINT` | No | 0/1 |
| `FhAlta` | `DATETIME YEAR TO SECOND` | No | Alta técnica |
| `IdUserAlta` | `CHAR(36)` | No | Usuario alta |

### Notas
- `CodAlcoholimetro` es el identificador operativo que puede vincularse al QR pegado en el equipo.
- El stock operativo se administra desde esta entidad.
- La selección del equipo en dispositivos móviles puede resolverse localmente en memoria temporal o mediante escaneo de QR.
- Esa preselección operativa no necesita persistirse en central.
- Si el equipo se deshabilita, debe quedar documentado el motivo.

---

## Tabla: AlcoholimetroVersion

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdAlcoholimetro` | `INT` | No | Equipo |
| `VerAlcoholimetro` | `SMALLINT` | No | Versión |
| `CodAlcoholimetro` | `VARCHAR(20)` | No | Código visible congelado |
| `NroSerie` | `VARCHAR(64)` | No | Número de serie congelado |
| `Marca` | `VARCHAR(64)` | Sí | Marca congelada |
| `Modelo` | `VARCHAR(64)` | Sí | Modelo congelado |
| `SiDeshabilitado` | `SMALLINT` | No | 0/1 |
| `ObsDeshabilitado` | `VARCHAR(255)` | Sí | Motivo o contexto |
| `SiActivo` | `SMALLINT` | No | 0/1 |
| `FhVigDesde` | `DATETIME YEAR TO DAY` | No | Inicio vigencia |
| `FhVigHasta` | `DATETIME YEAR TO DAY` | Sí | Fin vigencia |

### Notas
- El acta debe congelar la referencia al equipo utilizado mediante:
  - `IdAlcoholimetro`
  - `VerAlcoholimetro`
- Desde esa versión deben leerse los datos históricos del equipo usado al momento del acta.
- La calibración, número de serie, marca, modelo, estado y observaciones del equipo no deben duplicarse en `ActaTransito` si ya quedan resueltos por esta versión.
- La UX mobile puede trabajar con selección local temporal o lectura por QR sin necesidad de persistir esa preselección operativa en la base central.

---

## Tabla: RubroCom

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdRub` | `SMALLINT` | No | PK |
| `NomRub` | `CHAR(64)` | No | Denominación rubro |
| `SiDeshabilitado` | `SMALLINT` | No | 0/1 |

### Notas
- Esta tabla representa el catálogo actual simplificado de rubros proveniente de `rubrocom`.
- Se incluyen solo los campos relevantes para el modelo de faltas.
- El uso histórico en actas debe congelarse por versión, no por lectura directa de estado actual.

---

## Tabla: RubroComVersion

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdRub` | `SMALLINT` | No | Rubro |
| `VerRub` | `SMALLINT` | No | Versión |
| `NomRub` | `CHAR(64)` | No | Denominación congelada |
| `SiDeshabilitado` | `SMALLINT` | No | 0/1 |
| `FhVigDesde` | `DATETIME YEAR TO DAY` | No | Inicio vigencia |
| `FhVigHasta` | `DATETIME YEAR TO DAY` | Sí | Fin vigencia |

### Notas
- Esta tabla existe para sostener la referencia histórica `IdRub + VerRub` usada por el acta.
- Aunque la fuente original sea una tabla de consulta, el dominio de faltas necesita congelar el rubro efectivamente utilizado al momento del labrado.
- El acta no debe depender del estado actual del catálogo para reconstruir historia.

---

## Reglas generales del bloque

- Los equipos y catálogos operativos usados por el acta deben poder referenciarse por `Id + Ver`.
- La UX mobile puede trabajar con selección local temporal sin necesidad de persistir ese estado en la base central.
- Cuando el acta utiliza un equipo o catálogo relevante, debe persistir la referencia congelada y no depender del estado actual del maestro.

---

## Enumeraciones del bloque

### SiDeshabilitado
- `0 = NO`
- `1 = SI`

### SiActivo
- `0 = NO`
- `1 = SI`