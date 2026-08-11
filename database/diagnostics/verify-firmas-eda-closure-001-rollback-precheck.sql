-- verify-firmas-eda-closure-001-rollback-precheck.sql
-- SOLO LECTURA — No altera ningún objeto. Solo SELECT.
--
-- Propósito: verificar precondiciones ANTES de ejecutar
--            alter-firmas-eda-closure-001-rollback.sql. El rollback NUNCA
--            se declara completo/seguro de forma incondicional
--            (firma-eda-physical-model.md §14).
-- Motor    : MariaDB 12.3.2, sb_faltas_db
-- Trabajo  : SPEC-FIRMAS-EDA-CLOSURE-001
-- Uso      : ejecutar manualmente desde HeidiSQL ANTES de ejecutar el
--            rollback. Si CUALQUIER sección reporta 'BLOQUEADO', DETENER —
--            no ejecutar el rollback hasta resolver manualmente la causa.
--
-- Ejecutar TAMBIÉN database/ddl/rollback-grants-firmas-eda-bus.sql antes que
-- este rollback (revoca INSERT/SELECT/DELETE directos sobre bus_evento que
-- el rollback luego elimina).
-- ============================================================================

-- ============================================================================
-- SECCIÓN 1 — Filas con estado_firma_req entre 6 y 9 (estados nuevos sin
--              representación en el catálogo de 5 valores original)
-- ============================================================================

SELECT
    COUNT(*) AS filas_estado_nuevo,
    CASE WHEN COUNT(*) = 0 THEN 'OK_SIN_FILAS' ELSE 'BLOQUEADO_HAY_FILAS_ESTADO_NUEVO' END AS resultado
FROM fal_documento_firma_req
WHERE estado_firma_req BETWEEN 6 AND 9;

-- ============================================================================
-- SECCIÓN 2 — Filas en cualquiera de las 6 tablas nuevas del bus (cualquier
--              dato en las tablas nuevas se perdería sin posibilidad de
--              reconciliación). fal_bus_checkpoint es la única exenta del
--              bloqueo por conteo puro: su fila singleton de seed
--              (checkpoint_id=1) es esperada; lo que bloquea es que su
--              ultimo_evento_id ya haya avanzado más allá de 0 (evidencia de
--              lectura real del bus).
-- ============================================================================

SELECT 'fal_documento_firma_resultado' AS tabla, COUNT(*) AS filas,
       CASE WHEN COUNT(*) = 0 THEN 'OK_SIN_FILAS' ELSE 'BLOQUEADO_HAY_FILAS' END AS resultado
FROM fal_documento_firma_resultado
UNION ALL
SELECT 'fal_outbox', COUNT(*),
       CASE WHEN COUNT(*) = 0 THEN 'OK_SIN_FILAS' ELSE 'BLOQUEADO_HAY_FILAS' END
FROM fal_outbox
UNION ALL
SELECT 'bus_evento', COUNT(*),
       CASE WHEN COUNT(*) = 0 THEN 'OK_SIN_FILAS' ELSE 'BLOQUEADO_HAY_FILAS' END
FROM bus_evento
UNION ALL
SELECT 'fal_inbox', COUNT(*),
       CASE WHEN COUNT(*) = 0 THEN 'OK_SIN_FILAS' ELSE 'BLOQUEADO_HAY_FILAS' END
FROM fal_inbox
UNION ALL
SELECT 'fal_inbox_error', COUNT(*),
       CASE WHEN COUNT(*) = 0 THEN 'OK_SIN_FILAS' ELSE 'BLOQUEADO_HAY_FILAS' END
FROM fal_inbox_error;

-- 2b. fal_bus_checkpoint: exactamente 1 fila (seed), con ultimo_evento_id
--     todavía en 0 (nunca avanzó; ningún lector real lo usó)
SELECT
    COUNT(*) AS filas_presentes,
    MAX(ultimo_evento_id) AS ultimo_evento_id_maximo,
    CASE
        WHEN COUNT(*) <> 1 THEN 'BLOQUEADO_FILAS_INESPERADAS'
        WHEN MAX(ultimo_evento_id) <> 0 THEN 'BLOQUEADO_CHECKPOINT_YA_AVANZADO'
        ELSE 'OK_SOLO_SEED'
    END AS resultado
FROM fal_bus_checkpoint;

-- ============================================================================
-- SECCIÓN 3 — Columnas nuevas de fal_documento_firma_req / fal_documento_firma
--              / fal_documento con algún valor no NULL/no 0 (evidencia de
--              que ya se ejecutó código productivo dependiente)
-- ============================================================================

SELECT
    'fal_documento_firma_req' AS tabla,
    COUNT(*) AS filas_con_dato_no_default,
    CASE WHEN COUNT(*) = 0 THEN 'OK_SIN_DATO' ELSE 'BLOQUEADO_HAY_DATO_PRODUCTIVO' END AS resultado
FROM fal_documento_firma_req
WHERE version_row <> 0
   OR solicitud_firma_id IS NOT NULL
   OR revision_solicitud <> 0
   OR fh_solicitud IS NOT NULL
   OR fh_aceptacion IS NOT NULL
   OR fh_inicio_firma IS NOT NULL
   OR vence_en IS NOT NULL
   OR fh_terminal IS NOT NULL
   OR motivo_terminal_firma_req IS NOT NULL
   OR detalle_terminal_firma_req IS NOT NULL
   OR revision_artefacto_preparada IS NOT NULL
   OR hash_entrada_preparado IS NOT NULL
   OR content_length_preparado IS NOT NULL
   OR fh_preparacion IS NOT NULL;

SELECT
    'fal_documento_firma' AS tabla,
    COUNT(*) AS filas_con_dato_no_default,
    CASE WHEN COUNT(*) = 0 THEN 'OK_SIN_DATO' ELSE 'BLOQUEADO_HAY_DATO_PRODUCTIVO' END AS resultado
FROM fal_documento_firma
WHERE hash_salida IS NOT NULL
   OR revision_entrada IS NOT NULL
   OR revision_salida IS NOT NULL
   OR actor_tecnico_tipo IS NOT NULL
   OR resultado_firma_id IS NOT NULL
   OR algoritmo_hash IS NOT NULL
   OR resultado_validacion IS NOT NULL;

SELECT
    'fal_documento' AS tabla,
    COUNT(*) AS filas_con_dato_no_default,
    CASE WHEN COUNT(*) = 0 THEN 'OK_SIN_DATO' ELSE 'BLOQUEADO_HAY_DATO_PRODUCTIVO' END AS resultado
FROM fal_documento
WHERE revision_artefacto <> 0;

-- ============================================================================
-- SECCIÓN 4 — bus_evento todavía referenciada por grants activos de las 5
--              cuentas de aplicación (revisar que
--              rollback-grants-firmas-eda-bus.sql ya se ejecutó)
-- ============================================================================

SELECT
    grantee, table_name AS objeto, privilege_type,
    'REVISAR_GRANT_TODAVIA_ACTIVO' AS resultado
FROM information_schema.table_privileges
WHERE table_schema = DATABASE()
  AND table_name = 'bus_evento'
  AND grantee LIKE '%bus_%';
-- Esperado: 0 filas si rollback-grants-firmas-eda-bus.sql ya corrió antes.

-- ============================================================================
-- SECCIÓN 5 — VEREDICTO GLOBAL ÚNICO (READY/BLOCKED)
--              Consolida las secciones 1 a 2b (los bloqueos duros: estados
--              nuevos, filas en tablas nuevas fuera del seed esperado,
--              checkpoint ya avanzado, datos productivos en columnas
--              nuevas). La sección 4 (grants activos) queda como REVISAR
--              manual, no como bloqueo duro automático, porque un grant
--              activo es responsabilidad de ejecutar antes
--              rollback-grants-firmas-eda-bus.sql, no un dato irreversible.
-- ============================================================================

SELECT
    (SELECT COUNT(*) FROM fal_documento_firma_req WHERE estado_firma_req BETWEEN 6 AND 9) AS filas_estado_nuevo,
    (
        (SELECT COUNT(*) FROM fal_documento_firma_resultado)
        + (SELECT COUNT(*) FROM fal_outbox)
        + (SELECT COUNT(*) FROM bus_evento)
        + (SELECT COUNT(*) FROM fal_inbox)
        + (SELECT COUNT(*) FROM fal_inbox_error)
    ) AS filas_en_tablas_nuevas_sin_seed,
    (SELECT MAX(ultimo_evento_id) FROM fal_bus_checkpoint) AS checkpoint_ultimo_evento_id,
    (
        (SELECT COUNT(*) FROM fal_documento_firma_req
           WHERE version_row <> 0 OR solicitud_firma_id IS NOT NULL OR revision_solicitud <> 0
              OR fh_solicitud IS NOT NULL OR fh_aceptacion IS NOT NULL OR fh_inicio_firma IS NOT NULL
              OR vence_en IS NOT NULL OR fh_terminal IS NOT NULL OR motivo_terminal_firma_req IS NOT NULL
              OR detalle_terminal_firma_req IS NOT NULL OR revision_artefacto_preparada IS NOT NULL
              OR hash_entrada_preparado IS NOT NULL OR content_length_preparado IS NOT NULL
              OR fh_preparacion IS NOT NULL)
        + (SELECT COUNT(*) FROM fal_documento_firma
             WHERE hash_salida IS NOT NULL OR revision_entrada IS NOT NULL OR revision_salida IS NOT NULL
                OR actor_tecnico_tipo IS NOT NULL OR resultado_firma_id IS NOT NULL
                OR algoritmo_hash IS NOT NULL OR resultado_validacion IS NOT NULL)
        + (SELECT COUNT(*) FROM fal_documento WHERE revision_artefacto <> 0)
    ) AS filas_con_dato_productivo,
    CASE WHEN
        (SELECT COUNT(*) FROM fal_documento_firma_req WHERE estado_firma_req BETWEEN 6 AND 9) = 0
        AND (
            (SELECT COUNT(*) FROM fal_documento_firma_resultado)
            + (SELECT COUNT(*) FROM fal_outbox)
            + (SELECT COUNT(*) FROM bus_evento)
            + (SELECT COUNT(*) FROM fal_inbox)
            + (SELECT COUNT(*) FROM fal_inbox_error)
        ) = 0
        AND COALESCE((SELECT MAX(ultimo_evento_id) FROM fal_bus_checkpoint), 0) = 0
        AND (
            (SELECT COUNT(*) FROM fal_documento_firma_req
               WHERE version_row <> 0 OR solicitud_firma_id IS NOT NULL OR revision_solicitud <> 0
                  OR fh_solicitud IS NOT NULL OR fh_aceptacion IS NOT NULL OR fh_inicio_firma IS NOT NULL
                  OR vence_en IS NOT NULL OR fh_terminal IS NOT NULL OR motivo_terminal_firma_req IS NOT NULL
                  OR detalle_terminal_firma_req IS NOT NULL OR revision_artefacto_preparada IS NOT NULL
                  OR hash_entrada_preparado IS NOT NULL OR content_length_preparado IS NOT NULL
                  OR fh_preparacion IS NOT NULL)
            + (SELECT COUNT(*) FROM fal_documento_firma
                 WHERE hash_salida IS NOT NULL OR revision_entrada IS NOT NULL OR revision_salida IS NOT NULL
                    OR actor_tecnico_tipo IS NOT NULL OR resultado_firma_id IS NOT NULL
                    OR algoritmo_hash IS NOT NULL OR resultado_validacion IS NOT NULL)
            + (SELECT COUNT(*) FROM fal_documento WHERE revision_artefacto <> 0)
        ) = 0
    THEN 'READY' ELSE 'BLOCKED' END AS veredicto_global;

-- ============================================================================
-- FIN DEL ROLLBACK PRECHECK
-- ============================================================================
