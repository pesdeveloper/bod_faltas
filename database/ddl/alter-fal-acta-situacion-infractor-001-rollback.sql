-- =============================================================================
-- ROLLBACK - fal_acta: situacion de identificacion del infractor
-- Script: alter-fal-acta-situacion-infractor-001-rollback.sql
-- Trabajo: JDBC-DOMINIO-EDA-WAVE-1A2-B1.5B1
-- Decision: DECISION_DDL-ACTA-INFR-01
--
-- Revierte alter-fal-acta-situacion-infractor-001-forward.sql recorriendo el
-- orden inverso exacto:
--
--   forward                                 rollback
--   3 ADD COLUMN nullable                   5 DROP COLUMN
--   4 backfill                              (sin contraparte: es dato derivado)
--   6 MODIFY NOT NULL                       (absorbido por el DROP COLUMN)
--   7 MODIFY id_persona_infractor NULL      4 MODIFY id_persona_infractor NOT NULL
--   8 ADD chk_acta_situacion_infractor      3 DROP chk_acta_situacion_infractor
--   9 ADD chk_acta_infractor_coherente      2 DROP chk_acta_infractor_coherente
--
-- VENTANA
--   Solo es aplicable mientras ninguna fila tenga id_persona_infractor NULL, es
--   decir mientras no exista ninguna acta en situacion 3..6. Ese es
--   precisamente el estado que el forward hace representable, de modo que la
--   ventana se cierra con la primera acta sin sujeto identificado.
--
--   Este script NO inventa una persona para hacer NOT NULL posible. Si hay
--   filas 3..6, el rollback-precheck bloquea y la reversion exige una decision
--   humana sobre esas actas: no se crean centinelas.
--
-- ROLLBACK-PRECHECK OBLIGATORIO
--   database/diagnostics/verify-fal-acta-situacion-infractor-001-rollback-precheck.sql
--   Los cinco controles deben devolver 0.
--
-- EJECUCION
--   mariadb --abort-source-on-error ...
--
-- Motor: MariaDB 12.3.2 | InnoDB | utf8mb4 | utf8mb4_uca1400_ai_ci
-- =============================================================================

USE sb_faltas_db;

-- -----------------------------------------------------------------------------
-- 1. ROLLBACK-PRECHECK resumido. Ambas columnas deben devolver 0.
--    Con cualquiera distinta de 0, DETENER: el paso 4 fallaria y dejaria el
--    rollback parcial.
-- -----------------------------------------------------------------------------
SELECT
    COUNT(*)                                                AS filas_totales,
    SUM(id_persona_infractor IS NULL)                       AS persona_nula,
    SUM(situacion_identificacion_infractor IN (3,4,5,6))    AS sin_sujeto_identificado
  FROM fal_acta;

-- -----------------------------------------------------------------------------
-- 2. Baja del CHECK de coherencia. Primero el compuesto: depende de las dos
--    columnas y debe caer antes de que cualquiera cambie de nulabilidad.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_acta DROP CONSTRAINT chk_acta_infractor_coherente;

-- -----------------------------------------------------------------------------
-- 3. Baja del CHECK de dominio.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_acta DROP CONSTRAINT chk_acta_situacion_infractor;

-- -----------------------------------------------------------------------------
-- 4. Restitucion de id_persona_infractor a NOT NULL, con el COMMENT anterior
--    exacto. Falla si el paso 1 detecto filas con persona nula, y esa falla es
--    la proteccion, no un defecto.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_acta
    MODIFY COLUMN id_persona_infractor BIGINT NOT NULL
        COMMENT 'FK a la persona infractora (toda acta tiene sujeto técnico)';

-- -----------------------------------------------------------------------------
-- 5. Baja de la columna. El backfill no tiene contraparte porque el dato es
--    derivado: se recalcula por JOIN si el forward vuelve a aplicarse.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_acta DROP COLUMN situacion_identificacion_infractor;

-- -----------------------------------------------------------------------------
-- 6. Verificacion de la forma fisica anterior exacta.
--    Debe devolver: 56 columnas, id_persona_infractor NOT NULL, 2 CHECK,
--    la FK y el indice intactos.
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS columnas_fal_acta
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'fal_acta';

SELECT column_name, column_type, is_nullable, column_default
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'fal_acta'
   AND column_name IN ('situacion_identificacion_infractor', 'id_persona_infractor');

SELECT constraint_name, check_clause
  FROM information_schema.check_constraints
 WHERE constraint_schema = DATABASE() AND table_name = 'fal_acta'
 ORDER BY constraint_name;

SELECT k.constraint_name, k.column_name, k.referenced_table_name,
       r.update_rule, r.delete_rule
  FROM information_schema.key_column_usage k
  JOIN information_schema.referential_constraints r
    ON r.constraint_schema = k.constraint_schema AND r.constraint_name = k.constraint_name
 WHERE k.constraint_schema = DATABASE() AND k.table_name = 'fal_acta'
   AND k.constraint_name = 'fk_acta_persona_infractor';

SELECT index_name, column_name
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'fal_acta'
   AND index_name = 'ix_fal_acta_persona';

-- =============================================================================
-- FIN DEL ROLLBACK - fal_acta situacion de identificacion del infractor
-- =============================================================================
