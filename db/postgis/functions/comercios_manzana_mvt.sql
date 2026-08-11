-- =============================================================================
-- FUNCTION: comercios_manzana(z integer, x integer, y integer)
-- Base: sb_malvinas_geo (PostgreSQL 15.2 / PostGIS 3.3)
-- DDL extraído de pg_get_functiondef el 2026-07-30
-- Verificado: EXISTE y publicado en Martin como fuente 'comercios_manzana'
-- Consumidor Angular: capa 'comercios-manzana-dot' + 'comercios-manzana-label'
--   minzoom 15, maxzoom 18 (compañera de comercios_parcela en zoom 18+)
--
-- Dependencias:
--   tablas: geo_gmat_manzana, fal_inmueble, fal_comercio_inmueble, fal_comercio
--   SRID base: 22195; proyección MVT: 3857
--
-- Lógica:
--   - Solo activa para z >= 14 (SQL interno)
--   - Centra el punto en ST_Centroid de la manzana
--   - label: lista de hasta 5 nombres si cnt <= 5, si no el conteo
--   - source-layer MVT fijo: 'comercios_manzana'
-- =============================================================================

CREATE OR REPLACE FUNCTION public.comercios_manzana(z integer, x integer, y integer)
  RETURNS bytea
  LANGUAGE sql
  STABLE PARALLEL SAFE
AS $function$
  SELECT ST_AsMVT(tile, 'comercios_manzana', 4096, 'geom')
  FROM (
    SELECT
      ST_AsMVTGeom(
        ST_Centroid(ST_Transform(m.geom, 3857)),
        ST_TileEnvelope(z, x, y),
        4096, 64, true
      ) AS geom,
      m.lblmza                       AS manzana,
      COUNT(DISTINCT fc.id_bie)::int AS cnt,
      CASE
        WHEN COUNT(DISTINCT fc.id_bie) <= 5 THEN
          (SELECT STRING_AGG(n, ' · ' ORDER BY n)
           FROM (
             SELECT DISTINCT
               COALESCE(NULLIF(TRIM(fc2.nombre_fantasia),''), NULLIF(TRIM(fc2.rubro_nombre),'')) AS n
             FROM fal_inmueble fi2
             JOIN fal_comercio_inmueble fci2
               ON fci2.inmueble_id_suj = fi2.id_suj AND fci2.inmueble_id_bie = fi2.id_bie
             JOIN fal_comercio fc2
               ON fc2.id_suj = fci2.comercio_id_suj AND fc2.id_bie = fci2.comercio_id_bie
             WHERE fi2.circunscripcion::text = m.first_circ
               AND fi2.seccion              = m.first_secc
               AND fi2.quinta::text         = m.first_quin
               AND fi2.fraccion::text       = m.first_frac
               AND fi2.manzana              = m.first_manz
             ORDER BY n
             LIMIT 5
           ) AS top5)
        ELSE COUNT(DISTINCT fc.id_bie)::text
      END AS label
    FROM geo_gmat_manzana m
    JOIN fal_inmueble fi
      ON  fi.circunscripcion::text = m.first_circ
      AND fi.seccion               = m.first_secc
      AND fi.quinta::text          = m.first_quin
      AND fi.fraccion::text        = m.first_frac
      AND fi.manzana               = m.first_manz
    JOIN fal_comercio_inmueble fci
      ON fci.inmueble_id_suj = fi.id_suj AND fci.inmueble_id_bie = fi.id_bie
    JOIN fal_comercio fc
      ON fc.id_suj = fci.comercio_id_suj AND fc.id_bie = fci.comercio_id_bie
    WHERE z >= 14
      AND ST_Intersects(
            ST_Transform(m.geom, 3857),
            ST_TileEnvelope(z, x, y, margin => 64.0 / 4096)
          )
    GROUP  BY m.id, m.lblmza, m.geom
    HAVING COUNT(DISTINCT fc.id_bie) > 0
  ) AS tile
  WHERE geom IS NOT NULL
$function$;
