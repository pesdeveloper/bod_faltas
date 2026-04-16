# [14-SQL-OPERATIVO] 03A - SQL LOOKUPS DOMICILIO INFRACTOR

## Finalidad

Este archivo documenta la política operativa de búsquedas y lookups para el **domicilio del infractor**.

La resolución tiene dos estrategias:

1. domicilio dentro de Malvinas Argentinas
2. domicilio fuera de Malvinas Argentinas

La UX debe seguir siendo simple en ambos casos.

---

## Regla general UX

Campos visibles esperados:

1. Provincia
2. Municipio
3. Localidad
4. Calle
5. Altura
6. Piso / Depto / Observaciones opcionales

No se expone en UX:

- departamento
- localidad censal
- barrio técnico
- fallback administrativo

---

## Caso A - Domicilio del infractor en Malvinas Argentinas

### Tablas principales

- `localidad`
- `calle`
- `geo_calle_alturas_barrio`

### Tablas de apoyo

- `barrio`
- `manzana`
- `callexmza`

### Flujo operativo

1. localidad → lookup en `localidad`
2. calle → lookup en `calle`
3. altura → `altura > 0`
4. tramo → buscar en `geo_calle_alturas_barrio` con:
   - localidad local
   - calle local
   - `altura BETWEEN alt_desde AND alt_hasta`

### Resultado esperado

Si existe tramo:

- calle validada
- localidad validada
- barrio resuelto
- posibilidad de conocer `SiEjidoUrbano`

Si no existe tramo:

- el domicilio puede persistirse igual
- queda como validación parcial
- puede quedar sin barrio resuelto

### Persistencia esperada

Además del shape nacional general, cuando el domicilio del infractor sea de Malvinas se deben poder persistir también referencias locales finas:

- localidad local de Malvinas
- calle local de Malvinas
- barrio local resuelto
- altura
- indicador de normalización parcial si no hubo resolución completa

Esto permite compatibilidad nacional y resolución local fina al mismo tiempo.

---

## Caso B - Domicilio del infractor fuera de Malvinas Argentinas

### Tablas principales

- `geo_ign_provincia`
- `geo_ign_municipio`
- `geo_ign_departamento`
- `geo_indec_localidad`
- `geo_indec_localidad_censal`
- `geo_indec_calles`

### Tabla complementaria

- `geo_bahra_asentamiento`

### Principio central

El usuario ve siempre el campo:

- **Municipio**

Pero internamente ese valor puede provenir de:

- un municipio real
- o un departamento fallback

El usuario no distingue esa diferencia.  
El sistema sí la conoce y la persiste.

---

## Flujo operativo fuera de Malvinas

### 1. Provincia

- `geo_ign_provincia`
- selección obligatoria
- persistir `provincia_id`

### 2. Municipio lógico

Búsqueda principal:
- `geo_ign_municipio`
- por `provincia_id`
- por prefijo sobre nombre

Fallback:
- `geo_ign_departamento`
- mismo comportamiento visual en UX

Persistencia:
- `municipio_id` puede quedar `NULL`
- `departamento_id` debe quedar definido

### 3. Localidad

Si hay `municipio_id`:
- buscar en `geo_indec_localidad` por `provincia_id + municipio_id`

Si no hay `municipio_id`:
- buscar en `geo_indec_localidad` por `provincia_id + departamento_id`

Persistencia:
- `localidad_id`
- `localidad_censal_id`

### 4. Calle

- `geo_indec_calles`
- buscar por `localidad_censal_id`
- búsqueda por prefijo
- devolver `id`, `nombre`

### 5. Altura

- `altura > 0`

---

## Calle no encontrada en catálogo

### Regla obligatoria

Si la calle no existe en catálogo:

- se permite ingreso textual
- no se agrega al catálogo
- no se modifica el origen
- no se normaliza artificialmente
- el domicilio queda parcialmente normalizado

### Consecuencia operativa

Debe distinguirse entre:

- calle normalizada por ID
- calle textual libre

y además debe quedar marca explícita de normalización parcial cuando corresponda.

---

## Persistencia esperada fuera de Malvinas

Debe poder persistirse:

- `provincia_id`
- `municipio_id` nullable
- `departamento_id`
- `localidad_id`
- `localidad_censal_id`
- `calle_id` si existe normalización
- calle textual libre si no existe normalización
- `altura`
- piso / depto / observaciones
- indicador de normalización parcial

---

## Regla de persistencia general del domicilio del infractor

El modelo debe soportar simultáneamente:

- shape nacional común
- calle textual libre cuando no exista catálogo
- normalización parcial explícita
- referencias locales finas de Malvinas cuando apliquen

Esto evita perder riqueza local y mantiene compatibilidad con el modelo nacional.

---

## Reglas importantes

- no modificar catálogos IGN / INDEC desde faltas
- no asumir consistencia perfecta del origen
- usar fallback explícito de municipio a departamento
- admitir validación parcial
- mantener UX simple y persistencia rica

---
