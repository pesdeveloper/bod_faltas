# GeoDomicilios — Arquitectura y modelo canónico

> **Estado documental:** PRE_DDL_PLAN
> **Autoridad DDL:** SUPPORTING

Decisiones arquitectónicas, de nomenclatura y de modelado del subsistema de
georreferenciación de domicilios en BOD_Faltas. Este documento no contiene SQL
productivo, DDL ejecutable, implementación Java ni endpoints.

El trabajo de implementación (Java, DDL, HTTP, comandos, snapshot, guardrails y
tests) se registra en
[`../90-roadmap/current-roadmap.md`](../90-roadmap/current-roadmap.md).

---

## 1. Arquitectura

### 1.1 Responsabilidad por fuente de datos

| Fuente | Responsabilidad |
|---|---|
| MariaDB sandbox | Catálogos IGN/INDEC/BAHRA y catálogos locales versionados; rangos de altura auxiliares (`geo_calle_alturas_barrio`) |
| PostgreSQL/PostGIS sandbox | Autoridad espacial: parcelas, calles, manzanas, localidad, nomenclatura, inmuebles, comercios, titulares, relaciones, geocodificación calle-altura y reverse local |
| Martin | Servidor de teselas vectoriales MVT; publica capas directamente desde views/tablas PostGIS |
| MapLibre GL JS | Visor principal; consume las capas MVT de Martin |

Reglas de autoridad:

- La resolución de `id_loc` es exclusivamente espacial en PostGIS.
- Martin se ocupa exclusivamente de visualización de capas; no contiene reglas de negocio.
- El backend Spring (no Martin) es responsable de: comandos y validación funcional; punto → localidad; punto → parcela; búsqueda de inmueble/comercio; geocodificación calle-altura; reverse local; aplicación final al formulario.

### 1.2 Decisiones cerradas

- Backend dentro de BOD_Faltas: Spring Boot + JDBC, sin JPA/Hibernate.
- Sin persistencia GeoDomicilios en memoria (InMemory Geo): el módulo usa exclusivamente bases de datos reales.
- Angular 20 como prototipo de GeoDomicilios: independiente de la demo Angular 19.2 histórica.
- La demo Angular 19.2 histórica está fuera del alcance de este subsistema.
- Android Kotlin: fase posterior. Visor nativo: **MapLibre Native para Android** (MapLibre GL Native); sin Google Maps SDK ni Places SDK en el baseline.
- **MapLibre GL JS como visor principal** (pivot GEO-SPEC-002): sustituye a Google Maps JavaScript API como visor de capas.
- **Martin como servidor de capas MVT**: las views/tablas PostGIS son la fuente; Martin las publica como TileJSON/MVT.
- **Google Maps JavaScript API**: no es el visor principal.
- **Google Places / Geocoding**: no se implementan en el baseline. Solo podrían reconsiderarse si el geocodificador municipal no alcanza el gate de calidad (§ 14).
- **Google reverse geocoding**: no es el mecanismo estándar; se sustituye por reverse local mediante PostGIS (§ 17).
- **GeoJSON como transporte principal de capas**: eliminado como arquitectura principal; sustituido por MVT.
- **Identidad territorial de la calle**: se define por `ccalle`/`id_tca` + `id_loc`; nunca por código o nombre solos (ver §1.4).
- **Geocodificador en el backend**: el backend PostGIS ejecuta geocodificación calle-altura, tramo/lado, calidad, reverse y validación territorial; MapLibre solo visualiza capas y muestra candidatos (ver §12).
- **Etiqueta canónica centralizada**: `etiqueta_canonica` se construye en la vista fuente (`gis_parcela_web`); MapLibre la consume; no la construye en el cliente (ver §6.6).

### 1.3 Stack de visualización de capas

```text
PostgreSQL/PostGIS
  → views/tablas fuente
  → Martin
  → TileJSON/MVT
  → MapLibre GL JS
```

El backend Spring (JDBC) no produce GeoJSON de capas como flujo principal. Las capas vectoriales se sirven directamente desde PostGIS a través de Martin. El backend sigue siendo la autoridad para resolución espacial, geocodificación y aplicación al formulario.

El criterio para prescindir de Google es control territorial, rendimiento, uso de datos municipales y evitar resultados lejanos — no solo el costo.

### 1.4 Identidad territorial de la calle

La identidad funcional de una calle local **no se define únicamente por `ccalle`/`id_tca`**.
Debe resolverse mediante:

```text
ccalle/id_tca + id_loc
```

Reglas canónicas del autocomplete de calles:

- Toda opción del autocomplete debe mostrar la localidad junto al nombre de la calle.
- Calles homónimas en distintas localidades se presentan como opciones separadas.
- El usuario debe seleccionar la opción correspondiente a la localidad indicada.
- La selección conserva conjuntamente: `id_tca`/`ccalle`, `id_loc`, nombre de calle, nombre de localidad.
- Toda búsqueda o geocodificación por altura usa: `id_tca`/`ccalle` + `id_loc` + altura.
- Nunca se resuelve automáticamente una calle homónima usando solo código o nombre.
- Si la localidad ya está seleccionada, priorizar sus resultados; permitir que el usuario vea coincidencias de otras localidades para corregir una selección territorial equivocada.
- La localidad se muestra incluso cuando hay una sola coincidencia.

Ejemplos de presentación en el autocomplete:

```text
Dardo Rocha — Los Polvorines
Dardo Rocha — Grand Bourg
Dardo Rocha — Tierras Altas
```

Cuando corresponda, puede agregarse barrio o nombre histórico como información secundaria:

```text
Presidente Perón — Los Polvorines · ex José C. Paz
Dardo Rocha — Grand Bourg · Barrio El Sol
```

Contrato conceptual de selección:

```json
{
  "idTca": "02211",
  "idLoc": "LP",
  "calle": "DARDO ROCHA",
  "localidad": "LOS POLVORINES",
  "texto": "Dardo Rocha — Los Polvorines"
}
```

Esta decisión aplica en: autocomplete de calles, geocodificador calle-altura, reverse local, validación holdout, roadmap de backend, Angular y Android.

---

## 2. Nomenclatura catastral

### 2.1 Tipos canónicos

Los campos de nomenclatura catastral en `fal_acta_contravencion` quedan definidos con los
siguientes tipos:

| Campo    | Tipo         | Notas |
|----------|--------------|-------|
| `circ`   | `SMALLINT`   | Circunscripción catastral |
| `secc`   | `VARCHAR(7)` | Sección catastral |
| `quinta` | `VARCHAR(7)` | Quinta catastral; origen: `geo_qmat_parcela.quint` o `fal_inmueble.quinta` |
| `frac`   | `VARCHAR(7)` | Fracción catastral |
| `mza`    | `VARCHAR(7)` | Manzana catastral |
| `parc`   | `VARCHAR(7)` | Parcela catastral |
| `ufun`   | `VARCHAR(7)` | Unidad funcional catastral |
| `ucomp`  | `VARCHAR(3)` | Unidad complementaria catastral |

### 2.2 Reglas

- `quinta` proviene exclusivamente de `geo_qmat_parcela.quint` o de `fal_inmueble.quinta`.
- `chacra` queda excluido de todo el modelo: no existe como campo en `fal_acta_contravencion`,
  no se crea tabla propia ni atributo derivado con ese nombre.
- La etiqueta parcelaria compacta se construye con `circ`, `secc`, `quinta`, `frac`, `mza`, `parc`.
  Formato de referencia: `4-H-Q2-F1-M105B-P12`. Los segmentos `quinta` y `frac` se omiten
  cuando su valor es `null` o `0` (ver sección 2.4).
- `ufun` y `ucomp` no se incluyen en la etiqueta parcelaria general; se aplican al seleccionar
  un inmueble específico dentro de la parcela.

### 2.3 Delta respecto al modelo Java/DDL actual

| Campo    | Tipo en spec canónica | Estado en Java/DDL actual | Cambio requerido |
|----------|-----------------------|---------------------------|-----------------|
| `secc`   | `VARCHAR(7)`          | `VARCHAR(2)` (Java max 2; DDL VARCHAR(2)) | Ampliar longitud |
| `quinta` | `VARCHAR(7)`          | ausente                   | Agregar campo nuevo |
| `ucomp`  | `VARCHAR(3)`          | `VARCHAR(20)` (Java max 20; DDL VARCHAR(20)) | Reducir longitud |

El ajuste en Java (validación de longitud máxima) y en DDL (tipo de columna, nuevo campo
`quinta`) se registra como trabajo pendiente en el roadmap.

### 2.4 Valor `0` en quinta y fracción

En las fuentes de Geomática, el valor textual `0` en los campos `quinta` y `frac` representa
ausencia de valor catastral real, no un código catastral válido.

La representación física `0` se conserva deliberadamente en parcelas e inmuebles para
permitir joins directos e indexables sin coste de transformación.

Reglas canónicas:

- NULL, blanco o espacios provenientes de la fuente se normalizan a `0` al almacenar.
- En joins y almacenamiento se conserva `0`.
- En APIs, etiquetas y UI, `0` se interpreta como vacío: el segmento correspondiente se omite.
- Nunca mostrar `Q0` ni `F0` en etiquetas visibles.
- Los segmentos sin valor se omiten de la etiqueta canónica.

| Representación física | Etiqueta visible canónica |
|---|---|
| `4-S-0-0-7C-10` | `4-S-M7C-P10` |

Esta regla aplica en todo contexto de presentación: parcelas, inmuebles, lados de cuadra,
alturas y cualquier diagnóstico o auditoría de fuentes de nomenclatura.

---

## 3. Localidad municipal

### 3.1 Fuente de autoridad

La fuente de autoridad para identificación de localidades del partido de Malvinas Argentinas es
la tabla PostGIS:

```
public.geo_gmat_localidad
```

### 3.2 Códigos operativos

| Código | Localidad |
|--------|-----------|
| `AP`   | AREA DE PROMOCION |
| `GB`   | GRAND BOURG |
| `PN`   | ING. PABLO NOGUES |
| `VM`   | VILLA DE MAYO |
| `LP`   | LOS POLVORINES |
| `AS`   | ING. SOURDEAUX |
| `MA`   | MALVINAS ARGENTINAS |
| `TO`   | TORTUGUITAS |
| `TA`   | TIERRAS ALTAS |

El partido cuenta con ocho localidades principales más el área de promoción.

### 3.3 Reglas de resolución y caché

- El catálogo completo se carga una única vez al inicio del servicio, formando un snapshot inmutable en JVM.
- La caché se indexa por `id_loc` y por nombre normalizado.
- El reemplazo de la caché es atómico.
- La caché se refresca únicamente por reinicio del servicio o por operación administrativa explícita.
- La resolución espacial punto → localidad se ejecuta siempre en PostGIS.
- La caché valida y presenta el código `id_loc` resuelto espacialmente; no consulta `geo_gmat_localidad` por request.
- `id_loc` siempre proviene de GIS. Google Maps/Places no asigna `id_loc`.
- Si PostGIS no resuelve localidad, o retorna un código ausente en la caché, el domicilio local
  no puede aplicarse.

---

## 4. Comercio e inmueble

### 4.1 Invariantes generales

- `id_suj_i` e `id_bie_i`: ambos nulos o ambos informados.
- Si hay comercio registrado o no registrado, el par `id_suj_i`/`id_bie_i` es obligatorio.
- `id_suj_c` e `id_bie_c`: ambos nulos o ambos informados.
- Comercio registrado (`id_suj_c`/`id_bie_c` informados) es incompatible con `si_comercio_no_registrado=true`.
- Comercio registrado exige que los campos manuales de comercio no registrado sean nulos.
- Comercio no registrado exige que `id_suj_c` e `id_bie_c` sean nulos.
- No se guarda nunca un comercio sin inmueble.
- La nomenclatura se conserva siempre, independientemente del tipo de comercio.
- Comercio con una única relación a inmueble puede completar el inmueble automáticamente mostrando el resultado.
- Comercio con varias relaciones exige elección humana.

---

## 5. Comercio no registrado

### 5.1 Campos estructurales en `fal_acta_contravencion`

Los siguientes campos se incorporan como decisión productiva (no como atributos de la demo):

| Campo                             | Tipo                             | Descripción |
|-----------------------------------|----------------------------------|-------------|
| `si_comercio_no_registrado`       | `BOOLEAN NOT NULL DEFAULT FALSE` | Indica que el comercio no está formalmente registrado |
| `comercio_no_reg_nombre_fantasia` | `VARCHAR(64) NULL`               | Nombre de fantasía del comercio no registrado |
| `comercio_no_reg_titular`         | `VARCHAR(64) NULL`               | Titular del comercio no registrado |

No se agrega `comercio_no_reg_observaciones` ni ningún otro campo de texto libre en
`fal_acta_contravencion`.

### 5.2 Observaciones del flujo

Toda nota libre del operador sobre este flujo se persiste en `fal_observacion`
(ver spec completa en [`../40-domain/observaciones.md`](../40-domain/observaciones.md)):

- `EntidadTipoObservada.ACTA` (`entidad_tipo` = código 1).
- `entidad_id` = `acta_id`.
- `id_acta_contexto` = `acta_id`.
- `OrigenObservacion.USUARIO`.
- Texto entre 1 y 512 caracteres.
- Múltiples observaciones permitidas; append-only con baja lógica.
- No se crea un nuevo código de entidad tipo observada para este flujo.
- No se agrega columna embebida en `fal_acta_contravencion`.
- Si el command HTTP recibe una observación opcional, el servicio crea la `FalObservacion`
  separada dentro de la misma transacción funcional.

### 5.3 Reglas de negocio

- `si_comercio_no_registrado = true` → `id_suj_c` e `id_bie_c` deben ser `null`.
- `si_comercio_no_registrado = true` → `id_suj_i` e `id_bie_i` son obligatorios.
- `si_comercio_no_registrado = true` → nomenclatura catastral obligatoria.
- `si_comercio_no_registrado = true` → `comercio_no_reg_nombre_fantasia` obligatorio.
- `si_comercio_no_registrado = true` → `comercio_no_reg_titular` opcional.
- `si_comercio_no_registrado = false` → `comercio_no_reg_nombre_fantasia` y
  `comercio_no_reg_titular` deben ser `null`.
- Al cambiar de modo (registrado ↔ no registrado), los datos incompatibles se limpian.
- No se inventan IDs de comercio para comercios no registrados.

---

## 6. Parcelas y visualización

### 6.1 Vista `gis_parcela_web` — campos mínimos y fuente real

La vista de parcelas para el mapa interactivo se denomina `gis_parcela_web`. Fuente:
`public.geo_qmat_parcela` (79.169 filas en sandbox; SRID 22195 — POSGAR 94 / Argentina 5).

Campos expuestos con sus alias canónicos:

| Alias (vista) | Columna fuente | Tipo fuente | Notas |
|---|---|---|---|
| `parcela_id` | `id` | `INTEGER` | PK estable |
| `geom_22195` | `geom` | `geometry(MultiPolygon,22195)` | Geometría nativa; permite filtrar con el índice GiST |
| `geom_4326` | `geom` (transformado) | `geometry(MultiPolygon,4326)` | `ST_Transform(geom,4326)::geometry(MultiPolygon,4326)` |
| `label_point_4326` | `geom` (transformado) | `geometry(Point,4326)` | `ST_Transform(ST_PointOnSurface(geom),4326)::geometry(Point,4326)`; no centroide |
| `circ` | `circunsc` | `VARCHAR(254)` | Max 1 char; cast a SMALLINT posible |
| `secc` | `secc` | `VARCHAR(254)` | Max 1 char |
| `quinta` | `quint` | `VARCHAR(254)` | Max 2 chars; `0` indica ausencia de quinta; se omite en etiqueta visible (§ 2.4) |
| `frac` | `fracc` | `VARCHAR(254)` | Max 2 chars; `0` indica ausencia de fracción; se omite en etiqueta visible (§ 2.4) |
| `mza` | `manz` | `VARCHAR(254)` | Max 4 chars |
| `parc` | `parc` | `VARCHAR(254)` | Max 6 chars |
| `id_loc` | join texto a `geo_gmat_localidad` | `CHAR(2)` | 9 filas; join por nombre normalizado |
| `localidad` | `localidad` | `VARCHAR(254)` | Pre-poblado; 9 valores distintos |
| `barrio` | `barrio` | `VARCHAR(254)` | Pre-poblado; 93 valores distintos |
| `etiqueta_canonica` | construida desde `circ`, `secc`, `quinta`, `frac`, `mza`, `parc` | `TEXT` | Etiqueta visible; omite segmentos con valor `0`; nunca `Q0` ni `F0`; p.ej. `4-S-M7C-P10` (§ 2.4) |

Invariantes de la fuente:

- 8 filas con `geom IS NULL` (excluidas de la vista).
- 2 filas inválidas (`Self-intersection`; excluidas hasta que se defina política de corrección).
- La resolución de `id_loc` se hace por join de texto a `geo_gmat_localidad` (9 filas); no requiere join espacial porque `geo_qmat_parcela` ya tiene `localidad` pre-poblada.
- `ufun` y `ucomp` no están en `geo_qmat_parcela`; pertenecen a `fal_inmueble` y no se incluyen en la capa parcelaria general.

La vista devuelve tipos geometry nativos de PostGIS. `ST_AsGeoJSON` se aplica en la query del endpoint (Spring JDBC), no dentro de la view. Esto permite que el planner empuje el filtro de viewport al índice GiST de `geo_qmat_parcela` mediante `geom_22195`.

La view no debe incluir: titulares, cuentas, inmuebles completos, comercios, rubros ni datos personales.

### 6.2 Implementación de la vista: VIEW NORMAL

**Decisión cerrada por benchmark multicorrida (GEO-MODEL-002-C-POSTGIS-02-CORRECCION-01):**

`gis_parcela_web` se implementa como **view normal** (`CREATE OR REPLACE VIEW`).

Evidencia (mediana de 3 corridas por escenario; EXPLAIN ANALYZE FORMAT TEXT):

| Viewport | LIMIT | Filas retornadas | Execution time (mediana) |
|---|---|---|---|
| VP1 — Los Polvorines (zona densa, ~1.5km × 1km) | 500 | 500 | 19,2 ms |
| VP2 — Grand Bourg (zona media, ~3km × 2km) | 500 | 500 | 21,3 ms |
| VP3 — Área de Promoción (zona periférica, ~4km × 2.5km) | 500 | 500 | 6,5 ms |
| VP1 completo (sin LIMIT) | — | 2.754 | 63,9 ms |

GeoJSON equivalente VP1 completo: aproximadamente 930 KB (`~769 KB geom + ~161 KB label_points`).

Todas las consultas usan el índice GiST `sidx_parcelasgeom_5347_geom`. Join a `geo_gmat_localidad` añade < 2 ms (Materialize sobre 9 filas).

Motivos de la decisión:

- Los tiempos de respuesta son aceptables para un sandbox con un solo cliente.
- La tabla fuente tiene `localidad`, `barrio` pre-poblados, eliminando los joins costosos.
- Sin infraestructura de loader/refresh todavía; la view normal simplifica la operación.
- `ST_Transform` + `ST_PointOnSurface` representan ~10 ms adicionales por request; aceptable en esta etapa.

Si la concurrencia de producción lo requiere, migrar a `mv_gis_parcela_web` (materialized view con índice único para `CONCURRENTLY`) y exponer una fachada `gis_parcela_web` sobre ella. Las reglas de refresh (solo post-commit de carga exitosa, sin dry-run, sin source-unchanged) se aplican en ese momento.

### 6.3 Evidencia EXPLAIN

El diagnóstico multicorrida (slice GEO-MODEL-002-C-POSTGIS-02-CORRECCION-01) generó:

- **Un plan detallado** conservado para VP1 LIMIT 500: archivo exportado por `psql` en formato TEXT/JSON (no JSON puro directamente parseable); ubicado en `evidence/GEO-MODEL-002-C-POSTGIS-02-CORRECCION-01/explain/vp1-limit500-geom-nativa.json`.
- **Resumen multicorrida** para los demás escenarios (VP1 LIMIT 250/500/1000/full, VP2 LIMIT 500, VP3 LIMIT 500): tiempos de planning, mínimo, mediana y máximo; sin plan detallado separado por escenario.
- No existe una colección JSON completa de todos los planes.

El planner usa el índice GiST `sidx_parcelasgeom_5347_geom` de `geo_qmat_parcela` en todos los escenarios. El filtro de viewport se aplica sobre `geom_22195` con el operador `&&`:

```sql
WHERE geom_22195 && ST_Transform(ST_MakeEnvelope(:xmin,:ymin,:xmax,:ymax,4326), 22195)
```

El `LIMIT` es una política configurable del endpoint; la view no impone ningún límite. Para capas MVT bajo Martin, el zoom mínimo y la simplificación se configuran en Martin; los valores definitivos se cierran con evidencia de benchmark MVT (§ 6.5).

### 6.4 Visualización con MapLibre GL JS

- La capa parcelaria se sirve como teselas vectoriales MVT desde Martin; no como GeoJSON por viewport.
- MapLibre GL JS consume la fuente TileJSON de Martin para la capa parcelaria.
- Capa ligera: relleno transparente o casi transparente; límites finos.
- Hover y parcela seleccionada con resalte diferenciado.
- Etiqueta compacta (`etiqueta_canonica`; p.ej. `4-H-Q2-F1-M105B-P12`) con control de colisiones; nunca muestra `Q0` ni `F0` (§ 2.4).
- Las etiquetas se muestran sólo cuando el nivel de zoom es suficiente.
- Los zooms mínimos productivos, simplificación y atributos por nivel se cierran con evidencia de benchmark MVT; no se fijan sin datos reales.

### 6.5 `gis_parcela_web` como fuente candidata para Martin

La view v3 corregida (geom nativa, etiqueta canónica, LEFT JOIN exacto; ver handoff `GEO-MODEL-002-C-POSTGIS-02-CORRECCION-01`) es candidata como source de Martin, no como DDL aprobado ejecutado.

**Estado actual (GEO-SPEC-002):** Martin está operativo en el entorno de desarrollo. Las 5 capas activas son:

| Capa | Source-layer | Fuente |
|---|---|---|
| `geo_qmat_parcela` | `geo_qmat_parcela` | `public.geo_qmat_parcela.geom` |
| `geo_gmat_calle` | `geo_gmat_calle` | `public.geo_gmat_calle.geom` |
| `geo_gmat_manzana` | `geo_gmat_manzana` | `public.geo_gmat_manzana.geom` |
| `geo_gmat_localidad` | `geo_gmat_localidad` | `public.geo_gmat_localidad.geom` |
| `geo_gmat_barrio` | `geo_gmat_barrio` | `public.geo_gmat_barrio.geom` |

La ``etiqueta_canonica`` se construye en la vista fuente (``gis_parcela_web``), no en el cliente MapLibre. Ver seccion 6.6.

**Benchmark MVT (GEO-SPEC-002, sandbox un cliente, sin server-side cache):**

| Zona | z | Tile | P50 ms | Size KB | Nota |
|---|---|---|---|---|---|
| Full municipality | 13 | 2759/4933 | 128 | 1.776 | No apto para interactivo |
| Overview LP | 14 | 5519/9866 | 53 | 743 | Viewport completo en 1 tile |
| Los Polvorines (densa) | 15 | 11039/19732 | 13 | 146 | Óptimo interactivo |
| Grand Bourg (media) | 15 | 11038/19733 | 23 | 241 | Óptimo interactivo |
| Tortuguitas (media) | 15 | 11036/19730 | 22 | 131 | Óptimo interactivo |
| Tierras Altas (media) | 15 | 11037/19731 | 14 | 187 | Óptimo interactivo |
| Área de Promoción (periférica) | 15 | 11040/19728 | 3 | 13 | Muy escaso |
| Los Polvorines (densa) | 16 | 22079/39465 | 3 | 22 | Muy rápido |
| Grand Bourg (media) | 16 | 22077/39467 | 5 | 70 | Muy rápido |
| Los Polvorines (detalle) | 17 | 44159/78930 | 1 | 7 | Instant |

GeoJSON equivalente VP1 full: ~930 KB @ 64 ms (2.754 parcelas, consulta SQL directa).
MVT z=15 LP (1 tile): 146 KB @ 13 ms -- **6x menor, 5x mas rapido por tile**.
  Viewport estimado VP1 (~4 tiles): ~584 KB; carga paralela pendiente de medicion real (GEO-MODEL-002-E1).
MVT z=16 LP (1 tile): 22 KB @ 3 ms -- **42x menor, 21x mas rapido por tile**.
  Viewport estimado VP1 (~16 tiles): ~352 KB; mas requests; carga paralela pendiente de medicion real (GEO-MODEL-002-E1).

**Alcance del benchmark:** exploratorio single-client, sin concurrencia y sin MapLibre end-to-end.
Los factores por tile son indicativos; no extrapolar directamente al viewport completo.
La evaluacion de P50/P95, concurrencia, cache hit/miss, features por tile, atributos por zoom y
render MapLibre completo se realiza en GEO-MODEL-002-E1.

Cache: ETag presente (browser/CDN cache). Sin server-side tile cache configurado — cada request consulta PostgreSQL.

**Zooms productivos candidatos:** mínimo z=14 (overview); óptimo interactivo z=15–16; máximo detalle z=17. No fijar sin evaluación de concurrencia de producción.

Para configurar `gis_parcela_web` como source de Martin y obtener `etiqueta_canonica` en el tile, debe determinarse:

- si Martin acepta la view directamente o requiere materializada;
- si `geom_22195` (con reproyección en Martin) o `geom_4326` es preferible como geometría expuesta;
- si `label_point_4326` se sirve en la misma source o en una source de etiquetas separada.

No modificar ni reiniciar infraestructura Martin sin autorización humana.


### 6.6 Etiqueta canonica centralizada en la fuente

**Decision documental (GEO-SPEC-002-CORRECCION-01):**

- La fuente Martin productiva debe exponer `etiqueta_canonica`.
- La etiqueta se construye en `gis_parcela_web` o en la vista fuente equivalente.
- MapLibre solo consume la propiedad y controla estilo/visibilidad; no la construye en el cliente.
- Android y backend reciben la misma representacion.
- Nunca `Q0` ni `F0` en ninguna capa ni cliente.
- Construir la etiqueta en el cliente MapLibre queda eliminado como opcion productiva.

---


## 7. Pestañas funcionales

El prototipo Angular 20 independiente organiza la captura de georeferenciación en tres pestañas funcionales:

1. **Domicilio del infractor**
2. **Municipio emisor de licencia**
3. **Domicilio de la infracción**

### 7.1 Pestaña 1 — Domicilio del infractor

Caso de uso: registrar la ubicación de residencia o actividad del infractor.

Campos y mapeo compatible con `fal_persona_domicilio`:

| Campo | Descripción |
|---|---|
| Provincia | Provincia de residencia |
| Unidad territorial | Departamento/partido de residencia |
| Localidad | Localidad dentro de la unidad territorial |
| Calle | Nombre de la vía pública |
| Altura / sin altura | Numeración o indicador de ausencia |
| Unidad funcional | Piso/departamento u otra unidad funcional |
| Código postal | CP del domicilio |
| Modo local Malvinas / externo | Determina si aplica resolución local o catálogos externos |
| IDs IGN/INDEC | Identificadores nacionales de provincia, departamento y localidad |
| `id_loc_malvinas` | Código de localidad en el partido de Malvinas Argentinas |
| `id_tca_malvinas` | Identificador local de calle/trayecto de Malvinas Argentinas |
| Fallback textual controlado | Texto libre cuando los catálogos no resuelven |

El modelo de `fal_persona_domicilio` está cerrado; no se modifica en este subsistema.

### 7.2 Pestaña 2 — Municipio emisor de licencia

Caso de uso: registrar el municipio o unidad territorial que emitió la licencia de conducir.

Campos y mapeo:

| Campo | Columna en modelo |
|---|---|
| Provincia | `id_prov_lic` |
| Tipo de unidad territorial | `unidad_territorial_lic_tipo` |
| Municipio / departamento | `id_unidad_territorial_lic` |

El modelo asociado está cerrado; no se modifica en este subsistema.

### 7.3 Pestaña 3 — Domicilio de la infracción

Caso de uso: ubicar el punto exacto donde ocurrió la infracción.

Mecanismos disponibles dentro de esta pestaña:

- GPS: captura de coordenadas del dispositivo, con precisión y fecha/hora.
- Búsqueda calle-altura mediante el geocodificador municipal local (§ 13); no Google Places.
- Click sobre el mapa (MapLibre GL JS) para depositar el marcador.
- Drag del marcador para ajuste fino.
- Reverse local para sugerencia de dirección a partir del punto (PostGIS; § 17).
- Ajuste manual de campos de dirección.
- Confirmación obligatoria del inspector para cualquier punto sugerido automáticamente.

Reglas de resolución:

- El punto final se valida contra el polígono municipal real en PostGIS.
- La resolución punto → localidad y punto → parcela se ejecutan en PostGIS.
- `id_loc` siempre proviene de GIS; nunca lo asigna Google Maps.
- Si PostGIS no resuelve una localidad válida para el punto, el formulario queda bloqueado.
- El fallback es automático hacia la localidad GIS; no se presenta elección al usuario.
- El punto final es confirmado explícitamente por el inspector.
- El punto final nunca se reemplaza por el centroide ni por el punto representativo de la parcela.

---

## 8. Estados separados (pestaña 3)

Dentro de la pestaña de domicilio de la infracción coexisten tres estados independientes:

1. **Punto final actual:** coordenadas del marcador tal como está en el mapa (MapLibre GL JS).
2. **Parcela seleccionada:** parcela que el usuario eligió sobre la capa MVT parcelaria.
3. **Datos aplicados al formulario:** campos de ubicación, domicilio y nomenclatura ya escritos en el formulario.

Reglas:

- Seleccionar una parcela no sobrescribe silenciosamente el formulario.
- Mover el marcador no sobrescribe silenciosamente el formulario.
- Recibir una sugerencia de reverse geocoding no sobrescribe silenciosamente el formulario.
- La acción canónica que transfiere los datos al formulario es:
  **«Usar ubicación, domicilio y nomenclatura»**

---

## 9. Inmuebles

Al seleccionar una parcela, una modal permite consultar los inmuebles registrados en ella.

Datos que muestra la modal:

- `id_suj_i` (código de tipo de sujeto en Ingresos Municipales).
- `id_bie_i` / cuenta catastral.
- Titulares del inmueble.
- Unidad funcional (`ufun`).
- Unidad complementaria (`ucomp`).
- Nomenclatura catastral.
- Domicilio catastral relevante.

Búsqueda disponible por cuenta y por titular; siempre limitada a la parcela seleccionada.

Cuando hay más de diez resultados, se muestra el control de búsqueda; la lista puede
mostrarse paginada. Los endpoints de inmuebles soportan siempre filtro y paginación.

---

## 10. Comercios registrados

La consulta de comercios sigue la cadena:
parcela → inmueble → `fal_comercio_inmueble` → `fal_comercio`.

Datos disponibles por comercio:

| Campo | Descripción |
|---|---|
| `id_suj_c` | Código de tipo de sujeto en Ingresos Municipales |
| `id_bie_c` | Número de bien en Ingresos Municipales |
| Nombre de fantasía | Nombre comercial del establecimiento |
| Titulares | Personas vinculadas al comercio |
| `rubro_codigo` | Código del rubro de actividad |
| `rubro_nombre` | Nombre del rubro de actividad |
| Tipo de comercio | Clasificación del establecimiento |
| Inmueble relacionado | Inmueble base del comercio |
| Prioridad de relación | Orden de la relación inmueble-comercio |

Reglas:

- El rubro no se enriquece desde MariaDB.
- La búsqueda es por cuenta y por nombre de fantasía, siempre limitada a la parcela seleccionada.
- Cuando hay más de diez resultados, se muestra el control de búsqueda; la lista puede mostrarse paginada.
- Los endpoints de comercios soportan siempre filtro y paginación.
- Si existe una única relación inmueble-comercio, se puede seleccionar automáticamente mostrando el resultado.
- Si existen varias relaciones, se exige elección humana explícita.

---

## 11. Casos de uso

Casos de uso del prototipo Angular 20:

1. Cargar domicilio del infractor (pestaña 1).
2. Cargar municipio emisor de licencia (pestaña 2).
3. Obtener punto GPS desde el dispositivo.
4. Buscar dirección por calle y altura mediante el geocodificador municipal local (§ 13).
5. Ajustar punto con click en mapa (MapLibre GL JS) o drag del marcador.
6. Resolver localidad del punto en PostGIS.
7. Visualizar capa de parcelas MVT servida por Martin.
8. Seleccionar parcela sobre el mapa.
9. Aplicar ubicación, domicilio y nomenclatura al formulario mediante la acción canónica.
10. Consultar y seleccionar inmueble dentro de la parcela.
11. Consultar y seleccionar comercio registrado dentro de la parcela.
12. Resolver selección cuando hay múltiples inmuebles.
13. Ingresar comercio no registrado con inmueble base obligatorio.
14. Búsqueda condicional cuando hay más de diez resultados de inmuebles o comercios.
15. Limpiar o cambiar la selección activa de inmueble, comercio o punto.
16. Consolidar payload final con ubicación, domicilio, nomenclatura e identificadores.
17. Reconstrucción posterior de ubicación a partir de los datos almacenados.

---

## 12. Contratos conceptuales de endpoints

Los siguientes endpoints definen responsabilidades conceptuales. Los paths son provisionales
y se alinean con las convenciones HTTP existentes antes de la implementación.

| Método | Path | Estado |
|---|---|---|
| GET | `/api/geo/provincias` | IMPLEMENTADO |
| GET | `/api/geo/unidades-territoriales` | IMPLEMENTADO |
| GET | `/api/geo/localidades` | IMPLEMENTADO |
| GET | `/api/geo/calles` | IMPLEMENTADO |
| GET | `/api/geo/local/malvinas/localidades` | IMPLEMENTADO |
| GET | `/api/geo/local/malvinas/calles` | IMPLEMENTADO |
| GET | `/api/geo/map-config` | TO_IMPLEMENT |
| POST | `/api/geo/validar-punto` | TO_IMPLEMENT |
| POST | `/api/geo/resolver-punto` | TO_IMPLEMENT |
| POST | `/api/geo/geocodificar` — calle + altura → punto, cuadra, lado, calidad | IMPLEMENTADO |
| POST | `/api/geo/reverse` — punto → calle, cuadra, lado, altura estimada | IMPLEMENTADO |
| GET | `/api/catastro/parcelas/{id}/detalle` | TO_IMPLEMENT |
| GET | `/api/catastro/parcelas/{id}/inmuebles` | TO_IMPLEMENT |
| GET | `/api/catastro/parcelas/{id}/comercios` | TO_IMPLEMENT |

Las capas vectoriales MVT de parcelas se sirven directamente por Martin (TileJSON/MVT), no por endpoint Spring GeoJSON. El endpoint `/api/catastro/parcelas/geojson` ya no es el flujo principal de visualización.

---

## 13. Requisitos no funcionales

### 13.1 Seguridad y privacidad

- Las credenciales de acceso a bases de datos y servicios externos permanecen fuera del repositorio.
- La capa MVT de parcelas (Martin) no incluye titulares, cuentas, inmuebles, comercios ni datos personales.
- Los titulares de inmuebles y comercios se exponen únicamente en el endpoint de detalle, bajo autenticación.
- No se loguean titulares completos en trazas de aplicación.
- No usar información personal como feature de mapa ni como input de geocodificación.

### 13.2 Rendimiento y configurabilidad

- Paginación en todos los endpoints de lista.
- Zooms mínimos, simplificación y atributos MVT por nivel de zoom: configurables en Martin; valores definitivos se cierran con benchmark MVT (§ 6.5).
- Límite de LIMIT en consultas PostGIS: configurable por endpoint; la view no impone límite.
- Índices espaciales requeridos en PostGIS para resolución de punto, localidad, parcela y geocodificación.
- Latencia objetivo de geocodificación municipal: a medir; no fijar sin evidencia.

### 13.3 Escenarios de test de integración

Los tests de integración MariaDB/PostGIS deben cubrir al menos:

- Punto fuera del polígono municipal.
- Punto dentro del municipio sin localidad asignada.
- Punto dentro del municipio sin parcela superpuesta.
- Parcela sin inmuebles asociados.
- Parcela con múltiples inmuebles.
- Comercio con una única relación inmueble-comercio.
- Comercio con varias relaciones inmueble-comercio.
- Flujo completo de comercio no registrado.
- Caché de localidades inválida o vacía al inicio del servicio.
- Limpieza de estados al cambiar de modo (registrado a no registrado y viceversa).
- Geocodificación: calle con múltiples ramales candidatos (debe exigir selección).
- Geocodificación: altura fuera de rango observado (debe marcar baja confianza).
- Reverse local: punto sin rango confiable (debe devolver solo calle, no altura falsa).

---

## 14. Gate de calidad para prescindir de Google

El geocodificador local se considera suficiente inicialmente si logra:

- ≥ 85 % de los casos de validación en la misma calle y misma cuadra;
- alta cobertura en la misma localidad;
- baja tasa de puntos en cuadra adyacente;
- ningún resultado lejano aplicado silenciosamente;
- todos los resultados automáticos acompañados por calidad y evidencia;
- confirmación obligatoria del inspector.

No inventar porcentajes. Deben medirse con el conjunto holdout (§ 18).

Métricas de evaluación requeridas:

| Dimensión | Descripción |
|---|---|
| Misma parcela | Geocodificación aterriza en la parcela exacta del inmueble de referencia |
| Mismo frente | Aterriza en el frente/lado correcto de la manzana |
| Mismo lado | Mismo lado de la cuadra (paridad y manzana coinciden) |
| Misma cuadra | Misma cuadra entre intersecciones, aunque no el frente exacto |
| Misma calle | Mismo trayecto lógico (`id_tca`), cuadra adyacente o diferente |
| Misma localidad | Localidad resuelta por PostGIS coincide con `id_loc` del inmueble |
| Distancia al punto/frente real | Distancia métrica entre el punto geocodificado y el frente real |
| P50 / P75 / P90 / P95 | Percentiles de distancia sobre la distribución holdout |
| Tasa > 100 m | Fracción de casos con error > 100 m |
| Tasa > 250 m | Fracción de casos con error > 250 m |
| Tasa > 500 m | Fracción de casos con error > 500 m |
| Candidatos ambiguos | Casos con múltiples tramos candidatos |
| Sin cobertura | Casos sin ningún tramo disponible |

La política final de distancia umbral se decide con la distribución real de longitudes de cuadra medida en PostGIS.

---

## 15. Unidad de geocodificación: `gis_calle_tramo_altura`

**Decisión cerrada (GEO-SPEC-002-CORRECCION-02-R1):** La unidad principal de geocodificación es el **segmento físico de calle**, no el lado de cuadra. Una fila conceptual por segmento físico:

```text
calle lógica (ccalle + id_loc)
  → segmento físico (id de geo_gmat_calle)
    → rango general del segmento (altura_desde / altura_hasta)
    → rango izquierdo (izq_desde / izq_hasta) — evidencia secundaria
    → rango derecho (der_desde / der_hasta) — evidencia secundaria
    → punto: ST_LineInterpolatePoint(geom, 0.5) si orientación no disponible
```

Nombre conceptual cerrado: `gis_calle_tramo_altura`.

El lado (izquierdo/derecho) se conserva como evidencia secundaria de precisión y paridad; no es la unidad principal. Un inmueble es candidato de un segmento cuando su altura está contenida en al menos uno de los rangos laterales válidos del segmento. El segmento se cuenta una sola vez aunque coincidan ambos lados.

**No crear la tabla/view en este slice.** La creación se realiza exclusivamente en el slice G (§19).

### 15.1 Identificador estable del segmento

**Decisión (GEO-SPEC-002-CORRECCION-02-R1):**

| Columna | Tipo | Decisión |
|---|---|---|
| `id` | `INTEGER` PK | **Identificador estable del segmento.** Único, indexado (btree), 9.311 valores distintos. |
| `id_tramo` | `DOUBLE PRECISION` | No confiable: 14 duplicados detectados. No usar como identificador de segmento. |
| `fid` | `BIGINT` | Único (9.311 distintos) pero difiere de `id` en 5.184 casos. Identificador externo; no usar como PK del modelo. |
| `ccalle` | `VARCHAR(10)` | Código de la calle lógica. Corresponde al `ccalle`/`id_tca` referenciado en la spec. No existe la columna `id_tca` en la tabla física. |

`geo_gmat_calle` no tiene la columna `id_tca`. El código de calle lógica es `ccalle` (VARCHAR 10, 31 NULLs de 9.311). La identidad de calle lógica es `ccalle + id_loc`.

### 15.2 Campos candidatos del modelo `gis_calle_tramo_altura`

Basados en las columnas reales de `geo_gmat_calle` y los diagnósticos ejecutados:

| Campo | Origen | Descripción |
|---|---|---|
| `tramo_id` | `geo_gmat_calle.id` (PK) | Identificador estable del segmento físico |
| `ccalle` | `geo_gmat_calle.ccalle` | Código de la calle lógica (máx 10 chars; algunos NULL) |
| `id_loc` | `geo_gmat_calle.id_loc` | Código de localidad CHAR(2) |
| `calle_nombre` | `geo_gmat_calle.nombre` | Nombre completo de la calle |
| `geom_22195` | `geo_gmat_calle.geom` | Geometría del eje (MultiLineString, SRID 22195) |
| `longitud_metros` | `geo_gmat_calle.longitud` | Longitud métrica del segmento |
| `altura_desde` | derivado | `LEAST` entre extremos válidos de ambos lados |
| `altura_hasta` | derivado | `GREATEST` entre extremos válidos de ambos lados |
| `izq_desde` | `geo_gmat_calle.izq_desde` | Extremo inicial del rango izquierdo (evidencia secundaria) |
| `izq_hasta` | `geo_gmat_calle.izq_hasta` | Extremo final del rango izquierdo (evidencia secundaria) |
| `der_desde` | `geo_gmat_calle.der_desde` | Extremo inicial del rango derecho (evidencia secundaria) |
| `der_hasta` | `geo_gmat_calle.der_hasta` | Extremo final del rango derecho (evidencia secundaria) |
| `cantidad_inmuebles_evidencia` | conteo desde `fal_inmueble` | Inmuebles con altura en el segmento |
| `metodo_rango` | derivado | `RANGOS_LATERALES` / `NO_DATA` |
| `calidad` | derivado | Ver §16 |
| `si_aplicable_automaticamente` | derivado | Verdadero solo si confianza alta y sin ambigüedad lejana |
| `requiere_confirmacion` | derivado | Siempre verdadero para puntos sugeridos automáticamente |

### 15.3 Reglas de derivación del rango general

Para cada segmento:

1. Normalizar cada rango lateral con `LEAST/GREATEST`.
2. Ignorar rangos totalmente `0` o nulos.
3. `altura_desde` = mínimo entre los extremos válidos de izquierda y derecha.
4. `altura_hasta` = máximo entre los extremos válidos de izquierda y derecha.
5. No usar únicamente el rango general para decidir candidatos si genera huecos artificiales; validar siempre contra al menos un rango lateral.

### 15.4 Resultados del diagnóstico (GEO-SPEC-002-CORRECCION-02-R1)

Fuente: `fal_inmueble` (85.167 filas; `id_bie` único; `id_suj` siempre 1) cruzada con `geo_gmat_calle` (9.311 segmentos) via `TRIM(calle_codigo) = ccalle AND id_localidad = id_loc`.

**Cobertura por segmento físico** (67.013 inmuebles con `calle_numero > 0`):

| Categoría | Cantidad | % |
|---|---|---|
| Sin calle lógica en GIS | 2.307 | 3.4 % |
| Cero segmentos candidatos | 5.417 | 8.1 % |
| Exactamente un segmento | 58.724 | **87.6 %** |
| Múltiples segmentos | 2.872 | 4.3 % |
| Máximo segmentos por inmueble | 3 | — |
| Coincidencia solo lado izquierdo | 4.402 | — |
| Coincidencia solo lado derecho | 3.127 | — |
| Coincidencia ambos lados | 56.324 | — |

**Cobertura por localidad:**

| id_loc | Total | Sin cobertura | Un segmento | Múltiples | % cobertura |
|---|---|---|---|---|---|
| LP | 15.343 | 1.300 | 13.158 | 885 | 91.5 % |
| GB | 15.105 | 555 | 13.940 | 610 | 96.3 % |
| TO | 9.471 | 404 | 8.650 | 417 | 95.7 % |
| PN | 9.098 | 1.087 | 7.625 | 386 | 88.1 % |
| VM | 7.488 | 317 | 6.977 | 194 | 95.8 % |
| AS | 5.018 | 768 | 4.024 | 226 | 84.7 % |
| TA | 3.989 | 692 | 3.163 | 134 | 82.7 % |
| AP | 1.369 | 166 | 1.183 | 20 | 87.9 % |
| MA | 131 | 127 | 4 | 0 | 3.1 % — gap |

La localidad MA (Malvinas Argentinas) tiene cobertura casi nula: requiere investigación separada.

**Paridad (evaluación como mejora opcional):**

| Resultado | Cantidad | % de ambiguos |
|---|---|---|
| Paridad reduce a 1 segmento | 1.831 | 63.7 % |
| Paridad reduce parcialmente | 19 | 0.7 % |
| Paridad no reduce | 986 | 34.3 % |
| Paridad elimina todos (contradicción) | 36 | 1.3 % |

Patrón de paridad en la fuente (no normativo, estadístico): 97.4 % de los rangos izquierdos son impares; 99.7 % de los derechos son pares. No se asume como invariante sin verificación individual.

La paridad **mejora significativamente** en casos de ambigüedad: resuelve el 63.7 % de los 2.872 casos múltiples. Sin embargo, no es obligatoria para el baseline; aplica como fase de mejora en E2.

**Separación espacial de segmentos candidatos múltiples:**

| Categoría | Cantidad | % de ambiguos |
|---|---|---|
| Segmentos que se tocan (dist = 0 m) | 1.739 | 60.5 % |
| Contiguos ≤ 150 m | 809 | 28.2 % |
| Separación 150–250 m | 82 | 2.9 % |
| Separación 250–500 m | 106 | 3.7 % |
| Separación > 500 m | 136 | 4.7 % |
| **Ambigüedad cercana (≤ 100 m)** | **2.523** | **87.8 %** |
| **Ambigüedad lejana (> 100 m)** | **349** | **12.2 %** |

Percentiles de distancia mínima entre candidatos: P50=0.0 m, P75=22.7 m, P90=178.1 m, P95=448.1 m, máx=2.339.8 m.

La **ambigüedad lejana** afecta solo 349 / 67.013 = **0.5 %** del total de domicilios geocodificables. Estos casos no pueden aplicarse automáticamente bajo ninguna circunstancia.

### 15.5 Geocodificación dentro del segmento

Dado que `sentido` vale `0` en todos los 9.311 tramos, no puede orientarse la interpolación progresiva de forma directa.

Reglas:

- Si puede inferirse orientación por evidencia independiente (continuidad numérica con segmentos vecinos, inmuebles ordenados, extremos coherentes): interpolar proporcionalmente mediante `ST_LineInterpolatePoint(geom, fraccion)`.
- Si no puede inferirse orientación: usar `ST_LineInterpolatePoint(geom, 0.5)` (centro del segmento) y marcar `requiere_confirmacion=true`.
- El objetivo primario es ubicar la **cuadra correcta**, no afirmar precisión inexistente dentro del segmento.

### 15.6 Factibilidad respondida

| Pregunta | Evidencia | Conclusión |
|---|---|---|
| ¿Calle lógica por `ccalle`+`id_loc`? | 887 combinaciones distintas; 560 `ccalle` distintos | Sí. La clave es `ccalle + id_loc`. |
| ¿`id_tca` existe en la tabla? | No existe la columna `id_tca` | No. La columna es `ccalle`. |
| ¿Identificador de segmento estable? | `id` PK único; `id_tramo` tiene 14 duplicados | Sí: `id`. |
| ¿Sentido usable para progresivas? | `sentido = 0` en los 9.311 tramos | No, por ahora. |
| ¿Rangos laterales disponibles? | `izq_desde/hasta`, `der_desde/hasta` presentes | Sí. |
| ¿Paridad disponible? | Derivable del mínimo del rango | Sí (estadísticamente: izq=impar, der=par, no invariante). |

---

## 16. Métodos de cálculo de rangos de altura y posición de MariaDB

**Decisión (GEO-SPEC-002-CORRECCION-02-R1):** MariaDB no es fuente obligatoria del geocodificador municipal. Las fuentes autoritativas del geocodificador son exclusivamente PostgreSQL/PostGIS.

### 16.1 MariaDB fuera del baseline geográfico

- `geo_calle_alturas_barrio` (MariaDB) **no es fuente productiva obligatoria** del geocodificador calle-altura.
- No es necesario cruzar MariaDB con PostGIS para construir el baseline.
- No es necesario reconciliar localidades MariaDB para este flujo.
- Las localidades geográficas autoritativas son las nueve de PostGIS (`geo_gmat_localidad`).
- Calles, rangos, inmuebles, comercios y relaciones necesarias están en PostgreSQL/PostGIS.
- La evidencia MariaDB obtenida anteriormente (diagnóstico GEO-SPEC-002) queda como referencia histórica opcional; no es dependencia productiva.
- No se accede a MariaDB en el baseline de geocodificación.
- No se eliminan tablas ni documentación histórica; solo se aclara su falta de participación.

Notas históricas sobre `geo_calle_alturas_barrio` (GEO-SPEC-002-CORRECCION-01, valor de referencia):

- La columna id_loc contiene string vacío en todos los 1.814 registros activos.
- La tabla organiza rangos por barrio (id_bar), no por localidad.
- Para cruzar con PostGIS usar ccalle como clave; id_loc no es operativo en esta tabla.
- 7 calles (RP24, P188, RN8N, RN8S, RN9O, RP08, RP23) no tienen match en geo_malv_calle_version.
- 1 rango invertido: ccalle correspondiente a LOS AROMOS, alt_desde=301 > alt_hasta=300.

### 16.2 Jerarquía de calidad para asignar un rango a un segmento

La jerarquía aplica al segmento físico completo (no al lado individual):

1. Rangos laterales reales en `geo_gmat_calle` con inmuebles de evidencia en el segmento.
2. Rangos laterales en `geo_gmat_calle` sin inmuebles de evidencia (rango declarado).
3. Continuidad con segmentos vecinos.
4. Interpolación entre segmentos conocidos.
5. Progresión métrica de la calle.
6. Extrapolación limitada.
7. Sin cobertura.

`geo_calle_alturas_barrio` (MariaDB) no forma parte de esta jerarquía en el baseline. Puede incorporarse como fuente auxiliar opcional en una etapa posterior si se justifica con evidencia.

### 16.3 Calidades candidatas

| Código | Descripción |
|---|---|
| `OBSERVED_MULTIPLE_PROPERTIES` | Rango construido desde múltiples inmuebles del segmento |
| `OBSERVED_SINGLE_PROPERTY` | Rango construido desde un único inmueble |
| `DECLARED_RANGE` | Rango tomado directamente de los campos `izq/der_desde/hasta` sin inmuebles de evidencia |
| `INFERRED_FROM_ADJACENT_SEGMENTS` | Inferido por continuidad con segmentos vecinos |
| `INFERRED_FROM_STREET_PROGRESSION` | Inferido por progresión métrica de la calle |
| `INTERPOLATED_BY_LENGTH` | Interpolado por longitud de segmento |
| `AMBIGUOUS` | Candidatos múltiples sin criterio de desempate |
| `INCONSISTENT` | Datos contradictorios entre fuentes |
| `NO_DATA` | Sin evidencia disponible |

No cerrar enums productivos hasta que E2 confirme cobertura real con holdout.

**Nota sobre `sentido` (GEO-SPEC-002-CORRECCION-01, confirmado en GEO-SPEC-002-CORRECCION-02-R1):**

- `sentido` en `geo_gmat_calle` es tipo `DOUBLE PRECISION`; valor `0` en todos los 9.311 tramos.
- No puede usarse para orientar progresivas de numeración.
- No inferir orientación desde `sentido`. Ver §15.5.

---

## 17. Prevención de resultados lejanos

El algoritmo **no aplica automáticamente un punto** cuando:

- la calle tiene varios ramales candidatos;
- la localidad no coincide con la informada;
- el rango contradice cuadras vecinas;
- la altura está fuera del rango observado;
- la extrapolación supera límites configurables;
- la progresión cambia de sentido;
- la paridad es inconsistente;
- la confianza es baja;
- hay candidatos separados espacialmente.

Comportamiento según nivel de confianza:

| Confianza | Comportamiento |
|---|---|
| Alta | Mostrar punto sugerido y pedir confirmación al inspector |
| Media | Centrar en la cuadra/lado candidato sin aplicar al formulario |
| Baja | Centrar sobre la calle o zona; exigir selección manual |
| Sin cobertura | Búsqueda manual mediante MapLibre GL JS |

Nunca aplicar silenciosamente un punto lejano.

---

## 18. Reverse local

**Decisión (GEO-SPEC-002-CORRECCION-02-R1):** El flujo de reverse local usa el segmento físico como unidad principal. La altura se estima solo si existe orientación confiable.

Flujo de reverse local desde PostGIS:

```text
punto GPS / click en mapa
  → segmento de calle más próximo (ST_Distance a geo_gmat_calle.geom)
  → calle lógica (ccalle + id_loc)
  → rango general del segmento (altura_desde / altura_hasta)
  → altura estimada solo si existe orientación confiable
```

Si no puede inferirse orientación (sentido = 0, sin evidencia de continuidad):

```text
calle resuelta
segmento resuelto
altura = null
requiereAlturaManual = true
```

No devolver una altura falsa.

Si el rango no es confiable o el segmento es ambiguo:

- devolver calle y segmento;
- no devolver altura;
- marcar `requiereAlturaManual=true` en la respuesta.

El inspector siempre puede corregir el resultado del reverse local.

---

## 19. Validación holdout

Para medir el gate de calidad (§ 14) sin circularidad:

Para cada inmueble de validación:

1. Ocultar su propia altura/punto al modelo.
2. Construir el rango con el resto de la evidencia del segmento fisico (excluyendo el inmueble de validacion).
3. Geocodificar calle + altura.
4. Comparar el punto geocodificado contra la parcela/frente real.

Estratificación del conjunto holdout:

- las nueve localidades del partido;
- calles largas vs. cortas;
- zonas densas vs. periféricas;
- cuadras con muchos vs. pocos inmuebles;
- paridad par e impar;
- calles con cambios de nombre;
- calles multipartes;
- rangos completos vs. incompletos.

Registrar cobertura (fracción de casos con rango disponible) y error por estrato.

---

## 20. Mapa municipal — lenguaje visual y jerarquía de capas

**Decisión documental (GEO-SPEC-003-MAPLIBRE-STYLE-UX-01):**

### 20.1 Nombre del estilo

`Malvinas Claro`

### 20.2 Filosofía visual

```text
Estilo base:     Apple Maps Muted como filosofía visual
Familiaridad:    jerarquía vial y buscador similares a Google Maps
Identidad:       localidades en colores pastel propios del proyecto
```

Principio UX:

```text
El mapa debe sentirse familiar como Google Maps o Apple Maps,
pero mostrar progresivamente la información municipal
sin sobrecargar la visualización.
```

La inspiración es conceptual: claridad, baja saturación, jerarquía, familiaridad, progresión de detalle. No se copian assets, iconos, colores propietarios ni estilos exactos de Google o Apple.

Características:

- Fondo blanco cálido o gris muy claro; sin texturas ni ruido visual.
- Colores intensos reservados exclusivamente para estados interactivos (selección, búsqueda, GPS, advertencia, error, edición).
- Elementos normales con colores suaves y desaturados.
- Apto para escritorio, tablet y Android.
- Consistente con MapLibre GL JS y MapLibre Native.

### 20.3 Paleta de localidades

La paleta es una convención del proyecto; no es una paleta municipal oficial.
La fuente canónica única es este documento.

| Código | Localidad | Color |
|---|---|---|
| AP | Área de Promoción El Triángulo | `#F3DFC1` |
| AS | Ing. Adolfo Sourdeaux | `#D8E7F5` |
| GB | Grand Bourg | `#E7D9F3` |
| LP | Los Polvorines | `#D8ECD9` |
| MA | Malvinas Argentinas | `#F3E8B9` |
| PN | Ing. Pablo Nogués | `#D2E9E7` |
| TA | Tierras Altas | `#EED8CD` |
| TO | Tortuguitas | `#DCE0F4` |
| VM | Villa de Mayo | `#F0D9E0` |

Validado contra `public.geo_gmat_localidad` (PostGIS, 9 filas; GEO-SPEC-003-MAPLIBRE-STYLE-UX-01).

Reglas:

- Los nueve códigos coinciden exactamente con PostGIS; no existe un décimo código.
- Usar expresión `match` de MapLibre por `id_loc`; fallback `#E8E8E8`.
- No hardcodear la paleta en múltiples clientes; esta sección es la fuente canónica única.
- No depender solo del color para distinguir localidades (accesibilidad, daltonismo).
- Los codigos id_loc son autoritativos desde PostGIS (geo_gmat_localidad); la paleta de color es una convencion aprobada del proyecto, no una paleta municipal oficial. PostGIS valida que los nueve codigos existan y sean unicos.

Opacidades de relleno de localidad (calibrables):

```text
z10–z12: 0.38
z13:     0.24
z14:     0.12
z15:     0.04 (solo borde muy tenue)
z16+:    sin relleno ni borde de localidad
```

### 20.4 Jerarquía por zoom

Criterio funcional para parcelas:

```text
Las parcelas aparecen cuando el viewport muestra
aproximadamente doce manzanas o menos.
```

El valor `z17` es inicial. Debe calibrarse por dispositivo (escritorio, notebook, tablet, celular, Android).

Tokens calibrables:

| Token | Valor inicial |
|---|---|
| `parcelMinZoom` | 17 |
| `blockLabelMinZoom` | 15 |
| `blockLabelMaxZoom` | 17 |
| `parcelShortLabelMinZoom` | 17 |
| `parcelFullLabelMinZoom` | 19 |
| `localidadFillMaxZoom` | 16 |
| `localidadLabelMaxZoom` | 15 |

Nota sobre `maxzoom` en MapLibre: el campo `maxzoom` de una capa es **exclusivo**. Significa que las teselas se cargan para `zoom < maxzoom`, es decir, la capa es visible hasta el nivel `maxzoom - 1` inclusive. Ejemplo: `maxzoom: 16` hace que la capa sea visible en z15 como ultimo nivel; desaparece al llegar a z16.

| Capa | maxzoom | Ultimo zoom visible |
|---|---|---|
| locality-fill | 16 | z15 (con opacidad terminal 0.04) |
| locality-outline | 16 | z15 (con opacidad terminal 0.1) |
| locality-label | 15 | z14 (con fade progresivo) |
| barrio-fill | 15 | z14 |
| block-label | 17 | z16 (se oculta al aparecer parcelas) |

Matriz canónica:

| Nivel | Nombre | Contenido |
|---|---|---|
| z10–z12 | Distrito | Límite municipal, localidades con relleno pastel, nombres de localidad, rutas y ejes principales |
| z13–z14 | Aproximación territorial | Relleno de localidad más tenue, límites, avenidas, calles principales, barrios opcionales |
| z15 | Manzanas | Manzanas, etiqueta central de manzana, calles relevantes con nombres |
| z16 | Detalle de manzanas | Todas las manzanas, mayor densidad de calles y nombres |
| z17 | Parcelas | Parcelas, límites parcelarios, etiqueta corta, sin etiqueta de manzana, nombres de calle con alta cobertura |
| z18 | Parcelas detalladas | Etiqueta corta más frecuente, nomenclatura completa con espacio suficiente |
| z19+ | Máximo detalle | Nomenclatura completa visible; nunca saturar parcelas pequeñas |

### 20.5 Calles: representación visual

Jerarquía real en `geo_gmat_calle.jerarquia`:
CALLE (7.479), DIAGONAL (724), RUTA (292), PASAJE (216), ROTONDA (206), AUTOPISTA (189), AVENIDA (180), PEATONAL (21), SEMIPEATONAL (4).

Baseline de capas (GEO-SPEC-003-CORRECCION-01):

```text
street-casing-primary    (AUTOPISTA, RUTA)          desde z12
street-fill-primary      (AUTOPISTA, RUTA)          desde z12
street-casing-secondary  (AVENIDA, ROTONDA)         desde z13
street-fill-secondary    (AVENIDA, ROTONDA)         desde z13
street-casing-local      (CALLE, DIAGONAL)          desde z15
street-fill-local        (CALLE, DIAGONAL)          desde z15
street-fill-pedestrian   (PASAJE, PEATONAL, SEMIPEATONAL)  desde z16 (sin casing)
street-label-primary     (AUTOPISTA, RUTA)          desde z13
street-label-secondary   (AVENIDA, ROTONDA)         desde z14
street-label-local       (CALLE, DIAGONAL)          desde z15
street-label-passage     (PASAJE, PEATONAL, SEMIPEATONAL)  desde z16
```

Reglas de peatonales y semipeatonales:

- PEATONAL y SEMIPEATONAL **no usan casing vial normal**; se dibujan con `street-fill-pedestrian` (linea fina, sin exterior gris calido).
- PASAJE se incluye en el mismo grupo visual por similaridad de escala.
- Los tres tipos aparecen como fill y etiqueta unicamente desde z16.
- No se representan antes de z16 bajo ninguna clase.
- SEMIPEATONAL: 4 segmentos en la fuente actual. PEATONAL: 21 segmentos.


Doble trazo obligatorio:

```
manzana → borde/vereda visual → calle clara → borde/vereda visual → manzana
```

Dos capas sobre la misma geometría central:
1. `street-casing`: exterior gris cálido claro.
2. `street-fill`: interior blanco o marfil; avenida beige suave; ruta amarillo arena tenue.

Anchos orientativos (interpolate por zoom):

| Clase | Casing (px) | Interior (px) |
|---|---:|---:|
| Calle local | 11 | 8 |
| Diagonal | 14 | 10 |
| Avenida | 20 | 15 |
| Ruta/Autopista | 25 | 19 |

No fijar valores exactos sin prueba visual.

Orden de capas obligatorio:

1. `background`
2. `locality-fill`
3. `locality-outline`
4. `barrio-fill` (opcional)
5. `block-fill`
6. `block-outline`
7. `street-casing`
8. `street-fill`
9. `parcel-fill`
10. `parcel-outline`
11. `parcel-selected-fill`
12. `parcel-selected-outline`
13. `block-label`
14. `parcel-label-short`
15. `parcel-label-full`
16. `street-label-*` (por jerarquía)
17. `locality-label`
18. Puntos interactivos (GPS, resultado búsqueda, edición)

Las parcelas no deben tapar las calles. Evaluar cuidadosamente el orden según geometrías reales.

El corredor vial PostGIS (`gis_calle_corredor_web`) no forma parte del baseline. Solo se evalúa en una etapa posterior, autorizada explícitamente, si el doble trazo no produce sensación de calle suficiente.

### 20.6 Manzanas

Fuente: `geo_gmat_manzana` (Martin). Campo de etiqueta disponible: `lblmza`.

Estilo:

- Fill gris cálido muy suave (`#EEEBE4`); borde fino gris claro.
- Etiqueta de manzana: texto ~12 px, peso Medium, gris medio, halo blanco fino.
- Etiqueta visible aproximadamente entre z15 y z16.99; se oculta cuando aparecen parcelas (z17+).
- `symbol-placement: point`; MapLibre centra sobre el polígono.
- Prioridad menor que nombres de calle.

Gap: `geo_gmat_manzana` no expone punto de etiqueta explícito (ST_PointOnSurface). MapLibre usa el centroide automático, que puede caer fuera del polígono. Solución definitiva: exponer desde `gis_manzana_label_web` si se requiere.

### 20.7 Parcelas

Capas conceptuales:

```text
parcel-fill             — relleno casi blanco (#F5F3EF)
parcel-outline          — borde gris azulado suave (#A8B4C0)
parcel-selected-fill    — selección azul intensa (fill_opacity=0.18)
parcel-selected-outline — borde selección (#1D4ED8, 2.5 px)
parcel-label-short      — etiqueta corta (p.ej. P10), z17–z18
parcel-label-full       — nomenclatura completa (p.ej. 4-S-M7C-P10), z19+
```

Fuente actual: `geo_qmat_parcela` (Martin). Fuente definitiva: `gis_parcela_web` (pendiente).

Reglas:

- Parcelas normales casi blancas; selección claramente separada del estilo normal.
- No mostrar etiquetas en parcelas demasiado pequeñas (decidir por zoom y `area_m2`).
- Nomenclatura completa siempre disponible al seleccionar independientemente del zoom.
- La etiqueta canónica viene de la fuente Martin (`etiqueta_canonica` en `gis_parcela_web`); MapLibre no la construye en cliente (ver §6.6).
- Nunca mostrar `Q0` ni `F0` en ninguna etiqueta parcelaria.

### 20.8 Etiquetas parcelarias

| Nivel | Etiqueta mostrada | Fuente |
|---|---|---|
| z15–z16 | Etiqueta de manzana (p.ej. `4-H-105B`) | `lblmza` de `geo_gmat_manzana` |
| z17–z18 | Etiqueta corta de parcela (p.ej. `P10`) | `etiqueta_corta` de `gis_parcela_web` (pendiente) |
| z19+ | Nomenclatura completa (p.ej. `4-S-M7C-P10`) | `etiqueta_canonica` de `gis_parcela_web` (pendiente) |

Reglas: nunca `Q0`, nunca `F0`. La etiqueta canónica proviene exclusivamente de la fuente Martin.

### 20.9 Nombres de calles

Propuesta inicial:

```text
Fuente:          Noto Sans Medium o Inter Medium
Color:           #4A4A4A
Halo:            #FFFFFF
Halo width:      1.5–1.8 px
Halo blur:       0.3 px
Calle local:     11–14 px
Avenida:         12–16 px
```

Propiedades MapLibre requeridas:

- `symbol-placement: line`
- `text-rotation-alignment: map`
- `text-pitch-alignment: viewport`
- `text-max-angle`
- `symbol-spacing` (reducir a mayor zoom)
- `text-halo-width`
- `text-halo-blur`
- `text-letter-spacing`
- `text-keep-upright: true`
- `symbol-sort-key` (por jerarquía)

No usar fuentes propietarias de Google o Apple.

Objetivo: todas las calles visibles con al menos una etiqueta legible cuando el espacio lo permita.
Las reglas tipograficas estan definidas (tama?no, fuente, halo, espaciado). La legibilidad real es calibrable y permanece pendiente de validacion visual en GEO-MODEL-002-E1.
Fuente actual en el style draft: Open Sans Semibold y Open Sans Regular (fonts.openmaptiles.org).
Font arrays simplificados a un unico elemento por array en GEO-SPEC-003-CORRECCION-02 (stacks exactos verificados HTTP 200; Arial Unicode MS eliminado provisionalmente). Fuente productiva (Noto Sans o Inter) pendiente de evaluacion en E1.
No usar `text-allow-overlap=true` globalmente.

### 20.10 Localidades: etiquetas

- Mayúsculas.
- Gris oscuro con opacidad.
- Peso Medium; letter-spacing amplio (0.15).
- Sin caja de fondo; halo blanco muy tenue.
- Prioridad menor que rutas.
- Desaparecer progresivamente al acercarse; no mostrar donde compitan con parcelas.

### 20.11 Buscador flotante — contrato UX

Ubicación: parte superior del mapa.

```text
┌───────────────────────────────────────────┐
│ Dardo Rocha 3153                      🔍 │
└───────────────────────────────────────────┘
```

- Tarjeta blanca; sombra suave; esquinas 12–14 px.
- Ancho máximo ~520–580 px en escritorio; adaptación responsive en móvil.
- Icono de búsqueda como sufijo; botón limpiar cuando haya texto.

### 20.12 Autocomplete territorial

Toda opción muestra calle y localidad obligatoriamente:

```text
Dardo Rocha
Los Polvorines

Dardo Rocha
Grand Bourg
```

Puede mostrar información secundaria:

```text
Presidente Perón
Los Polvorines · ex José C. Paz
```

Contrato de selección:

```json
{
  "idTca": "02211",
  "idLoc": "LP",
  "calle": "DARDO ROCHA",
  "localidad": "LOS POLVORINES"
}
```

Reglas:

- Identidad `id_tca/ccalle + id_loc`; nunca resolver calle homónima sin localidad.
- Localidad siempre visible incluso con una sola coincidencia.
- Si hay localidad seleccionada, priorizarla; permitir corregir.
- Separar nombre actual e histórico.
- Soportar calle + altura.
- Geocodificar solo después de selección territorial.

### 20.13 Accesibilidad

Requisitos documentados:

- Navegación por teclado: flechas arriba/abajo, Enter, Escape.
- Foco visible en buscador y resultados.
- Labels ARIA en todos los controles interactivos del mapa.
- Contraste suficiente (WCAG 4.5:1 para texto; 3:1 para elementos gráficos).
- No depender solo del color para distinguir localidades.
- Targets táctiles mínimos: 44 px.
- Soporte de zoom del navegador.
- Respetar `prefers-reduced-motion`.

### 20.14 Rendimiento

Presupuestos iniciales (indicativos; medir en GEO-MODEL-002-E1):

- No cargar parcelas antes del zoom acordado (`parcelMinZoom`).
- Evitar atributos MVT innecesarios en cada nivel de zoom.
- Etiquetas separadas por zoom; evitar `allow-overlap` masivo.
- Evitar capas duplicadas sin necesidad.

Métricas a medir:

- Tiempo hasta primer render.
- Tiles solicitados por viewport.
- KB por viewport (actual: z=15 LP ≈ 146 KB/tile; z=16 LP ≈ 22 KB/tile).
- FPS durante pan/zoom (objetivo: 60 fps).
- Tiempo de aparición de etiquetas.
- Memoria MapLibre.
- Comportamiento diferenciado por dispositivo (desktop/tablet/mobile/Android).

El benchmark productivo se realiza en GEO-MODEL-002-E1.

### 20.15 Capas contextuales de mapa cotidiano (pendiente)

El mapa actual es un **mapa municipal minimo**: calles, manzanas, parcelas, localidades.
No es todavia un basemap equivalente visualmente a Apple Maps o Google Maps.

Para E1, evaluar la incorporacion de:

| Capa contextual | Fuente candidata | Estado |
|---|---|---|
| Cuerpos de agua (rios, arroyos, lagunas) | PostGIS / fuente externa | PENDIENTE E1 |
| Espacios verdes (plazas, parques, bosques) | PostGIS / fuente externa | PENDIENTE E1 |
| Ferrocarril / vias | PostGIS / fuente externa | PENDIENTE E1 |
| Edificios / equipamientos (hospitales, escuelas) | PostGIS / fuente externa | PENDIENTE E1 |
| Limite / contexto exterior (municipios vecinos) | PostGIS / fuente externa | PENDIENTE E1 |

Hasta que estas capas se incorporen, el mapa se denomina **mapa municipal minimo** y no debe compararse funcionalmente con los basemaps de Apple o Google.

### 20.16 Fuentes Martin actuales y fuentes futuras candidatas

Fuentes actuales operativas:

| Fuente Martin | Source-layer | Campos útiles para style | Gaps |
|---|---|---|---|
| `geo_qmat_parcela` | `geo_qmat_parcela` | `id`, `barrio`, `circunsc`, `fracc`, `localidad`, `manz`, `nomenclatu`, `parc`, `quint`, `secc`, `area_5347` | Sin `id_loc`, `etiqueta_canonica`, `etiqueta_corta`, label_point |
| `geo_gmat_calle` | `geo_gmat_calle` | `nombre`, `ccalle`, `id_loc`, `jerarquia`, `longitud`, `alias`, rangos de altura | Sin `clase_vial` normalizada, `prioridad_label` |
| `geo_gmat_manzana` | `geo_gmat_manzana` | `id`, `lblmza`, `first_manz`, `first_circ`, `first_secc`, `first_loca` | Sin label_point (ST_PointOnSurface) |
| `geo_gmat_localidad` | `geo_gmat_localidad` | `id_loc`, `localidad`, `area`, `pob2010` | — |
| `geo_gmat_barrio` | `geo_gmat_barrio` | `id`, `nombre` | Sin `id_loc`, `localidad` |

Fuentes futuras candidatas (no crear en este slice):

| Fuente | Propósito | Slice |
|---|---|---|
| `gis_parcela_web` | Parcelas con `etiqueta_canonica`, `etiqueta_corta`, `id_loc`, label_point, `area_m2` | GEO-MODEL-002-G |
| `gis_calle_label_web` | Segmentos optimizados para label, prioridad y longitud | Solo si `geo_gmat_calle` no alcanza |
| `gis_calle_corredor_web` | Corredor vial (fallback si doble trazo falla) | Etapa posterior autorizada, solo si necesario |
| `gis_manzana_label_web` | Manzanas con ST_PointOnSurface explícito | Solo si `geo_gmat_manzana` no ubica bien |


---

## 21. GAPs de DDL (GEO-INFRA-SPEC-001) — Estado al 2026-07-30

Todos los objetos fueron verificados en `sb_malvinas_geo` (PostgreSQL 15.2 / PostGIS 3.3).
El DDL real esta versionado en `db/postgis/`. Ver ese README para el manifiesto y orden de ejecucion.

### GAP HB-1: funcion comercios_parcela (MVT) — RESUELTO

**Objeto:** `comercios_parcela(z integer, x integer, y integer) RETURNS bytea LANGUAGE sql STABLE PARALLEL SAFE`
**Motor:** PostgreSQL/PostGIS, base `sb_malvinas_geo`
**Estado en Martin:** publicada como fuente MVT `comercios_parcela` (confirmado via `/catalog` el 2026-07-30).
**Archivo:** `db/postgis/functions/comercios_parcela_mvt.sql` — DDL real versionado.

### GAP HB-2: tablas fal_inmueble, fal_comercio, fal_comercio_inmueble — RESUELTO

**Objetos:** tablas en PostGIS, base `sb_malvinas_geo` (no en MariaDB).
**Filas verificadas al 2026-07-30:** fal_inmueble 85.167 / fal_comercio 13.024 / fal_comercio_inmueble 12.737.
**Sin geometria propia.** Join espacial via clave catastral con `geo_qmat_parcela` (SRID 22195).
**Archivos:** `db/postgis/tables/fal_inmueble.sql`, `fal_comercio.sql`, `fal_comercio_inmueble.sql` — DDL real versionado.

### GAP HB-3: funcion geo_buscar_calles_tolerante — RESUELTO

**Objeto:** `geo_buscar_calles_tolerante(p_query text, p_id_loc text DEFAULT NULL, p_limit integer DEFAULT 5)`
**Motor:** PostgreSQL/PostGIS, base `sb_malvinas_geo`. No es fuente Martin; es llamada desde el backend Java.
**Dependencias reales:** `geo_normalizar_termino`, `geo_tokens_sorted`, tabla `geo_malv_calle_indice_busqueda`.
**Correccion:** la dependencia documentada anteriormente como `geo_calle_nombre_lookup` era incorrecta.
**Archivo:** `db/postgis/functions/geo_buscar_calles_tolerante.sql` — DDL real versionado.

### GAP HB-4: version validada de PostgreSQL/PostGIS — RESUELTO

**Versions validadas:**
- PostgreSQL 15.2 (Visual C++ build 1914, 64-bit)
- PostGIS 3.3 (USE_GEOS=1 USE_PROJ=1 USE_STATS=1)

Ver `db/postgis/README.md` §Versiones validadas y `apps/web-geodomicilios/docs/spec/60-infrastructure/postgis-local-setup.md` §1.

---

### Objeto adicional descubierto: comercios_manzana

La fuente `comercios_manzana` esta publicada en Martin pero no tiene DDL versionado ni documentacion
en el layer-catalog. Pendiente de documentación en `apps/web-geodomicilios/docs/spec/20-map/layer-catalog.md` y de versionar su DDL en `db/postgis/`.
