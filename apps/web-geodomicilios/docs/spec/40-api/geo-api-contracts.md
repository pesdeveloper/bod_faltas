# Contratos de API Geo

> **Estado documental:** NORMATIVE
> **Alcance:** endpoints del backend Faltas Core consumidos por el frontend GeoDomicilios.
> **Total de endpoints implementados:** 12 (verificado contra `GeoController.java`)

---

## 1. URL base y proxy

En desarrollo: proxy Vite en `proxy.conf.json` enruta `/api -> http://localhost:8080`.
En el codigo: `API_CONFIG.baseUrl + path`.

---

## 2. Endpoints de catalogos externos (IGN/INDEC)

Fuente: MariaDB (`sb_faltas_db`). Implementados en `GeoCatalogMariaDbRepository`.

| Endpoint | Descripcion | Response |
|---|---|---|
| `GET /api/geo/provincias` | Lista de provincias | `ProvinciaGeoDto[]` |
| `GET /api/geo/unidades-territoriales?provinciaId=X` | Municipios y departamentos de una provincia | `UnidadTerritorialGeoDto[]` |
| `GET /api/geo/localidades?unidadTerritorialId=X` | Localidades de un municipio/departamento | `LocalidadGeoDto[]` |
| `GET /api/geo/calles?q=X&localidadId=X` | Busqueda de calles en catalogo externo (min 2 chars, max 20 resultados) | `CalleGeoDto[]` |

---

## 3. Endpoints de datos locales Malvinas

Fuente: PostGIS (`sb_malvinas_geo`). Implementados en `GeoRepository`.

### GET /api/geo/local/malvinas/localidades

Devuelve todas las localidades del partido.

Response: `LocalidadGeoDto[]` - campos: `idLoc`, `nombre`, `fuente`.

### GET /api/geo/local/malvinas/calles

Busca calles con autocomplete fuzzy (funcion `geo_buscar_calles_tolerante`).

| Parametro | Tipo | Obligatorio | Descripcion |
|---|---|---|---|
| `q` | string | Si | Texto de busqueda (min 2 chars, max 5 resultados) |
| `idLoc` | string | No | Filtro por localidad |
| `altura` | int | No | Filtra tramos donde esa altura no es valida |

Response: `CalleGeoDto[]` - campos: `idTca`, `nombre`, `idLoc`, `localidad`, `nombreAnterior`, `texto`, `fuente`.

### POST /api/geo/geocodificar

Geocodifica una direccion por calle + altura. Estrategia detallada en `30-geocoding/geocoding-contracts.md`.

Request body: `GeocodeCalleAlturaRequest`

| Campo | Tipo | Descripcion |
|---|---|---|
| `idTca` | String | Codigo interno de la calle (de CalleGeoDto.idTca) |
| `idLoc` | String | Codigo de localidad |
| `altura` | Integer | Numero de puerta (1 a 99999) |

Response: `GeocodeCandidato[]` - ver seccion 4.

### POST /api/geo/reverse

Geocodificacion inversa: de un punto a la calle mas cercana.

Request body: `ReversePointRequest`

| Campo | Tipo | Descripcion |
|---|---|---|
| `lat` | double | Latitud WGS84 |
| `lng` | double | Longitud WGS84 |

Response: `GeocodeCandidato[]` (hasta 3 resultados, radio 100 m).

### POST /api/geo/geocodificar-esquina

Geocodifica una interseccion de dos calles.

Request body: `EsquinaRequest`

| Campo | Tipo | Descripcion |
|---|---|---|
| `idTcaCalle1` | String | Codigo interno de la primera calle |
| `idLocCalle1` | String | Codigo de localidad de la primera calle |
| `idTcaCalle2` | String | Codigo interno de la segunda calle |
| `idLocCalle2` | String | Codigo de localidad de la segunda calle |

Response: `GeocodeCandidato[]`.

### POST /api/geo/geocodificar-entre-calles

Geocodifica el segmento de una calle delimitado por dos calles transversales.

Request body: `EntreCallesRequest`

| Campo | Tipo | Descripcion |
|---|---|---|
| `idTcaCalleObjetivo` | String | Codigo de la calle objetivo |
| `idLocCalleObjetivo` | String | Localidad de la calle objetivo |
| `idTcaLimite1` | String | Primera calle limite |
| `idLocLimite1` | String | Localidad del primer limite |
| `idTcaLimite2` | String | Segunda calle limite |
| `idLocLimite2` | String | Localidad del segundo limite |

Response: `GeocodeCandidato[]`.

### GET /api/geo/local/malvinas/parcela/detalle

Devuelve el detalle completo de una parcela.

| Parametro | Descripcion |
|---|---|
| `circ` | Circunscripcion |
| `secc` | Seccion |
| `quint` | Quinta (usar `0` si ausente) |
| `fracc` | Fraccion (usar `0` si ausente) |
| `manz` | Manzana |
| `parc` | Parcela |

Response: `ParcelaDetalleDto` - ver seccion 4.

**Regla** [GEO-API-001]: todos los campos se comparan con `TRIM()` en el SQL.
Enviar siempre `quint=0` y `fracc=0` cuando esten ausentes.

### GET /api/geo/local/malvinas/parcela/geom

Devuelve la geometria completa de la parcela como GeoJSON (Polygon o MultiPolygon, EPSG:4326).
Mismos parametros que el endpoint de detalle.

Usado para el highlight completo de la parcela seleccionada (evita el clipeo de los tiles MVT).

---

## 4. DTOs principales

### GeocodeCandidato

| Campo | Tipo | Descripcion |
|---|---|---|
| `lat` | double | Latitud WGS84 |
| `lng` | double | Longitud WGS84 |
| `calle` | String | Nombre canonico de la calle |
| `idTca` | String | Codigo interno de la calle |
| `alturaAprox` | Integer | Altura aproximada geocodificada |
| `alturaRangoDesde` | Integer | Inicio del rango de altura usado |
| `alturaRangoHasta` | Integer | Fin del rango de altura usado |
| `ladoCuadra` | String | Lado de la cuadra (I/D) |
| `idLoc` | String | Codigo de localidad |
| `localidad` | String | Nombre de localidad |
| `barrio` | String | Nombre de barrio (si disponible) |
| `manzana` | String | Codigo de manzana |
| `nomenclatura` | String | Nomenclatura catastral del punto |
| `calidad` | String | ALTA o MEDIA |
| `evidencia` | String | Fuente usada: `fal_inmueble_interp_snapped` o `range_interp` |
| `advertencias` | String[] | Avisos no bloqueantes |
| `descripcion` | String | Texto legible del resultado |

### ParcelaDetalleDto

| Campo | Tipo | Descripcion |
|---|---|---|
| `nomenclatura` | String | Nomenclatura legible (omite Q0 y F0) |
| `direccion` | String | Direccion mas cercana inferida |
| `centLat` | double | Latitud del centroide de la parcela |
| `centLng` | double | Longitud del centroide de la parcela |
| `inmuebles` | `InmuebleResumenDto[]` | Inmuebles asociados (desde `fal_inmueble` en PostGIS) |
| `comercios` | `ComercioResumenDto[]` | Comercios habilitados (desde `fal_comercio` en PostGIS) |

### InmuebleResumenDto

| Campo | Tipo | Descripcion |
|---|---|---|
| `idSuj` | Long | ID sujeto catastral |
| `idBie` | Long | ID bien catastral |
| `cuentaDisplay` | String | Numero de cuenta formateado para display |

### ComercioResumenDto

| Campo | Tipo | Descripcion |
|---|---|---|
| `idSuj` | Long | ID sujeto |
| `idBie` | Long | ID bien |
| `nombreFantasia` | String | Nombre fantasia del comercio |
| `cuentaDisplay` | String | Numero de cuenta formateado para display |

### LocalidadGeoDto

| Campo | Tipo | Descripcion |
|---|---|---|
| `idLoc` | String | Codigo de localidad (2 chars en fuente Malvinas) |
| `nombre` | String | Nombre de la localidad |
| `fuente` | String | `MALVINAS_LOCAL` o `EXTERNO_IGN_INDEC` |

### CalleGeoDto

| Campo | Tipo | Descripcion |
|---|---|---|
| `idTca` | String | Codigo interno de la calle |
| `nombre` | String | Nombre canonico |
| `idLoc` | String | Codigo de localidad |
| `localidad` | String | Nombre de localidad |
| `nombreAnterior` | String | Nombre anterior (si existe) |
| `texto` | String | Texto para display en autocomplete |
| `fuente` | String | `MALVINAS_LOCAL` o `EXTERNO_IGN_INDEC` |

---

## 5. Reglas de matching catastral  [GEO-API-002]

- Todos los campos catastrales tienen espacios en blanco en la base de datos. Siempre aplicar `TRIM()` en el SQL.
- `quint` y `fracc`: valor `0` representa ausencia. Nunca pasar NULL.
- `circunsc`: columna CHAR(1). Cast con `::text` al comparar con varchar.
- Joins entre `fal_inmueble` y `geo_qmat_parcela`: usar los cinco campos con TRIM en ambos lados.
- Nomenclatura visible: omitir segmentos `Q0` y `F0`.
