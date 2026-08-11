# db/postgis — Scripts SQL de GeoDomicilios

Directorio de scripts SQL ejecutables para el entorno PostgreSQL/PostGIS de GeoDomicilios.
Base de datos de referencia: `sb_malvinas_geo`.

---

## Versiones validadas (2026-07-30)

| Componente | Version validada |
|---|---|
| PostgreSQL | 15.2 (Visual C++ build 1914, 64-bit) |
| PostGIS | 3.3 (USE_GEOS=1 USE_PROJ=1 USE_STATS=1) |
| Martin  | Ultima version estable disponible y compatible |

---

## Precondiciones

Antes de ejecutar cualquier script:

1. La base `sb_malvinas_geo` debe existir con la extension PostGIS habilitada.
2. El backup de datos geoespaciales debe estar restaurado (ver `apps/web-geodomicilios/docs/spec/60-infrastructure/postgis-local-setup.md`, seccion 4).
3. Las tablas base deben existir:
   `geo_qmat_parcela`, `geo_gmat_manzana`, `geo_gmat_calle`, `geo_gmat_barrio`, `geo_gmat_localidad`,
   `geo_gmat_entorno_muni`, `geo_gmat_espacio_verde`, `geo_gmat_ferrocarril`, `geo_gmat_ferrocarril_estaciones`,
   `fal_inmueble`, `fal_comercio`, `fal_comercio_inmueble`.
4. Para `functions/geo_buscar_calles_tolerante.sql`: las funciones `geo_normalizar_termino` y
   `geo_tokens_sorted` y la tabla `geo_malv_calle_indice_busqueda` deben existir.

---

## Orden de ejecucion

Ejecutar en este orden. Cada script es idempotente (usa `IF NOT EXISTS` o `CREATE OR REPLACE`).

| Orden | Archivo | Dependencias | Estado |
|---|---|---|---|
| 1 | `tables/fal_inmueble.sql` | backup restaurado | **RESUELTO — DDL verificado 2026-07-30** |
| 2 | `tables/fal_comercio.sql` | backup restaurado | **RESUELTO — DDL verificado 2026-07-30** |
| 3 | `tables/fal_comercio_inmueble.sql` | fal_inmueble, fal_comercio | **RESUELTO — DDL verificado 2026-07-30** |
| 4 | `functions/geo_buscar_calles_tolerante.sql` | geo_malv_calle_indice_busqueda, geo_normalizar_termino, geo_tokens_sorted | **RESUELTO — DDL verificado 2026-07-30** |
| 5 | `views/v_martin_label_views.sql` | tablas base con SRID 22195 | DISPONIBLE |
| 6 | `functions/comercios_parcela_mvt.sql` | fal_comercio, fal_comercio_inmueble, geo_qmat_parcela | **RESUELTO — DDL verificado 2026-07-30** |
| 7 | `functions/comercios_manzana_mvt.sql` | fal_comercio, fal_inmueble, geo_gmat_manzana | **RESUELTO — DDL versionado 2026-07-30** |

> La vista materializada `geo_calle_altura_ref` tiene su DDL documentado en
> `apps/web-geodomicilios/docs/spec/30-geocoding/geocoding-contracts.md`, seccion 4.
> Pendiente de mover a `views/geo_calle_altura_ref.sql` en un slice posterior.

---

## GAPs — Estado actualizado al 2026-07-30

### GAP HB-1: comercios_parcela — RESUELTO

Funcion verificada en `sb_malvinas_geo`. DDL real volcado en `functions/comercios_parcela_mvt.sql`.
Firma: `comercios_parcela(z integer, x integer, y integer) RETURNS bytea LANGUAGE sql STABLE PARALLEL SAFE`.
Estado en Martin: publicada como fuente MVT `comercios_parcela` (confirmado via `/catalog`).

### GAP HB-2: fal_inmueble / fal_comercio / fal_comercio_inmueble — RESUELTO

Las tres tablas existen en `sb_malvinas_geo`. DDL real volcado desde information_schema + pg_constraint + pg_indexes.
Filas al 2026-07-30: fal_inmueble 85.167 / fal_comercio 13.024 / fal_comercio_inmueble 12.737.
Sin columna de geometria propia; el join espacial se realiza via clave catastral con geo_qmat_parcela.

### GAP HB-3: geo_buscar_calles_tolerante — RESUELTO

Funcion verificada en `sb_malvinas_geo`. DDL real volcado en `functions/geo_buscar_calles_tolerante.sql`.
Firma: `geo_buscar_calles_tolerante(p_query text, p_id_loc text DEFAULT NULL, p_limit integer DEFAULT 5)`.
Dependencias reales: `geo_normalizar_termino`, `geo_tokens_sorted`, `geo_malv_calle_indice_busqueda`.
Nota: la dependencia documentada anteriormente como `geo_calle_nombre_lookup` era incorrecta; la tabla
real que usa esta funcion es `geo_malv_calle_indice_busqueda`.

### GAP HB-4: versiones PostgreSQL/PostGIS — RESUELTO

Ver tabla "Versiones validadas" al inicio de este archivo.

---

### OBS-1: comercios_manzana — RESUELTO_Y_VERSIONADO

Funcion verificada en `sb_malvinas_geo`. DDL real versionado en `functions/comercios_manzana_mvt.sql`.
Firma: `comercios_manzana(z integer, x integer, y integer) RETURNS bytea LANGUAGE sql STABLE PARALLEL SAFE`.
Estado en Martin: publicada como fuente MVT `comercios_manzana`.
Consumidor Angular: capas `comercios-manzana-dot` + `comercios-manzana-label` activas en zoom 15–17
(compañera de `comercios-parcela` que actua en zoom 18+).

---

## Mantenimiento post-ETL — tablas fal_*

### Proceso ETL externo

Las tablas `fal_inmueble`, `fal_comercio` y `fal_comercio_inmueble` son pobladas por un proceso
ETL externo implementado en .NET 9, ubicado en un repositorio separado. Este proceso no forma
parte de `Bod-Faltas`.

El mecanismo de carga es diferencial inteligente: el ETL compara el `row_hash` de cada fila
entrante con el registrado en la tabla y aplica UPSERT solo sobre las filas que cambiaron o
son nuevas. La columna `updated_at` registra el timestamp de la ultima actualizacion de cada fila.
Ambas columnas (`row_hash`, `updated_at`) son activas y forman parte del diseno productivo del loader.

### Acciones requeridas despues de una carga ETL

**No se requiere ninguna accion manual** en PostgreSQL tras la ejecucion del ETL.

Las funciones MVT `comercios_parcela` y `comercios_manzana` son `STABLE PARALLEL SAFE` y se
evaluan en tiempo real para cada request de tiles que Martin recibe. No existen artefactos
derivados de `fal_*` que requieran REFRESH, reindexacion o notificacion.

En resumen:

| Evento | Accion requerida en PostgreSQL |
|--------|-------------------------------|
| ETL actualiza `fal_*` | Ninguna. Martin sirve datos frescos en el siguiente request. |
| Se actualizan tablas geo/catastrales (evento distinto) | REFRESH de las 5 vistas materializadas `v_martin_*` (ver seccion siguiente). |

Las vistas materializadas `v_martin_*` **no dependen** de las tablas `fal_*`; dependen de las
tablas geo/catastrales (`geo_gmat_manzana`, `geo_gmat_localidad`, etc.). Un refresh de `v_martin_*`
despues de una carga ETL de `fal_*` no es necesario ni perjudicial, pero tampoco tiene efecto.

---

## Script disponible: views/v_martin_label_views.sql

Crea las 5 vistas materializadas para las capas de etiquetas de Martin:

- `v_martin_localidad_label`
- `v_martin_manzana_label`
- `v_martin_parcela_label` (incluye logica de nomenclatura_label sin Q0/F0)
- `v_martin_espacio_verde_label`
- `v_martin_ferrocarril_estaciones_label`

Precondicion: tablas base con SRID 22195 (ver postgis-local-setup.md §10.2).

Refresh manual:
```sql
REFRESH MATERIALIZED VIEW CONCURRENTLY v_martin_localidad_label;
REFRESH MATERIALIZED VIEW CONCURRENTLY v_martin_manzana_label;
REFRESH MATERIALIZED VIEW CONCURRENTLY v_martin_parcela_label;
REFRESH MATERIALIZED VIEW CONCURRENTLY v_martin_espacio_verde_label;
REFRESH MATERIALIZED VIEW CONCURRENTLY v_martin_ferrocarril_estaciones_label;
```

---

## Estructura de directorios

```
db/postgis/
  README.md                          este archivo
  views/
    v_martin_label_views.sql         MVs de etiquetas para Martin (disponible)
  functions/
    geo_buscar_calles_tolerante.sql  autocomplete tolerante (RESUELTO HB-3)
    comercios_parcela_mvt.sql        funcion MVT para Martin (RESUELTO HB-1)
    comercios_manzana_mvt.sql        funcion MVT manzana-level para Martin (RESUELTO OBS-1)
  tables/
    fal_inmueble.sql                 tabla de inmuebles en PostGIS (RESUELTO HB-2)
    fal_comercio.sql                 tabla de comercios en PostGIS (RESUELTO HB-2)
    fal_comercio_inmueble.sql        relacion inmueble-comercio (RESUELTO HB-2)
```
