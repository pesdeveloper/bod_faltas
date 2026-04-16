# Convenciones DDL Informix

## Finalidad

Este archivo fija las convenciones físicas base para bajar el modelo a Informix 12.10.

La política general de nomenclatura del proyecto se define en:

- [spec/00-overview/03-convenciones-de-nomenclatura.md](../00-overview/03-convenciones-de-nomenclatura.md)

Este bloque DDL la adopta íntegramente.

---

## Motor

- **Informix 12.10**

---

## PK

Las PK deben usar el tipo adecuado al volumen esperado real.

Criterio base:

- `INT8` para tablas principales de alto crecimiento
- `INT` para tablas medianas o referenciales con crecimiento acotado
- `SMALLINT` para catálogos o referencias de cardinalidad baja

No se usará `SERIAL`.

La generación de IDs se resuelve con:

- **secuencias**

---

## FK

Las FK deben seguir la convención:

- `Id[Entidad]`

El tipo debe ser compatible con la PK referenciada.

Ejemplos:

- `IdActa INT8`
- `IdDep INT`
- `IdTipoEvt SMALLINT`

---

## Secuencias

Regla base:

- una secuencia por tabla principal que la requiera
- uso explícito desde SQL o backend
- sin dependencia de `SERIAL`

Esto permite obtener el ID antes de insertar hijos dentro de la misma transacción.

---

## IDs técnicos

Los IDs técnicos GUID/UUID deben persistirse como:

- `CHAR(36)`

No usar longitudes mayores a 36.

---

## Fechas y horas

### Fecha sin hora
- `DATETIME YEAR TO DAY`

### Fecha con hora
- `DATETIME YEAR TO SECOND`

### Hora sola
- `DATETIME HOUR TO SECOND`

---

## Flags

Por compatibilidad conservadora, los flags deben persistirse como:

- `SMALLINT`

Convención recomendada:

- `0` = falso
- `1` = verdadero

---

## Strings

Reglas:

- evitar longitudes excesivas
- usar `VARCHAR` con tamaño ajustado
- usar `CHAR` solo cuando el largo fijo tenga sentido claro

Regla práctica:

- GUID técnico → `CHAR(36)`
- códigos o números visibles → `VARCHAR(n)`
- nombres o descripciones cortas → `VARCHAR(n)` ajustado
- observaciones breves → `VARCHAR(n)` ajustado

`VARCHAR` no debe superar 255.

---

## Textos largos

La convención base del modelo físico es:

- usar `VARCHAR(n)` para textos acotados
- usar `LVARCHAR` para textos largos habituales
- no usar `TEXT`

Cuando un contenido pueda exceder el rango razonable de `LVARCHAR`, deberá persistirse en más de un campo físico.

La reconstrucción del valor completo quedará a cargo de la capa de acceso de datos.

---

## Tipos numéricos

Usar tipos compatibles con Informix.

En particular:

- `INT8` para tablas principales de alto crecimiento
- `INT` para referencias medianas
- `SMALLINT` para catálogos, flags y referencias pequeñas

No usar terminología ajena como `BIGINT`.

---

## Usuarios de auditoría

Los campos de auditoría de usuario deben persistir el identificador externo estable del usuario autenticado.

Cuando ese identificador sea GUID/UUID textual, se usará:

- `CHAR(36)`

Ejemplos:

- `IdUserAlta`
- `IdUserUltMod`

Cuando el identificador del dominio sea entero interno, deberá respetarse ese tipo.

---

## Nullability

- `NOT NULL` solo cuando el dato sea obligatorio por semántica real
- `NULL` cuando el proceso admita incorporación progresiva

---

## Unicidad

Casos típicos:

- PK por `Id`
- unicidad de `IdTecnico`, cuando corresponda
- unicidad operativa de numeración visible dentro de su contexto
- unicidad de versiones dentro de su entidad padre, cuando corresponda

---

## Nombres

Convenciones base:

- aplicar la política definida en [spec/00-overview/03-convenciones-de-nomenclatura.md](../00-overview/03-convenciones-de-nomenclatura.md)
- PK: `Id`
- FK: `Id[Entidad]`
- nombres compactos, consistentes y explícitos
- evitar nombres excesivamente largos
- cuando el nombre corto no sea evidente, documentarlo con observación breve en el spec

---

## Constraints

Definir explícitamente al menos:

- `PRIMARY KEY`
- `FOREIGN KEY`
- `UNIQUE`, cuando corresponda
- `CHECK`, cuando agregue valor real

---

## Índices

Los índices secundarios se definirán después.

En esta etapa solo se asumen los inevitables por PK, FK o unicidad estructural.

---

## Resultado esperado

Estas convenciones aplican a todo el bloque DDL físico salvo excepción justificada.