# Catalogo de Capas y Fuentes MapLibre

> **Estado documental:** NORMATIVE
> **Alcance:** todas las fuentes y capas del mapa Malvinas Argentinas.
> Ante contradiccion sobre nombres de capas, fuentes, source-layers o configuracion de glyphs, este documento prevalece.

---

## 1. Regla de glyphs  [GEO-MAP-001]

El campo `glyphs` del style JSON se sobreescribe en runtime (en `MalvinasMapComponent.initMap()`):

```
style.glyphs = MARTIN_BASE_URL + /font/{fontstack}/{range}
```

Reglas obligatorias:
- El endpoint NO lleva extension `.pbf`.
- Martin sirve exactamente **un font por request**. No soporta stacks combinados como `Open Sans Regular,Arial Unicode MS Regular`.
- Todas las capas con texto deben declarar `text-font` como array de **un unico nombre**.
- Las capas del style JSON que tengan `text-font` con mas de un elemento se truncan a `[text-font[0]]` en el mismo init.
- Las capas dinamicas agregadas en runtime (ej. `comercios-parcela-label`) **deben** declarar `text-font` explicitamente.
  Si no lo hacen, MapLibre usa su fallback combinado interno y Martin devuelve 404.

---

## 2. Fuentes de datos

### 2.1 Fuentes Martin (vector tiles)

Todas las fuentes del style JSON son de tipo `vector` y apuntan a endpoints TileJSON de Martin.
En el style, la clave `url` contiene el placeholder `__MARTIN_BASE_URL__/{objeto}`;
el componente `MalvinasMapComponent.initMap()` reemplaza ese placeholder con el valor
runtime de `GEO_CONSTANTS.MARTIN_BASE_URL` antes de pasar el style a MapLibre.

#### Datos catastrales primarios

| source id | source-layer | Objeto PostGIS | Descripcion |
|---|---|---|---|
| `martin-localidad` | `geo_gmat_localidad` | tabla `geo_gmat_localidad` | Poligonos de localidades |
| `martin-barrio` | `geo_gmat_barrio` | tabla `geo_gmat_barrio` | Poligonos de barrios |
| `martin-manzana` | `geo_gmat_manzana` | tabla `geo_gmat_manzana` | Poligonos de manzanas |
| `martin-parcela` | `geo_qmat_parcela` | tabla `geo_qmat_parcela` | Poligonos de parcelas |
| `martin-calle` | `geo_gmat_calle` | tabla `geo_gmat_calle` | Tramos de calles |

#### Capas decorativas (migradas desde GeoJSON estatico en v0.7.0)

Estas fuentes existian anteriormente como archivos `.e1.geojson` en `public/assets/geo/`.
En la version 0.7.0 se migraron a tablas PostGIS con SRID 22195 y se publican via Martin MVT.
Los assets GeoJSON originales fueron eliminados del repositorio.

| source id | source-layer | Objeto PostGIS | Descripcion |
|---|---|---|---|
| `e1-entorno-muni` | `geo_gmat_entorno_muni` | tabla `geo_gmat_entorno_muni` | Limite del municipio |
| `e1-espacio-verde` | `geo_gmat_espacio_verde` | tabla `geo_gmat_espacio_verde` | Poligonos de espacios verdes |
| `e1-ferrocarril` | `geo_gmat_ferrocarril` | tabla `geo_gmat_ferrocarril` | Trazado ferroviario |

#### Vistas materializadas de etiquetas (migradas desde GeoJSON estatico en v0.7.0)

DDL en `db/postgis/views/v_martin_label_views.sql`.

| source id | source-layer | Objeto PostGIS | Descripcion |
|---|---|---|---|
| `e1-ferrocarril-estaciones` | `v_martin_ferrocarril_estaciones_label` | MV `v_martin_ferrocarril_estaciones_label` | Estaciones de ferrocarril |
| `e1-localidad-label` | `v_martin_localidad_label` | MV `v_martin_localidad_label` | Puntos de etiqueta de localidades |
| `e1-manzana-label` | `v_martin_manzana_label` | MV `v_martin_manzana_label` | Puntos de etiqueta de manzanas |
| `e1-parcela-label` | `v_martin_parcela_label` | MV `v_martin_parcela_label` | Puntos de etiqueta de parcelas |
| `e1-espacio-verde-label` | `v_martin_espacio_verde_label` | MV `v_martin_espacio_verde_label` | Puntos de etiqueta de espacios verdes |

### 2.2 Fuentes runtime (agregadas por MalvinasMapComponent)

Estas fuentes no estan en el style JSON; el componente las crea despues del evento `load`.

| source id | Tipo | Descripcion |
|---|---|---|
| `selected-parcela` | GeoJSON vacio inicial | Geometria completa de la parcela seleccionada (obtenida del backend, no del tile) |
| `comercios-parcela` | vector Martin | Comercios por parcela (funcion MVT `comercios_parcela`); minzoom 18 — DDL en `db/postgis/functions/comercios_parcela_mvt.sql` |
| `comercios-manzana` | vector Martin | Comercios agregados por manzana (funcion MVT `comercios_manzana`); zoom 15–17 — DDL en `db/postgis/functions/comercios_manzana_mvt.sql` |

---

## 3. Catalogo de capas

### 3.1 Capas del style JSON (`malvinas-claro.style.json`)

| id capa | tipo | fuente | source-layer | font |
|---|---|---|---|---|
| `background` | background | - | - | - |
| `entorno-muni-line` | line | e1-entorno-muni | geo_gmat_entorno_muni | - |
| `locality-fill` | fill | martin-localidad | geo_gmat_localidad | - |
| `locality-outline` | line | martin-localidad | geo_gmat_localidad | - |
| `espacio-verde-fill` | fill | e1-espacio-verde | geo_gmat_espacio_verde | - |
| `espacio-verde-outline` | line | e1-espacio-verde | geo_gmat_espacio_verde | - |
| `barrio-fill` | fill | martin-barrio | geo_gmat_barrio | - |
| `block-fill` | fill | martin-manzana | geo_gmat_manzana | - |
| `block-outline` | line | martin-manzana | geo_gmat_manzana | - |
| `parcel-fill` | fill | martin-parcela | geo_qmat_parcela | - |
| `parcel-outline` | line | martin-parcela | geo_qmat_parcela | - |
| `parcel-selected-fill` | fill | martin-parcela | geo_qmat_parcela | - |
| `parcel-selected-outline` | line | martin-parcela | geo_qmat_parcela | - |
| `ferrocarril-casing` | line | e1-ferrocarril | geo_gmat_ferrocarril | - |
| `ferrocarril-fill` | line | e1-ferrocarril | geo_gmat_ferrocarril | - |
| `ferrocarril-ticks` | line | e1-ferrocarril | geo_gmat_ferrocarril | - |
| `street-casing-primary` | line | martin-calle | geo_gmat_calle | - |
| `street-fill-primary` | line | martin-calle | geo_gmat_calle | - |
| `street-casing-secondary` | line | martin-calle | geo_gmat_calle | - |
| `street-fill-secondary` | line | martin-calle | geo_gmat_calle | - |
| `street-casing-local` | line | martin-calle | geo_gmat_calle | - |
| `street-fill-local` | line | martin-calle | geo_gmat_calle | - |
| `road-unknown-casing` | line | martin-calle | geo_gmat_calle | - |
| `road-unknown-fill` | line | martin-calle | geo_gmat_calle | - |
| `street-fill-pedestrian` | line | martin-calle | geo_gmat_calle | - |
| `ferrocarril-estacion-circle` | circle | e1-ferrocarril-estaciones | v_martin_ferrocarril_estaciones_label | - |
| `street-label-primary` | symbol | martin-calle | geo_gmat_calle | Open Sans Semibold |
| `street-label-secondary` | symbol | martin-calle | geo_gmat_calle | Open Sans Semibold |
| `street-label-local` | symbol | martin-calle | geo_gmat_calle | Open Sans Semibold |
| `road-unknown-label` | symbol | martin-calle | geo_gmat_calle | Open Sans Semibold |
| `street-label-passage` | symbol | martin-calle | geo_gmat_calle | Open Sans Regular |
| `barrio-outline` | line | martin-barrio | geo_gmat_barrio | - |
| `barrio-label` | symbol | martin-barrio | geo_gmat_barrio | Open Sans Semibold |
| `block-label` | symbol | e1-manzana-label | v_martin_manzana_label | Open Sans Semibold |
| `parcel-label-short` | symbol | e1-parcela-label | v_martin_parcela_label | Open Sans Semibold |
| `parcel-label-full` | symbol | e1-parcela-label | v_martin_parcela_label | Open Sans Regular |
| `locality-label` | symbol | e1-localidad-label | v_martin_localidad_label | Open Sans Semibold |
| `espacio-verde-label` | symbol | e1-espacio-verde-label | v_martin_espacio_verde_label | Open Sans Regular |
| `ferrocarril-estacion-label` | symbol | e1-ferrocarril-estaciones | v_martin_ferrocarril_estaciones_label | Open Sans Semibold |

### 3.2 Capas runtime agregadas por MalvinasMapComponent

| id capa | tipo | fuente | minzoom | font | Descripcion |
|---|---|---|---|---|---|
| `parcela-highlight-fill` | fill | selected-parcela | - | - | Relleno azul parcela seleccionada |
| `parcela-highlight-outline` | line | selected-parcela | - | - | Borde azul parcela seleccionada |
| `comercios-parcela-dot` | symbol | comercios-parcela | 18 | - | Icono del comercio en la parcela |
| `comercios-parcela-label` | symbol | comercios-parcela | 18 | Open Sans Regular | Etiqueta del comercio |
| `comercios-manzana-dot` | symbol | comercios-manzana | 15–17 | - | Icono agregado por manzana |
| `comercios-manzana-label` | symbol | comercios-manzana | 15–17 | Open Sans Regular | Etiqueta con lista o conteo de comercios de la manzana |

---

## 4. Prioridad de etiquetas  [GEO-MAP-002]

La etiqueta de calle debe tener **siempre** prioridad sobre la etiqueta de comercio:

- `comercios-parcela-label`: `symbol-sort-key: 10` (mayor = menor prioridad), `text-ignore-placement: true` (no bloquea otras etiquetas), `text-optional: true` (puede ocultarse).
- `comercios-manzana-label`: `symbol-sort-key: 11`, mismas reglas que `comercios-parcela-label`. Actua en zoom 15–17; en zoom 18+ la capa parcela toma el relevo.
- Capas de etiquetas de calle: `symbol-sort-key` menor (mayor prioridad).

---

## 5. Parcela seleccionada  [GEO-MAP-003]

Cuando el usuario hace click en `parcel-fill`:

1. Se obtiene el `ParcelaRef` desde las propiedades del feature (circunsc, secc, quint, fracc, manz, parc).
2. Se llama a `GET /api/geo/local/malvinas/parcela/geom` para obtener la **geometria completa** de PostGIS.
3. Se actualiza el source `selected-parcela` con el GeoJSON recibido.
4. Las capas `parcela-highlight-fill` y `parcela-highlight-outline` muestran el highlight.

**Razon**: los tiles MVT clipean la geometria en los bordes del tile. Usar la geometria del feature clickeado
como highlight resultaria en un poligono parcial. La consulta al backend devuelve la geometria completa sin clipeo.

**Fallback**: si el endpoint falla, se usa la geometria clipada del tile como ultimo recurso.

---

## 6. Trazabilidad migracion GeoJSON a Martin (v0.7.0)

| Asset anterior eliminado | Source id actual | Source-layer PostGIS | Estado |
|---|---|---|---|
| `malvinas-entorno-muni.e1.geojson` | `e1-entorno-muni` | `geo_gmat_entorno_muni` | MIGRADO_Y_CONSUMIDO |
| `malvinas-entorno-muni-lines.e1.geojson` | *(eliminado, sin consumidor)* | - | REFERENCIA_DOCUMENTAL_OBSOLETA |
| `malvinas-espacio-verde.e1.geojson` | `e1-espacio-verde` | `geo_gmat_espacio_verde` | MIGRADO_Y_CONSUMIDO |
| `malvinas-espacio-verde-label-points.e1.geojson` | `e1-espacio-verde-label` | `v_martin_espacio_verde_label` | MIGRADO_Y_CONSUMIDO |
| `malvinas-ferrocarril.e1.geojson` | `e1-ferrocarril` | `geo_gmat_ferrocarril` | MIGRADO_Y_CONSUMIDO |
| `malvinas-ferrocarril-estaciones.e1.geojson` | `e1-ferrocarril-estaciones` | `v_martin_ferrocarril_estaciones_label` | MIGRADO_Y_CONSUMIDO |
| `malvinas-localidad-label-points.e1.geojson` | `e1-localidad-label` | `v_martin_localidad_label` | MIGRADO_Y_CONSUMIDO |
| `malvinas-manzana-label-points.e1.geojson` | `e1-manzana-label` | `v_martin_manzana_label` | MIGRADO_Y_CONSUMIDO |
| `malvinas-parcela-label-points.e1.geojson` | `e1-parcela-label` | `v_martin_parcela_label` | MIGRADO_Y_CONSUMIDO |

> La fuente `malvinas-entorno-muni-lines.e1.geojson` y su source id `e1-entorno-muni-lines`
> no tienen ninguna capa consumidora en el style actual. Fueron eliminados sin reemplazo.
> El borde del municipio se sirve via `e1-entorno-muni` (poligono completo).
