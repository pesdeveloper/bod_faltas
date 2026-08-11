-- DIAGNOSTICO READ-ONLY - no modifica datos ni esquema
-- =============================================================================
-- POSTCHECK - fal_persona: documento opcional, canonico y estructurado
-- Script: verify-fal-persona-documento-opcional-001-postcheck.sql
-- Trabajo: JDBC-DOMINIO-EDA-WAVE-1A2-B1.5B0-B
-- Decision: DECISION_DDL-PERS-01, paso 13 del plan de migracion
--
-- Se ejecuta DESPUES de
-- database/ddl/alter-fal-persona-documento-opcional-001-forward.sql.
--
-- Verifica que el esquema ejecutado coincide con el modelo objetivo y que los
-- datos sobrevivieron sin perdida.
--
-- Uso: toda fila de las secciones 1 a 5 debe devolver 'OK'. Cualquier otro
-- valor exige detener y evaluar el rollback.
-- =============================================================================

USE sb_faltas_db;

-- -----------------------------------------------------------------------------
-- 1. Tipos y nulabilidad del modelo objetivo.
-- -----------------------------------------------------------------------------
SELECT esperado.columna,
       esperado.tipo_esperado,
       esperado.nulabilidad_esperada,
       fisico.column_type   AS tipo_real,
       fisico.is_nullable   AS nulabilidad_real,
       CASE WHEN fisico.column_type = esperado.tipo_esperado
             AND fisico.is_nullable = esperado.nulabilidad_esperada
            THEN 'OK' ELSE 'DIVERGENCIA' END AS estado
FROM (
    SELECT 'tipo_documento'     AS columna, 'smallint(6)'         AS tipo_esperado, 'YES' AS nulabilidad_esperada
    UNION ALL SELECT 'prefijo_cuit_cuil',   'tinyint(3) unsigned',      'YES'
    UNION ALL SELECT 'nro_doc',             'varchar(12)',              'YES'
    UNION ALL SELECT 'digito_verificador',  'tinyint(3) unsigned',      'YES'
    UNION ALL SELECT 'doc_key',             'varchar(12)',              'YES'
) esperado
LEFT JOIN information_schema.columns fisico
       ON fisico.table_schema = DATABASE()
      AND fisico.table_name   = 'fal_persona'
      AND fisico.column_name  = esperado.columna
ORDER BY esperado.columna;

-- -----------------------------------------------------------------------------
-- 2. doc_key es STORED GENERATED, sin COALESCE, sin LPAD y sin el prefijo
--    'tipo_documento-'.
-- -----------------------------------------------------------------------------
SELECT extra,
       generation_expression,
       CASE WHEN extra = 'STORED GENERATED'
             AND LOWER(generation_expression) NOT LIKE '%coalesce%'
             AND LOWER(generation_expression) NOT LIKE '%lpad%'
             AND generation_expression LIKE '%tipo_documento%is null%'
            THEN 'OK' ELSE 'DIVERGENCIA' END AS estado
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name   = 'fal_persona'
   AND column_name  = 'doc_key';

-- -----------------------------------------------------------------------------
-- 3. Constraints: los ocho CHECK del modelo objetivo mas los cuatro ajenos al
--    documento; chk_fal_persona_nro_doc ya no existe.
-- -----------------------------------------------------------------------------
SELECT esperado.constraint_name,
       esperado.debe_existir,
       (fisico.constraint_name IS NOT NULL) AS existe,
       CASE WHEN esperado.debe_existir = (fisico.constraint_name IS NOT NULL)
            THEN 'OK' ELSE 'DIVERGENCIA' END AS estado
FROM (
    SELECT 'chk_fal_persona_doc_coherente'    AS constraint_name, 1 AS debe_existir
    UNION ALL SELECT 'chk_fal_persona_doc_tipo',         1
    UNION ALL SELECT 'chk_fal_persona_doc_cuit',         1
    UNION ALL SELECT 'chk_fal_persona_doc_no_cuit',      1
    UNION ALL SELECT 'chk_fal_persona_nro_doc_canonico', 1
    UNION ALL SELECT 'chk_fal_persona_nro_doc_dni',      1
    UNION ALL SELECT 'chk_fal_persona_prefijo',          1
    UNION ALL SELECT 'chk_fal_persona_digito',           1
    UNION ALL SELECT 'chk_fal_persona_tipo',             1
    UNION ALL SELECT 'chk_fal_persona_id_suj',           1
    UNION ALL SELECT 'chk_fal_persona_id_bie',           1
    UNION ALL SELECT 'chk_fal_persona_id_bie_suj',       1
    UNION ALL SELECT 'chk_fal_persona_nro_doc',          0
) esperado
LEFT JOIN information_schema.check_constraints fisico
       ON fisico.constraint_schema = DATABASE()
      AND fisico.table_name        = 'fal_persona'
      AND fisico.constraint_name   = esperado.constraint_name
ORDER BY esperado.constraint_name;

-- Clausula literal de cada CHECK vigente, para inspeccion.
SELECT constraint_name, check_clause
  FROM information_schema.check_constraints
 WHERE constraint_schema = DATABASE() AND table_name = 'fal_persona'
 ORDER BY constraint_name;

-- -----------------------------------------------------------------------------
-- 4. El prefijo exige dos digitos significativos (10..99), no 0..99.
-- -----------------------------------------------------------------------------
SELECT check_clause,
       CASE WHEN check_clause LIKE '%between 10 and 99%' THEN 'OK' ELSE 'DIVERGENCIA' END AS estado
  FROM information_schema.check_constraints
 WHERE constraint_schema = DATABASE()
   AND table_name        = 'fal_persona'
   AND constraint_name   = 'chk_fal_persona_prefijo';

-- -----------------------------------------------------------------------------
-- 5. Indices: el UNIQUE compuesto se conserva; los dos indices de nombre
--    tambien. Debe devolver exactamente 4 claves.
-- -----------------------------------------------------------------------------
SELECT index_name,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columnas,
       MAX(non_unique)                                 AS no_unico
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'fal_persona'
 GROUP BY index_name
 ORDER BY index_name;

-- -----------------------------------------------------------------------------
-- 6. Coherencia de los datos bajo el modelo nuevo. Todo debe ser 0.
-- -----------------------------------------------------------------------------
SELECT control, cantidad,
       CASE WHEN cantidad = 0 THEN 'OK' ELSE 'DIVERGENCIA' END AS estado
FROM (
    SELECT 'Q01 doc_key NULL donde hay documento' AS control, COUNT(*) AS cantidad
      FROM fal_persona WHERE tipo_documento IS NOT NULL AND doc_key IS NULL
    UNION ALL SELECT 'Q02 doc_key informada donde no hay documento', COUNT(*)
      FROM fal_persona WHERE tipo_documento IS NULL AND doc_key IS NOT NULL
    UNION ALL SELECT 'Q03 CUIT/CUIL sin los tres componentes', COUNT(*)
      FROM fal_persona WHERE tipo_documento IN (2, 3)
                         AND (prefijo_cuit_cuil IS NULL OR digito_verificador IS NULL
                              OR nro_doc NOT REGEXP '^[0-9]{8}$')
    UNION ALL SELECT 'Q04 CUIT/CUIL cuya doc_key no rinde 11 digitos', COUNT(*)
      FROM fal_persona WHERE tipo_documento IN (2, 3) AND doc_key NOT REGEXP '^[0-9]{11}$'
    UNION ALL SELECT 'Q05 duplicados de (tipo_documento, doc_key)', COUNT(*)
      FROM (SELECT tipo_documento, doc_key FROM fal_persona
             WHERE tipo_documento IS NOT NULL
             GROUP BY 1, 2 HAVING COUNT(*) > 1) duplicados
    UNION ALL SELECT 'Q06 nro_doc fuera de la forma canonica', COUNT(*)
      FROM fal_persona WHERE nro_doc IS NOT NULL AND nro_doc NOT REGEXP '^[0-9A-Z]{1,12}$'
) controles
ORDER BY control;

-- -----------------------------------------------------------------------------
-- 7. Conteo de filas. Debe coincidir con el del precheck.
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS filas_fal_persona FROM fal_persona;

-- =============================================================================
-- FIN DEL POSTCHECK - fal_persona documento opcional
-- =============================================================================
