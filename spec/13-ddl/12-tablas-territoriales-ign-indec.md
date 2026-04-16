# [13-DDL] 12 - TABLAS TERRITORIALES IGN / INDEC

## Finalidad

Este archivo documenta las tablas nacionales IGN / INDEC utilizadas por faltas para:

- selección de provincia
- resolución de municipio lógico
- fallback a departamento
- lookup de localidad
- lookup de localidad censal
- lookup de calles fuera de Malvinas
- enriquecimiento territorial complementario

---

## 1. Tabla: `geo_ign_provincia`

### Finalidad funcional
Fuente principal de provincias para selección de provincia y filtro territorial superior.

### Campos relevantes

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `id` | `SMALLINT` | No | PK |
| `categoria` | `CHAR(26)` | No | Categoría origen |
| `fuente` | `VARCHAR(12)` | No | Fuente |
| `iso_id` | `VARCHAR(4)` | No | Código ISO |
| `iso_nombre` | `VARCHAR(48)` | No | Nombre ISO |
| `nombre` | `VARCHAR(64)` | No | Nombre visible |
| `nombre_completo` | `VARCHAR(84)` | No | Nombre extendido |

### PK
- `id`

### Índices relevantes
- por `nombre`
- por `nombre_completo`
- `ux_geo_ign_provincia_iso_id`

---

## 2. Tabla: `geo_ign_municipio`

### Finalidad funcional
Fuente de municipios reales.

### Campos relevantes

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `id` | `INT` | No | PK |
| `categoria` | `CHAR(26)` | No | Categoría |
| `fuente` | `VARCHAR(12)` | No | Fuente |
| `nombre` | `VARCHAR(64)` | No | Nombre corto |
| `nombre_completo` | `VARCHAR(64)` | No | Nombre extendido |
| `provincia_id` | `SMALLINT` | No | Provincia |
| `provincia_nombre` | `VARCHAR(64)` | No | Denormalizado |

### PK
- `id`

### Índices relevantes
- por `provincia_id`
- por `(provincia_id, nombre)`
- por `(provincia_id, nombre_completo)`

---

## 3. Tabla: `geo_ign_departamento`

### Finalidad funcional
Fuente de departamentos, usada como fallback cuando no hay municipio real aplicable.

### Campos relevantes

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `id` | `INT` | No | PK |
| `fuente` | `VARCHAR(12)` | No | Fuente |
| `categoria` | `CHAR(36)` | No | Categoría |
| `nombre` | `VARCHAR(48)` | No | Nombre corto |
| `nombre_completo` | `VARCHAR(64)` | No | Nombre extendido |
| `provincia_id` | `SMALLINT` | No | Provincia |
| `provincia_nombre` | `VARCHAR(64)` | No | Denormalizado |

### PK
- `id`

### Índices relevantes
- por `provincia_id`
- por `(provincia_id, nombre)`
- por `(provincia_id, nombre_completo)`

---

## 4. Tabla: `geo_indec_localidad`

### Finalidad funcional
Fuente de localidades para resolver la localidad visible del domicilio fuera de Malvinas.

### Campos relevantes

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `id` | `INT8` | Sí | PK declarada en origen |
| `categoria` | `CHAR(36)` | No | Categoría |
| `departamento_id` | `INT` | Sí | Departamento |
| `departamento_nombre` | `VARCHAR(48)` | Sí | Nombre departamento |
| `fuente` | `VARCHAR(12)` | No | Fuente |
| `localidad_censal_id` | `INT8` | No | Referencia censal |
| `localidad_censal_nombre` | `VARCHAR(64)` | Sí | Nombre censal |
| `municipio_id` | `INT` | Sí | Municipio |
| `municipio_nombre` | `VARCHAR(64)` | Sí | Nombre municipio |
| `nombre` | `VARCHAR(64)` | No | Nombre visible |
| `provincia_id` | `SMALLINT` | No | Provincia |
| `provincia_nombre` | `VARCHAR(64)` | No | Denormalizado |

### PK
- `id`

### Índices relevantes
- por `provincia_id`
- por `(provincia_id, municipio_id)`
- por `(provincia_id, departamento_id)`
- por `(provincia_id, nombre)`

### Observación
Puede haber inconsistencias entre localidad, municipio y departamento.

---

## 5. Tabla: `geo_indec_localidad_censal`

### Finalidad funcional
Fuente de localidad censal, usada como soporte técnico para calles y persistencia territorial completa.

### Campos relevantes

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `id` | `INT8` | No | PK |
| `categoria` | `CHAR(36)` | No | Categoría |
| `departamento_id` | `INT` | Sí | Departamento |
| `departamento_nombre` | `VARCHAR(48)` | Sí | Nombre |
| `fuente` | `VARCHAR(12)` | No | Fuente |
| `municipio_id` | `INT` | Sí | Municipio |
| `municipio_nombre` | `VARCHAR(64)` | Sí | Nombre |
| `nombre` | `VARCHAR(64)` | No | Nombre visible |
| `provincia_id` | `SMALLINT` | No | Provincia |
| `provincia_nombre` | `VARCHAR(64)` | No | Denormalizado |

### PK
- `id`

### Índices relevantes
- por `provincia_id`
- por `(provincia_id, municipio_id)`
- por `(provincia_id, departamento_id)`
- por `(provincia_id, nombre)`

---

## 6. Tabla: `geo_indec_calles`

### Finalidad funcional
Fuente principal de calles fuera de Malvinas Argentinas.

### Campos relevantes

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `categoria` | `CHAR(36)` | No | Categoría |
| `departamento_id` | `INT` | Sí | Departamento |
| `departamento_nombre` | `VARCHAR(48)` | Sí | Nombre |
| `fuente` | `VARCHAR(12)` | No | Fuente |
| `id` | `INT8` | No | PK |
| `localidad_censal_id` | `INT8` | No | Localidad censal |
| `localidad_censal_nombre` | `VARCHAR(64)` | Sí | Nombre censal |
| `nombre` | `VARCHAR(64)` | No | Nombre visible |
| `provincia_id` | `SMALLINT` | No | Provincia |
| `provincia_nombre` | `VARCHAR(64)` | No | Denormalizado |

### PK
- `id`

### Índices relevantes
- por `nombre`
- por `localidad_censal_id`
- por `(localidad_censal_id, nombre)`
- por `(provincia_id, nombre)`

### Regla importante
Si la calle no existe en catálogo:

- se permite ingreso textual
- no se inserta en catálogo
- no se corrige el origen desde el formulario
- el domicilio queda parcialmente normalizado

---

## 7. Tabla: `geo_bahra_asentamiento`

### Finalidad funcional
Fuente complementaria para asentamientos, barrios o denominaciones territoriales.

No es tabla principal del flujo de domicilio, pero puede servir para:

- enriquecimiento
- sugerencias
- resolución complementaria
- análisis territorial futuro

### Campos relevantes

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `categoria` | `CHAR(36)` | No | Categoría |
| `departamento_id` | `INT` | Sí | Departamento |
| `departamento_nombre` | `VARCHAR(48)` | Sí | Nombre |
| `fuente` | `VARCHAR(36)` | No | Fuente |
| `id` | `VARCHAR(20)` | No | PK |
| `localidad_censal_id` | `INT8` | No | Localidad censal |
| `localidad_censal_nombre` | `VARCHAR(64)` | Sí | Nombre |
| `municipio_id` | `INT` | Sí | Municipio |
| `municipio_nombre` | `VARCHAR(64)` | Sí | Nombre |
| `nombre` | `VARCHAR(84)` | No | Nombre visible |
| `provincia_id` | `SMALLINT` | No | Provincia |
| `provincia_nombre` | `VARCHAR(64)` | No | Denormalizado |

### PK
- `id`

---
