# PostgreSQL/PostGIS - Instalacion y Entorno Local

> **Estado documental:** SUPPORTING_CURRENT
> **Guardrail**: este documento no incluye contrasenas, usuarios de base de datos,
> ni ningun secreto. Las credenciales se gestionan exclusivamente a traves del
> archivo local de configuracion no versionado (`geodomicilios.local.ps1`).

---

## 1. Requisitos

| Componente | Version minima | Version validada localmente |
|---|---|---|
| PostgreSQL | 14 | 15.2 (Visual C++ build 1914, 64-bit) |
| PostGIS | 3.x | 3.3 (USE_GEOS=1 USE_PROJ=1 USE_STATS=1) |
| Sistema operativo | Windows 10/11, Windows Server 2019+, o Linux |
| Espacio en disco | minimo 2 GB para la base de datos con datos geograficos |

---

## 2. Instalacion de PostgreSQL con PostGIS

### En Windows

1. Descargar el instalador de PostgreSQL desde: https://www.postgresql.org/download/windows/
2. Ejecutar el instalador. Seleccionar todos los componentes, incluido StackBuilder.
3. Al finalizar, StackBuilder permite instalar la extension PostGIS.

Alternativa con EnterpriseDB: https://www.enterprisedb.com/downloads/postgres-postgresql-downloads

### En Linux (Ubuntu/Debian)

`ash
sudo apt-get install postgresql postgresql-contrib postgis postgresql-14-postgis-3
`

---

## 3. Crear la base de datos para GeoDomicilios

Conectarse a PostgreSQL como superusuario y ejecutar:

`sql
CREATE DATABASE sb_malvinas_geo;
\c sb_malvinas_geo
CREATE EXTENSION IF NOT EXISTS postgis;
SELECT PostGIS_version();
`

El nombre de la base (`sb_malvinas_geo`) es el nombre de referencia para este proyecto.

---

## 4. Restauracion desde backup

Se provee un backup de la base de datos con los datos geograficos del partido.
El backup incluye:

- Tablas de geometrias: `geo_qmat_parcela`, `geo_gmat_manzana`, `geo_gmat_calle`, `geo_gmat_barrio`, `geo_gmat_localidad`.
- Tablas de Faltas: `fal_inmueble`, `fal_comercio`, `fal_comercio_inmueble`.
- Tabla de lookup de calles: `geo_calle_nombre_lookup`.
- Vista materializada: `geo_calle_altura_ref`.

### Procedimiento de restore

**Para formato custom (.dump)**:

`ash
pg_restore -d sb_malvinas_geo -Fc -v backup.dump
`

**Para formato SQL plano (.sql)**:

`ash
psql -d sb_malvinas_geo -f backup.sql
`

Desde PowerShell (cargando el entorno local primero):

`powershell
. 'S:\TEMP\Bod-Faltas\Config\geodomicilios.local.ps1'
# La conexion usa las variables PGHOST, PGPORT, PGUSER, PGDATABASE del entorno local
pg_restore -h $env:PGHOST -p $env:PGPORT -U $env:PGUSER -d $env:PGDATABASE -Fc -v backup.dump
`

---

## 5. Objetos geo requeridos

Ademas de las tablas del backup, la feature GeoDomicilios requiere estos objetos
que pueden no estar en el backup o que deben recrearse:

### 5.1 Vista materializada geo_calle_altura_ref

Necesaria para geocodificacion de precision por inmuebles.
DDL completo en `30-geocoding/geocoding-contracts.md`, seccion 4.

Crear antes de iniciar el backend Spring Boot.
Refrescar despues de actualizaciones masivas con:

`sql
REFRESH MATERIALIZED VIEW geo_calle_altura_ref;
`

### 5.2 Funcion MVT comercios_parcela

Necesaria para que Martin sirva el tile de comercios.
Firma requerida: `comercios_parcela(z int, x int, y int) RETURNS bytea`.
Crear antes de iniciar Martin.

> **GAP HB-1**: el DDL de esta funcion no esta versionado en el repositorio.
> No existe en este documento ni en `martin-setup.md` (la referencia circular ha sido eliminada).
> Cuando se obtenga el cuerpo de la funcion, versionarlo en:
> `db/postgis/functions/comercios_parcela_mvt.sql`

### 5.3 Funcion de busqueda tolerante de calles

La funcion `geo_buscar_calles_tolerante(query text, id_loc text, max_results int) RETURNS TABLE`
es necesaria para el autocomplete de calles en el backend Java.
Usada por `GeoRepository.findCalles()`.

> **GAP HB-3**: el DDL de esta funcion no esta versionado en el repositorio.
> Cuando se obtenga, versionarlo en:
> `db/postgis/functions/geo_buscar_calles_tolerante.sql`

### 5.4 Tablas de inmuebles y comercios

Las tablas `fal_inmueble`, `fal_comercio` y `fal_comercio_inmueble` residen en PostGIS
(`sb_malvinas_geo`), no en MariaDB. Son necesarias para `GET /api/geo/local/malvinas/parcela/detalle`.

> **GAP HB-2**: el DDL de estas tablas no esta versionado en el repositorio.
> Cuando se obtenga el DDL, versionarlo en:
> `db/postgis/tables/fal_inmueble.sql`, `db/postgis/tables/fal_comercio.sql`,
> `db/postgis/tables/fal_comercio_inmueble.sql`

### 5.5 Extension PostGIS y SRID 22195

`sql
SELECT PostGIS_version();
SELECT SRID, proj4text FROM spatial_ref_sys WHERE SRID = 22195;
`

El SRID 22195 (Argentina Faja 5) debe estar presente para las transformaciones de coordenadas.

---

## 6. Tabla de lookup de calles

Si no esta en el backup, crearla con:

`sql
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE IF NOT EXISTS geo_calle_nombre_lookup (
    ccalle      VARCHAR(20),
    id_loc      CHAR(2),
    nombre_norm VARCHAR(200),
    nombre_orig VARCHAR(200),
    soundex_val CHAR(4),
    PRIMARY KEY (ccalle, id_loc)
);

INSERT INTO geo_calle_nombre_lookup
SELECT DISTINCT ccalle, id_loc, unaccent(nombre), nombre, soundex(nombre)
FROM geo_gmat_calle
WHERE nombre IS NOT NULL;
`

---

## 7. Indices recomendados

`sql
CREATE INDEX IF NOT EXISTS idx_parcela_geom    ON geo_qmat_parcela USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_calle_geom      ON geo_gmat_calle   USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_manzana_geom    ON geo_gmat_manzana USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_parcela_nom     ON geo_qmat_parcela (circunsc, secc, manz, parc);
CREATE INDEX IF NOT EXISTS idx_calle_ccalle    ON geo_gmat_calle (ccalle, id_loc);
CREATE INDEX IF NOT EXISTS idx_inmueble_cat    ON fal_inmueble (circunscripcion, seccion, manzana, parcela);
`

---

## 8. Levantar el entorno de desarrollo completo

`powershell
# 1. Cargar el archivo de configuracion local
. 'S:\TEMP\Bod-Faltas\Config\geodomicilios.local.ps1'

# 2. Iniciar el backend Spring Boot
# cd backend/api-faltas-core
# mvn spring-boot:run

# 3. Iniciar el frontend Angular
# cd apps/web-geodomicilios/angular
# ng serve --poll=2000
# --poll=2000 necesario en unidades de red para que Vite detecte cambios

# 4. Iniciar Martin (ver martin-setup.md)
# . geodomicilios.local.ps1
# & C:\tools\martin\start-martin.ps1
`

Una vez levantado el entorno, abrir http://localhost:4200 en el navegador.

---

## 9. Verificacion del entorno

`sql
SELECT count(*) FROM geo_qmat_parcela;     -- debe tener registros
SELECT count(*) FROM geo_gmat_calle;       -- debe tener registros
SELECT count(*) FROM geo_calle_altura_ref; -- debe tener registros
SELECT proname FROM pg_proc WHERE proname = 'comercios_parcela';  -- debe devolver la funcion
`

Desde el frontend, verificar:
- El mapa carga y muestra parcelas al zoom 17+.
- El buscador devuelve sugerencias al escribir una calle.
- Al hacer click en una parcela se muestra el panel de detalle.
- La capa de comercios aparece a zoom 18+.

---

## 10. Migracion GeoJSON a PostGIS/Martin (completada en v0.7.0)

### 10.1 Contexto (migracion completada)

En la version 0.7.0 las 8 capas GeoJSON estaticas fueron migradas a fuentes MVT servidas
por Martin. Los assets `.e1.geojson` fueron eliminados del repositorio.
Las capas de etiquetas (label points) y capas decorativas (ferrocarril, espacio verde,
entorno municipal) se sirven desde PostGIS via Martin.
La trazabilidad completa de cada capa migrada se documenta en
`20-map/layer-catalog.md`, seccion 6.

### 10.2 Correccion de SRID en tablas base

Las 4 tablas que tenian SRID=0 fueron corregidas a SRID=22195 (Argentina Faja 5).
Martin no indexa tablas con SRID=0.

Tablas corregidas:
- `geo_gmat_entorno_muni` (MultiLineString, 22195)
- `geo_gmat_espacio_verde` (MultiPolygon, 22195)
- `geo_gmat_ferrocarril` (MultiLineString, 22195)
- `geo_gmat_ferrocarril_estaciones` (MultiPolygon, 22195)

### 10.3 Vistas materializadas

El DDL completo esta en `db/postgis/views/v_martin_label_views.sql`.

| Vista | Tipo | Fuente | Columnas clave |
|---|---|---|---|
| `v_martin_localidad_label` | POINT | `geo_gmat_localidad` | `id`, `localidad` |
| `v_martin_manzana_label` | POINT | `geo_gmat_manzana` | `id`, `lblmza` |
| `v_martin_parcela_label` | POINT | `geo_qmat_parcela` | `id`, `parc`, `manz`, `nomenclatura_label` |
| `v_martin_espacio_verde_label` | POINT | `geo_gmat_espacio_verde` | `id`, `nombre` |
| `v_martin_ferrocarril_estaciones_label` | POINT | `geo_gmat_ferrocarril_estaciones` | `id`, `estacion` |

Cada vista tiene:
- Indice unico en `id` (requerido por Martin para servir MVT)
- Indice GIST en `geom` (para eficiencia de tiles espaciales)

### 10.4 Refresh de datos

Cuando las tablas base se actualicen desde el sistema GIS fuente, refrescar las vistas:

```sql
REFRESH MATERIALIZED VIEW CONCURRENTLY v_martin_localidad_label;
REFRESH MATERIALIZED VIEW CONCURRENTLY v_martin_manzana_label;
REFRESH MATERIALIZED VIEW CONCURRENTLY v_martin_parcela_label;
REFRESH MATERIALIZED VIEW CONCURRENTLY v_martin_espacio_verde_label;
REFRESH MATERIALIZED VIEW CONCURRENTLY v_martin_ferrocarril_estaciones_label;
```

Martin no requiere reinicio para reflejar cambios de datos en vistas existentes.
Solo reiniciar Martin al agregar nuevas tablas o vistas.

### 10.5 Reinicio de Martin

Martin obtiene la conexion a PostgreSQL desde la variable de entorno `DATABASE_URL`
(ver `martin-setup.md` seccion 4 para el mecanismo canonico de configuracion).

Para reiniciar Martin cuando sea necesario, usar el mecanismo de gestion del servidor
donde esta instalado. La IP y el procedimiento de acceso remoto son configuracion local
no versionada; no deben incluirse en este documento.

Ver `martin-setup.md` para el script de inicio (`start-martin.ps1`) y el procedimiento
de verificacion post-reinicio.

### 10.6 Verificacion post-migracion

```sql
-- Verificar SRID correcto
SELECT f_table_name, type, srid FROM geometry_columns
WHERE f_table_name IN (
  'geo_gmat_entorno_muni','geo_gmat_espacio_verde','geo_gmat_ferrocarril',
  'geo_gmat_ferrocarril_estaciones','v_martin_localidad_label',
  'v_martin_manzana_label','v_martin_parcela_label',
  'v_martin_espacio_verde_label','v_martin_ferrocarril_estaciones_label'
)
ORDER BY f_table_name;

-- Verificar poblacion de vistas
SELECT matviewname, ispopulated FROM pg_matviews
WHERE matviewname LIKE 'v_martin_%'
ORDER BY matviewname;
```

Desde el catalogo Martin (sin secretos):

```powershell
. 'S:\TEMP\Bod-Faltas\Config\geodomicilios.local.ps1'
$catalog = (Invoke-WebRequest -Uri "$env:MARTIN_BASE_URL/catalog" -UseBasicParsing).Content | ConvertFrom-Json
$catalog.tiles.PSObject.Properties.Name | Sort-Object
```

Se deben ver: `v_martin_localidad_label`, `v_martin_manzana_label`, `v_martin_parcela_label`,
`v_martin_espacio_verde_label`, `v_martin_ferrocarril_estaciones_label`,
`geo_gmat_entorno_muni`, `geo_gmat_espacio_verde`, `geo_gmat_ferrocarril`.
