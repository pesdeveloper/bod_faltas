# [14-SQL-OPERATIVO] 03 - SQL DE FORMULARIOS Y LOOKUPS

## Finalidad

Este archivo funciona como puerta de entrada para el bloque de consultas SQL de:

- formularios
- autocompletados
- combos
- validaciones UX
- búsquedas territoriales
- lookups de catálogos operativos
- lookups de numeración y storage

El detalle se reparte en archivos satélite para mantener el bloque navegable.

---

## Cómo leer este bloque

- domicilio del infractor → [`03a-sql-lookups-domicilio-infractor.md`](./03a-sql-lookups-domicilio-infractor.md)
- licencia y jurisdicción → [`03b-sql-lookups-licencia-y-jurisdiccion.md`](./03b-sql-lookups-licencia-y-jurisdiccion.md)
- catálogos y validaciones → [`03c-sql-lookups-catalogos-y-validaciones.md`](./03c-sql-lookups-catalogos-y-validaciones.md)

---

## Reglas madre

- Estas consultas no son reportes.
- Deben ser rápidas, acotadas y orientadas a selección.
- El patrón principal de búsqueda es por prefijo.
- El resultado esperado es corto y utilizable por la UX.
- Debe distinguirse dato normalizado de dato textual libre.
- Los catálogos externos o compartidos no se modifican desde faltas.
- Se admite validación parcial cuando la calidad del origen no permite resolución completa.
- La UX debe ser simple aunque el backend persista más detalle territorial.

---

## Relación con otras partes de la spec

- tablas territoriales externas y compartidas → [`../13-ddl/11-tablas-territoriales-externas-introduccion.md`](../13-ddl/11-tablas-territoriales-externas-introduccion.md)
- tablas IGN / INDEC → [`../13-ddl/12-tablas-territoriales-ign-indec.md`](../13-ddl/12-tablas-territoriales-ign-indec.md)
- tablas locales Malvinas → [`../13-ddl/13-tablas-territoriales-malvinas-locales.md`](../13-ddl/13-tablas-territoriales-malvinas-locales.md)
- georreferenciación territorial → [`../13-ddl/14-tablas-georreferenciacion-territorial.md`](../13-ddl/14-tablas-georreferenciacion-territorial.md)

---
