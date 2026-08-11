-- DIAGNOSTICO READ-ONLY - no modifica datos ni esquema
-- =============================================================================
-- PRECHECK DE ROLLBACK - fal_persona: documento opcional
-- Script: verify-fal-persona-documento-opcional-001-rollback-precheck.sql
-- Trabajo: JDBC-DOMINIO-EDA-WAVE-1A2-B1.5B0-B
-- Decision: DECISION_DDL-PERS-01, "Ventana de rollback y punto de no retorno"
--
-- Se ejecuta ANTES de
-- database/ddl/alter-fal-persona-documento-opcional-001-rollback.sql.
--
-- No verifica la misma precondicion que el precheck del forward: verifica la
-- contraria. El forward exige que ninguna fila viole el modelo nuevo; el
-- rollback exige que ninguna fila necesite el modelo nuevo para existir.
--
-- VENTANA DE ROLLBACK
-- El retorno a INT UNSIGNED NOT NULL / SMALLINT NOT NULL es seguro solo
-- mientras:
--   1. ninguna fila tenga tipo_documento NULL;
--   2. ninguna fila tenga nro_doc NULL;
--   3. ninguna fila tenga nro_doc con un caracter no numerico;
--   4. ningun nro_doc quede fuera de 1..99999999;
--   5. ningun prefijo quede fuera del rango antiguo 0..99.
--
-- La primera fila que incumpla cualquiera de las cinco condiciones cierra la
-- ventana: INT UNSIGNED NOT NULL no puede representarla y el retorno pasaria a
-- ser con perdida. En ese caso NO ejecutar el rollback; el retorno exige
-- restituir desde el backup logico del paso 2 del forward.
--
-- Uso: si `estado` <> 'VENTANA_ABIERTA' en alguna fila de la seccion 2,
-- DETENER.
-- =============================================================================

USE sb_faltas_db;

-- -----------------------------------------------------------------------------
-- 1. Identidad del entorno y forma fisica de partida.
-- -----------------------------------------------------------------------------
SELECT
    DATABASE()                                                    AS base_actual,
    CASE WHEN DATABASE() = 'sb_faltas_db' THEN 'SI' ELSE 'NO' END AS base_esperada;

SELECT column_name, column_type, is_nullable
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name   = 'fal_persona'
   AND column_name IN ('tipo_documento', 'nro_doc', 'prefijo_cuit_cuil', 'doc_key')
 ORDER BY ordinal_position;

-- -----------------------------------------------------------------------------
-- 2. Controles bloqueantes de la ventana. Toda fila debe devolver cantidad = 0.
-- -----------------------------------------------------------------------------
SELECT control, cantidad,
       CASE WHEN cantidad = 0 THEN 'VENTANA_ABIERTA' ELSE 'VENTANA_CERRADA' END AS estado
FROM (
    SELECT 'R01 tipo_documento NULL: el modelo viejo no lo admite' AS control,
           COUNT(*) AS cantidad
      FROM fal_persona WHERE tipo_documento IS NULL
    UNION ALL SELECT 'R02 nro_doc NULL: el modelo viejo no lo admite', COUNT(*)
      FROM fal_persona WHERE nro_doc IS NULL
    UNION ALL SELECT 'R03 nro_doc alfanumerico: INT UNSIGNED no puede representarlo', COUNT(*)
      FROM fal_persona WHERE nro_doc IS NOT NULL AND nro_doc NOT REGEXP '^[0-9]+$'
    UNION ALL SELECT 'R04 nro_doc fuera de 1..99999999', COUNT(*)
      FROM fal_persona
     WHERE nro_doc REGEXP '^[0-9]+$'
       AND (CAST(nro_doc AS UNSIGNED) < 1 OR CAST(nro_doc AS UNSIGNED) > 99999999)
    UNION ALL SELECT 'R05 nro_doc con cero inicial: el retorno seria con perdida', COUNT(*)
      FROM fal_persona WHERE nro_doc REGEXP '^0'
    UNION ALL SELECT 'R06 prefijo fuera del rango antiguo 0..99', COUNT(*)
      FROM fal_persona WHERE prefijo_cuit_cuil IS NOT NULL
                         AND prefijo_cuit_cuil NOT BETWEEN 0 AND 99
    UNION ALL SELECT 'R07 duplicados bajo la doc_key antigua', COUNT(*)
      FROM (SELECT tipo_documento,
                   CASE WHEN tipo_documento IN (2, 3)
                          THEN CONCAT(LPAD(COALESCE(prefijo_cuit_cuil, 0), 2, 0),
                                      LPAD(COALESCE(nro_doc, '0'), 8, 0),
                                      COALESCE(digito_verificador, 0))
                        ELSE CONCAT(tipo_documento, '-', nro_doc) END AS vieja_doc_key
              FROM fal_persona
             GROUP BY 1, 2 HAVING COUNT(*) > 1) duplicados
) controles
ORDER BY control;

-- -----------------------------------------------------------------------------
-- 3. PK tecnicas de las filas que cierran la ventana. Debe devolver cero filas.
--    No expone ningun documento en claro.
-- -----------------------------------------------------------------------------
SELECT id                                   AS pk_tecnica,
       (tipo_documento IS NULL)             AS sin_tipo,
       (nro_doc IS NULL)                    AS sin_numero,
       CHAR_LENGTH(nro_doc)                 AS longitud_nro_doc,
       (nro_doc NOT REGEXP '^[0-9]+$')      AS alfanumerico,
       (nro_doc REGEXP '^0')                AS con_cero_inicial
  FROM fal_persona
 WHERE tipo_documento IS NULL
    OR nro_doc IS NULL
    OR nro_doc NOT REGEXP '^[0-9]+$'
    OR nro_doc REGEXP '^0'
 ORDER BY id;

-- =============================================================================
-- FIN DEL PRECHECK DE ROLLBACK - fal_persona documento opcional
-- =============================================================================
