# [13-DDL] 13 - TABLAS TERRITORIALES MALVINAS LOCALES

## Finalidad

Este archivo documenta las tablas locales de Malvinas Argentinas utilizadas por faltas para resolución fina del domicilio del infractor dentro del municipio.

Se usan principalmente para:

- localidad local
- calle local
- validación por altura
- resolución de tramo
- resolución de barrio
- soporte catastral local

---

## 1. Tabla: `localidad`

### Finalidad funcional
Catálogo local de localidades de Malvinas Argentinas.

### Campos relevantes

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `id_loc` | `CHAR(2)` | No | PK |
| `deno` | `CHAR(40)` | No | Denominación |
| `cp` | `SMALLINT` | Sí | Código postal |

### PK
- `id_loc`

### Índices relevantes
- por `deno`

---

## 2. Tabla: `calle`

### Finalidad funcional
Catálogo local de calles de Malvinas Argentinas.

### Campos relevantes

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `id_tca` | `CHAR(5)` | No | PK |
| `deno` | `CHAR(48)` | No | Nombre principal |
| `denobusq` | `VARCHAR(40)` | Sí | Denominación de búsqueda |
| `denoant1` | `VARCHAR(40)` | Sí | Alias |
| `denoant2` | `VARCHAR(40)` | Sí | Alias |
| `denoant3` | `VARCHAR(40)` | Sí | Alias |
| `denoant4` | `VARCHAR(40)` | Sí | Alias |
| `id_tca_pant` | `CHAR(5)` | Sí | Relación |
| `id_tca_ppost` | `CHAR(5)` | Sí | Relación |
| `id_tca_nace` | `CHAR(5)` | Sí | Relación |
| `id_tca_fin` | `CHAR(5)` | Sí | Relación |

### PK
- `id_tca`

### Índices relevantes
- por `deno`

---

## 3. Tabla: `barrio`

### Finalidad funcional
Catálogo local de barrios.

### Campos relevantes

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `id_bar` | `SMALLINT` | No | PK |
| `deno` | `CHAR(20)` | No | Nombre |
| `clase` | `CHAR(1)` | Sí | Clase |

### PK
- `id_bar`

### Índices relevantes
- por `deno`

---

## 4. Tabla: `geo_calle_alturas_barrio`

### Finalidad funcional
Tabla principal de resolución fina para domicilios dentro de Malvinas Argentinas.

Se usa para:

- validar calle + altura
- resolver tramo
- resolver barrio
- identificar localidad local
- determinar ejido urbano

### Campos relevantes

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `id_tca` | `CHAR(5)` | No | Calle |
| `alt_desde` | `INT` | No | Desde |
| `alt_hasta` | `INT` | No | Hasta |
| `calle_nombre` | `VARCHAR(64)` | No | Nombre visible |
| `localidad_geo` | `VARCHAR(64)` | No | Localidad texto |
| `id_bar` | `SMALLINT` | No | Barrio |
| `barrio_geo` | `VARCHAR(64)` | No | Barrio texto |
| `SiEjidoUrbano` | `SMALLINT` | No | 0/1 |
| `id_loc` | `CHAR(2)` | No | Localidad |

### PK
- `(id_tca, id_loc, id_bar, alt_desde, alt_hasta)`

### Índices relevantes
- `(id_loc, calle_nombre, alt_desde, alt_hasta)`
- `(localidad_geo, calle_nombre, alt_desde, alt_hasta)`
- por `calle_nombre`
- por `localidad_geo`
- por `id_bar`

### Regla operativa
Si no hay coincidencia exacta de tramo para la altura:

- el domicilio puede persistirse igual
- se considera validación parcial
- puede quedar sin barrio resuelto

---

## 5. Tabla: `manzana`

### Finalidad funcional
Soporte catastral local complementario.

No es tabla principal del lookup básico del formulario, pero forma parte del ecosistema local de resolución territorial.

### Campos relevantes

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `id_mza` | `INT` | No | PK |
| `id_grg` | `SMALLINT` | No | Grupo geográfico |
| `id_zdi` | `SMALLINT` | Sí | Zona |
| `id_loc` | `CHAR(2)` | No | Localidad |
| `id_bar` | `SMALLINT` | Sí | Barrio |
| `circ` | `SMALLINT` | No | Circunscripción |
| `secc` | `CHAR(2)` | Sí | Sección |
| `chacra` | `CHAR(7)` | Sí | Chacra |
| `quinta` | `CHAR(7)` | Sí | Quinta |
| `frac` | `CHAR(7)` | Sí | Fracción |
| `mza` | `CHAR(7)` | Sí | Manzana |
| `parc` | `CHAR(7)` | Sí | Parcela |
| `clase` | `DECIMAL(1,0)` | Sí | Clase |
| `cuadrante` | `DECIMAL(1,0)` | Sí | Cuadrante |
| `normalizado` | `DECIMAL(1,0)` | Sí | Flag |
| `tipo` | `CHAR(1)` | Sí | Tipo |

---

## 6. Tabla: `callexmza`

### Finalidad funcional
Relación entre calle y manzana / tramo local.

Es tabla de apoyo catastral local.

### Campos relevantes

| Campo | Tipo físico | Null | Observación |
|---|---|---:|---|
| `id_mza` | `INT` | No | PK/FK |
| `id_tca` | `CHAR(5)` | No | PK/FK |
| `sentido` | `SMALLINT` | No | PK |
| `desdealt` | `SMALLINT` | No | Desde |
| `hastaalt` | `SMALLINT` | No | Hasta |
| `impar` | `DECIMAL(1,0)` | Sí | Paridad |
| `a1_id_tca` | `CHAR(5)` | Sí | Calle auxiliar |
| `a2_id_tca` | `CHAR(5)` | Sí | Calle auxiliar |
| `reflejar` | `DECIMAL(1,0)` | Sí | Flag |
| `tramo` | `INT` | Sí | Tramo |

---

## Resumen funcional de uso

### Tablas principales del lookup local
- `localidad`
- `calle`
- `geo_calle_alturas_barrio`

### Tablas de apoyo
- `barrio`
- `manzana`
- `callexmza`

---
