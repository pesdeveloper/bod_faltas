-- =============================================================================
-- FUNCTION: comercios_parcela(z integer, x integer, y integer)
-- Base: sb_malvinas_geo (PostgreSQL 15.2 / PostGIS 3.3)
-- DDL extraído de pg_get_functiondef el 2026-07-30
-- Verificado: EXISTE y publicado en Martin como fuente 'comercios_parcela'
--
-- Dependencias:
--   tablas: geo_qmat_parcela, fal_inmueble, fal_comercio_inmueble, fal_comercio
--   SRID base: 22195 (faja 5 Gauss-Krüger Argentina)
--
-- Post-despliegue:
--   1. Reiniciar Martin para que descubra la función.
--   2. Verificar: GET /catalog  → debe aparecer "comercios_parcela"
--   3. Verificar: GET /comercios_parcela/{z}/{x}/{y}  → debe devolver 200
-- =============================================================================

CREATE OR REPLACE FUNCTION public.comercios_parcela(z integer, x integer, y integer)
  RETURNS bytea
  LANGUAGE sql
  STABLE PARALLEL SAFE
AS $function$
  WITH
  bounds AS (
    SELECT ST_TileEnvelope(z, x, y) AS env
  ),
  parcelas AS (
    SELECT
      p.geom,
      p.manz,
      p.parc,
      COUNT(DISTINCT c.id_bie)   AS n_comercios,
      MIN(c.nombre_fantasia)     AS primer_nombre
    FROM geo_qmat_parcela AS p
    INNER JOIN fal_inmueble AS i
        ON i.circunscripcion::CHAR(1) = p.circunsc::CHAR(1)
       AND i.seccion   = p.secc
       AND i.quinta    = p.quint
       AND i.fraccion  = p.fracc
       AND i.manzana   = p.manz
       AND i.parcela   = p.parc
    INNER JOIN fal_comercio_inmueble AS ci
        ON ci.inmueble_id_suj = i.id_suj
       AND ci.inmueble_id_bie = i.id_bie
    INNER JOIN fal_comercio AS c
        ON c.id_suj = ci.comercio_id_suj
       AND c.id_bie = ci.comercio_id_bie
    WHERE p.manz != '0'
      AND ST_Intersects(
            p.geom,
            ST_Transform((SELECT env FROM bounds), 22195)
          )
    GROUP BY p.geom, p.manz, p.parc
  ),
  mvt_rows AS (
    SELECT
      ST_AsMVTGeom(
        ST_Transform(
          ST_Translate(
            ST_Centroid(geom),
            0.0,
            -(ST_YMax(geom) - ST_YMin(geom)) * 0.28
          ),
          3857
        ),
        (SELECT env FROM bounds),
        4096, 256, true
      ) AS geom,
      manz,
      parc,
      n_comercios,
      CASE
        WHEN n_comercios = 1 THEN primer_nombre
        ELSE n_comercios::text
      END AS label
    FROM parcelas
  )
  SELECT ST_AsMVT(mvt_rows, 'comercios_parcela', 4096, 'geom')
  FROM mvt_rows
  WHERE geom IS NOT NULL
$function$;
