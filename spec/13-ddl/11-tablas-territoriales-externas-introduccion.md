# [13-DDL] 11 - TABLAS TERRITORIALES EXTERNAS INTRODUCCION

## Finalidad

Este archivo introduce las tablas territoriales externas y compartidas que el sistema de faltas utiliza para:

- resolución de domicilios
- lookups territoriales
- fallback administrativo
- validación parcial de direcciones
- resolución de municipio lógico
- soporte a jurisdicción emisora de licencia

Estas tablas **no son creadas ni gobernadas** por el sistema de faltas, pero forman parte del contexto funcional necesario para operar correctamente.

---

## Naturaleza de estas tablas

Estas tablas se consideran:

- externas o compartidas
- consumidas funcionalmente por faltas
- fuente de lookup y resolución territorial
- no parte del núcleo físico propio de faltas

En consecuencia:

- no usan prefijo `Fal`
- no deben re-bautizarse artificialmente
- deben conservar sus nombres reales existentes

---

## Observación sobre calidad de datos

Las tablas territoriales externas presentan problemas heredados de origen. Deben asumirse como posibles:

- registros huérfanos
- redundancias
- inconsistencias nominales
- estructuras no completamente normalizadas
- relaciones incompletas entre niveles territoriales
- cobertura irregular según provincia o jurisdicción

Por eso el sistema de faltas debe consumirlas con:

- políticas explícitas de búsqueda
- fallback territorial
- validación parcial
- tolerancia a datos incompletos
- separación entre dato normalizado y dato textual libre

No debe asumirse consistencia perfecta del origen.

---

## Clasificación general

### Tablas IGN / INDEC
Se documentan en:
- [`12-tablas-territoriales-ign-indec.md`](./12-tablas-territoriales-ign-indec.md)

### Tablas locales Malvinas Argentinas
Se documentan en:
- [`13-tablas-territoriales-malvinas-locales.md`](./13-tablas-territoriales-malvinas-locales.md)

### Tablas de georreferenciación territorial
Se documentan en:
- [`14-tablas-georreferenciacion-territorial.md`](./14-tablas-georreferenciacion-territorial.md)

---

## Relación con otros archivos

- lógica de búsqueda y fallback: [`../14-sql-operativo/03-sql-formularios-y-lookups.md`](../14-sql-operativo/03-sql-formularios-y-lookups.md)
- domicilio del infractor: [`../14-sql-operativo/03a-sql-lookups-domicilio-infractor.md`](../14-sql-operativo/03a-sql-lookups-domicilio-infractor.md)
- licencia y jurisdicción: [`../14-sql-operativo/03b-sql-lookups-licencia-y-jurisdiccion.md`](../14-sql-operativo/03b-sql-lookups-licencia-y-jurisdiccion.md)

---
