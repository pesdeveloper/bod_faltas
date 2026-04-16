# [14-SQL-OPERATIVO] 03B - SQL LOOKUPS LICENCIA Y JURISDICCION

## Finalidad

Este archivo documenta los lookups de:

- municipio emisor de licencia de conducir
- jurisdicción administrativa visible para UX
- municipio lógico basado en municipio real o departamento fallback

Esto **no es un domicilio completo**.

No requiere:

- calle
- altura
- barrio
- resolución de tramo

---

## Tablas principales

- `geo_ign_provincia`
- `geo_ign_municipio`
- `geo_ign_departamento`

---

## Regla UX

El usuario debe ver un campo simple:

- **Municipio emisor**

Pero internamente el valor puede resolverse como:

- municipio real
- o departamento fallback

La UX no obliga a distinguir entre ambos.

---

## Flujo operativo

### 1. Provincia

- `geo_ign_provincia`
- selección obligatoria
- persistir `provincia_id`

### 2. Municipio emisor lógico

Búsqueda principal:
- `geo_ign_municipio`
- filtrar por `provincia_id`
- búsqueda por prefijo

Fallback:
- `geo_ign_departamento`
- mismo comportamiento visual en UX

### Persistencia esperada

Debe persistirse:

- provincia emisora
- municipio emisor real cuando exista
- departamento emisor cuando opere fallback
- tipo de jurisdicción emisora para distinguir si el valor persistido corresponde a municipio o departamento

La UX sigue mostrando un único campo lógico, pero la persistencia debe conservar la diferencia real.

### Shape UX esperado

- `id`
- `tipo` = `MUNICIPIO` o `DEPARTAMENTO`
- `descripcion`
- `provincia_id`
- `provincia_nombre`

---

## Reglas importantes

- no bajar a localidad, calle ni altura
- no mezclar lookup jurisdiccional con lookup de domicilio
- no modificar tablas territoriales externas
- no asumir que toda provincia tendrá municipios utilizables en todos los casos

---
