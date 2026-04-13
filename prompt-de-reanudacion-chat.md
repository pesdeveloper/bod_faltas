# Prompt de continuidad

Estamos retomando el repo multiproyecto del sistema de faltas municipal.

## Reglas de trabajo

- Continuar en español.
- No rediseñar desde cero.
- Usar `spec/` como fuente principal de verdad.
- Mantener archivos chicos, claros, navegables y orientados a `spec-as-source`.
- No volver a discutir fundamentos ya cerrados salvo contradicción real.
- No inflar los documentos con explicación innecesaria.
- Priorizar definiciones compactas y útiles para implementación asistida.

## Estado ya consolidado

Ya quedaron consolidados y alineados:

- bloque base de overview
- reglas transversales principales
- bandejas canónicas
- bloque `spec/01-dominio/`
- bloque `spec/12-datos/`
- bloque `spec/04-backend/`
- bloque `spec/09-integraciones/`

### Decisiones cerradas importantes

- La unidad principal de gestión es Acta / Expediente.
- La notificación es un proceso transversal único.
- El snapshot es derivado y regenerable.
- Archivo y cerrada no son equivalentes.
- El motor de firma es externo al sistema de faltas y en este repo solo existe integración.
- `Dependencia` e `Inspector` forman parte del dominio.
- `Dependencia` e `Inspector` requieren versionado referencial para congelar contexto histórico sin snapshot textual embebido.
- `Talonario y numeración` quedó definido como mecanismo administrativo transversal del dominio.
- En backend se trabajará con SQL explícito.
- No se utilizará ORM como mecanismo principal de persistencia del núcleo transaccional.
- El storage documental debe resolverse desacoplado del documento lógico mediante `StorageKey`.
- La autenticación del sistema de faltas se integra con un IdP propio del ecosistema implementado con OpenIddict.

### Acta: identidad y contexto

Quedó firme que en `Acta` conviven:

- clave interna
- identidad técnica distribuida de origen
- numeración administrativa visible

También quedó firme que:

- el `Acta` pertenece a una `Dependencia`
- el `Inspector` está asignado a una `Dependencia`
- el `Acta` queda asociado al `Inspector` que la captó/labró
- la `Dependencia` aporta contexto operativo inicial, incluyendo si el circuito es de tránsito o no
- en actas, los talonarios se asocian a dependencias y no a inspectores

### Documento

Quedó firme que `Documento`:

- tiene identidad lógica separada del soporte material
- puede tener número visible o no
- puede pre-numerarse o numerarse al firmar
- puede firmarse digitalmente por integración externa
- o imprimirse y firmarse físicamente/ológrafamente, con posterior incorporación o reemplazo de la versión firmada
- resuelve storage físico o lógico mediante `StorageKey`

### Snapshot

Quedó firme que el snapshot:

- es derivado
- es regenerable
- puede persistirse por razones operativas
- no es fuente primaria de verdad

## Bloque DDL físico abierto

Se abrió `spec/13-ddl/` y ya existen:

- `00-mapa-ddl-fisico.md`
- `01-convenciones-ddl-informix.md`
- `02-tablas-nucleo-expediente.md`

### Reglas físicas ya cerradas

- motor objetivo: **Informix 12.10**
- no usar `SERIAL`
- usar **secuencias**
- no todo ID debe ser `INT8`
- criterio base:
  - `INT8` para tablas de alto crecimiento
  - `INT` para tablas medianas o referenciales
  - `SMALLINT` para catálogos y referencias pequeñas
- GUID/UUID técnicos:
  - `CHAR(36)`
- fechas:
  - sin hora → `DATETIME YEAR TO DAY`
  - con hora → `DATETIME YEAR TO SECOND`
  - hora sola → `DATETIME HOUR TO SECOND`
- flags:
  - `SMALLINT`
- `VARCHAR` ajustado
- no usar `TEXT`
- usar `LVARCHAR` solo si hace falta
- si un contenido excede el rango razonable de `LVARCHAR`, debe fragmentarse en más de un campo físico y recomponerse en la capa de acceso
- campos de auditoría de usuario:
  - `IdUserAlta`
  - `IdUserUltMod`
  - como `CHAR(36)` apuntando al `SubjectId` externo

## Punto exacto donde se cortó

El trabajo DDL ya empezó, pero `02-tablas-nucleo-expediente.md` quedó como primer borrador y **debe corregirse antes de seguir**.

Temas ya detectados para ajustar:

- revisar tipos físicos de varias columnas
- evitar `INT8` donde no corresponde
- revisar longitudes de `VARCHAR`
- usar `IdUser...` en lugar de `Usr...`
- revisar mejor `Acta` y `ActaEvento`
- no olvidar que faltan las tablas satélite reales del acta:
  - tránsito
  - contravención
  - mercadería
  - decomiso
  - medidas preventivas
- esos datos no deben absorberse dentro de `Acta` si corresponden a satélites

## Próximo paso exacto

Al retomar:

1. corregir `spec/13-ddl/02-tablas-nucleo-expediente.md`
2. continuar con:
   - `03-tablas-referenciales-y-versionado.md`
   - `04-tablas-acta-y-satelites.md`
3. después seguir con:
   - tablas documentales
   - tablas de notificación
   - tablas de numeración y talonarios
   - snapshot y auxiliares
4. luego pasar a:
   - diagrama relacional
   - SQL de creación

## Regla al continuar

- no rehacer `01-dominio`
- no rehacer `12-datos`
- no rehacer `04-backend`
- no rehacer `09-integraciones`
- usar esos bloques como base estable
- seguir con el mismo estilo compacto y útil para `spec-as-source`
- enfocarse primero en corregir el núcleo DDL antes de seguir expandiendo tablas