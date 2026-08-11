-- DIAGNÓSTICO READ-ONLY — no modifica datos ni esquema
-- =============================================================================
-- POSTCHECK — fir_outbox / fir_bus_checkpoint / fir_inbox / fir_inbox_error
--             (bounded context Firmas)
-- Script: verify-firmas-eda-outbox-inbox-postcheck.sql
-- Trabajo: SPEC-FIRMAS-EDA-CLOSURE-001 (DEC-EDA-028)
--
-- Se ejecuta DESPUÉS de database/ddl/firmas/create-firmas-eda-outbox-inbox.sql.
-- Verifica:
--   1. que las 4 tablas existen, con ENGINE=InnoDB y charset/collation
--      utf8mb4/utf8mb4_uca1400_ai_ci;
--   2. el conteo exacto de columnas esperado (17 en fir_outbox, 3 en
--      fir_bus_checkpoint, 18 en fir_inbox —con envelope completo
--      evento_id/productor_codigo/schema_version/correlation_id/
--      causation_id/ocurrido_en—, 6 en fir_inbox_error, igual que
--      fal_outbox/fal_bus_checkpoint/fal_inbox/fal_inbox_error);
--   3. la presencia EXACTA (no solo parcial) de las 12 claves/constraints
--      declaradas (incluye uq_fir_inbox_evento_id, idempotencia de
--      ingestión por identidad lógica, distinta del cursor técnico
--      bus_evento_id);
--   4. la fila singleton inicial de fir_bus_checkpoint (checkpoint_id=1,
--      ultimo_evento_id=0);
--   5. la FK fir_inbox_error -> fir_inbox.
--
-- Uso: revisar manualmente que todas las filas reporten OK.
-- =============================================================================

SELECT
    table_name,
    engine,
    table_collation,
    CASE WHEN engine = 'InnoDB' AND table_collation = 'utf8mb4_uca1400_ai_ci' THEN 'OK' ELSE 'REVISAR' END AS estado
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('fir_outbox', 'fir_bus_checkpoint', 'fir_inbox', 'fir_inbox_error');

SELECT
    table_name,
    COUNT(*) AS columnas_reales,
    CASE
        WHEN table_name = 'fir_outbox' AND COUNT(*) = 17 THEN 'OK'
        WHEN table_name = 'fir_bus_checkpoint' AND COUNT(*) = 3 THEN 'OK'
        WHEN table_name = 'fir_inbox' AND COUNT(*) = 18 THEN 'OK'
        WHEN table_name = 'fir_inbox_error' AND COUNT(*) = 6 THEN 'OK'
        ELSE 'REVISAR'
    END AS estado
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('fir_outbox', 'fir_bus_checkpoint', 'fir_inbox', 'fir_inbox_error')
GROUP BY table_name;

SELECT
    esperado.constraint_name,
    esperado.table_name,
    CASE WHEN tc.constraint_name IS NOT NULL THEN 'OK' ELSE 'FALTANTE' END AS estado
FROM (
    SELECT 'uq_fir_outbox_evento_id' AS constraint_name, 'fir_outbox' AS table_name
    UNION ALL SELECT 'chk_fir_outbox_status', 'fir_outbox'
    UNION ALL SELECT 'chk_fir_outbox_schema_version', 'fir_outbox'
    UNION ALL SELECT 'chk_fir_outbox_attempts', 'fir_outbox'
    UNION ALL SELECT 'chk_fir_outbox_si_error', 'fir_outbox'
    UNION ALL SELECT 'chk_fir_bus_checkpoint_singleton', 'fir_bus_checkpoint'
    UNION ALL SELECT 'uq_fir_inbox_evento_id', 'fir_inbox'
    UNION ALL SELECT 'chk_fir_inbox_status', 'fir_inbox'
    UNION ALL SELECT 'chk_fir_inbox_attempts', 'fir_inbox'
    UNION ALL SELECT 'chk_fir_inbox_si_error', 'fir_inbox'
    UNION ALL SELECT 'uq_fir_inbox_error_attempt', 'fir_inbox_error'
    UNION ALL SELECT 'fk_fir_inbox_error_inbox', 'fir_inbox_error'
) esperado
LEFT JOIN information_schema.table_constraints tc
    ON tc.table_schema = DATABASE()
   AND tc.table_name = esperado.table_name
   AND tc.constraint_name = esperado.constraint_name
ORDER BY esperado.table_name, esperado.constraint_name;

SELECT
    COUNT(*)                                       AS constraints_presentes,
    12                                              AS constraints_esperados,
    CASE WHEN COUNT(*) = 12 THEN 'OK' ELSE 'BLOQUEADO_FALTAN_CONSTRAINTS' END AS veredicto
FROM information_schema.table_constraints
WHERE table_schema = DATABASE()
  AND table_name IN ('fir_outbox', 'fir_bus_checkpoint', 'fir_inbox', 'fir_inbox_error')
  AND constraint_name IN (
      'uq_fir_outbox_evento_id', 'chk_fir_outbox_status', 'chk_fir_outbox_schema_version',
      'chk_fir_outbox_attempts', 'chk_fir_outbox_si_error',
      'chk_fir_bus_checkpoint_singleton',
      'uq_fir_inbox_evento_id',
      'chk_fir_inbox_status', 'chk_fir_inbox_attempts', 'chk_fir_inbox_si_error',
      'uq_fir_inbox_error_attempt', 'fk_fir_inbox_error_inbox'
  );

-- fir_inbox usa bus_evento_id como PRIMARY KEY (no un id técnico propio)
SELECT
    column_name,
    CASE WHEN column_key = 'PRI' THEN 'OK' ELSE 'DIVERGENCIA_PK_INCORRECTA' END AS resultado
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'fir_inbox'
  AND column_name = 'bus_evento_id';

-- fir_bus_checkpoint: fila singleton inicial presente (checkpoint_id=1,
-- ultimo_evento_id=0)
SELECT
    COUNT(*) AS filas_presentes,
    1        AS filas_esperadas,
    CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'DIVERGENCIA_SEED_CHECKPOINT' END AS resultado
FROM fir_bus_checkpoint
WHERE checkpoint_id = 1 AND ultimo_evento_id = 0;

-- =============================================================================
-- FIN DEL POSTCHECK — fir_outbox / fir_bus_checkpoint / fir_inbox / fir_inbox_error
-- =============================================================================
