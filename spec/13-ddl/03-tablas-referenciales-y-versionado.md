# Tablas referenciales y versionado

## Finalidad

Este archivo define las tablas referenciales y de versionado necesarias para congelar contexto histórico dentro del núcleo del acta.

Incluye:

- `Dependencia`
- `DependenciaVersion`
- `Inspector`
- `InspectorVersion`
- `MedidaPreventiva`

Estas tablas no son periféricas al dominio del acta: forman parte del contexto congelado que el acta necesita para mantener trazabilidad histórica consistente.

---

## Tabla: Dependencia

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdDep` | `INT` | No | PK |
| `CodDep` | `VARCHAR(20)` | Sí | Código interno de dependencia |
| `NomDep` | `VARCHAR(120)` | No | Nombre visible actual |
| `IdDepPadre` | `INT` | Sí | Dependencia padre actual |
| `SiActiva` | `SMALLINT` | No | 0/1 |

### Notas
- `Dependencia` representa la entidad organizacional actual.
- La reconstrucción histórica no se resuelve desde esta tabla sino desde `DependenciaVersion`.
- La relación padre/hija es parte del modelo organizacional del dominio.

---

## Tabla: DependenciaVersion

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdDep` | `INT` | No | Dependencia |
| `VerDep` | `SMALLINT` | No | Versión |
| `NomDep` | `VARCHAR(120)` | No | Nombre congelado |
| `IdDepPadre` | `INT` | Sí | Dependencia padre congelada |
| `VerDepPadre` | `SMALLINT` | Sí | Versión dependencia padre congelada |
| `FhVigDesde` | `DATETIME YEAR TO DAY` | No | Inicio vigencia |
| `FhVigHasta` | `DATETIME YEAR TO DAY` | Sí | Fin vigencia |
| `SiActiva` | `SMALLINT` | No | 0/1 |

### Notas
- Esta tabla permite reconstruir el organigrama histórico.
- La versión no debe congelar solo el nombre sino también la referencia al padre y su versión.
- El acta debe guardar `IdDep` + `VerDep` y no depender del estado actual de la dependencia.

---

## Tabla: Inspector

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdInsp` | `INT` | No | PK |
| `IdUser` | `INT` | No | Usuario del tenant asociado al inspector |
| `LegajoInsp` | `INT` | No | Legajo actual |
| `NomInsp` | `VARCHAR(120)` | No | Nombre actual |
| `SiActivo` | `SMALLINT` | No | 0/1 |

### Notas
- `Inspector` está desacoplado del modelo interno del IdP.
- El IdP emite claims construidos con `IdInsp` y `VerInsp`, pero no versiona esta entidad.
- `IdUser` vincula el inspector con el usuario autenticable del tenant.
- La historia del inspector no se resuelve desde esta tabla sino desde `InspectorVersion`.

---

## Tabla: InspectorVersion

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdInsp` | `INT` | No | Inspector |
| `VerInsp` | `SMALLINT` | No | Versión |
| `LegajoInsp` | `INT` | No | Legajo congelado |
| `NomInsp` | `VARCHAR(120)` | No | Nombre congelado |
| `IdDep` | `INT` | No | Dependencia congelada |
| `VerDep` | `SMALLINT` | No | Versión dependencia congelada |
| `FhVigDesde` | `DATETIME YEAR TO DAY` | No | Inicio vigencia |
| `FhVigHasta` | `DATETIME YEAR TO DAY` | Sí | Fin vigencia |
| `SiActivo` | `SMALLINT` | No | 0/1 |

### Notas
- Esta tabla contiene el snapshot versionado del inspector.
- No depende del versionado del maestro de usuarios.
- El acta debe persistir `IdInsp` + `VerInsp`.
- Si cambian datos relevantes del inspector, se genera una nueva versión.

---

## Tabla: MedidaPreventiva

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `IdMedPrev` | `INT` | No | PK lógica de medida |
| `VerMedPrev` | `SMALLINT` | No | Versión |
| `NomMedPrev` | `VARCHAR(120)` | No | Nombre medida |
| `DescMedPrev` | `VARCHAR(255)` | Sí | Descripción opcional |
| `SiActiva` | `SMALLINT` | No | 0/1 |
| `IdDep` | `INT` | No | Dependencia propietaria |
| `VerDep` | `SMALLINT` | No | Versión dependencia |

### Notas
- Las medidas preventivas están organizadas por dependencia.
- No todas las dependencias comparten necesariamente las mismas medidas preventivas.
- `ActaMedidaPreventiva` debe referenciar una versión concreta de este catálogo.
- El texto libre en la aplicación de la medida no reemplaza al catálogo.

---

## Reglas generales del bloque

- Toda tabla versionada debe permitir reconstrucción histórica sin depender del estado actual.
- El núcleo del acta debe persistir pares `Id + Ver` para todo dato referencial congelado.
- El objetivo no es congelar texto arbitrario embebido sino versión estructurada del dato referencial.

---

## Enumeraciones del bloque

### SiActiva
- `0 = NO`
- `1 = SI`