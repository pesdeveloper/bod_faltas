# Persistencia y SQL

## Finalidad

Este bloque define cómo se baja el modelo lógico consolidado a persistencia relacional y SQL.

No rehace dominio ni reemplaza `spec/12-datos/`: fija criterios de persistencia, integridad, versionado, proyecciones y consultas SQL del sistema.

---

## Alcance

Este bloque cubre:

- persistencia del núcleo del expediente
- referencias y versionado referencial
- persistencia documental y de notificaciones
- numeración y talonarios
- snapshot operativo
- criterios generales de consultas SQL

No cubre todavía:

- DDL físico final completo
- índices definitivos
- tuning
- migraciones
- sentencias SQL finales

---

## Reglas base

La persistencia debe respetar lo ya consolidado en:

- `spec/01-dominio/`
- `spec/12-datos/`

Por lo tanto:

- no redefine el dominio
- no convierte al snapshot en fuente primaria
- no mezcla semánticas ya separadas
- no delega el diseño en ORM

---

## Criterio de acceso a datos

El backend debe implementarse con SQL explícito.

Por lo tanto:

- el modelo debe bajar bien a SQL nativo
- las consultas deben ser claras y controlables
- la integridad del diseño no debe depender de mapeo objeto-relacional automático

---

## Ejes de persistencia

La persistencia del sistema se organiza alrededor de:

- `Acta`
- `ActaEvento`
- `Documento`
- `Notificacion`

Y de estos soportes:

- `Dependencia`
- `Inspector`
- versiones referenciales
- `Talonario` y política de numeración
- medidas y liberaciones
- catálogos y maestros
- snapshot operativo
- soporte documental e integraciones auxiliares

---

## Reglas generales

- `Acta` se persiste como entidad principal del expediente.
- `ActaEvento` se persiste como núcleo de trazabilidad relevante.
- `Documento` debe separar identidad lógica, numeración visible, estado y soporte material.
- `Notificacion` se persiste como proceso transversal propio.
- `Dependencia` e `Inspector` requieren persistencia de versión aplicable para congelar contexto histórico.
- La numeración visible debe persistirse con su contexto de numeración aplicable.
- El snapshot puede persistirse, pero sigue siendo derivado y regenerable.

---

## SQL para consultas

Las consultas SQL deben distinguir entre:

- núcleo transaccional
- snapshot operativo
- trazabilidad
- documentos
- notificaciones
- numeración y control

En general:

- bandejas y listados deberían apoyarse en snapshot o proyecciones equivalentes
- reconstrucción histórica debería apoyarse en `ActaEvento`
- documentos y notificaciones deben consultarse en sus estructuras propias

---

## Integridad

La persistencia debe asegurar, según corresponda:

- unicidad de identidades internas
- unicidad de identidades técnicas
- unicidad operativa de numeraciones visibles dentro de su contexto
- referencias obligatorias del expediente a sus piezas principales
- coherencia entre entidad referencial y versión aplicada
- separación entre dato principal y derivado

---

## Desarrollo del bloque

Los detalles de persistencia y SQL se desarrollan en los siguientes archivos:

- [01-esquema-relacional-base](08-persistencia-y-sql/01-esquema-relacional-base.md)
- [02-versionado-referencial](08-persistencia-y-sql/02-versionado-referencial.md)
- [03-persistencia-documental](08-persistencia-y-sql/03-persistencia-documental.md)
- [04-numeracion-y-talonarios](08-persistencia-y-sql/04-numeracion-y-talonarios.md)
- [05-snapshot-y-reproyeccion](08-persistencia-y-sql/05-snapshot-y-reproyeccion.md)
- [06-storage-documental](08-persistencia-y-sql/06-storage-documental.md)
- [07-consultas-operativas-y-bandejas](08-persistencia-y-sql/07-consultas-operativas-y-bandejas.md)