-- DIAGNOSTICO READ-ONLY - no modifica datos ni esquema
-- =============================================================================
-- ROLLBACK-PRECHECK - fal_acta: situacion de identificacion del infractor
-- Script: verify-fal-acta-situacion-infractor-001-rollback-precheck.sql
-- Trabajo: JDBC-DOMINIO-EDA-WAVE-1A2-B1.5B1
-- Decision: DECISION_DDL-ACTA-INFR-01
--
-- Se ejecuta ANTES de
-- database/ddl/alter-fal-acta-situacion-infractor-001-rollback.sql.
--
-- La ventana del rollback se cierra con la primera acta en situacion 3..6,
-- porque esa fila tiene id_persona_infractor NULL y el rollback restituye la
-- columna a NOT NULL. El rollback NO inventa una persona para hacerlo posible:
-- si hay filas sin sujeto identificado, exige una estrategia humana de
-- reversion y no crea centinelas.
--
-- Uso: si alguna fila de la seccion 2 devuelve 'BLOQUEADO', DETENER.
-- =============================================================================

USE sb_faltas_db;

-- -----------------------------------------------------------------------------
-- 1. Estado del que se parte.
-- -----------------------------------------------------------------------------
SELECT
    DATABASE()                                                    AS base_actual,
    VERSION()                                                     AS version_motor,
    CASE WHEN DATABASE() = 'sb_faltas_db' THEN 'SI' ELSE 'NO' END AS base_esperada;

SELECT situacion_identificacion_infractor AS situacion,
       COUNT(*)                           AS filas,
       SUM(id_persona_infractor IS NULL)  AS con_persona_nula
  FROM fal_acta
 GROUP BY situacion_identificacion_infractor
 ORDER BY situacion;

-- -----------------------------------------------------------------------------
-- 2. Controles bloqueantes. Toda fila debe devolver cantidad = 0.
-- -----------------------------------------------------------------------------
SELECT control, cantidad,
       CASE WHEN cantidad = 0 THEN 'LISTO_PARA_ROLLBACK' ELSE 'BLOQUEADO' END AS estado
FROM (
    SELECT 'R01 filas con id_persona_infractor NULL' AS control, COUNT(*) AS cantidad
      FROM fal_acta WHERE id_persona_infractor IS NULL

    UNION ALL SELECT 'R02 filas en situacion 3..6 (sin sujeto identificado)', COUNT(*)
      FROM fal_acta WHERE situacion_identificacion_infractor IN (3, 4, 5, 6)

    UNION ALL SELECT 'R03 la columna objetivo no existe: nada que revertir',
           CASE WHEN EXISTS (
               SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'fal_acta'
                  AND column_name = 'situacion_identificacion_infractor')
           THEN 0 ELSE 1 END

    UNION ALL SELECT 'R04 falta alguno de los dos CHECK objetivo',
           2 - (SELECT COUNT(*) FROM information_schema.check_constraints
                 WHERE constraint_schema = DATABASE() AND table_name = 'fal_acta'
                   AND constraint_name IN ('chk_acta_situacion_infractor',
                                           'chk_acta_infractor_coherente'))

    UNION ALL SELECT 'R05 actas con persona inexistente (huerfanas)', COUNT(*)
      FROM fal_acta a
     WHERE a.id_persona_infractor IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM fal_persona p WHERE p.id = a.id_persona_infractor)
) AS controles
ORDER BY control;

-- =============================================================================
-- FIN DEL ROLLBACK-PRECHECK
-- =============================================================================
