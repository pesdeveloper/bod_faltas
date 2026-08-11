# Contratos de Geocodificacion

> **Estado documental:** NORMATIVE
> **Alcance:** logica de busqueda de direcciones y geocodificacion de precision.

---

## 1. Modos de busqueda  [GEO-GEO-001]

El input analiza el texto en tiempo real para detectar el modo:

| Modo | Patron | Ejemplo | Accion |
|---|---|---|---|
| `partial` | Solo texto sin numero | Dardo Rocha | Autocomplete de calles |
| `calle-altura` | Texto + numero | Dardo Rocha 3153 | Autocomplete filtrado por altura |
| `esquina` | Dos calles con y/e | Dardo Rocha y San Martin | Geocodificar esquina |
| `entre-calles` | Patron entre X y Z | Cordoba entre San Martin y Maipu | Geocodificar tramo |

---

## 2. Autocomplete  [GEO-GEO-002]

| Parametro | Valor |
|---|---|
| Debounce | 280 ms |
| Minimo de caracteres | 2 |
| Maximo de sugerencias | 5 |
| Normalizacion de acentos | Si (servidor y frontend) |
| Busqueda fonetica/fuzzy | Via tabla `geo_calle_nombre_lookup` |
| Validacion de alturas | Si: modo `calle-altura` envia parametro `altura` al backend |

Cuando el modo es `calle-altura`, el frontend envia la altura como parametro opcional a
`GET /api/geo/local/malvinas/calles`.
El backend filtra tramos donde la altura no cae en ningun rango de calle.
Esto evita sugerencias con alturas inexistentes.

---

## 3. Estrategia de precision  [GEO-GEO-003]

El backend intenta geocodificar en dos etapas:

### Etapa 1 (primaria): interpolacion por inmuebles conocidos

1. Consulta `geo_calle_altura_ref` por `ccalle` e `id_loc`.
2. Busca el inmueble de altura inferior (`alt_lo`) y superior (`alt_hi`).
3. Interpola linealmente el centroide de la parcela entre ambos puntos.
4. Snap a la calle: aplica `ST_ClosestPoint` sobre el tramo mas cercano de `geo_gmat_calle`.
5. Devuelve el punto sobre la linea de calle.

**Calidad**: `ALTA` (ambos extremos) o `MEDIA` (un extremo). Campo `evidencia`: `fal_inmueble_interp_snapped`.

### Etapa 2 (fallback): interpolacion por rangos de altura

Si la etapa 1 no devuelve resultado:
1. Busca tramos en `geo_gmat_calle` donde la altura cae en el rango.
2. Calcula la fraccion y aplica `ST_LineInterpolatePoint` sobre el tramo.

---

## 4. Vista materializada geo_calle_altura_ref  [GEO-GEO-004]

Vincula inmuebles (`fal_inmueble`) con geometria de parcela (`geo_qmat_parcela`),
indexada por codigo de calle, localidad y altura catastral.

### DDL

`sql
DROP MATERIALIZED VIEW IF EXISTS geo_calle_altura_ref;

CREATE MATERIALIZED VIEW geo_calle_altura_ref AS
SELECT
    TRIM(i.calle_codigo)                           AS ccalle,
    TRIM(i.id_localidad)                           AS id_loc,
    i.calle_numero                                 AS altura,
    ST_Y(ST_Transform(ST_Centroid(p.geom), 4326))  AS lat,
    ST_X(ST_Transform(ST_Centroid(p.geom), 4326))  AS lng,
    TRIM(i.circunscripcion::text)                  AS circ,
    TRIM(i.seccion)                                AS secc,
    TRIM(i.manzana)                                AS manz,
    TRIM(i.parcela)                                AS parc
FROM fal_inmueble i
JOIN geo_qmat_parcela p
    ON  i.circunscripcion::text   = TRIM(p.circunsc)
    AND TRIM(i.seccion)           = TRIM(p.secc)
    AND TRIM(COALESCE(i.quinta,   '0'))   = TRIM(COALESCE(p.quint, '0'))
    AND TRIM(COALESCE(i.fraccion, '0'))   = TRIM(COALESCE(p.fracc, '0'))
    AND TRIM(i.manzana)           = TRIM(p.manz)
    AND TRIM(i.parcela)           = TRIM(p.parc)
WHERE i.calle_numero > 0
  AND i.calle_codigo IS NOT NULL
  AND p.geom IS NOT NULL;

CREATE INDEX ON geo_calle_altura_ref (ccalle, id_loc, altura);
CREATE INDEX ON geo_calle_altura_ref (ccalle, id_loc);
`

Refresco: ejecutar `REFRESH MATERIALIZED VIEW geo_calle_altura_ref`
despues de actualizaciones masivas de `fal_inmueble` o `geo_qmat_parcela`.

---

## 5. Snap-to-street  [GEO-GEO-005]

El punto geocodificado debe caer **sobre la linea de calle**, no dentro de la parcela.

`sql
CROSS JOIN LATERAL (
    SELECT geom FROM geo_gmat_calle
    WHERE TRIM(ccalle) = TRIM(:idTca) AND TRIM(id_loc) = TRIM(:idLoc)
    ORDER BY ST_Distance(
        ST_Transform(geom, 4326)::geography,
        ST_SetSRID(ST_MakePoint(:raw_lng, :raw_lat), 4326)::geography
    )     LIMIT 1
) seg
-- Punto mas proximo sobre el tramo:
ST_ClosestPoint(ST_Transform(seg.geom, 4326), punto_interpolado)
`

---

## 6. Navegacion al resultado  [GEO-GEO-006]

Al seleccionar un candidato geocodificado:

1. Se almacena en `GeoDomiciliosStateService.setPunto()` con origen `GEOCODIFICADO_CALLE_ALTURA`.
2. Se llama a `MalvinasMapComponent.flyTo(lat, lng)`.
3. El zoom objetivo es el nivel mas cercano al rango 18-19 del ladder discreto.
4. El marcador rojo se coloca en las coordenadas del resultado.
5. El marcador es draggable; al soltarlo se emite un nuevo `mapClick` con las nuevas coordenadas.
