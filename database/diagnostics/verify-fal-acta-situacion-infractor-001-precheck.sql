-- DIAGNOSTICO READ-ONLY - no modifica datos ni esquema
-- =============================================================================
-- PRECHECK - fal_acta: situacion de identificacion del infractor
-- Script: verify-fal-acta-situacion-infractor-001-precheck.sql
-- Trabajo: JDBC-DOMINIO-EDA-WAVE-1A2-B1.5B1
-- Decision: DECISION_DDL-ACTA-INFR-01
--
-- Se ejecuta ANTES de
-- database/ddl/alter-fal-acta-situacion-infractor-001-forward.sql.
--
-- El plan NO asume la tabla vacia: que hoy tenga cero filas es un hecho
-- coyuntural verificado, no un contrato. Estas consultas deben ser correctas
-- con filas.
--
-- Uso: revisar el resultado de la seccion 3. Si alguna fila devuelve
-- 'BLOQUEADO', DETENER y remediar antes de ejecutar el forward. Este script no
-- clasifica por inferencia, no crea personas y no sanea datos.
--
-- Codigos de situacion: 1..6, verificados contra
-- SituacionIdentificacionInfractor.java y contra
-- 10-domain/states-events-catalogs.md. Prohibido derivarlos de ordinal().
--
-- Codigos documentales: el nombre fisico canonico es tipo_documento.
-- tipo_doc esta prohibido como nombre de columna y no aparece aqui.
--
-- Ninguna consulta expone un documento en claro ni datos personales: se
-- informan cantidades y PK tecnicas.
-- =============================================================================

USE sb_faltas_db;

-- -----------------------------------------------------------------------------
-- 1. Identidad del entorno y forma fisica de partida.
-- -----------------------------------------------------------------------------
SELECT
    DATABASE()                                                    AS base_actual,
    VERSION()                                                     AS version_motor,
    @@foreign_key_checks                                          AS fk_checks,
    CASE WHEN DATABASE() = 'sb_faltas_db' THEN 'SI' ELSE 'NO' END AS base_esperada;

SELECT column_name, column_type, is_nullable, column_default, extra
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name   = 'fal_acta'
   AND column_name IN ('situacion_identificacion_infractor', 'id_persona_infractor')
 ORDER BY ordinal_position;

SELECT COUNT(*) AS columnas_fal_acta
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'fal_acta';

-- -----------------------------------------------------------------------------
-- 2. Volumen y clasificacion del backfill.
--    Con la columna todavia NOT NULL no existe una tercera rama posible: toda
--    fila tiene persona y esa persona tiene documento o no lo tiene.
-- -----------------------------------------------------------------------------
SELECT
    COUNT(*)                                                        AS filas_fal_acta,
    SUM(p.tipo_documento IS NOT NULL AND p.nro_doc IS NOT NULL)     AS clasifican_completo_1,
    SUM(p.tipo_documento IS NULL     AND p.nro_doc IS NULL)         AS clasifican_parcial_2,
    SUM(p.id IS NULL)                                               AS sin_persona_resoluble
  FROM fal_acta a
  LEFT JOIN fal_persona p ON p.id = a.id_persona_infractor;

-- -----------------------------------------------------------------------------
-- 3. Controles bloqueantes. Toda fila debe devolver cantidad = 0.
-- -----------------------------------------------------------------------------
SELECT control, cantidad,
       CASE WHEN cantidad = 0 THEN 'LISTO_PARA_FORWARD' ELSE 'BLOQUEADO' END AS estado
FROM (
    SELECT 'P01 entorno inesperado (base/version/engine/collation)' AS control,
           SUM(CASE
                 WHEN DATABASE() <> 'sb_faltas_db' THEN 1
                 WHEN t.engine <> 'InnoDB' THEN 1
                 WHEN t.table_collation <> 'utf8mb4_uca1400_ai_ci' THEN 1
                 WHEN VERSION() NOT LIKE '12.3.%' THEN 1
                 ELSE 0
               END) AS cantidad
      FROM information_schema.tables t
     WHERE t.table_schema = DATABASE() AND t.table_name = 'fal_acta'

    UNION ALL SELECT 'P02 la columna objetivo ya existe', COUNT(*)
      FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'fal_acta'
       AND column_name = 'situacion_identificacion_infractor'

    UNION ALL SELECT 'P03 alguno de los dos CHECK objetivo ya existe', COUNT(*)
      FROM information_schema.check_constraints
     WHERE constraint_schema = DATABASE() AND table_name = 'fal_acta'
       AND constraint_name IN ('chk_acta_situacion_infractor', 'chk_acta_infractor_coherente')

    UNION ALL SELECT 'P04 id_persona_infractor ya es nullable', COUNT(*)
      FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'fal_acta'
       AND column_name = 'id_persona_infractor' AND is_nullable = 'YES'

    UNION ALL SELECT 'P05 FK o indice canonicos ausentes o con otra semantica', (
        SELECT 2
             - (SELECT COUNT(*)
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
             - (SELECT COUNT(DISTINCT s.index_name)
                  FROM information_schema.statistics s
                 WHERE s.table_schema = DATABASE() AND s.table_name = 'fal_acta'
                   AND s.index_name = 'ix_fal_acta_persona'
                   AND s.column_name = 'id_persona_infractor'))

    UNION ALL SELECT 'P06 filas con id_persona_infractor NULL antes del ALTER', COUNT(*)
      FROM fal_acta WHERE id_persona_infractor IS NULL

    UNION ALL SELECT 'P07 actas con persona inexistente (huerfanas)', COUNT(*)
      FROM fal_acta a
     WHERE a.id_persona_infractor IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM fal_persona p WHERE p.id = a.id_persona_infractor)

    UNION ALL SELECT 'P08 persona vinculada con solo uno de tipo_documento/nro_doc', COUNT(*)
      FROM fal_acta a
      JOIN fal_persona p ON p.id = a.id_persona_infractor
     WHERE (p.tipo_documento IS NULL) <> (p.nro_doc IS NULL)

    UNION ALL SELECT 'P09 persona completa con componentes incompatibles con sus CHECK', COUNT(*)
      FROM fal_acta a
      JOIN fal_persona p ON p.id = a.id_persona_infractor
     WHERE p.tipo_documento IS NOT NULL
       AND (p.tipo_documento NOT IN (1, 2, 3, 4, 5, 9)
            OR p.nro_doc NOT REGEXP '^[0-9A-Z]{1,12}$'
            OR (p.tipo_documento IN (2, 3)
                AND (p.prefijo_cuit_cuil IS NULL
                     OR p.digito_verificador IS NULL
                     OR p.nro_doc NOT REGEXP '^[0-9]{8}$'))
            OR (p.tipo_documento NOT IN (2, 3)
                AND (p.prefijo_cuit_cuil IS NOT NULL OR p.digito_verificador IS NOT NULL))
            OR (p.tipo_documento = 1 AND p.nro_doc NOT REGEXP '^[1-9][0-9]{0,7}$'))

    UNION ALL SELECT 'P10 nombres de constraint objetivo ocupados en otra tabla', COUNT(*)
      FROM information_schema.table_constraints
     WHERE constraint_schema = DATABASE()
       AND constraint_name IN ('chk_acta_situacion_infractor', 'chk_acta_infractor_coherente')
       AND table_name <> 'fal_acta'

    UNION ALL SELECT 'P11 triggers sobre fal_acta o fal_persona en el camino del ALTER', COUNT(*)
      FROM information_schema.triggers
     WHERE trigger_schema = DATABASE()
       AND event_object_table IN ('fal_acta', 'fal_persona')
       AND event_manipulation IN ('INSERT', 'UPDATE')

    UNION ALL SELECT 'P12 filas que no clasifican en 1 ni en 2', COUNT(*)
      FROM fal_acta a
      LEFT JOIN fal_persona p ON p.id = a.id_persona_infractor
     WHERE NOT ((p.tipo_documento IS NOT NULL AND p.nro_doc IS NOT NULL)
                OR (p.tipo_documento IS NULL AND p.nro_doc IS NULL AND p.id IS NOT NULL))

    UNION ALL SELECT 'P13 foreign_key_checks distinto de 1',
           CASE WHEN @@foreign_key_checks = 1 THEN 0 ELSE 1 END

    UNION ALL SELECT 'P14 rollback no seguro segun el estado previo', (
        SELECT COUNT(*) FROM fal_acta WHERE id_persona_infractor IS NULL)
) AS controles
ORDER BY control;

-- =============================================================================
-- FIN DEL PRECHECK - fal_acta situacion de identificacion del infractor
-- =============================================================================
