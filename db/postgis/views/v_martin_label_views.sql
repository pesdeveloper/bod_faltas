-- =============================================================================
-- Vistas materializadas para fuentes de etiquetas en Martin (MVT)
-- Base: sb_malvinas_geo (PostgreSQL/PostGIS)
-- Proposito: Proveer puntos de etiqueta (centroides) para capas del mapa
--            que en PostGIS son poligonos pero MapLibre necesita como puntos.
-- =============================================================================
--
-- PROCESO DE ACTUALIZACION:
-- Cuando las tablas base (geo_gmat_*, geo_qmat_parcela) se actualizan
-- desde el sistema GIS fuente, ejecutar:
--
--   REFRESH MATERIALIZED VIEW CONCURRENTLY v_martin_localidad_label;
--   REFRESH MATERIALIZED VIEW CONCURRENTLY v_martin_manzana_label;
--   REFRESH MATERIALIZED VIEW CONCURRENTLY v_martin_parcela_label;
--   REFRESH MATERIALIZED VIEW CONCURRENTLY v_martin_espacio_verde_label;
--   REFRESH MATERIALIZED VIEW CONCURRENTLY v_martin_ferrocarril_estaciones_label;
--
-- Martin no necesita reinicio para reflejar cambios de datos.
-- Solo reiniciar Martin si se agregan nuevas tablas o vistas.
-- =============================================================================

-- 1. Localidades: label point
DROP MATERIALIZED VIEW IF EXISTS v_martin_localidad_label;
CREATE MATERIALIZED VIEW v_martin_localidad_label AS
SELECT id, localidad, ST_PointOnSurface(geom) AS geom
FROM geo_gmat_localidad;
CREATE UNIQUE INDEX ON v_martin_localidad_label (id);

-- 2. Manzanas: label point con lblmza
DROP MATERIALIZED VIEW IF EXISTS v_martin_manzana_label;
CREATE MATERIALIZED VIEW v_martin_manzana_label AS
SELECT id, lblmza, ST_PointOnSurface(geom) AS geom
FROM geo_gmat_manzana;
CREATE UNIQUE INDEX ON v_martin_manzana_label (id);

-- 3. Parcelas: label point con nomenclatura catastral computada
--    Campos con valor '0' en quint/fracc significan ausencia y se omiten.
--    Regla: {circunsc}-{secc}[-Q{quint}][-F{fracc}]-M{manz}-P{parc}
DROP MATERIALIZED VIEW IF EXISTS v_martin_parcela_label;
CREATE MATERIALIZED VIEW v_martin_parcela_label AS
SELECT
    id,
    parc,
    manz,
    CONCAT(
        circunsc, '-', secc,
        CASE WHEN quint != '0' THEN '-Q' || quint ELSE '' END,
        CASE WHEN fracc != '0' THEN '-F' || fracc ELSE '' END,
        '-M', manz, '-P', parc
    ) AS nomenclatura_label,
    ST_PointOnSurface(geom) AS geom
FROM geo_qmat_parcela;
CREATE UNIQUE INDEX ON v_martin_parcela_label (id);

-- 4. Espacios verdes: label point con nombre
DROP MATERIALIZED VIEW IF EXISTS v_martin_espacio_verde_label;
CREATE MATERIALIZED VIEW v_martin_espacio_verde_label AS
SELECT id, nombre, ST_PointOnSurface(geom) AS geom
FROM geo_gmat_espacio_verde;
CREATE UNIQUE INDEX ON v_martin_espacio_verde_label (id);

-- 5. Estaciones ferrocarril: centroide desde MULTIPOLYGON
--    La tabla almacena poligonos de estaciones. MapLibre necesita punto.
DROP MATERIALIZED VIEW IF EXISTS v_martin_ferrocarril_estaciones_label;
CREATE MATERIALIZED VIEW v_martin_ferrocarril_estaciones_label AS
SELECT id, estacion, ST_PointOnSurface(geom) AS geom
FROM geo_gmat_ferrocarril_estaciones;
CREATE UNIQUE INDEX ON v_martin_ferrocarril_estaciones_label (id);