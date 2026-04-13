# Estado actual y próximo paso

## Estado actual consolidado

El repo ya tiene consolidado el bloque base de especificación y puede tomarse `spec/` como fuente principal de verdad.

A la fecha, quedaron cerrados y alineados los siguientes bloques:

- overview base
- reglas transversales principales
- bandejas canónicas
- bloque `01-dominio`
- bloque `12-datos`
- bloque `04-backend`
- bloque `09-integraciones`

También quedó abierto el bloque físico:

- `spec/13-ddl/`

---

## Dominio consolidado

El bloque `spec/01-dominio/` quedó consolidado como definición conceptual del negocio.

Puntos cerrados importantes:

- la unidad principal de gestión es Acta / Expediente
- la notificación es un proceso transversal único
- el snapshot es derivado y regenerable
- archivo y cerrada no son equivalentes
- el motor de firma es externo al sistema de faltas y en este repo solo existe integración
- `Dependencia` e `Inspector` forman parte del dominio
- `Dependencia` e `Inspector` requieren versionado referencial para congelar contexto histórico sin snapshot textual embebido
- `Talonario y numeración` quedó definido como mecanismo administrativo transversal del dominio

También quedó firme que:

- el `Acta` pertenece a una `Dependencia`
- el `Inspector` está asignado a una `Dependencia`
- el `Acta` queda asociado al `Inspector` que la captó/labró
- la `Dependencia` aporta contexto operativo inicial, incluyendo si el circuito es de tránsito o no

---

## Datos consolidados

El bloque `spec/12-datos/` quedó consolidado como modelo lógico de datos previo a persistencia/SQL.

Puntos cerrados importantes:

- la identidad del `Acta` no se agota en una sola referencia
- en `Acta` conviven:
  - clave interna
  - identidad técnica distribuida de origen
  - numeración administrativa visible
- la numeración administrativa puede depender de talonarios
- en actas, los talonarios se asocian a dependencias y no a inspectores
- el documento separa identidad lógica, numeración visible y soporte material
- el documento puede pre-numerarse o numerarse al firmar
- el documento puede firmarse digitalmente por integración externa o por circuito material/ológrafo con posterior incorporación o reemplazo de la versión firmada
- el snapshot quedó definido como derivado, regenerable y útil para operación, pero no como fuente primaria de verdad

---

## Backend consolidado

El bloque `spec/04-backend/` quedó consolidado a nivel de responsabilidades, persistencia y procesos.

Puntos cerrados importantes:

- el backend se concibe como núcleo compartido del ecosistema y no como backends separados por app
- el backend se organiza por servicios de expediente, documentos, firma, notificación, snapshot, bandejas y gestión externa
- medidas y liberaciones quedaron tratadas como responsabilidad coordinada principalmente por el servicio de expediente
- la persistencia backend se implementará con SQL explícito
- no se utilizará ORM como mecanismo principal de persistencia del núcleo transaccional
- `08-persistencia-y-sql.md` quedó como archivo índice
- se abrió el subbloque `spec/04-backend/08-persistencia-y-sql/` con:
  - esquema relacional base
  - versionado referencial
  - persistencia documental
  - numeración y talonarios
  - snapshot y reproyección
  - storage documental
  - consultas operativas y bandejas
- `09-jobs-workers-y-procesos.md` quedó definido como bloque de procesos diferidos, reproyección, integración y consistencia

También quedó firme que:

- el storage documental debe resolverse desacoplado del documento lógico mediante `StorageKey`
- las bandejas deben apoyarse preferentemente en snapshot o proyecciones equivalentes
- la reconstrucción histórica debe seguir apoyándose en el núcleo transaccional, especialmente en `ActaEvento`

---

## Integraciones consolidadas

El bloque `spec/09-integraciones/` quedó consolidado.

Puntos cerrados importantes:

- gestión externa desacoplada y correlacionada con efecto material sobre el expediente
- storage documental desacoplado del documento lógico mediante `StorageKey`
- autenticación y roles integrados con un IdP propio del ecosistema implementado con OpenIddict
- la identidad autenticada no debe confundirse con `Inspector`, `Dependencia` ni otras entidades del dominio
- la autorización del backend debe mantenerse explícita

---

## DDL físico abierto

Se abrió el bloque físico:

- `spec/13-ddl/`

Ya quedaron creados y avanzados:

- `00-mapa-ddl-fisico.md`
- `01-convenciones-ddl-informix.md`
- `02-tablas-nucleo-expediente.md`

---

## Reglas físicas ya cerradas para Informix

Quedaron definidas estas reglas base para el DDL físico:

- motor objetivo: **Informix 12.10**
- no usar `SERIAL`
- usar **secuencias**
- no todos los IDs deben ser `INT8`
- criterio base:
  - `INT8` para tablas de alto crecimiento
  - `INT` para tablas medianas o referenciales
  - `SMALLINT` para catálogos y referencias pequeñas
- GUID/UUID técnicos:
  - `CHAR(36)`
- fechas:
  - fecha sin hora → `DATETIME YEAR TO DAY`
  - fecha con hora → `DATETIME YEAR TO SECOND`
  - hora sola → `DATETIME HOUR TO SECOND`
- flags:
  - `SMALLINT`
- `VARCHAR` ajustado y no sobredimensionado
- no usar `TEXT`
- usar `LVARCHAR` solo cuando haga falta texto largo
- si un contenido excede el rango razonable de `LVARCHAR`, debe fragmentarse en más de un campo físico y recomponerse desde la capa de acceso a datos
- campos de auditoría de usuario:
  - `IdUserAlta`
  - `IdUserUltMod`
  - como `CHAR(36)` apuntando al `SubjectId` externo

---

## Estado real del trabajo DDL

El bloque DDL ya comenzó, pero todavía no puede considerarse cerrado.

Punto importante actual:

- `02-tablas-nucleo-expediente.md` quedó como primer borrador útil, pero requiere corrección antes de seguir bajando el resto de las tablas

Temas ya detectados para corregir en la próxima sesión:

- revisar tipos físicos de varias columnas
- evitar usar `INT8` donde no corresponde
- revisar longitudes de `VARCHAR`
- ajustar campos de auditoría para usar `IdUser...` en lugar de `Usr...`
- revisar campos de `Acta` y `ActaEvento`
- no perder de vista que todavía faltan las tablas satélite del acta real:
  - tránsito
  - contravención
  - mercadería
  - decomiso
  - medidas preventivas
- no meter esos datos dentro de `Acta` si deben resolverse como satélites

---

## Próximo paso correcto

El próximo paso natural al retomar es:

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

---

## Criterio para continuar

Al retomar el bloque físico:

- no rehacer dominio, datos, backend ni integraciones
- usar esa base como cerrada
- mantener archivos compactos y orientados a `spec-as-source`
- bajar a físico solo lo ya consolidado
- corregir primero el núcleo antes de seguir con el resto del esquema

---

## Estado de continuidad

Si se retoma en otro chat, debe asumirse como punto de partida que:

- `01-dominio` está cerrado
- `12-datos` está cerrado
- `04-backend` está cerrado
- `09-integraciones` está cerrado
- `13-ddl` está abierto
- el próximo punto exacto es corregir `02-tablas-nucleo-expediente.md`
- no corresponde volver a discutir fundamentos ya cerrados salvo contradicción real