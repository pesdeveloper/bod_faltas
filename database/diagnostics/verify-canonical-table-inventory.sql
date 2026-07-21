-- verify-canonical-table-inventory.sql
-- SOLO LECTURA — No altera ningún objeto. Solo SELECT sobre information_schema.
--
-- Propósito: verificar el estado del inventario canónico de 65 tablas Faltas.
--            Mientras el script DDL esté incompleto, informa faltantes sin tratarlos
--            como error fatal del slice R1.
-- Motor    : MariaDB 12.3.2, sb_faltas_db
-- Uso      : ejecutar luego del DDL del dominio (o parcialmente durante construcción).
-- ============================================================================

-- ============================================================================
-- 1. Estado de cada tabla canónica esperada (65 tablas)
--    Muestra presencia/ausencia sin tratarla como error bloqueante.
-- ============================================================================
SELECT
    expected.tabla_canonica             AS tabla,
    expected.estado_esperado            AS estado_spec,
    CASE
        WHEN t.table_name IS NOT NULL THEN 'PRESENTE'
        ELSE 'AUSENTE'
    END                                 AS estado_fisico,
    expected.slice_previsto             AS slice_previsto
FROM (
    -- PREEXISTING_CANONICAL_ADOPTED (1)
    SELECT 'fal_rubro_version'                          AS tabla_canonica,
           'PREEXISTING_CANONICAL_ADOPTED'              AS estado_esperado,
           'BASELINE'                                   AS slice_previsto
    -- TO_CREATE (64) — orden topológico G1..G12
    UNION ALL SELECT 'fal_dependencia',          'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_inspector',            'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_firmante',             'TO_CREATE', 'R1'
    UNION ALL SELECT 'fal_persona',              'TO_CREATE', 'R1'
    UNION ALL SELECT 'fal_vehiculo_marca',       'TO_CREATE', 'R1'
    UNION ALL SELECT 'num_politica',             'TO_CREATE', 'R1'
    UNION ALL SELECT 'fal_dia_no_computable',    'TO_CREATE', 'R1'
    UNION ALL SELECT 'fal_motivo_archivo',       'TO_CREATE', 'R1'
    UNION ALL SELECT 'fal_normativa_faltas',     'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_medida_preventiva',    'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_lote_correo',          'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_documento_plantilla',  'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_dependencia_version',  'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_inspector_version',    'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_firmante_version',     'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_vehiculo_modelo',      'TO_CREATE', 'R1'
    UNION ALL SELECT 'num_talonario',            'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_dependencia_normativa','TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_articulo_normativa_faltas', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_documento_plantilla_firma_req', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_documento_plantilla_contenido', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_documento_plantilla_default', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_firmante_version_habilitacion', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_tarifario_unidad_faltas', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_articulo_medida_preventiva', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'num_talonario_ambito',     'TO_CREATE', 'R2+'
    UNION ALL SELECT 'num_talonario_inspector',  'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_persona_domicilio',    'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta',                 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_evidencia',       'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_observacion',          'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_transito',        'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_transito_alcoholemia', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_vehiculo',        'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_contravencion',   'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_sustancias_alimenticias', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_paralizacion',    'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_archivo',         'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_articulo_infringido', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_qr_acceso',       'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_gestion_externa', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_valorizacion',    'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_bloqueante_cierre_material', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_medida_preventiva', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_documento',            'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_valorizacion_item', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_documento',       'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_documento_firma_req',  'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_documento_redaccion',  'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_notificacion',         'TO_CREATE', 'R2+'
    UNION ALL SELECT 'num_talonario_movimiento', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_fallo',           'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_documento_firma',      'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_notificacion_intento', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_notificacion_acuse',   'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_evento',          'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_apelacion',       'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_obligacion_pago', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_snapshot',        'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_apelacion_documento', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_forma_pago',      'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_plan_pago_ref',   'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_pago_movimiento', 'TO_CREATE', 'R2+'
    UNION ALL SELECT 'fal_acta_economia_proyeccion', 'TO_CREATE', 'R2+'
) AS expected
LEFT JOIN information_schema.tables AS t
    ON t.table_schema = DATABASE()
   AND t.table_name = expected.tabla_canonica
ORDER BY expected.slice_previsto, expected.tabla_canonica;

-- ============================================================================
-- 2. Resumen numérico del estado del inventario
-- ============================================================================
SELECT
    65                                      AS tablas_canonicas_esperadas_total,
    1                                       AS preexisting_esperadas,
    64                                      AS to_create_esperadas,
    (SELECT COUNT(*) FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name IN (
           'fal_rubro_version',
           'fal_dependencia','fal_inspector','fal_firmante','fal_persona',
           'fal_vehiculo_marca','num_politica','fal_dia_no_computable','fal_motivo_archivo',
           'fal_normativa_faltas','fal_medida_preventiva','fal_lote_correo',
           'fal_documento_plantilla','fal_dependencia_version','fal_inspector_version',
           'fal_firmante_version','fal_vehiculo_modelo','num_talonario',
           'fal_dependencia_normativa','fal_articulo_normativa_faltas',
           'fal_documento_plantilla_firma_req','fal_documento_plantilla_contenido',
           'fal_documento_plantilla_default','fal_firmante_version_habilitacion',
           'fal_tarifario_unidad_faltas','fal_articulo_medida_preventiva',
           'num_talonario_ambito','num_talonario_inspector','fal_persona_domicilio',
           'fal_acta','fal_acta_evidencia','fal_observacion','fal_acta_transito',
           'fal_acta_transito_alcoholemia','fal_acta_vehiculo','fal_acta_contravencion',
           'fal_acta_sustancias_alimenticias','fal_acta_paralizacion','fal_acta_archivo',
           'fal_acta_articulo_infringido','fal_acta_qr_acceso','fal_acta_gestion_externa',
           'fal_acta_valorizacion','fal_acta_bloqueante_cierre_material',
           'fal_acta_medida_preventiva','fal_documento','fal_acta_valorizacion_item',
           'fal_acta_documento','fal_documento_firma_req','fal_documento_redaccion',
           'fal_notificacion','num_talonario_movimiento','fal_acta_fallo',
           'fal_documento_firma','fal_notificacion_intento','fal_notificacion_acuse',
           'fal_acta_evento','fal_acta_apelacion','fal_acta_obligacion_pago',
           'fal_acta_snapshot','fal_acta_apelacion_documento','fal_acta_forma_pago',
           'fal_acta_plan_pago_ref','fal_acta_pago_movimiento','fal_acta_economia_proyeccion'))
    AS tablas_canonicas_presentes_actual,

    -- Verificar que las tablas sync y geo NO están en el conteo canónico:
    (SELECT COUNT(*) FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name IN ('fal_informix_sync_error','fal_informix_sync_run'))
    AS tablas_sync_no_canonicas_presentes;

-- ============================================================================
-- 3. Tablas FULL-R1 específicamente (deben estar presentes después del slice FULL-R1)
--    Incluye fal_persona: GAP-DDL-R1-PERSONA-LONGITUD-01 cerrado en FULL-R1.
-- ============================================================================
SELECT
    expected.tabla_r1                   AS tabla_r1,
    CASE WHEN t.table_name IS NOT NULL THEN 'PRESENTE' ELSE 'AUSENTE' END AS estado
FROM (
    SELECT 'fal_dia_no_computable' AS tabla_r1
    UNION ALL SELECT 'fal_firmante'
    UNION ALL SELECT 'fal_motivo_archivo'
    UNION ALL SELECT 'fal_persona'
    UNION ALL SELECT 'fal_vehiculo_marca'
    UNION ALL SELECT 'fal_vehiculo_modelo'
    UNION ALL SELECT 'num_politica'
) AS expected
LEFT JOIN information_schema.tables AS t
    ON t.table_schema = DATABASE() AND t.table_name = expected.tabla_r1
ORDER BY tabla_r1;

-- Esperado post-FULL-R1: 7 filas con estado PRESENTE.
-- fal_persona incluida en FULL-R1: GAP-DDL-R1-PERSONA-LONGITUD-01 CERRADO.
