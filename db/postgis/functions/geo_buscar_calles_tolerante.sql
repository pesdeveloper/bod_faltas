-- =============================================================================
-- FUNCTION: geo_buscar_calles_tolerante(p_query text, p_id_loc text, p_limit integer)
-- Base: sb_malvinas_geo (PostgreSQL 15.2 / PostGIS 3.3)
-- DDL extraído de pg_get_functiondef el 2026-07-30
-- Verificado: EXISTE. No es fuente Martin MVT; es llamada desde el backend Java.
--
-- Dependencias (deben existir antes de crear esta función):
--   funciones:  geo_normalizar_termino(text), geo_tokens_sorted(text)
--   tabla:      geo_malv_calle_indice_busqueda
--                 columnas relevantes: si_activo, termino_normalizado,
--                   termino_tokens_sorted, ccalle, id_loc, localidad_nombre,
--                   nombre_vigente, termino_original, tipo_termino, prioridad
--
-- Algoritmo de scoring:
--   1.00  exact    → termino_normalizado = query normalizado
--   0.92  reorder  → tokens reordenados coinciden
--   0.88  prefix   → empieza con query
--   0.75  substring → contiene query
--   0.xx  trigram  → similarity pg_trgm > 0.25
--   Boost x1.3 cuando el id_loc coincide con p_id_loc
--   Umbral final: score > 0.20
-- =============================================================================

CREATE OR REPLACE FUNCTION public.geo_buscar_calles_tolerante(
    p_query  text,
    p_id_loc text    DEFAULT NULL,
    p_limit  integer DEFAULT 5
)
RETURNS TABLE(
    ccalle           text,
    id_loc           text,
    localidad_nombre text,
    nombre_vigente   text,
    termino_original text,
    tipo_termino     text,
    match_type       text,
    score            double precision
)
LANGUAGE sql
STABLE
AS $function$
  WITH q AS (
    SELECT
      geo_normalizar_termino(p_query) AS norm,
      geo_tokens_sorted(geo_normalizar_termino(p_query)) AS sorted
  ),
  raw AS (
    SELECT
      idx.ccalle,
      idx.id_loc,
      idx.localidad_nombre,
      idx.nombre_vigente,
      idx.termino_original,
      idx.tipo_termino,
      idx.prioridad,
      q.norm,
      q.sorted,
      CASE
        WHEN idx.termino_normalizado = q.norm                  THEN 1.00
        WHEN idx.termino_tokens_sorted = q.sorted              THEN 0.92
        WHEN idx.termino_normalizado LIKE q.norm || '%'        THEN 0.88
        WHEN idx.termino_normalizado LIKE '%' || q.norm || '%' THEN 0.75
        ELSE GREATEST(
          similarity(idx.termino_normalizado, q.norm),
          similarity(idx.termino_tokens_sorted, q.sorted)
        )
      END AS base_score,
      CASE
        WHEN idx.termino_normalizado = q.norm                  THEN 'EXACT'
        WHEN idx.termino_tokens_sorted = q.sorted              THEN 'TOKEN_REORDER'
        WHEN idx.termino_normalizado LIKE q.norm || '%'        THEN 'PREFIX'
        WHEN idx.termino_normalizado LIKE '%' || q.norm || '%' THEN 'TOKENS'
        ELSE 'TRIGRAM'
      END AS match_type
    FROM geo_malv_calle_indice_busqueda idx, q
    WHERE idx.si_activo = TRUE
      AND (
        idx.termino_normalizado = q.norm
        OR idx.termino_tokens_sorted = q.sorted
        OR idx.termino_normalizado LIKE q.norm || '%'
        OR idx.termino_normalizado LIKE '%' || q.norm || '%'
        OR similarity(idx.termino_normalizado, q.norm) > 0.25
        OR similarity(idx.termino_tokens_sorted, q.sorted) > 0.25
      )
  ),
  scored AS (
    SELECT
      r.ccalle, r.id_loc, r.localidad_nombre, r.nombre_vigente,
      r.termino_original, r.tipo_termino, r.match_type, r.prioridad,
      r.base_score * CASE WHEN p_id_loc IS NOT NULL AND r.id_loc = p_id_loc THEN 1.3 ELSE 1.0 END
        AS final_score
    FROM raw r
    WHERE r.base_score > 0.20
  )
  SELECT DISTINCT ON (s.ccalle, s.id_loc)
    s.ccalle::TEXT,
    s.id_loc::TEXT,
    s.localidad_nombre,
    s.nombre_vigente,
    s.termino_original,
    s.tipo_termino,
    s.match_type,
    s.final_score
  FROM scored s
  ORDER BY s.ccalle, s.id_loc, s.final_score DESC, s.prioridad ASC
  LIMIT p_limit
$function$;
