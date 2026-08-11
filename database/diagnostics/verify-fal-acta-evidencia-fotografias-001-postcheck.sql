-- =============================================================================
-- POSTCHECK - fal_acta_evidencia: columnas de fotografia (Parte 3D)
-- Script: verify-fal-acta-evidencia-fotografias-001-postcheck.sql
-- Trabajo: WAVE-04-PARTE-3D-EVIDENCIAS-FOTOGRAFICAS
-- Decision: DECISION_DDL-EVID-03
-- =============================================================================

USE sb_faltas_db;

-- 1. Las diez columnas nuevas deben existir, todas NULL-able.
SELECT column_name, column_type, is_nullable
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name   = 'fal_acta_evidencia'
   AND column_name IN ('orden','descripcion','nombre_original','mime_tipo',
                       'tamanio_bytes','ancho_px','alto_px','fecha_captura_exif',
                       'bloque_al_incorporar','origen_incorporacion')
 ORDER BY ordinal_position;

-- 2. Los dos UNIQUE nuevos deben existir.
SELECT index_name, column_name, non_unique
  FROM information_schema.statistics
 WHERE table_schema = DATABASE()
   AND table_name   = 'fal_acta_evidencia'
   AND index_name IN ('uq_evidencia_acta_orden','uq_evidencia_acta_hash')
 ORDER BY index_name, seq_in_index;

-- 3. Ningun CHECK nuevo debe existir (DECISION_DDL-SIN-CHECKS-CODIGOS-01: la
--    coherencia FOTO<->campos se valida en Java, no en el motor). Debe
--    devolver exactamente 1 fila: chk_evidencia_hash, la unica CHECK que
--    permanece en fal_acta_evidencia.
SELECT constraint_name, check_clause
  FROM information_schema.check_constraints
 WHERE constraint_schema = DATABASE()
   AND table_name = 'fal_acta_evidencia';

-- 4. Semantica NULL-en-UNIQUE: dos filas no-FOTO con orden/hash NULL no deben
--    colisionar. Se verifica insertando y revirtiendo dentro de una
--    transaccion de prueba (no deja rastro).
START TRANSACTION;
INSERT INTO fal_acta_evidencia (id_acta, tipo_evid, storage_key, fecha_registro, fh_alta, id_user_alta)
SELECT id, 48, 'postcheck://null-unique-test/1', NOW(6), NOW(6), '00000000-0000-0000-0000-000000000000'
  FROM fal_acta LIMIT 1;
INSERT INTO fal_acta_evidencia (id_acta, tipo_evid, storage_key, fecha_registro, fh_alta, id_user_alta)
SELECT id, 48, 'postcheck://null-unique-test/2', NOW(6), NOW(6), '00000000-0000-0000-0000-000000000000'
  FROM fal_acta LIMIT 1;
SELECT COUNT(*) AS filas_null_unique_ok FROM fal_acta_evidencia WHERE storage_key LIKE 'postcheck://null-unique-test/%';
ROLLBACK;

-- 5. La tabla original de FIRMA_OLOGRAFA_INFRACTOR (codigo 48) sigue
--    aceptando filas con las columnas nuevas en NULL (no rompe DECISION_DDL-EVID-01).
SELECT COUNT(*) AS confirmado_sin_residuo
  FROM fal_acta_evidencia
 WHERE storage_key LIKE 'postcheck://null-unique-test/%';
