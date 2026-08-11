-- =============================================================================
-- ALTER — Unificacion del tipo fisico de coordenadas geograficas
-- Script: alter-coord-type-unify-001-forward.sql
-- Trabajo: MARIADB-COORD-TYPE-UNIFY-001
--
-- MOTIVO
-- El mismo dato —un par latitud/longitud en grados decimales— estaba tipado de
-- dos formas distintas:
--
--   fal_persona_domicilio.lat / .lon   DECIMAL(10,7)   <- decision cerrada
--   fal_acta.lat_captura / .lon_captura DECIMAL(12,8)  <- desviacion
--   fal_acta.lat_infr / .lon_infr       DECIMAL(12,8)  <- desviacion
--
-- DECIMAL(10,7) es la decision humana cerrada para coordenadas
-- (SPEC-MODEL-DDL-CLOSURE-001), verificada por el guardrail PHY
-- "fal_persona_domicilio: lat/lon son DECIMAL(10,7) - no DECIMAL(12,8)".
-- mariadb-logical-model.md pedia explicitamente coherencia con esa tabla; el
-- baseline ejecutado no la siguio para fal_acta.
--
-- Este script alinea fal_acta con la decision cerrada.
--
-- POR QUE DECIMAL(10,7) Y NO DECIMAL(12,8)
--   3 digitos enteros bastan y sobran para el rango real de una coordenada
--   (latitud -90..90, longitud -180..180). DECIMAL(12,8) reservaba un cuarto
--   digito entero que admite valores imposibles (hasta 9999 grados).
--   7 decimales dan una resolucion de ~1,1 cm, suficiente para el lugar del
--   hecho y para un domicilio.
--
-- FUERA DE ALCANCE
--   precision_captura_m DECIMAL(8,2) NO se toca: son metros, no una coordenada.
--   (8,2) admite hasta 999.999,99 m con resolucion de centimetro, que es el
--   tipo correcto para una precision de GPS.
--
-- Estado: DDL_EXECUTED — 2026-08-03 sobre sb_faltas_db
-- Motor: InnoDB | Charset: utf8mb4 | Collation: utf8mb4_uca1400_ai_ci
-- Precondicion: create-bod-faltas-domain.sql ya ejecutado (fal_acta existe).
--
-- PRECHECK OBLIGATORIO
-- La conversion a DECIMAL(10,7) reduce un digito decimal y un digito entero.
-- Antes de ejecutar, la consulta de la seccion 1 debe devolver 0 en las cuatro
-- columnas: cualquier valor fuera de rango o con mas de 7 decimales
-- significativos seria truncado por MariaDB.
--
-- ROLLBACK: database/ddl/alter-coord-type-unify-001-rollback.sql
-- =============================================================================

USE sb_faltas_db;

-- -----------------------------------------------------------------------------
-- 1. PRECHECK — debe devolver 0 en las cuatro columnas antes de continuar
-- -----------------------------------------------------------------------------
SELECT
    COUNT(*)                                                              AS filas_totales,
    SUM(lat_captura IS NOT NULL
        AND (ABS(lat_captura) >= 1000 OR lat_captura <> ROUND(lat_captura, 7))) AS lat_captura_no_convertible,
    SUM(lon_captura IS NOT NULL
        AND (ABS(lon_captura) >= 1000 OR lon_captura <> ROUND(lon_captura, 7))) AS lon_captura_no_convertible,
    SUM(lat_infr IS NOT NULL
        AND (ABS(lat_infr) >= 1000 OR lat_infr <> ROUND(lat_infr, 7)))          AS lat_infr_no_convertible,
    SUM(lon_infr IS NOT NULL
        AND (ABS(lon_infr) >= 1000 OR lon_infr <> ROUND(lon_infr, 7)))          AS lon_infr_no_convertible
FROM fal_acta;

-- -----------------------------------------------------------------------------
-- 2. ALTER — cuatro columnas de fal_acta a DECIMAL(10,7)
--    MODIFY COLUMN conserva la posicion ordinal de cada columna.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_acta
    MODIFY COLUMN lat_captura DECIMAL(10,7) NULL
        COMMENT 'Latitud del punto de captura (grados decimales; SPEC-MODEL-DDL-CLOSURE-001: DECIMAL(10,7)).',
    MODIFY COLUMN lon_captura DECIMAL(10,7) NULL
        COMMENT 'Longitud del punto de captura (grados decimales; SPEC-MODEL-DDL-CLOSURE-001: DECIMAL(10,7)).',
    MODIFY COLUMN lat_infr DECIMAL(10,7) NULL
        COMMENT 'Latitud final del lugar del hecho (grados decimales; SPEC-MODEL-DDL-CLOSURE-001: DECIMAL(10,7)).',
    MODIFY COLUMN lon_infr DECIMAL(10,7) NULL
        COMMENT 'Longitud final del lugar del hecho (grados decimales; SPEC-MODEL-DDL-CLOSURE-001: DECIMAL(10,7)).';

-- -----------------------------------------------------------------------------
-- 3. POSTCHECK — las seis columnas de coordenadas del dominio, un unico tipo
-- -----------------------------------------------------------------------------
SELECT table_name, column_name, column_type, is_nullable
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND ((table_name = 'fal_acta' AND column_name IN ('lat_captura','lon_captura','lat_infr','lon_infr'))
     OR (table_name = 'fal_persona_domicilio' AND column_name IN ('lat','lon')))
 ORDER BY table_name, column_name;

-- precision_captura_m debe seguir siendo DECIMAL(8,2): son metros.
SELECT table_name, column_name, column_type
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'fal_acta'
   AND column_name = 'precision_captura_m';
