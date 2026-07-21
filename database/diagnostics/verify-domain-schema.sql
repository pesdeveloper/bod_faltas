-- verify-domain-schema.sql
-- SOLO LECTURA — No altera ningún objeto. Solo SELECT sobre information_schema.
--
-- Propósito: verificar el esquema del dominio BOD Faltas tras ejecutar el DDL.
--            Cubre las 64 tablas TO_CREATE + 1 tabla preexistente adoptada.
-- Motor    : MariaDB 12.3.2, sb_faltas_db
-- Trabajo  : DDL-MARIADB-MANUAL-001-FULL-R1
-- Uso      : ejecutar manualmente desde HeidiSQL DESPUÉS de ejecutar el DDL.
-- ============================================================================
--
-- Advertencia: NO ejecutar antes de auditar el ZIP integral y ejecutar el DDL.
-- El diagnóstico reporta divergencias; nunca las oculta ni las corrige.
-- Para los CHECK, la normalización final puede requerir ajuste tras la primera
-- ejecución en MariaDB 12.3.2 (ver database/design/ddl-full-scope.md).
--
-- ============================================================================

-- ============================================================================
-- SECCIÓN 1 — Conteo de tablas canónicas presentes
-- ============================================================================

-- 1a. Tablas TO_CREATE presentes (esperado: 64)
SELECT
    COUNT(*)                            AS tablas_dominio_presentes,
    64                                  AS tablas_dominio_esperadas,
    CASE WHEN COUNT(*) = 64 THEN 'OK' ELSE 'DIVERGENCIA' END AS resultado
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE'
  AND table_name IN (
      'fal_dependencia','fal_inspector','fal_firmante','fal_persona',
      'fal_vehiculo_marca','num_politica','fal_dia_no_computable',
      'fal_motivo_archivo','fal_normativa_faltas','fal_medida_preventiva',
      'fal_lote_correo','fal_documento_plantilla',
      'fal_dependencia_version','fal_inspector_version','fal_firmante_version',
      'fal_vehiculo_modelo','num_talonario','fal_dependencia_normativa',
      'fal_articulo_normativa_faltas','fal_documento_plantilla_firma_req',
      'fal_documento_plantilla_contenido','fal_documento_plantilla_default',
      'fal_firmante_version_habilitacion','fal_tarifario_unidad_faltas',
      'fal_articulo_medida_preventiva','num_talonario_ambito','num_talonario_inspector',
      'fal_persona_domicilio','fal_acta',
      'fal_acta_evidencia','fal_observacion','fal_acta_transito',
      'fal_acta_transito_alcoholemia','fal_acta_vehiculo','fal_acta_contravencion',
      'fal_acta_sustancias_alimenticias','fal_acta_paralizacion','fal_acta_archivo',
      'fal_acta_articulo_infringido','fal_acta_qr_acceso','fal_acta_gestion_externa',
      'fal_acta_valorizacion','fal_acta_medida_preventiva','fal_acta_bloqueante_cierre_material',
      'fal_documento','fal_acta_valorizacion_item',
      'fal_acta_documento','fal_documento_firma_req','fal_documento_redaccion',
      'fal_notificacion','num_talonario_movimiento','fal_acta_fallo',
      'fal_documento_firma','fal_notificacion_intento','fal_notificacion_acuse',
      'fal_acta_evento','fal_acta_apelacion','fal_acta_obligacion_pago',
      'fal_acta_snapshot','fal_acta_apelacion_documento','fal_acta_forma_pago',
      'fal_acta_plan_pago_ref','fal_acta_pago_movimiento','fal_acta_economia_proyeccion'
  );

-- 1b. Tabla adoptada presente (esperado: 1 fila, tipo BASE TABLE)
SELECT
    table_name                          AS tabla_adoptada,
    table_type                          AS tipo,
    CASE WHEN table_type = 'BASE TABLE' THEN 'OK' ELSE 'ERROR_TIPO' END AS resultado
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'fal_rubro_version';

-- 1c. Tablas TO_CREATE faltantes (esperado: 0 filas)
SELECT
    t_esp.tabla_esperada                AS tabla_faltante,
    'FALTANTE'                          AS diagnostico
FROM (
    SELECT 'fal_dependencia'            AS tabla_esperada
    UNION ALL SELECT 'fal_inspector'
    UNION ALL SELECT 'fal_firmante'
    UNION ALL SELECT 'fal_persona'
    UNION ALL SELECT 'fal_vehiculo_marca'
    UNION ALL SELECT 'num_politica'
    UNION ALL SELECT 'fal_dia_no_computable'
    UNION ALL SELECT 'fal_motivo_archivo'
    UNION ALL SELECT 'fal_normativa_faltas'
    UNION ALL SELECT 'fal_medida_preventiva'
    UNION ALL SELECT 'fal_lote_correo'
    UNION ALL SELECT 'fal_documento_plantilla'
    UNION ALL SELECT 'fal_dependencia_version'
    UNION ALL SELECT 'fal_inspector_version'
    UNION ALL SELECT 'fal_firmante_version'
    UNION ALL SELECT 'fal_vehiculo_modelo'
    UNION ALL SELECT 'num_talonario'
    UNION ALL SELECT 'fal_dependencia_normativa'
    UNION ALL SELECT 'fal_articulo_normativa_faltas'
    UNION ALL SELECT 'fal_documento_plantilla_firma_req'
    UNION ALL SELECT 'fal_documento_plantilla_contenido'
    UNION ALL SELECT 'fal_documento_plantilla_default'
    UNION ALL SELECT 'fal_firmante_version_habilitacion'
    UNION ALL SELECT 'fal_tarifario_unidad_faltas'
    UNION ALL SELECT 'fal_articulo_medida_preventiva'
    UNION ALL SELECT 'num_talonario_ambito'
    UNION ALL SELECT 'num_talonario_inspector'
    UNION ALL SELECT 'fal_persona_domicilio'
    UNION ALL SELECT 'fal_acta'
    UNION ALL SELECT 'fal_acta_evidencia'
    UNION ALL SELECT 'fal_observacion'
    UNION ALL SELECT 'fal_acta_transito'
    UNION ALL SELECT 'fal_acta_transito_alcoholemia'
    UNION ALL SELECT 'fal_acta_vehiculo'
    UNION ALL SELECT 'fal_acta_contravencion'
    UNION ALL SELECT 'fal_acta_sustancias_alimenticias'
    UNION ALL SELECT 'fal_acta_paralizacion'
    UNION ALL SELECT 'fal_acta_archivo'
    UNION ALL SELECT 'fal_acta_articulo_infringido'
    UNION ALL SELECT 'fal_acta_qr_acceso'
    UNION ALL SELECT 'fal_acta_gestion_externa'
    UNION ALL SELECT 'fal_acta_valorizacion'
    UNION ALL SELECT 'fal_acta_medida_preventiva'
    UNION ALL SELECT 'fal_acta_bloqueante_cierre_material'
    UNION ALL SELECT 'fal_documento'
    UNION ALL SELECT 'fal_acta_valorizacion_item'
    UNION ALL SELECT 'fal_acta_documento'
    UNION ALL SELECT 'fal_documento_firma_req'
    UNION ALL SELECT 'fal_documento_redaccion'
    UNION ALL SELECT 'fal_notificacion'
    UNION ALL SELECT 'num_talonario_movimiento'
    UNION ALL SELECT 'fal_acta_fallo'
    UNION ALL SELECT 'fal_documento_firma'
    UNION ALL SELECT 'fal_notificacion_intento'
    UNION ALL SELECT 'fal_notificacion_acuse'
    UNION ALL SELECT 'fal_acta_evento'
    UNION ALL SELECT 'fal_acta_apelacion'
    UNION ALL SELECT 'fal_acta_obligacion_pago'
    UNION ALL SELECT 'fal_acta_snapshot'
    UNION ALL SELECT 'fal_acta_apelacion_documento'
    UNION ALL SELECT 'fal_acta_forma_pago'
    UNION ALL SELECT 'fal_acta_plan_pago_ref'
    UNION ALL SELECT 'fal_acta_pago_movimiento'
    UNION ALL SELECT 'fal_acta_economia_proyeccion'
) AS t_esp
LEFT JOIN information_schema.tables AS t
    ON t.table_schema = DATABASE()
   AND t.table_name = t_esp.tabla_esperada
   AND t.table_type = 'BASE TABLE'
WHERE t.table_name IS NULL
ORDER BY tabla_faltante;

-- Esperado: 0 filas.

-- ============================================================================
-- SECCIÓN 2 — Engine, charset y collation por tabla
-- ============================================================================

-- 2a. Tablas con engine o collation incorrectos (esperado: 0 filas)
SELECT
    table_name                          AS tabla,
    engine                              AS motor_real,
    table_collation                     AS collation_real,
    CASE
        WHEN engine <> 'InnoDB'                         THEN 'ENGINE_INCORRECTO'
        WHEN table_collation <> 'utf8mb4_uca1400_ai_ci' THEN 'COLLATION_INCORRECTA'
        ELSE 'OK'
    END                                 AS resultado
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE'
  AND table_name IN (
      'fal_dependencia','fal_inspector','fal_firmante','fal_persona',
      'fal_vehiculo_marca','num_politica','fal_dia_no_computable',
      'fal_motivo_archivo','fal_normativa_faltas','fal_medida_preventiva',
      'fal_lote_correo','fal_documento_plantilla',
      'fal_dependencia_version','fal_inspector_version','fal_firmante_version',
      'fal_vehiculo_modelo','num_talonario','fal_dependencia_normativa',
      'fal_articulo_normativa_faltas','fal_documento_plantilla_firma_req',
      'fal_documento_plantilla_contenido','fal_documento_plantilla_default',
      'fal_firmante_version_habilitacion','fal_tarifario_unidad_faltas',
      'fal_articulo_medida_preventiva','num_talonario_ambito','num_talonario_inspector',
      'fal_persona_domicilio','fal_acta',
      'fal_acta_evidencia','fal_observacion','fal_acta_transito',
      'fal_acta_transito_alcoholemia','fal_acta_vehiculo','fal_acta_contravencion',
      'fal_acta_sustancias_alimenticias','fal_acta_paralizacion','fal_acta_archivo',
      'fal_acta_articulo_infringido','fal_acta_qr_acceso','fal_acta_gestion_externa',
      'fal_acta_valorizacion','fal_acta_medida_preventiva','fal_acta_bloqueante_cierre_material',
      'fal_documento','fal_acta_valorizacion_item',
      'fal_acta_documento','fal_documento_firma_req','fal_documento_redaccion',
      'fal_notificacion','num_talonario_movimiento','fal_acta_fallo',
      'fal_documento_firma','fal_notificacion_intento','fal_notificacion_acuse',
      'fal_acta_evento','fal_acta_apelacion','fal_acta_obligacion_pago',
      'fal_acta_snapshot','fal_acta_apelacion_documento','fal_acta_forma_pago',
      'fal_acta_plan_pago_ref','fal_acta_pago_movimiento','fal_acta_economia_proyeccion'
  )
  AND (engine <> 'InnoDB' OR table_collation <> 'utf8mb4_uca1400_ai_ci')
ORDER BY table_name;

-- Esperado: 0 filas.

-- ============================================================================
-- SECCIÓN 3 — Columnas sin COMMENT (esperado: 0 filas)
-- ============================================================================
SELECT
    table_name                          AS tabla,
    column_name                         AS columna,
    'SIN_COMMENT'                       AS diagnostico
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND column_comment = ''
  AND table_name IN (
      'fal_dependencia','fal_inspector','fal_firmante','fal_persona',
      'fal_vehiculo_marca','num_politica','fal_dia_no_computable',
      'fal_motivo_archivo','fal_normativa_faltas','fal_medida_preventiva',
      'fal_lote_correo','fal_documento_plantilla',
      'fal_dependencia_version','fal_inspector_version','fal_firmante_version',
      'fal_vehiculo_modelo','num_talonario','fal_dependencia_normativa',
      'fal_articulo_normativa_faltas','fal_documento_plantilla_firma_req',
      'fal_documento_plantilla_contenido','fal_documento_plantilla_default',
      'fal_firmante_version_habilitacion','fal_tarifario_unidad_faltas',
      'fal_articulo_medida_preventiva','num_talonario_ambito','num_talonario_inspector',
      'fal_persona_domicilio','fal_acta',
      'fal_acta_evidencia','fal_observacion','fal_acta_transito',
      'fal_acta_transito_alcoholemia','fal_acta_vehiculo','fal_acta_contravencion',
      'fal_acta_sustancias_alimenticias','fal_acta_paralizacion','fal_acta_archivo',
      'fal_acta_articulo_infringido','fal_acta_qr_acceso','fal_acta_gestion_externa',
      'fal_acta_valorizacion','fal_acta_medida_preventiva','fal_acta_bloqueante_cierre_material',
      'fal_documento','fal_acta_valorizacion_item',
      'fal_acta_documento','fal_documento_firma_req','fal_documento_redaccion',
      'fal_notificacion','num_talonario_movimiento','fal_acta_fallo',
      'fal_documento_firma','fal_notificacion_intento','fal_notificacion_acuse',
      'fal_acta_evento','fal_acta_apelacion','fal_acta_obligacion_pago',
      'fal_acta_snapshot','fal_acta_apelacion_documento','fal_acta_forma_pago',
      'fal_acta_plan_pago_ref','fal_acta_pago_movimiento','fal_acta_economia_proyeccion'
  )
ORDER BY table_name, ordinal_position;

-- Esperado: 0 filas (todas las columnas tienen COMMENT).

-- ============================================================================
-- SECCIÓN 4 — Tablas sin Primary Key (esperado: 0 filas)
-- ============================================================================
SELECT
    t.table_name                        AS tabla_sin_pk,
    'SIN_PK'                            AS diagnostico
FROM information_schema.tables AS t
LEFT JOIN information_schema.table_constraints AS tc
    ON tc.table_schema = t.table_schema
   AND tc.table_name = t.table_name
   AND tc.constraint_type = 'PRIMARY KEY'
WHERE t.table_schema = DATABASE()
  AND t.table_type = 'BASE TABLE'
  AND t.table_name IN (
      'fal_dependencia','fal_inspector','fal_firmante','fal_persona',
      'fal_vehiculo_marca','num_politica','fal_dia_no_computable',
      'fal_motivo_archivo','fal_normativa_faltas','fal_medida_preventiva',
      'fal_lote_correo','fal_documento_plantilla',
      'fal_dependencia_version','fal_inspector_version','fal_firmante_version',
      'fal_vehiculo_modelo','num_talonario','fal_dependencia_normativa',
      'fal_articulo_normativa_faltas','fal_documento_plantilla_firma_req',
      'fal_documento_plantilla_contenido','fal_documento_plantilla_default',
      'fal_firmante_version_habilitacion','fal_tarifario_unidad_faltas',
      'fal_articulo_medida_preventiva','num_talonario_ambito','num_talonario_inspector',
      'fal_persona_domicilio','fal_acta',
      'fal_acta_evidencia','fal_observacion','fal_acta_transito',
      'fal_acta_transito_alcoholemia','fal_acta_vehiculo','fal_acta_contravencion',
      'fal_acta_sustancias_alimenticias','fal_acta_paralizacion','fal_acta_archivo',
      'fal_acta_articulo_infringido','fal_acta_qr_acceso','fal_acta_gestion_externa',
      'fal_acta_valorizacion','fal_acta_medida_preventiva','fal_acta_bloqueante_cierre_material',
      'fal_documento','fal_acta_valorizacion_item',
      'fal_acta_documento','fal_documento_firma_req','fal_documento_redaccion',
      'fal_notificacion','num_talonario_movimiento','fal_acta_fallo',
      'fal_documento_firma','fal_notificacion_intento','fal_notificacion_acuse',
      'fal_acta_evento','fal_acta_apelacion','fal_acta_obligacion_pago',
      'fal_acta_snapshot','fal_acta_apelacion_documento','fal_acta_forma_pago',
      'fal_acta_plan_pago_ref','fal_acta_pago_movimiento','fal_acta_economia_proyeccion'
  )
  AND tc.constraint_name IS NULL
ORDER BY t.table_name;

-- Esperado: 0 filas.

-- ============================================================================
-- SECCIÓN 5 — FK presentes por tabla (inventario; sin valor esperado fijo)
-- ============================================================================
SELECT
    tc.table_name                       AS tabla,
    tc.constraint_name                  AS fk_nombre,
    kcu.column_name                     AS columna,
    kcu.referenced_table_name           AS tabla_referenciada,
    kcu.referenced_column_name          AS columna_referenciada,
    rc.delete_rule                      AS on_delete,
    rc.update_rule                      AS on_update
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON kcu.constraint_schema = tc.constraint_schema
   AND kcu.constraint_name = tc.constraint_name
   AND kcu.table_name = tc.table_name
JOIN information_schema.referential_constraints AS rc
    ON rc.constraint_schema = tc.constraint_schema
   AND rc.constraint_name = tc.constraint_name
WHERE tc.table_schema = DATABASE()
  AND tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_name IN (
      'fal_dependencia','fal_inspector','fal_firmante','fal_persona',
      'fal_vehiculo_marca','num_politica','fal_dia_no_computable',
      'fal_motivo_archivo','fal_normativa_faltas','fal_medida_preventiva',
      'fal_lote_correo','fal_documento_plantilla',
      'fal_dependencia_version','fal_inspector_version','fal_firmante_version',
      'fal_vehiculo_modelo','num_talonario','fal_dependencia_normativa',
      'fal_articulo_normativa_faltas','fal_documento_plantilla_firma_req',
      'fal_documento_plantilla_contenido','fal_documento_plantilla_default',
      'fal_firmante_version_habilitacion','fal_tarifario_unidad_faltas',
      'fal_articulo_medida_preventiva','num_talonario_ambito','num_talonario_inspector',
      'fal_persona_domicilio','fal_acta',
      'fal_acta_evidencia','fal_observacion','fal_acta_transito',
      'fal_acta_transito_alcoholemia','fal_acta_vehiculo','fal_acta_contravencion',
      'fal_acta_sustancias_alimenticias','fal_acta_paralizacion','fal_acta_archivo',
      'fal_acta_articulo_infringido','fal_acta_qr_acceso','fal_acta_gestion_externa',
      'fal_acta_valorizacion','fal_acta_medida_preventiva','fal_acta_bloqueante_cierre_material',
      'fal_documento','fal_acta_valorizacion_item',
      'fal_acta_documento','fal_documento_firma_req','fal_documento_redaccion',
      'fal_notificacion','num_talonario_movimiento','fal_acta_fallo',
      'fal_documento_firma','fal_notificacion_intento','fal_notificacion_acuse',
      'fal_acta_evento','fal_acta_apelacion','fal_acta_obligacion_pago',
      'fal_acta_snapshot','fal_acta_apelacion_documento','fal_acta_forma_pago',
      'fal_acta_plan_pago_ref','fal_acta_pago_movimiento','fal_acta_economia_proyeccion'
  )
ORDER BY tc.table_name, tc.constraint_name, kcu.ordinal_position;

-- ============================================================================
-- SECCIÓN 6 — Columnas version_row: tablas que deben tenerlo (OCC)
-- ============================================================================
-- Tablas con OCC según inventario (version_row INT NOT NULL DEFAULT 0):
-- num_talonario, fal_acta, fal_acta_gestion_externa, fal_acta_valorizacion,
-- fal_acta_bloqueante_cierre_material, fal_documento, fal_notificacion,
-- fal_acta_fallo, fal_acta_apelacion, fal_acta_obligacion_pago, fal_acta_snapshot,
-- fal_acta_forma_pago, fal_acta_plan_pago_ref, fal_acta_economia_proyeccion

-- 6a. Tablas OCC sin version_row (esperado: 0 filas)
SELECT
    t_occ.tabla_occ                     AS tabla,
    'SIN_VERSION_ROW'                   AS diagnostico
FROM (
    SELECT 'num_talonario'                      AS tabla_occ
    UNION ALL SELECT 'fal_acta'
    UNION ALL SELECT 'fal_acta_gestion_externa'
    UNION ALL SELECT 'fal_acta_valorizacion'
    UNION ALL SELECT 'fal_acta_bloqueante_cierre_material'
    UNION ALL SELECT 'fal_documento'
    UNION ALL SELECT 'fal_notificacion'
    UNION ALL SELECT 'fal_acta_fallo'
    UNION ALL SELECT 'fal_acta_apelacion'
    UNION ALL SELECT 'fal_acta_obligacion_pago'
    UNION ALL SELECT 'fal_acta_snapshot'
    UNION ALL SELECT 'fal_acta_forma_pago'
    UNION ALL SELECT 'fal_acta_plan_pago_ref'
    UNION ALL SELECT 'fal_acta_economia_proyeccion'
) AS t_occ
LEFT JOIN information_schema.columns AS c
    ON c.table_schema = DATABASE()
   AND c.table_name = t_occ.tabla_occ
   AND c.column_name = 'version_row'
   AND c.data_type = 'int'
   AND c.is_nullable = 'NO'
WHERE c.column_name IS NULL
ORDER BY tabla;

-- Esperado: 0 filas.

-- ============================================================================
-- SECCIÓN 7 — CHECK constraints críticos (muestra; normalización puede diferir)
-- ============================================================================
-- Esta sección muestra los CHECK definidos para auditoría visual.
-- La normalización de la cláusula por MariaDB puede diferir de la redacción
-- en el DDL (ej: espacios, mayúsculas, paréntesis). Ajustar manualmente tras
-- la primera ejecución si hay divergencias.

SELECT
    tc.table_name                       AS tabla,
    tc.constraint_name                  AS check_nombre,
    cc.check_clause                     AS clausula_real
FROM information_schema.table_constraints AS tc
JOIN information_schema.check_constraints AS cc
    ON cc.constraint_schema = tc.constraint_schema
   AND cc.constraint_name = tc.constraint_name
WHERE tc.table_schema = DATABASE()
  AND tc.constraint_type = 'CHECK'
  AND tc.table_name IN (
      'fal_dependencia','fal_inspector','fal_persona',
      'fal_acta','fal_acta_obligacion_pago','fal_acta_forma_pago',
      'fal_acta_fallo','fal_acta_apelacion','fal_acta_evento',
      'fal_acta_pago_movimiento','num_talonario','fal_lote_correo',
      'fal_acta_articulo_infringido','fal_acta_transito_alcoholemia',
      'fal_notificacion_intento','num_talonario_inspector','num_talonario_movimiento'
  )
ORDER BY tc.table_name, tc.constraint_name;

-- ============================================================================
-- SECCIÓN 8 — UNIQUE constraints críticos
-- ============================================================================
SELECT
    tc.table_name                       AS tabla,
    tc.constraint_name                  AS unique_nombre,
    GROUP_CONCAT(kcu.column_name ORDER BY kcu.ordinal_position SEPARATOR ', ')
                                        AS columnas_uk
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON kcu.constraint_schema = tc.constraint_schema
   AND kcu.constraint_name = tc.constraint_name
   AND kcu.table_name = tc.table_name
WHERE tc.table_schema = DATABASE()
  AND tc.constraint_type = 'UNIQUE'
  AND tc.table_name IN (
      'fal_dependencia','fal_inspector','fal_firmante','fal_persona',
      'fal_vehiculo_marca','num_politica','fal_dia_no_computable',
      'fal_motivo_archivo','fal_normativa_faltas',
      'fal_dependencia_version','fal_inspector_version','fal_firmante_version',
      'fal_vehiculo_modelo','num_talonario',
      'fal_documento_plantilla_contenido','fal_firmante_version_habilitacion',
      'fal_persona_domicilio','fal_acta',
      'fal_acta_transito_alcoholemia','fal_acta_articulo_infringido',
      'fal_documento','fal_documento_redaccion',
      'fal_documento_firma_req','fal_notificacion_intento',
      'fal_acta_apelacion','fal_acta_obligacion_pago','fal_acta_forma_pago',
      'fal_acta_plan_pago_ref','fal_acta_pago_movimiento'
  )
GROUP BY tc.table_name, tc.constraint_name
ORDER BY tc.table_name, tc.constraint_name;

-- ============================================================================
-- SECCIÓN 9 — Objetos protegidos intactos (no deben existir como tablas del dominio)
-- ============================================================================
-- Verifica que los objetos protegidos NO fueron recreados como tablas dominio
-- por error. Solo se valida que no están entre las tablas del dominio creadas.
SELECT
    expected.objeto_protegido           AS objeto_protegido,
    t.table_type                        AS tipo_encontrado,
    CASE
        WHEN t.table_name IS NULL THEN 'OK_NO_TOCADO'
        ELSE 'ALERTA_VERIFICAR_MANUALMENTE'
    END                                 AS estado
FROM (
    SELECT 'fal_informix_sync_error'    AS objeto_protegido
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
    UNION ALL SELECT 'vw_fal_rubro_actual'
    UNION ALL SELECT 'vw_geo_malv_calle_actual'
    UNION ALL SELECT 'vw_geo_malv_localidad_actual'
    UNION ALL SELECT 'vw_geo_municipio_departamento'
) AS expected
LEFT JOIN information_schema.tables AS t
    ON t.table_schema = DATABASE()
   AND t.table_name = expected.objeto_protegido
ORDER BY expected.objeto_protegido;

-- Esperado: 20 filas, todas con estado='OK_NO_TOCADO' (o 'ALERTA_...' si el objeto existe,
-- lo cual es normal: significa que el baseline protegido está presente e intacto).

-- ============================================================================
-- SECCIÓN 10 — UNIQUE inesperadas en tablas del dominio
-- ============================================================================
-- Detecta UNIQUE constraints cuyo nombre NO sigue el patrón esperado uq_<tabla>_*.
-- Diagnóstico: UNIQUE_INESPERADA indica un constraint que requiere revisión manual.
SELECT
    tc.table_name                       AS tabla,
    tc.constraint_name                  AS nombre_constraint,
    GROUP_CONCAT(kcu.column_name ORDER BY kcu.ordinal_position SEPARATOR ', ')
                                        AS columnas,
    CASE
        WHEN tc.constraint_name LIKE CONCAT('uq_', tc.table_name, '%') THEN 'OK_NOMBRE_ESPERADO'
        ELSE 'UNIQUE_INESPERADA'
    END                                 AS diagnostico
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON kcu.constraint_schema = tc.constraint_schema
   AND kcu.constraint_name = tc.constraint_name
   AND kcu.table_name = tc.table_name
WHERE tc.table_schema = DATABASE()
  AND tc.constraint_type = 'UNIQUE'
  AND tc.table_name IN (
      'fal_dependencia','fal_inspector','fal_firmante','fal_persona',
      'fal_vehiculo_marca','num_politica','fal_dia_no_computable',
      'fal_motivo_archivo','fal_normativa_faltas','fal_medida_preventiva',
      'fal_lote_correo','fal_documento_plantilla',
      'fal_dependencia_version','fal_inspector_version','fal_firmante_version',
      'fal_vehiculo_modelo','num_talonario','fal_dependencia_normativa',
      'fal_articulo_normativa_faltas','fal_documento_plantilla_firma_req',
      'fal_documento_plantilla_contenido','fal_documento_plantilla_default',
      'fal_firmante_version_habilitacion','fal_tarifario_unidad_faltas',
      'fal_articulo_medida_preventiva','num_talonario_ambito','num_talonario_inspector',
      'fal_persona_domicilio','fal_acta',
      'fal_acta_evidencia','fal_observacion','fal_acta_transito',
      'fal_acta_transito_alcoholemia','fal_acta_vehiculo','fal_acta_contravencion',
      'fal_acta_sustancias_alimenticias','fal_acta_paralizacion','fal_acta_archivo',
      'fal_acta_articulo_infringido','fal_acta_qr_acceso','fal_acta_gestion_externa',
      'fal_acta_valorizacion','fal_acta_medida_preventiva','fal_acta_bloqueante_cierre_material',
      'fal_documento','fal_acta_valorizacion_item',
      'fal_acta_documento','fal_documento_firma_req','fal_documento_redaccion',
      'fal_notificacion','num_talonario_movimiento','fal_acta_fallo',
      'fal_documento_firma','fal_notificacion_intento','fal_notificacion_acuse',
      'fal_acta_evento','fal_acta_apelacion','fal_acta_obligacion_pago',
      'fal_acta_snapshot','fal_acta_apelacion_documento','fal_acta_forma_pago',
      'fal_acta_plan_pago_ref','fal_acta_pago_movimiento','fal_acta_economia_proyeccion'
  )
GROUP BY tc.table_name, tc.constraint_name
ORDER BY tc.table_name, tc.constraint_name;

-- Si aparece UNIQUE_INESPERADA: verificar si el constraint es intencional o un error.

-- ============================================================================
-- SECCIÓN 11 — FK inesperadas: referencias a tablas fuera del dominio esperado
-- ============================================================================
-- Detecta FKs que apuntan a tablas que NO son parte del dominio Faltas ni del baseline.
-- Diagnóstico: FK_INESPERADA indica una referencia que requiere revisión manual.
SELECT
    tc.table_name                       AS tabla,
    tc.constraint_name                  AS fk_nombre,
    kcu.column_name                     AS columna,
    kcu.referenced_table_name           AS tabla_referenciada,
    CASE
        WHEN kcu.referenced_table_name IN (
            'fal_dependencia','fal_inspector','fal_firmante','fal_persona',
            'fal_vehiculo_marca','num_politica','fal_dia_no_computable',
            'fal_motivo_archivo','fal_normativa_faltas','fal_medida_preventiva',
            'fal_lote_correo','fal_documento_plantilla',
            'fal_dependencia_version','fal_inspector_version','fal_firmante_version',
            'fal_vehiculo_modelo','num_talonario','fal_dependencia_normativa',
            'fal_articulo_normativa_faltas','fal_documento_plantilla_firma_req',
            'fal_documento_plantilla_contenido','fal_documento_plantilla_default',
            'fal_firmante_version_habilitacion','fal_tarifario_unidad_faltas',
            'fal_articulo_medida_preventiva','num_talonario_ambito','num_talonario_inspector',
            'fal_persona_domicilio','fal_acta',
            'fal_acta_evidencia','fal_observacion','fal_acta_transito',
            'fal_acta_transito_alcoholemia','fal_acta_vehiculo','fal_acta_contravencion',
            'fal_acta_sustancias_alimenticias','fal_acta_paralizacion','fal_acta_archivo',
            'fal_acta_articulo_infringido','fal_acta_qr_acceso','fal_acta_gestion_externa',
            'fal_acta_valorizacion','fal_acta_medida_preventiva','fal_acta_bloqueante_cierre_material',
            'fal_documento','fal_acta_valorizacion_item',
            'fal_acta_documento','fal_documento_firma_req','fal_documento_redaccion',
            'fal_notificacion','num_talonario_movimiento','fal_acta_fallo',
            'fal_documento_firma','fal_notificacion_intento','fal_notificacion_acuse',
            'fal_acta_evento','fal_acta_apelacion','fal_acta_obligacion_pago',
            'fal_acta_snapshot','fal_acta_apelacion_documento','fal_acta_forma_pago',
            'fal_acta_plan_pago_ref','fal_acta_pago_movimiento','fal_acta_economia_proyeccion',
            -- Baseline adoptado y geo
            'fal_rubro_version',
            'geo_malv_localidad_version','geo_malv_calle_version'
        ) THEN 'OK_REFERENCIA_ESPERADA'
        ELSE 'FK_INESPERADA'
    END                                 AS diagnostico
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON kcu.constraint_schema = tc.constraint_schema
   AND kcu.constraint_name = tc.constraint_name
   AND kcu.table_name = tc.table_name
WHERE tc.table_schema = DATABASE()
  AND tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_name IN (
      'fal_dependencia','fal_inspector','fal_firmante','fal_persona',
      'fal_vehiculo_marca','num_politica','fal_dia_no_computable',
      'fal_motivo_archivo','fal_normativa_faltas','fal_medida_preventiva',
      'fal_lote_correo','fal_documento_plantilla',
      'fal_dependencia_version','fal_inspector_version','fal_firmante_version',
      'fal_vehiculo_modelo','num_talonario','fal_dependencia_normativa',
      'fal_articulo_normativa_faltas','fal_documento_plantilla_firma_req',
      'fal_documento_plantilla_contenido','fal_documento_plantilla_default',
      'fal_firmante_version_habilitacion','fal_tarifario_unidad_faltas',
      'fal_articulo_medida_preventiva','num_talonario_ambito','num_talonario_inspector',
      'fal_persona_domicilio','fal_acta',
      'fal_acta_evidencia','fal_observacion','fal_acta_transito',
      'fal_acta_transito_alcoholemia','fal_acta_vehiculo','fal_acta_contravencion',
      'fal_acta_sustancias_alimenticias','fal_acta_paralizacion','fal_acta_archivo',
      'fal_acta_articulo_infringido','fal_acta_qr_acceso','fal_acta_gestion_externa',
      'fal_acta_valorizacion','fal_acta_medida_preventiva','fal_acta_bloqueante_cierre_material',
      'fal_documento','fal_acta_valorizacion_item',
      'fal_acta_documento','fal_documento_firma_req','fal_documento_redaccion',
      'fal_notificacion','num_talonario_movimiento','fal_acta_fallo',
      'fal_documento_firma','fal_notificacion_intento','fal_notificacion_acuse',
      'fal_acta_evento','fal_acta_apelacion','fal_acta_obligacion_pago',
      'fal_acta_snapshot','fal_acta_apelacion_documento','fal_acta_forma_pago',
      'fal_acta_plan_pago_ref','fal_acta_pago_movimiento','fal_acta_economia_proyeccion'
  )
ORDER BY tc.table_name, tc.constraint_name;

-- Si aparece FK_INESPERADA: la FK apunta a una tabla fuera del dominio esperado.
-- Verificar si es intencional o un error de constraint.

-- ============================================================================
-- SECCIÓN 12 — CHECK constraints con cláusula normalizada (diagnóstico exacto)
-- ============================================================================
-- Normaliza la cláusula real del CHECK con LOWER() y REGEXP_REPLACE para comparar
-- contra la cláusula esperada y detectar divergencias.
-- Diagnóstico: CLAUSULA_DIVERGENTE indica que MariaDB almacenó la cláusula distinta.
SELECT
    tc.table_name                       AS tabla,
    tc.constraint_name                  AS check_nombre,
    cc.check_clause                     AS clausula_real,
    lower(REGEXP_REPLACE(cc.check_clause, '[[:space:]]+', ' '))
                                        AS clausula_normalizada,
    CASE
        WHEN lower(REGEXP_REPLACE(cc.check_clause, '[[:space:]]+', ' '))
             LIKE CONCAT('%', lower(tc.constraint_name COLLATE utf8mb4_uca1400_ai_ci), '%')
          OR cc.check_clause IS NULL    THEN 'OK'
        ELSE                            'CLAUSULA_DIVERGENTE'
    END                                 AS diagnostico
FROM information_schema.table_constraints AS tc
JOIN information_schema.check_constraints AS cc
    ON cc.constraint_schema = tc.constraint_schema
   AND cc.constraint_name = tc.constraint_name
WHERE tc.table_schema = DATABASE()
  AND tc.constraint_type = 'CHECK'
  AND tc.table_name IN (
      'fal_dependencia','fal_inspector','fal_persona',
      'num_politica','fal_dia_no_computable',
      'fal_acta','fal_acta_obligacion_pago','fal_acta_forma_pago',
      'fal_acta_fallo','fal_acta_apelacion','fal_acta_evento',
      'fal_acta_pago_movimiento','num_talonario','fal_lote_correo',
      'fal_acta_articulo_infringido','fal_acta_transito_alcoholemia',
      'fal_notificacion_intento','num_talonario_inspector','num_talonario_movimiento'
  )
ORDER BY tc.table_name, tc.constraint_name;

-- Si aparece CLAUSULA_DIVERGENTE: ajustar la cláusula esperada con la real de MariaDB.

-- Verifica que los objetos protegidos NO fueron recreados como tablas dominio
-- por error. Solo se valida que no están entre las tablas del dominio creadas.
SELECT
    expected.objeto_protegido           AS objeto_protegido,
    t.table_type                        AS tipo_encontrado,
    CASE
        WHEN t.table_name IS NULL THEN 'OK_NO_TOCADO'
        ELSE 'ALERTA_VERIFICAR_MANUALMENTE'
    END                                 AS estado
FROM (
    SELECT 'fal_informix_sync_error'    AS objeto_protegido
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
    UNION ALL SELECT 'vw_fal_rubro_actual'
    UNION ALL SELECT 'vw_geo_malv_calle_actual'
    UNION ALL SELECT 'vw_geo_malv_localidad_actual'
    UNION ALL SELECT 'vw_geo_municipio_departamento'
) AS expected
LEFT JOIN information_schema.tables AS t
    ON t.table_schema = DATABASE()
   AND t.table_name = expected.objeto_protegido
ORDER BY expected.objeto_protegido;

-- Esperado: 20 filas, todas con estado='OK_NO_TOCADO' (o 'ALERTA_...' si el objeto existe,
-- lo cual es normal: significa que el baseline protegido está presente e intacto).

-- ============================================================================
-- RESUMEN FINAL
-- ============================================================================
SELECT
    (SELECT COUNT(*) FROM information_schema.tables
     WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
       AND table_name IN (
           'fal_dependencia','fal_inspector','fal_firmante','fal_persona',
           'fal_vehiculo_marca','num_politica','fal_dia_no_computable',
           'fal_motivo_archivo','fal_normativa_faltas','fal_medida_preventiva',
           'fal_lote_correo','fal_documento_plantilla',
           'fal_dependencia_version','fal_inspector_version','fal_firmante_version',
           'fal_vehiculo_modelo','num_talonario','fal_dependencia_normativa',
           'fal_articulo_normativa_faltas','fal_documento_plantilla_firma_req',
           'fal_documento_plantilla_contenido','fal_documento_plantilla_default',
           'fal_firmante_version_habilitacion','fal_tarifario_unidad_faltas',
           'fal_articulo_medida_preventiva','num_talonario_ambito','num_talonario_inspector',
           'fal_persona_domicilio','fal_acta',
           'fal_acta_evidencia','fal_observacion','fal_acta_transito',
           'fal_acta_transito_alcoholemia','fal_acta_vehiculo','fal_acta_contravencion',
           'fal_acta_sustancias_alimenticias','fal_acta_paralizacion','fal_acta_archivo',
           'fal_acta_articulo_infringido','fal_acta_qr_acceso','fal_acta_gestion_externa',
           'fal_acta_valorizacion','fal_acta_medida_preventiva','fal_acta_bloqueante_cierre_material',
           'fal_documento','fal_acta_valorizacion_item',
           'fal_acta_documento','fal_documento_firma_req','fal_documento_redaccion',
           'fal_notificacion','num_talonario_movimiento','fal_acta_fallo',
           'fal_documento_firma','fal_notificacion_intento','fal_notificacion_acuse',
           'fal_acta_evento','fal_acta_apelacion','fal_acta_obligacion_pago',
           'fal_acta_snapshot','fal_acta_apelacion_documento','fal_acta_forma_pago',
           'fal_acta_plan_pago_ref','fal_acta_pago_movimiento','fal_acta_economia_proyeccion'))
    AS tablas_dominio_presentes,
    64                                  AS tablas_dominio_esperadas,
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND column_comment = ''
       AND table_name IN (
           'fal_dependencia','fal_inspector','fal_firmante','fal_persona',
           'fal_vehiculo_marca','num_politica','fal_dia_no_computable',
           'fal_motivo_archivo','fal_normativa_faltas','fal_medida_preventiva',
           'fal_lote_correo','fal_documento_plantilla',
           'fal_dependencia_version','fal_inspector_version','fal_firmante_version',
           'fal_vehiculo_modelo','num_talonario','fal_dependencia_normativa',
           'fal_articulo_normativa_faltas','fal_documento_plantilla_firma_req',
           'fal_documento_plantilla_contenido','fal_documento_plantilla_default',
           'fal_firmante_version_habilitacion','fal_tarifario_unidad_faltas',
           'fal_articulo_medida_preventiva','num_talonario_ambito','num_talonario_inspector',
           'fal_persona_domicilio','fal_acta',
           'fal_acta_evidencia','fal_observacion','fal_acta_transito',
           'fal_acta_transito_alcoholemia','fal_acta_vehiculo','fal_acta_contravencion',
           'fal_acta_sustancias_alimenticias','fal_acta_paralizacion','fal_acta_archivo',
           'fal_acta_articulo_infringido','fal_acta_qr_acceso','fal_acta_gestion_externa',
           'fal_acta_valorizacion','fal_acta_medida_preventiva','fal_acta_bloqueante_cierre_material',
           'fal_documento','fal_acta_valorizacion_item',
           'fal_acta_documento','fal_documento_firma_req','fal_documento_redaccion',
           'fal_notificacion','num_talonario_movimiento','fal_acta_fallo',
           'fal_documento_firma','fal_notificacion_intento','fal_notificacion_acuse',
           'fal_acta_evento','fal_acta_apelacion','fal_acta_obligacion_pago',
           'fal_acta_snapshot','fal_acta_apelacion_documento','fal_acta_forma_pago',
           'fal_acta_plan_pago_ref','fal_acta_pago_movimiento','fal_acta_economia_proyeccion'))
    AS columnas_sin_comment,
    0                                   AS columnas_sin_comment_esperadas;

-- Esperado: tablas_dominio_presentes=64, columnas_sin_comment=0.

-- ============================================================================
-- SECCIÓN 13 — Delta FULL-R1.2-CORRECCION-01: FK versiones, FK Rubros, FK GEO
-- ============================================================================
-- Verifica explícitamente las FK corregidas o agregadas en este delta.
-- Diagnóstico: OK | FK_AUSENTE | FK_DIVERGENTE
-- Compara: tabla_hija, columnas_hija, tabla_padre, columnas_padre, ON_DELETE, ON_UPDATE
-- ============================================================================

-- 13a. FK versionadas de fal_acta: ver_dep (smallint NOT NULL) y ver_insp (smallint NULL)
-- LEFT JOIN sobre expectativas explícitas. Siempre devuelve 2 filas.
-- Diagnostico: OK | COLUMNA_AUSENTE | TIPO_DIVERGENTE | NULLABILITY_DIVERGENTE
SELECT
    'fal_acta'                          AS tabla,
    esp.columna_esp                     AS columna,
    col.data_type                       AS tipo_real,
    col.is_nullable                     AS nullable_real,
    esp.tipo_esp                        AS tipo_esperado,
    esp.nullable_esp                    AS nullable_esperado,
    CASE
        WHEN col.column_name IS NULL            THEN 'COLUMNA_AUSENTE'
        WHEN col.data_type   <> esp.tipo_esp    THEN 'TIPO_DIVERGENTE'
        WHEN col.is_nullable <> esp.nullable_esp THEN 'NULLABILITY_DIVERGENTE'
        ELSE                                         'OK'
    END                                 AS diagnostico
FROM (
    SELECT 'ver_dep'  AS columna_esp, 'smallint' AS tipo_esp, 'NO'  AS nullable_esp
    UNION ALL
    SELECT 'ver_insp',                 'smallint',             'YES'
) AS esp
LEFT JOIN information_schema.columns AS col
    ON col.table_schema = DATABASE()
   AND col.table_name   = 'fal_acta'
   AND col.column_name  = esp.columna_esp
ORDER BY esp.columna_esp;

-- Siempre devuelve 2 filas: ver_dep (NOT NULL), ver_insp (NULL).
-- COLUMNA_AUSENTE: columna no existe en fal_acta.
-- TIPO_DIVERGENTE: tipo de dato difiere de smallint.
-- NULLABILITY_DIVERGENTE: nullability difiere de lo esperado.

-- 13b/13c. FK versionadas de fal_acta → fal_dependencia_version y fal_inspector_version
-- Expectativas explícitas con LEFT JOIN. Siempre devuelve 2 filas.
-- Diagnostico: OK | FK_AUSENTE | FK_DIVERGENTE
WITH esp_ver AS (
    SELECT 'fk_acta_dep_ver'           AS constraint_esp,
           'fal_acta'                  AS tabla_hija_esp,
           'id_dep,ver_dep'            AS columnas_hija_esperadas,
           'fal_dependencia_version'   AS tabla_padre_esp,
           'id_dep,ver_dep'            AS columnas_padre_esperadas,
           'RESTRICT'                  AS delete_esp,
           'RESTRICT'                  AS update_esp
    UNION ALL
    SELECT 'fk_acta_insp_ver',
           'fal_acta',
           'id_insp,ver_insp',
           'fal_inspector_version',
           'id_insp,ver_insp',
           'RESTRICT',
           'RESTRICT'
),
real_ver AS (
    SELECT
        tc.constraint_name,
        tc.table_name                                                                             AS tabla_hija,
        GROUP_CONCAT(kcu.column_name        ORDER BY kcu.ordinal_position SEPARATOR ',')         AS columnas_hija,
        kcu.referenced_table_name                                                                AS tabla_padre,
        GROUP_CONCAT(kcu.referenced_column_name ORDER BY kcu.ordinal_position SEPARATOR ',')     AS columnas_padre,
        rc.delete_rule,
        rc.update_rule
    FROM information_schema.table_constraints AS tc
    JOIN information_schema.key_column_usage AS kcu
        ON kcu.constraint_schema = tc.constraint_schema
       AND kcu.constraint_name   = tc.constraint_name
       AND kcu.table_name        = tc.table_name
    JOIN information_schema.referential_constraints AS rc
        ON rc.constraint_schema  = tc.constraint_schema
       AND rc.constraint_name    = tc.constraint_name
    WHERE tc.table_schema = DATABASE()
      AND tc.constraint_type = 'FOREIGN KEY'
      AND tc.constraint_name IN ('fk_acta_dep_ver', 'fk_acta_insp_ver')
    GROUP BY tc.constraint_name, tc.table_name, kcu.referenced_table_name, rc.delete_rule, rc.update_rule
)
SELECT
    esp_ver.constraint_esp              AS constraint_name,
    esp_ver.columnas_hija_esperadas,
    esp_ver.tabla_padre_esp             AS tabla_padre_esperada,
    esp_ver.columnas_padre_esperadas,
    real_ver.columnas_hija              AS columnas_hija_real,
    real_ver.columnas_padre             AS columnas_padre_real,
    real_ver.delete_rule                AS on_delete,
    real_ver.update_rule                AS on_update,
    CASE
        WHEN real_ver.constraint_name IS NULL                THEN 'FK_AUSENTE'
        WHEN real_ver.tabla_hija     <> esp_ver.tabla_hija_esp
          OR real_ver.columnas_hija  <> esp_ver.columnas_hija_esperadas
          OR real_ver.tabla_padre    <> esp_ver.tabla_padre_esp
          OR real_ver.columnas_padre <> esp_ver.columnas_padre_esperadas
          OR real_ver.delete_rule    <> esp_ver.delete_esp
          OR real_ver.update_rule    <> esp_ver.update_esp   THEN 'FK_DIVERGENTE'
        ELSE                                                      'OK'
    END                                 AS diagnostico
FROM esp_ver
LEFT JOIN real_ver ON real_ver.constraint_name = esp_ver.constraint_esp
ORDER BY esp_ver.constraint_esp;

-- Siempre devuelve 2 filas: fk_acta_dep_ver, fk_acta_insp_ver.
-- FK_AUSENTE: constraint no existe en la base. FK_DIVERGENTE: algún campo difiere.

-- 13d. FK a fal_rubro_version: fk_acta_ctv_rubro y fk_acta_sus_alim_rubro
-- Expectativas explícitas con LEFT JOIN. Siempre devuelve 2 filas.
-- Diagnostico: OK | FK_AUSENTE | FK_DIVERGENTE
WITH esp_rub AS (
    SELECT 'fk_acta_ctv_rubro'               AS constraint_esp,
           'fal_acta_contravencion'           AS tabla_hija_esp,
           'rubro_id'                         AS columnas_hija_esperadas,
           'fal_rubro_version'                AS tabla_padre_esp,
           'rubro_id'                         AS columnas_padre_esperadas,
           'RESTRICT'                         AS delete_esp,
           'RESTRICT'                         AS update_esp
    UNION ALL
    SELECT 'fk_acta_sus_alim_rubro',
           'fal_acta_sustancias_alimenticias',
           'rubro_id',
           'fal_rubro_version',
           'rubro_id',
           'RESTRICT',
           'RESTRICT'
),
real_rub AS (
    SELECT
        tc.constraint_name,
        tc.table_name               AS tabla_hija,
        kcu.column_name             AS columna_hija,
        kcu.referenced_table_name   AS tabla_padre,
        kcu.referenced_column_name  AS columna_padre,
        rc.delete_rule,
        rc.update_rule
    FROM information_schema.table_constraints AS tc
    JOIN information_schema.key_column_usage AS kcu
        ON kcu.constraint_schema = tc.constraint_schema
       AND kcu.constraint_name   = tc.constraint_name
       AND kcu.table_name        = tc.table_name
    JOIN information_schema.referential_constraints AS rc
        ON rc.constraint_schema  = tc.constraint_schema
       AND rc.constraint_name    = tc.constraint_name
    WHERE tc.table_schema = DATABASE()
      AND tc.constraint_type = 'FOREIGN KEY'
      AND tc.constraint_name IN ('fk_acta_ctv_rubro', 'fk_acta_sus_alim_rubro')
)
SELECT
    esp_rub.constraint_esp              AS constraint_name,
    esp_rub.columnas_hija_esperadas,
    esp_rub.tabla_padre_esp             AS tabla_padre_esperada,
    esp_rub.columnas_padre_esperadas,
    real_rub.columna_hija               AS columna_hija_real,
    real_rub.columna_padre              AS columna_padre_real,
    real_rub.delete_rule                AS on_delete,
    real_rub.update_rule                AS on_update,
    CASE
        WHEN real_rub.constraint_name IS NULL                THEN 'FK_AUSENTE'
        WHEN real_rub.tabla_hija    <> esp_rub.tabla_hija_esp
          OR real_rub.columna_hija  <> esp_rub.columnas_hija_esperadas
          OR real_rub.tabla_padre   <> esp_rub.tabla_padre_esp
          OR real_rub.columna_padre <> esp_rub.columnas_padre_esperadas
          OR real_rub.delete_rule   <> esp_rub.delete_esp
          OR real_rub.update_rule   <> esp_rub.update_esp    THEN 'FK_DIVERGENTE'
        ELSE                                                      'OK'
    END                                 AS diagnostico
FROM esp_rub
LEFT JOIN real_rub ON real_rub.constraint_name = esp_rub.constraint_esp
ORDER BY esp_rub.constraint_esp;

-- Siempre devuelve 2 filas: fk_acta_ctv_rubro, fk_acta_sus_alim_rubro.
-- FK_AUSENTE: constraint no existe. FK_DIVERGENTE: tabla/columna/reglas difieren.

-- 13e. Cuatro FK GEO canónicas (constraint_name explícito)
-- Expectativas explícitas con LEFT JOIN. Siempre devuelve 4 filas.
-- Diagnostico: OK | FK_AUSENTE | FK_DIVERGENTE
WITH esp_geo AS (
    SELECT 'fk_pers_dom_loc_malv'              AS constraint_esp,
           'fal_persona_domicilio'             AS tabla_hija_esp,
           'localidad_malvinas_version_id'     AS columnas_hija_esperadas,
           'geo_malv_localidad_version'        AS tabla_padre_esp,
           'localidad_version_id'              AS columnas_padre_esperadas,
           'RESTRICT'                          AS delete_esp,
           'RESTRICT'                          AS update_esp
    UNION ALL
    SELECT 'fk_pers_dom_calle_malv',
           'fal_persona_domicilio',
           'calle_malvinas_version_id',
           'geo_malv_calle_version',
           'calle_version_id',
           'RESTRICT',
           'RESTRICT'
    UNION ALL
    SELECT 'fk_acta_loc_infr_malv',
           'fal_acta',
           'localidad_infr_malvinas_version_id',
           'geo_malv_localidad_version',
           'localidad_version_id',
           'RESTRICT',
           'RESTRICT'
    UNION ALL
    SELECT 'fk_acta_calle_infr_malv',
           'fal_acta',
           'calle_infr_malvinas_version_id',
           'geo_malv_calle_version',
           'calle_version_id',
           'RESTRICT',
           'RESTRICT'
),
real_geo AS (
    SELECT
        tc.constraint_name,
        tc.table_name               AS tabla_hija,
        kcu.column_name             AS columna_hija,
        kcu.referenced_table_name   AS tabla_padre,
        kcu.referenced_column_name  AS columna_padre,
        rc.delete_rule,
        rc.update_rule
    FROM information_schema.table_constraints AS tc
    JOIN information_schema.key_column_usage AS kcu
        ON kcu.constraint_schema = tc.constraint_schema
       AND kcu.constraint_name   = tc.constraint_name
       AND kcu.table_name        = tc.table_name
    JOIN information_schema.referential_constraints AS rc
        ON rc.constraint_schema  = tc.constraint_schema
       AND rc.constraint_name    = tc.constraint_name
    WHERE tc.table_schema = DATABASE()
      AND tc.constraint_type = 'FOREIGN KEY'
      AND tc.constraint_name IN (
          'fk_pers_dom_loc_malv', 'fk_pers_dom_calle_malv',
          'fk_acta_loc_infr_malv', 'fk_acta_calle_infr_malv'
      )
)
SELECT
    esp_geo.constraint_esp              AS constraint_name_esperado,
    esp_geo.columnas_hija_esperadas,
    esp_geo.tabla_padre_esp             AS tabla_padre_esperada,
    esp_geo.columnas_padre_esperadas,
    real_geo.columna_hija               AS columna_hija_real,
    real_geo.columna_padre              AS columna_padre_real,
    real_geo.delete_rule                AS on_delete,
    real_geo.update_rule                AS on_update,
    CASE
        WHEN real_geo.constraint_name IS NULL                THEN 'FK_AUSENTE'
        WHEN real_geo.constraint_name <> esp_geo.constraint_esp
          OR real_geo.tabla_hija      <> esp_geo.tabla_hija_esp
          OR real_geo.columna_hija    <> esp_geo.columnas_hija_esperadas
          OR real_geo.tabla_padre     <> esp_geo.tabla_padre_esp
          OR real_geo.columna_padre   <> esp_geo.columnas_padre_esperadas
          OR real_geo.delete_rule     <> esp_geo.delete_esp
          OR real_geo.update_rule     <> esp_geo.update_esp   THEN 'FK_DIVERGENTE'
        ELSE                                                       'OK'
    END                                 AS diagnostico
FROM esp_geo
LEFT JOIN real_geo ON real_geo.constraint_name = esp_geo.constraint_esp
ORDER BY esp_geo.constraint_esp;

-- Siempre devuelve 4 filas: fk_acta_calle_infr_malv, fk_acta_loc_infr_malv,
--                             fk_pers_dom_calle_malv, fk_pers_dom_loc_malv.
-- FK_AUSENTE: constraint no existe (verificar ALTER TABLE del DDL).
-- FK_DIVERGENTE: algún campo difiere (nombre, tabla, columna o reglas).

-- ============================================================================
-- SECCIÓN 14 — Delta FULL-R1.2-CORRECCION-02: domicilios, acta y QR
-- ============================================================================
-- Verifica los contratos físicos de las columnas ajustadas en este delta.
-- Diagnóstico: OK | COLUMNA_AUSENTE | COLUMNA_INESPERADA | TIPO_DIVERGENTE |
--              LONGITUD_DIVERGENTE | NULLABILITY_DIVERGENTE
-- Siempre devuelve todas las filas esperadas (LEFT JOIN sobre expectativas).
-- ============================================================================

-- 14a. Contratos de columnas: 5 expectativas positivas
-- Devuelve 5 filas siempre.
SELECT
    esp.tabla_esp                       AS tabla,
    esp.columna_esp                     AS columna,
    col.data_type                       AS tipo_real,
    col.character_maximum_length        AS longitud_real,
    col.is_nullable                     AS nullable_real,
    esp.tipo_esp                        AS tipo_esperado,
    esp.longitud_esp                    AS longitud_esperada,
    esp.nullable_esp                    AS nullable_esperado,
    CASE
        WHEN col.column_name IS NULL
            THEN 'COLUMNA_AUSENTE'
        WHEN col.data_type <> esp.tipo_esp
            THEN 'TIPO_DIVERGENTE'
        WHEN col.character_maximum_length <> esp.longitud_esp
            THEN 'LONGITUD_DIVERGENTE'
        WHEN col.is_nullable <> esp.nullable_esp
            THEN 'NULLABILITY_DIVERGENTE'
        ELSE 'OK'
    END                                 AS diagnostico
FROM (
    SELECT 'fal_persona_domicilio' AS tabla_esp, 'calle_txt'       AS columna_esp, 'varchar' AS tipo_esp,  48 AS longitud_esp, 'YES' AS nullable_esp
    UNION ALL
    SELECT 'fal_persona_domicilio',               'domicilio_txt',                 'varchar',              196,               'YES'
    UNION ALL
    SELECT 'fal_acta',                            'domicilio_hecho',               'varchar',              196,               'YES'
    UNION ALL
    SELECT 'fal_acta',                            'dom_txt_infr',                  'varchar',              196,               'YES'
    UNION ALL
    SELECT 'fal_acta',                            'codigo_qr',                     'varchar',              128,               'NO'
) AS esp
LEFT JOIN information_schema.columns AS col
    ON col.table_schema  = DATABASE()
   AND col.table_name    = esp.tabla_esp
   AND col.column_name   = esp.columna_esp
ORDER BY esp.tabla_esp, esp.columna_esp;

-- Esperado: 5 filas, todas con diagnostico='OK'.
-- COLUMNA_AUSENTE    : columna no existe — el DDL no la creó o fue eliminada por error.
-- TIPO_DIVERGENTE    : el tipo de dato difiere de varchar.
-- LONGITUD_DIVERGENTE: la longitud máxima difiere del contrato canónico.
-- NULLABILITY_DIVERGENTE: la nullabilidad difiere de lo esperado.

-- 14b. Expectativa de ausencia: validacion_domicilio NO debe existir
-- Devuelve 1 fila siempre.
SELECT
    'fal_persona_domicilio'             AS tabla,
    'validacion_domicilio'              AS columna,
    CASE
        WHEN col.column_name IS NULL THEN 'OK'
        ELSE                              'COLUMNA_INESPERADA'
    END                                 AS diagnostico
FROM (SELECT 1) AS dummy
LEFT JOIN information_schema.columns AS col
    ON col.table_schema = DATABASE()
   AND col.table_name   = 'fal_persona_domicilio'
   AND col.column_name  = 'validacion_domicilio';

-- Esperado: 1 fila con diagnostico='OK'.
-- COLUMNA_INESPERADA: validacion_domicilio existe en la base — debe eliminarse.

-- ============================================================================
-- SECCIÓN 15 — Delta FULL-R1.2-CORRECCION-03: fal_documento
--   storage_key VARCHAR(196) NULL (era VARCHAR(500)); descripcion eliminado.
--   HUMAN_DECISION_CLOSED (spec § fal_documento, 2026-07-20).
-- ============================================================================

-- 15a. Expectativa de tipo y longitud: storage_key VARCHAR(196) NULL
SELECT
    esp.tabla_esp                       AS tabla,
    esp.columna_esp                     AS columna,
    col.data_type                       AS tipo_real,
    col.character_maximum_length        AS longitud_real,
    col.is_nullable                     AS nullable_real,
    esp.tipo_esp                        AS tipo_esperado,
    esp.longitud_esp                    AS longitud_esperada,
    esp.nullable_esp                    AS nullable_esperado,
    CASE
        WHEN col.column_name IS NULL
            THEN 'COLUMNA_AUSENTE'
        WHEN col.data_type <> esp.tipo_esp
            THEN 'TIPO_DIVERGENTE'
        WHEN col.character_maximum_length <> esp.longitud_esp
            THEN 'LONGITUD_DIVERGENTE'
        WHEN col.is_nullable <> esp.nullable_esp
            THEN 'NULLABILITY_DIVERGENTE'
        ELSE 'OK'
    END                                 AS diagnostico
FROM (
    SELECT 'fal_documento' AS tabla_esp, 'storage_key' AS columna_esp, 'varchar' AS tipo_esp, 196 AS longitud_esp, 'YES' AS nullable_esp
) AS esp
LEFT JOIN information_schema.columns AS col
    ON col.table_schema  = DATABASE()
   AND col.table_name    = esp.tabla_esp
   AND col.column_name   = esp.columna_esp
ORDER BY esp.tabla_esp, esp.columna_esp;

-- Esperado: 1 fila con diagnostico='OK'.
-- COLUMNA_AUSENTE      : storage_key no existe en la tabla.
-- LONGITUD_DIVERGENTE  : la longitud es distinta de 196 (ej. 500 = no migrada).
-- NULLABILITY_DIVERGENTE: nullabilidad difiere.

-- 15b. Expectativa de ausencia: descripcion NO debe existir en fal_documento
SELECT
    'fal_documento'                     AS tabla,
    'descripcion'                       AS columna,
    CASE
        WHEN col.column_name IS NULL THEN 'OK'
        ELSE                              'COLUMNA_INESPERADA'
    END                                 AS diagnostico
FROM (SELECT 1) AS dummy
LEFT JOIN information_schema.columns AS col
    ON col.table_schema = DATABASE()
   AND col.table_name   = 'fal_documento'
   AND col.column_name  = 'descripcion';

-- Esperado: 1 fila con diagnostico='OK'.
-- COLUMNA_INESPERADA: descripcion existe en fal_documento — fue eliminada en FULL-R1.2-CORRECCION-03.

-- ============================================================================
-- SECCIÓN 16 — Delta FULL-R1.2-CORRECCION-04: plantilla y contenido versionado
--   fal_documento_plantilla: codigo VARCHAR(12), nombre VARCHAR(64), sin descripcion.
--   fal_documento_plantilla_contenido: titulo VARCHAR(64) y metadata JSON declarativa.
--   HUMAN_DECISION_CLOSED (spec § plantillas documentales, 2026-07-20).
-- ============================================================================

-- 16a. Expectativas de tipo, longitud y nullability para campos VARCHAR canónicos
SELECT
    esp.tabla_esp                       AS tabla,
    esp.columna_esp                     AS columna,
    col.data_type                       AS tipo_real,
    col.character_maximum_length        AS longitud_real,
    col.is_nullable                     AS nullable_real,
    esp.tipo_esp                        AS tipo_esperado,
    esp.longitud_esp                    AS longitud_esperada,
    esp.nullable_esp                    AS nullable_esperado,
    CASE
        WHEN col.column_name IS NULL
            THEN 'COLUMNA_AUSENTE'
        WHEN col.data_type <> esp.tipo_esp
            THEN 'TIPO_DIVERGENTE'
        WHEN col.character_maximum_length <> esp.longitud_esp
            THEN 'LONGITUD_DIVERGENTE'
        WHEN col.is_nullable <> esp.nullable_esp
            THEN 'NULLABILITY_DIVERGENTE'
        ELSE 'OK'
    END                                 AS diagnostico
FROM (
    SELECT 'fal_documento_plantilla' AS tabla_esp, 'codigo' AS columna_esp, 'varchar' AS tipo_esp, 12 AS longitud_esp, 'NO' AS nullable_esp
    UNION ALL
    SELECT 'fal_documento_plantilla', 'nombre', 'varchar', 64, 'NO'
    UNION ALL
    SELECT 'fal_documento_plantilla_contenido', 'titulo', 'varchar', 64, 'NO'
) AS esp
LEFT JOIN information_schema.columns AS col
    ON col.table_schema  = DATABASE()
   AND col.table_name    = esp.tabla_esp
   AND col.column_name   = esp.columna_esp
ORDER BY esp.tabla_esp, esp.columna_esp;

-- Esperado: 3 filas, todas con diagnostico='OK'.
-- COLUMNA_AUSENTE / TIPO_DIVERGENTE / LONGITUD_DIVERGENTE /
-- NULLABILITY_DIVERGENTE identifican cualquier desvío respecto del contrato canónico.

-- 16b. variables_declaradas_json: obligatorio y documentado como metadata tipada,
--      requerida y etiquetada. MariaDB expone JSON como alias de LONGTEXT en
--      information_schema.columns; se aceptan ambos valores para compatibilidad.
SELECT
    'fal_documento_plantilla_contenido' AS tabla,
    'variables_declaradas_json'         AS columna,
    col.data_type                       AS tipo_real,
    col.is_nullable                     AS nullable_real,
    col.column_comment                  AS comentario_real,
    CASE
        WHEN col.column_name IS NULL
            THEN 'COLUMNA_AUSENTE'
        WHEN col.data_type NOT IN ('json', 'longtext')
            THEN 'TIPO_DIVERGENTE'
        WHEN col.is_nullable <> 'NO'
            THEN 'NULLABILITY_DIVERGENTE'
        WHEN LOWER(COALESCE(col.column_comment, '')) NOT LIKE '%tipad%'
          OR LOWER(COALESCE(col.column_comment, '')) NOT LIKE '%requerid%'
          OR LOWER(COALESCE(col.column_comment, '')) NOT LIKE '%etiquet%'
            THEN 'COMMENT_DIVERGENTE'
        ELSE 'OK'
    END                                 AS diagnostico
FROM (SELECT 1) AS dummy
LEFT JOIN information_schema.columns AS col
    ON col.table_schema = DATABASE()
   AND col.table_name   = 'fal_documento_plantilla_contenido'
   AND col.column_name  = 'variables_declaradas_json';

-- Esperado: 1 fila con diagnostico='OK'.
-- COMMENT_DIVERGENTE: el COMMENT físico no expresa metadata tipada/requerida/etiquetada.

-- 16c. Expectativa de ausencia: descripcion NO debe existir en fal_documento_plantilla
SELECT
    'fal_documento_plantilla'           AS tabla,
    'descripcion'                       AS columna,
    CASE
        WHEN col.column_name IS NULL THEN 'OK'
        ELSE                              'COLUMNA_INESPERADA'
    END                                 AS diagnostico
FROM (SELECT 1) AS dummy
LEFT JOIN information_schema.columns AS col
    ON col.table_schema = DATABASE()
   AND col.table_name   = 'fal_documento_plantilla'
   AND col.column_name  = 'descripcion';

-- Esperado: 1 fila con diagnostico='OK'.
-- COLUMNA_INESPERADA: descripcion reapareció en fal_documento_plantilla.
