# [13-DDL] 14 - TABLAS DE GEOREFERENCIACION TERRITORIAL

## Finalidad

Este archivo documenta las tablas de georreferenciación territorial del municipio de Malvinas Argentinas utilizadas como soporte GIS / PostGIS para:

- resolución espacial por GPS
- geocodificación inversa local
- enriquecimiento automático de domicilios
- determinación de localidad, barrio, manzana, parcela y calle a partir de coordenadas
- validación espacial
- soporte cartográfico para operación y consulta

Estas tablas no forman parte del núcleo físico transaccional del sistema de faltas, pero sí constituyen una dependencia funcional relevante para la resolución territorial local.

---

## Naturaleza de estas tablas

Estas tablas:

- provienen de capas municipales de la Dirección de Geomática
- residen en PostGIS
- representan capas espaciales reales
- complementan la resolución territorial tabular ya documentada en:
  - `11-tablas-territoriales-externas-introduccion.md`
  - `12-tablas-territoriales-ign-indec.md`
  - `13-tablas-territoriales-malvinas-locales.md`

Estas capas deben considerarse fuente oficial geoespacial municipal para los usos GIS del sistema.

---

## Proyección / sistema de referencia

### Proyección declarada

- `EPSG:22195`
- **Campo Inchauspe / Argentina 5**

### Regla operativa

Las operaciones espaciales del sistema deben tener presente que:

- las capas municipales están almacenadas en `EPSG:22195`
- las coordenadas GPS capturadas desde dispositivos móviles o navegador normalmente vendrán en `WGS84` (`EPSG:4326`)
- cuando se crucen coordenadas GPS con estas capas, deberá aplicarse la transformación espacial correspondiente antes de ejecutar contención, intersección o proximidad

---

## Prefijo y origen de las capas

Se adopta la familia:

- `geo_gmat_*`

donde:

- `geo` = familia geoespacial
- `gmat` = origen Geomática municipal

Esto permite distinguir estas capas de:

- tablas territoriales tabulares IGN / INDEC
- catastro tabular local
- otras posibles fuentes GIS futuras

---

## Capas disponibles

### Capas principales

- `geo_gmat_localidad`
- `geo_gmat_barrio`
- `geo_gmat_manzana`
- `geo_gmat_parcela`
- `geo_gmat_nomenclatura`
- `geo_gmat_calle`

---

# 1. Tabla: `geo_gmat_localidad`

## Finalidad funcional

Capa poligonal de localidades del municipio.

Cumple rol de contenedor territorial superior para:

- manzanas
- parcelas
- nomenclaturas
- barrios cuando existan
- calles en contexto local

## Estructura relevante

| Campo | Tipo | Observación |
|---|---|---|
| `id` | `integer` | PK |
| `geom` | `geometry(MultiPolygon)` | geometría de localidad |
| `fid` | `bigint` | identificador auxiliar de origen |
| `area` | `double precision` | área |
| `localidad` | `varchar(20)` | nombre de localidad |
| `perimetro` | `bigint` | perímetro |
| `pob2010` | `integer` | población |
| `volume` | `double precision` | dato auxiliar |
| `sarea` | `double precision` | dato auxiliar |

## PK
- `id`

## Uso en faltas

- determinar localidad desde un punto GPS
- validar si una parcela o manzana pertenece a una localidad
- enriquecer domicilio o ubicación de infracción
- contexto espacial superior de resolución local

---

# 2. Tabla: `geo_gmat_barrio`

## Finalidad funcional

Capa poligonal de barrios municipales.

Es una capa contextual intermedia y su uso puede ser opcional según cobertura o definición territorial disponible.

## Estructura relevante

| Campo | Tipo | Observación |
|---|---|---|
| `id` | `integer` | PK |
| `geom` | `geometry(MultiPolygon)` | geometría de barrio |
| `fid` | `bigint` | identificador auxiliar |
| `nombre` | `varchar(40)` | nombre del barrio |

## PK
- `id`

## Uso en faltas

- determinar barrio por contención espacial
- enriquecer domicilio o ubicación
- proveer referencia barrial cuando exista definición GIS válida

## Observación

No debe asumirse que toda ubicación tendrá barrio resuelto.  
El uso de esta capa es complementario y dependerá de cobertura efectiva.

---

# 3. Tabla: `geo_gmat_manzana`

## Finalidad funcional

Capa poligonal de manzanas.

Representa la unidad espacial urbana intermedia entre localidad/barrio y parcela.

## Estructura relevante

| Campo | Tipo | Observación |
|---|---|---|
| `id` | `integer` | PK |
| `geom` | `geometry(MultiPolygon)` | geometría de manzana |
| `fid` | `bigint` | identificador auxiliar |
| `first_circ` | `varchar(4)` | circunscripción |
| `first_secc` | `varchar(4)` | sección |
| `first_quin` | `varchar(10)` | quinta |
| `first_frac` | `varchar(10)` | fracción |
| `first_manz` | `varchar(10)` | manzana |
| `first_parc` | `varchar(10)` | parcela |
| `first_loca` | `varchar(20)` | localidad asociada |
| `first_barr` | `varchar(50)` | barrio asociado |
| `lblmza` | `varchar(30)` | etiqueta visible |

## PK
- `id`

## Uso en faltas

- determinar manzana por contención espacial
- enriquecer ubicación de infracción o inmueble
- servir de contenedor espacial intermedio para parcela y nomenclatura
- soporte cartográfico para consulta y validación

---

# 4. Tabla: `geo_gmat_parcela`

## Finalidad funcional

Capa poligonal de parcelas.

Es el objeto territorial clickeable principal para consulta parcelaria.

## Estructura relevante

| Campo | Tipo | Observación |
|---|---|---|
| `id` | `integer` | PK |
| `geom` | `geometry(MultiPolygon)` | geometría de parcela |
| `fid` | `bigint` | identificador auxiliar |
| `gid` | `bigint` | identificador auxiliar |
| `area` | `double precision` | área |
| `perimeter` | `double precision` | perímetro |

## PK
- `id`

## Uso en faltas

- identificar la parcela donde cae un punto GPS
- resolver el objeto territorial clickeable principal
- servir como base para consulta parcelaria
- enlazar espacialmente con nomenclatura, manzana y calle

## Regla importante

La parcela es el objeto espacial principal para interacción cartográfica parcelaria.  
La nomenclatura no reemplaza a la parcela.

---

# 5. Tabla: `geo_gmat_nomenclatura`

## Finalidad funcional

Capa puntual de nomenclaturas parcelarias.

Está pensada como capa de puntos/etiquetas asociadas a parcelas, no como geometría parcelaria en sí misma.

## Estructura relevante

| Campo | Tipo | Observación |
|---|---|---|
| `fid` | `bigint` | PK |
| `geom` | `geometry(Point)` | punto etiquetador |
| `id` | `bigint` | auxiliar |
| `id_1` | `bigint` | auxiliar |
| `cuenta_num` | `varchar(254)` | cuenta |
| `domi_calle` | `varchar(254)` | calle textual |
| `domi_num` | `varchar(254)` | número |
| `domi_piso` | `varchar(254)` | piso |
| `domi_depto` | `varchar(254)` | depto |
| `domi_local` | `varchar(254)` | local |
| `coord_x` | `varchar(254)` | coordenada textual |
| `coord_y` | `varchar(254)` | coordenada textual |
| `nomencla` | `varchar(48)` | nomenclatura |

## PK
- `fid`

## Uso en faltas

- mostrar nomenclatura parcelaria
- enriquecer la información de una parcela
- resolver etiqueta o identificación visible de un inmueble o cuenta
- soporte a consultas catastrales

## Regla importante

`geo_gmat_nomenclatura` no representa la geometría real de la parcela.  
Debe tratarse como:

- punto etiquetador
- referencia parcelaria
- enriquecimiento catastral

No debe usarse como reemplazo de `geo_gmat_parcela`.

---

# 6. Tabla: `geo_gmat_calle`

## Finalidad funcional

Capa lineal de calles municipales.

Representa ejes viales/tramos y contiene además metadata vial y rangos de altura.

## Estructura relevante

| Campo | Tipo | Observación |
|---|---|---|
| `id` | `integer` | PK |
| `geom` | `geometry(MultiLineString)` | geometría lineal |
| `fid` | `bigint` | identificador auxiliar |
| `id_tramo` | `double precision` | identificador de tramo |
| `ccalle` | `varchar(10)` | código de calle |
| `nombre` | `varchar(100)` | nombre principal |
| `alias` | `varchar(100)` | alias |
| `izq_desde` | `double precision` | rango izquierdo desde |
| `izq_hasta` | `double precision` | rango izquierdo hasta |
| `der_desde` | `double precision` | rango derecho desde |
| `der_hasta` | `double precision` | rango derecho hasta |
| `veralt` | `double precision` | vereda / altura |
| `jerarquia` | `varchar(15)` | jerarquía |
| `jurisdicc` | `varchar(10)` | jurisdicción |
| `acceso` | `varchar(10)` | acceso |
| `materializ` | `varchar(10)` | materialización |
| `longitud` | `double precision` | longitud |
| `anchoclz` | `double precision` | ancho calzada |
| `ancholm` | `double precision` | ancho línea municipal |
| `calzada` | `varchar(20)` | tipo de calzada |
| `pavimento` | `varchar(2)` | pavimento |
| `datapav` | `double precision` | dato auxiliar |
| `ccuneta` | `varchar(2)` | cuneta |
| `datacor` | `double precision` | dato auxiliar |
| `sentido` | `double precision` | sentido |
| `desnivel` | `varchar(10)` | desnivel |
| `cruceffcc` | `varchar(2)` | cruce FFCC |
| `agua` | `varchar(3)` | servicio |
| `gas` | `varchar(2)` | servicio |
| `cloaca` | `varchar(2)` | servicio |
| `pluvial` | `varchar(2)` | servicio |
| `transpub` | `varchar(2)` | transporte público |
| `memo` | `varchar(250)` | observación |
| `auditoria` | `varchar(10)` | dato auxiliar |
| `localidad` | `varchar(30)` | localidad textual |
| `restricc` | `bigint` | auxiliar |
| `etiq` | `bigint` | auxiliar |
| `alt_min` | `bigint` | altura mínima |
| `pav_cvp` | `bigint` | auxiliar |
| `clas_cuma` | `varchar(15)` | clasificación |
| `pav_b_cerr` | `varchar(10)` | auxiliar |
| `ulg_lim` | `varchar(100)` | auxiliar |
| `via_mae` | `double precision` | auxiliar |
| `etiq_pav` | `varchar(10)` | auxiliar |
| `reali por` | `varchar(50)` | origen |
| `fuente` | `varchar(50)` | fuente |

## PK
- `id`

## Uso en faltas

- determinar tramo vial frente a una parcela
- enriquecer domicilio o ubicación de infracción
- resolver calle más próxima o calle contigua al inmueble
- soporte a validación espacial de altura y tramo

---

## Organización territorial declarada

Según la disposición funcional informada:

- `geo_gmat_localidad`
- `geo_gmat_barrio`
- `geo_gmat_manzana`
- `geo_gmat_nomenclatura`
- `geo_gmat_parcela`
- `geo_gmat_calle`

### Relación conceptual entre capas

- la nomenclatura está contenida dentro de la parcela
- la parcela está contenida dentro de la manzana
- la manzana está contenida dentro de barrio cuando exista barrio aplicable
- barrio y manzana están dentro de localidad
- las calles son líneas entre manzanas y frente a parcelas

---

## Reglas funcionales de resolución espacial

### 1. Resolución territorial por punto GPS

Dado un punto GPS transformado a `EPSG:22195`, el sistema puede intentar resolver:

1. localidad contenedora
2. barrio contenedor, si existe
3. manzana contenedora
4. parcela contenedora
5. nomenclatura asociable a la parcela
6. calle frontal o calle más próxima

### 2. Resolución parcelaria

Cuando el punto cae dentro de una parcela:

- la parcela pasa a ser el objeto espacial principal
- puede obtenerse la manzana contenedora
- puede obtenerse localidad
- puede obtenerse barrio si existe
- puede buscarse nomenclatura asociada
- puede buscarse tramo de calle correspondiente

### 3. Resolución de nomenclatura

La nomenclatura debe resolverse como enriquecimiento parcelario:

- no reemplaza la parcela
- no debe asumirse como geometría del inmueble
- representa etiqueta o identificación catastral visible

### 4. Resolución de calle

La calle debe resolverse como:

- tramo frontal o más próximo a la parcela
- o línea vial asociada por cercanía y contexto espacial

La lógica exacta de “calle frente a parcela” podrá requerir proximidad espacial, heurísticas geométricas o reglas adicionales.

---

## Uso funcional en el sistema de faltas

Estas capas pueden utilizarse para:

- enriquecer ubicación de infracción capturada por GPS
- proponer automáticamente localidad, barrio, manzana, parcela y calle
- validar consistencia espacial de una ubicación
- asistir consultas cartográficas y catastrales
- mejorar resolución local en domicilios o ubicaciones dentro del municipio
- soportar flujos móviles o inspecciones en terreno

---

## Relación con domicilio textual

La resolución espacial no reemplaza necesariamente el domicilio textual.

Debe poder cumplir uno o más de estos roles:

- confirmar
- enriquecer
- sugerir
- completar parcialmente
- asistir validación

La decisión final de persistencia y uso operativo deberá surgir del flujo correspondiente.

---

## Consultas espaciales esperables

Estas capas habilitan, entre otras, consultas del tipo:

- contención espacial
- intersección
- proximidad
- búsqueda de calle más próxima
- determinación de parcela contenedora
- determinación de localidad, barrio o manzana por punto

Operadores/funciones esperables en PostGIS:

- `ST_Contains`
- `ST_Intersects`
- `ST_DWithin`
- `ST_Distance`
- `ST_ClosestPoint`
- `ST_Transform`

---

## Reglas de implementación recomendadas

- crear índices espaciales adecuados sobre `geom`
- documentar claramente cualquier transformación entre `EPSG:4326` y `EPSG:22195`
- no asumir que todas las capas tienen cobertura perfecta o completa
- tratar `geo_gmat_nomenclatura` como capa de etiquetado/referencia
- tratar `geo_gmat_parcela` como objeto espacial principal para selección parcelaria
- validar si la lógica de calle frontal requiere una función o proceso específico adicional

---

## Relación con otros archivos

- territorial tabular y lookups administrativos: `11-tablas-territoriales-externas-introduccion.md`
- IGN / INDEC: `12-tablas-territoriales-ign-indec.md`
- tablas locales Malvinas: `13-tablas-territoriales-malvinas-locales.md`
- lógica SQL de formularios y lookups: `../14-sql-operativo/03-sql-formularios-y-lookups.md`

---