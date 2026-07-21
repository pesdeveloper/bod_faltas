-- verify-protected-baseline.sql
-- SOLO LECTURA — No altera ningún objeto. Solo SELECT sobre information_schema.
--
-- Propósito: verificar que los objetos protegidos del baseline existen en sb_faltas_db
--            con engine, charset y collation correctos donde aplica.
-- Motor    : MariaDB 12.3.2, sb_faltas_db
-- Trabajo  : DDL-MARIADB-MANUAL-001-FULL-R1
-- Uso      : ejecutar manualmente desde HeidiSQL antes de correr el DDL del dominio.
-- ============================================================================

-- ============================================================================
-- 1. Tablas protegidas (16) — deben ser BASE TABLE, InnoDB, utf8mb4_uca1400_ai_ci
-- ============================================================================
SELECT
    table_name                          AS tabla,
    table_type                          AS tipo,
    engine                              AS motor,
    table_collation                     AS collation,
    CASE
        WHEN table_type <> 'BASE TABLE'           THEN 'ERROR_TIPO_INCORRECTO'
        WHEN engine <> 'InnoDB'                   THEN 'REVISAR_ENGINE'
        WHEN table_collation = 'utf8mb4_uca1400_ai_ci' THEN 'OK'
        ELSE 'REVISAR_COLLATION'
    END                                 AS resultado
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
      'fal_informix_sync_error',
      'fal_informix_sync_run',
      'fal_rubro_version',
      'geo_bahra_asentamiento',
      'geo_calle_alturas_barrio',
      'geo_dataset_load_error',
      'geo_dataset_load_run',
      'geo_dataset_row_version',
      'geo_ign_departamento',
      'geo_ign_municipio',
      'geo_ign_provincia',
      'geo_indec_calles',
      'geo_indec_localidad',
      'geo_indec_localidad_censal',
      'geo_malv_calle_version',
      'geo_malv_localidad_version'
  )
ORDER BY table_name;

-- Esperado: 16 filas, todas con resultado='OK'.
-- Si tabla_type != 'BASE TABLE': el objeto existe con nombre correcto pero tipo incorrecto.
-- Si hay menos de 16 filas: las faltantes no existen en el baseline.

-- ============================================================================
-- 2. Tablas protegidas ausentes (debería devolver 0 filas si todo está bien)
-- ============================================================================
SELECT
    expected.tabla_esperada             AS tabla_ausente,
    'FALTANTE_EN_BASELINE'              AS diagnostico
FROM (
    SELECT 'fal_informix_sync_error'    AS tabla_esperada
    UNION ALL SELECT 'fal_informix_sync_run'
    UNION ALL SELECT 'fal_rubro_version'
    UNION ALL SELECT 'geo_bahra_asentamiento'
    UNION ALL SELECT 'geo_calle_alturas_barrio'
    UNION ALL SELECT 'geo_dataset_load_error'
    UNION ALL SELECT 'geo_dataset_load_run'
    UNION ALL SELECT 'geo_dataset_row_version'
    UNION ALL SELECT 'geo_ign_departamento'
    UNION ALL SELECT 'geo_ign_municipio'
    UNION ALL SELECT 'geo_ign_provincia'
    UNION ALL SELECT 'geo_indec_calles'
    UNION ALL SELECT 'geo_indec_localidad'
    UNION ALL SELECT 'geo_indec_localidad_censal'
    UNION ALL SELECT 'geo_malv_calle_version'
    UNION ALL SELECT 'geo_malv_localidad_version'
) AS expected
LEFT JOIN information_schema.tables AS t
    ON t.table_schema = DATABASE()
   AND t.table_name = expected.tabla_esperada
   AND t.table_type = 'BASE TABLE'
WHERE t.table_name IS NULL
ORDER BY tabla_ausente;

-- Esperado: 0 filas.
-- Si aparece una fila: el objeto puede no existir o puede existir con tipo incorrecto (no BASE TABLE).

-- ============================================================================
-- 3. Vistas protegidas (4) — deben ser VIEW
-- ============================================================================
SELECT
    table_name                          AS vista,
    table_type                          AS tipo,
    CASE
        WHEN table_type = 'VIEW' THEN 'OK'
        ELSE 'ERROR_TIPO_INCORRECTO'
    END                                 AS resultado
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'VIEW'
  AND table_name IN (
      'vw_fal_rubro_actual',
      'vw_geo_malv_calle_actual',
      'vw_geo_malv_localidad_actual',
      'vw_geo_municipio_departamento'
  )
ORDER BY table_name;

-- Esperado: 4 filas, todas con resultado='OK'.

-- ============================================================================
-- 4. Vistas protegidas ausentes
-- ============================================================================
SELECT
    expected.vista_esperada             AS vista_ausente,
    'FALTANTE_EN_BASELINE'              AS diagnostico
FROM (
    SELECT 'vw_fal_rubro_actual'        AS vista_esperada
    UNION ALL SELECT 'vw_geo_malv_calle_actual'
    UNION ALL SELECT 'vw_geo_malv_localidad_actual'
    UNION ALL SELECT 'vw_geo_municipio_departamento'
) AS expected
LEFT JOIN information_schema.tables AS v
    ON v.table_schema = DATABASE()
   AND v.table_name = expected.vista_esperada
   AND v.table_type = 'VIEW'
WHERE v.table_name IS NULL
ORDER BY vista_ausente;

-- Esperado: 0 filas.

-- ============================================================================
-- 5. Verificación específica de fal_rubro_version (PREEXISTING_CANONICAL_ADOPTED)
--    Debe ser BASE TABLE (no vista, no sequence, no otro tipo)
-- ============================================================================
SELECT
    'fal_rubro_version'                 AS tabla,
    table_type                          AS tipo_real,
    engine                              AS motor,
    CASE
        WHEN table_type = 'BASE TABLE' THEN 'PRESENTE_COMO_BASE_TABLE_OK'
        WHEN table_type IS NOT NULL    THEN 'ERROR_EXISTE_PERO_TIPO_INCORRECTO'
        ELSE 'AUSENTE_BLOQUEANTE_PARA_DDL'
    END                                 AS estado
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'fal_rubro_version';

-- Esperado: 1 fila, estado = 'PRESENTE_COMO_BASE_TABLE_OK'.

-- ============================================================================
-- 6. Resumen general
-- ============================================================================
SELECT
    (SELECT COUNT(*) FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_type = 'BASE TABLE'
       AND table_name IN (
           'fal_informix_sync_error','fal_informix_sync_run','fal_rubro_version',
           'geo_bahra_asentamiento','geo_calle_alturas_barrio','geo_dataset_load_error',
           'geo_dataset_load_run','geo_dataset_row_version','geo_ign_departamento',
           'geo_ign_municipio','geo_ign_provincia','geo_indec_calles','geo_indec_localidad',
           'geo_indec_localidad_censal','geo_malv_calle_version','geo_malv_localidad_version'))
    AS tablas_protegidas_base_table_presentes,
    16                                  AS tablas_protegidas_esperadas,
    (SELECT COUNT(*) FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_type = 'VIEW'
       AND table_name IN (
           'vw_fal_rubro_actual','vw_geo_malv_calle_actual',
           'vw_geo_malv_localidad_actual','vw_geo_municipio_departamento'))
    AS vistas_protegidas_presentes,
    4                                   AS vistas_protegidas_esperadas,
    (SELECT COUNT(*) FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name IN (
           'fal_informix_sync_error','fal_informix_sync_run','fal_rubro_version',
           'geo_bahra_asentamiento','geo_calle_alturas_barrio','geo_dataset_load_error',
           'geo_dataset_load_run','geo_dataset_row_version','geo_ign_departamento',
           'geo_ign_municipio','geo_ign_provincia','geo_indec_calles','geo_indec_localidad',
           'geo_indec_localidad_censal','geo_malv_calle_version','geo_malv_localidad_version')
       AND table_type <> 'BASE TABLE')
    AS tablas_con_tipo_incorrecto;

-- Esperado: tablas_protegidas_base_table_presentes=16, vistas_protegidas_presentes=4,
--           tablas_con_tipo_incorrecto=0.
