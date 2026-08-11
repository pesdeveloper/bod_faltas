# Glosario GeoDomicilios

> **Estado documental:** NORMATIVE
> **Alcance:** vocabulario canonico del modulo `web-geodomicilios`.
> Ante ambiguedad terminologica, este glosario es autoridad.

---

## 1. Tecnologias del mapa

### MapLibre GL JS
Biblioteca JavaScript de mapas vectoriales, open-source. Version usada: 5.x.
Renderiza estilos MapLibre (JSON) usando WebGL. Toda la logica de visualizacion del mapa vive en este componente.

### Martin
Servidor de vector tiles (MVT) y fuentes tipograficas (glyphs PBF), basado en Rust.
Expone tablas y funciones de PostGIS como tiles protobuf accesibles via HTTP.
En este proyecto sirve tiles de capas municipales y fuentes tipograficas para MapLibre.

### MVT / Mapbox Vector Tiles
Formato de datos geoespaciales comprimidos (protobuf) por tile. Cada tile contiene geometrias vectoriales
para una celda del grid z/x/y. Martin genera MVT desde PostGIS via ST_AsMVT.

### PBF / Glyphs
Protocol Buffer Format. Los glyphs son rangos de caracteres tipograficos en formato PBF, usados por MapLibre
para renderizar etiquetas en el mapa. Martin los genera desde fuentes TTF/OTF.
Endpoint: `GET /font/{fontstack}/{range}` (sin extension .pbf).

### SRID 22195 / EPSG:22195
Sistema de referencia Argentina Faja 5 (GK). Proyeccion usada por el dato catastral de Malvinas Argentinas.
Las APIs y el frontend trabajan en WGS84 (EPSG:4326); la conversion la hace PostGIS con ST_Transform.

### EPSG:4326 / WGS84
Sistema de coordenadas geograficas global (latitud/longitud). Todos los endpoints de la API devuelven
coordenadas en este sistema.

### GeoJSON

Formato JSON estandar para geometrias geograficas (Point, LineString, Polygon, MultiPolygon, etc.).

En este proyecto se usa exclusivamente para la fuente runtime `selected-parcela`
(geometria completa de la parcela seleccionada, obtenida del backend).
Las capas de visualizacion del mapa usan exclusivamente fuentes Martin (vector tiles MVT);
los assets `.e1.geojson` que existian previamente fueron migrados a PostGIS/Martin en v0.7.0.
Ver trazabilidad en `20-map/layer-catalog.md` seccion 6.

---

## 2. Datos geograficos

### parcela
Unidad catastral minima. Identificada por los campos: `circunsc`, `secc`, `quint`, `fracc`, `manz`, `parc`.
Geometria almacenada en `geo_qmat_parcela`.

### manzana
Agrupacion de parcelas. Identificada por circunscripcion, seccion, quinta, fraccion y manzana.
Geometria en `geo_gmat_manzana`.

### barrio
Division territorial de segundo nivel dentro de una localidad. Geometria en `geo_gmat_barrio`.

### localidad
Division territorial de primer nivel. Identificada por `id_loc` (2 chars en MALVINAS_LOCAL).
Localidades del municipio: AP, AS, GB, LP, MA, PN, TA, TO, VM.
Geometria en `geo_gmat_localidad`.

### circunsc / secc / quint / fracc / manz / parc
Campos de la nomenclatura catastral. Los campos `quint` (quinta) y `fracc` (fraccion)
tienen valor `0` cuando estan ausentes en la fuente. Nunca se muestran en etiquetas visibles.

### ParcelaRef
Estructura minimal para identificar una parcela: `{ circ, secc, quint, fracc, manz, parc }`.
Se emite cuando el usuario hace click sobre una parcela en el mapa.

### geo_qmat_parcela
Tabla PostGIS con geometrias de parcelas del partido de Malvinas Argentinas.
Columnas relevantes: `circunsc, secc, quint, fracc, manz, parc, geom` (SRID 22195).

### geo_gmat_calle
Tabla PostGIS con tramos de calles. Columnas: `ccalle, id_loc, nombre, izq_desde, izq_hasta, der_desde, der_hasta, geom`.
Fuente para geocodificacion por rangos de alturas.

### geo_calle_altura_ref
Vista materializada de precision. Cruza `fal_inmueble` con `geo_qmat_parcela` para obtener
coordenadas de centroides de parcelas ligados a alturas reales de inmuebles.
Usada como fuente primaria de geocodificacion por altura.

---

## 3. Componentes y servicios

### GEO_CONSTANTS
Objeto de constantes del frontend: `MARTIN_BASE_URL`, `ZOOM_LEVELS`, `MAP_CENTER`,
`MAP_BOUNDS_SW/NE`, `STYLE_URL`, `ZOOM_DEFAULT`.
No debe contener IPs o URLs hardcodeadas en produccion.

### OrigenUbicacion
Enum que clasifica como se obtuvo un punto de ubicacion:
`MANUAL_MAPA`, `GEOCODIFICADO_CALLE_ALTURA`, `GEOCODIFICADO_ESQUINA`,
`GEOCODIFICADO_ENTRE_CALLES`, `PARCELA`.

### colocar-modo
Modo de interaccion del mapa en que el cursor es una cruz y el siguiente click coloca el marcador.
Activado desde la UX cuando el usuario quiere colocar manualmente un punto.

---

## 4. Geocodificacion

### geocodificacion
Proceso de convertir una descripcion de direccion (calle + altura, esquina, etc.) en coordenadas geograficas.

### snap-to-street
Ajuste del punto geocodificado para que caiga sobre la linea de calle mas cercana,
en lugar de en el centroide de la parcela. Se implementa con `ST_ClosestPoint` en PostGIS.

### calidad del geocodeo
Campo `calidad` del resultado: `ALTA` (interpolado entre dos inmuebles conocidos),
`MEDIA` (un solo extremo disponible). Campo `evidencia` registra la fuente usada.
