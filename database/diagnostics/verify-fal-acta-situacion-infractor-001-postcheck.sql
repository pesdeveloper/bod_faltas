-- DIAGNOSTICO READ-ONLY - no modifica datos ni esquema
-- =============================================================================
-- POSTCHECK - fal_acta: situacion de identificacion del infractor
-- Script: verify-fal-acta-situacion-infractor-001-postcheck.sql
-- Trabajo: JDBC-DOMINIO-EDA-WAVE-1A2-B1.5B1
-- Decision: DECISION_DDL-ACTA-INFR-01
--
-- Se ejecuta DESPUES de
-- database/ddl/alter-fal-acta-situacion-infractor-001-forward.sql.
--
-- Uso: si alguna fila de la seccion 4 devuelve 'DIVERGENCIA', el forward no
-- quedo en el estado objetivo y no debe declararse aplicado.
-- =============================================================================

USE sb_faltas_db;

-- -----------------------------------------------------------------------------
-- 1. Forma fisica objetivo.
-- -----------------------------------------------------------------------------
SELECT ordinal_position, column_name, column_type, is_nullable, column_default,
       extra, column_comment
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name   = 'fal_acta'
   AND column_name IN ('situacion_identificacion_infractor', 'id_persona_infractor')
 ORDER BY ordinal_position;

-- -----------------------------------------------------------------------------
-- 2. Los cuatro CHECK de fal_acta.
-- -----------------------------------------------------------------------------
SELECT constraint_name, check_clause
  FROM information_schema.check_constraints
 WHERE constraint_schema = DATABASE() AND table_name = 'fal_acta'
 ORDER BY constraint_name;

-- -----------------------------------------------------------------------------
-- 3. FK e indice conservados con la misma semantica.
-- -----------------------------------------------------------------------------
SELECT k.constraint_name, k.column_name, k.referenced_table_name,
       k.referenced_column_name, r.update_rule, r.delete_rule
  FROM information_schema.key_column_usage k
  JOIN information_schema.referential_constraints r
    ON r.constraint_schema = k.constraint_schema AND r.constraint_name = k.constraint_name
 WHERE k.constraint_schema = DATABASE() AND k.table_name = 'fal_acta'
   AND k.constraint_name = 'fk_acta_persona_infractor';

SELECT index_name, seq_in_index, column_name, non_unique
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'fal_acta'
   AND index_name = 'ix_fal_acta_persona';

-- -----------------------------------------------------------------------------
-- 4. Controles de conformidad. Toda fila debe devolver 'OK'.
-- -----------------------------------------------------------------------------
SELECT control, cantidad,
       CASE WHEN cantidad = 0 THEN 'OK' ELSE 'DIVERGENCIA' END AS estado
FROM (
    SELECT 'Q01 fal_acta no tiene 57 columnas' AS control,
           ABS(57 - (SELECT COUNT(*) FROM information_schema.columns
                      WHERE table_schema = DATABASE() AND table_name = 'fal_acta')) AS cantidad

    UNION ALL SELECT 'Q02 la columna objetivo no es SMALLINT NOT NULL sin default',
           (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = DATABASE() AND table_name = 'fal_acta'
               AND column_name = 'situacion_identificacion_infractor'
               AND NOT (column_type = 'smallint(6)'
                        AND is_nullable = 'NO'
                        AND column_default IS NULL
                        AND extra = ''))
           + (SELECT 1 - COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'fal_acta'
                 AND column_name = 'situacion_identificacion_infractor')

    UNION ALL SELECT 'Q03 id_persona_infractor no es BIGINT NULL',
           (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = DATABASE() AND table_name = 'fal_acta'
               AND column_name = 'id_persona_infractor'
               AND NOT (column_type = 'bigint(20)' AND is_nullable = 'YES'))

    UNION ALL SELECT 'Q04 fal_acta no tiene exactamente 4 CHECK',
           ABS(4 - (SELECT COUNT(*) FROM information_schema.check_constraints
                     WHERE constraint_schema = DATABASE() AND table_name = 'fal_acta'))

    UNION ALL SELECT 'Q05 falta alguno de los dos CHECK objetivo',
           2 - (SELECT COUNT(*) FROM information_schema.check_constraints
                 WHERE constraint_schema = DATABASE() AND table_name = 'fal_acta'
                   AND constraint_name IN ('chk_acta_situacion_infractor',
                                           'chk_acta_infractor_coherente'))

    UNION ALL SELECT 'Q06 FK fk_acta_persona_infractor ausente o sin RESTRICT/RESTRICT',
           1 - (SELECT COUNT(*)
                  FROM information_schema.referential_constraints r
                  JOIN information_schema.key_column_usage k
                    ON k.constraint_schema = r.constraint_schema
                   AND k.constraint_name   = r.constraint_name
                 WHERE r.constraint_schema = DATABASE()
                   AND r.constraint_name   = 'fk_acta_persona_infractor'
                   AND k.table_name        = 'fal_acta'
                   AND k.column_name       = 'id_persona_infractor'
                   AND k.referenced_table_name = 'fal_persona'
                   AND r.update_rule = 'RESTRICT' AND r.delete_rule = 'RESTRICT')

    UNION ALL SELECT 'Q07 indice ix_fal_acta_persona ausente',
           1 - (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE() AND table_name = 'fal_acta'
                   AND index_name = 'ix_fal_acta_persona'
                   AND column_name = 'id_persona_infractor')

    UNION ALL SELECT 'Q08 filas con situacion fuera de 1..6', (
        SELECT COUNT(*) FROM fal_acta
         WHERE situacion_identificacion_infractor NOT BETWEEN 1 AND 6)

    UNION ALL SELECT 'Q09 filas que violan la matriz situacion/persona', (
        SELECT COUNT(*) FROM fal_acta
         WHERE NOT ((situacion_identificacion_infractor IN (1,2)
                     AND id_persona_infractor IS NOT NULL)
                    OR (situacion_identificacion_infractor IN (3,4,5,6)
                        AND id_persona_infractor IS NULL)))

    UNION ALL SELECT 'Q10 actas identificadas con persona inexistente', (
        SELECT COUNT(*) FROM fal_acta a
         WHERE a.id_persona_infractor IS NOT NULL
           AND NOT EXISTS (SELECT 1 FROM fal_persona p WHERE p.id = a.id_persona_infractor))

    UNION ALL SELECT 'Q11 fila 1 cuya persona no tiene documento completo', (
        SELECT COUNT(*) FROM fal_acta a
          JOIN fal_persona p ON p.id = a.id_persona_infractor
         WHERE a.situacion_identificacion_infractor = 1
           AND (p.tipo_documento IS NULL OR p.nro_doc IS NULL))

    UNION ALL SELECT 'Q12 el COMMENT de la columna no cita la decision',
           (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = DATABASE() AND table_name = 'fal_acta'
               AND column_name = 'situacion_identificacion_infractor'
               AND column_comment NOT LIKE '%DECISION_DDL-ACTA-INFR-01%')
) AS controles
ORDER BY control;

-- -----------------------------------------------------------------------------
-- 5. Distribucion resultante del backfill.
-- -----------------------------------------------------------------------------
SELECT situacion_identificacion_infractor AS situacion,
       COUNT(*)                           AS filas,
       SUM(id_persona_infractor IS NOT NULL) AS con_persona
  FROM fal_acta
 GROUP BY situacion_identificacion_infractor
 ORDER BY situacion;

-- =============================================================================
-- FIN DEL POSTCHECK - fal_acta situacion de identificacion del infractor
-- =============================================================================
