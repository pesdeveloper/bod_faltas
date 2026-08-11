-- DIAGNÓSTICO READ-ONLY — no modifica datos ni esquema
-- =============================================================================
-- ROLLBACK PRECHECK — fir_outbox / fir_bus_checkpoint / fir_inbox /
--                      fir_inbox_error (bounded context Firmas)
-- Script: verify-firmas-eda-outbox-inbox-rollback-precheck.sql
-- Trabajo: SPEC-FIRMAS-EDA-CLOSURE-001 (DEC-EDA-028)
--
-- Se ejecuta ANTES de database/ddl/firmas/rollback-firmas-eda-outbox-inbox.sql.
-- Independiente de verify-firmas-eda-outbox-inbox-precheck.sql (ese precheck
-- valida condiciones para el CREATE; este valida condiciones para el DROP).
--
-- Igual que el resto del paquete de Firmas, NO ejecuta `USE <base>`: el
-- operador debe seleccionar la base de destino antes de ejecutar este
-- archivo.
--
-- Verifica:
--   1. que el cliente seleccionó explícitamente una base de datos;
--   2. que las 4 tablas existen (si no existen, el rollback no tiene nada
--      que revertir: revisar manualmente antes de continuar);
--   3. que fir_outbox, fir_inbox y fir_inbox_error no tienen filas — el
--      rollback (DROP TABLE) destruye datos sin posibilidad de
--      reconciliación;
--   4. que fir_bus_checkpoint solo tiene su fila singleton de seed
--      (ultimo_evento_id todavía en 0; ningún lector real avanzó el
--      checkpoint).
--
-- Uso: revisar manualmente el resultado antes de continuar. Si
-- `resultado` <> 'OK_SIN_FILAS'/'OK_SOLO_SEED' para alguna tabla presente,
-- DETENER — no ejecutar el rollback hasta resolver manualmente la causa
-- (los datos se perderían de forma irreversible).
-- =============================================================================

SELECT
    DATABASE() AS base_actual,
    CASE WHEN DATABASE() IS NULL THEN 'NO' ELSE 'SI' END AS base_seleccionada;

SELECT
    t_esp.tabla,
    CASE
        WHEN DATABASE() IS NULL THEN 'BLOQUEADO_SIN_BASE_SELECCIONADA'
        WHEN NOT EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = DATABASE() AND table_name = t_esp.tabla
        ) THEN 'REVISAR_NO_EXISTE_NADA_QUE_REVERTIR'
        ELSE 'OK_TABLA_PRESENTE'
    END AS estado
FROM (
    SELECT 'fir_outbox' AS tabla
    UNION ALL SELECT 'fir_bus_checkpoint'
    UNION ALL SELECT 'fir_inbox'
    UNION ALL SELECT 'fir_inbox_error'
) t_esp;

-- Conteo de filas: solo se ejecuta de forma segura si la tabla existe; si
-- alguna de las 4 tablas no existe todavía, esta sección fallará con
-- "table doesn't exist" — señal adicional, coherente con el resultado de la
-- sección anterior, de que no hay nada que revertir.
SELECT 'fir_outbox' AS tabla, COUNT(*) AS filas,
       CASE WHEN COUNT(*) = 0 THEN 'OK_SIN_FILAS' ELSE 'BLOQUEADO_HAY_FILAS' END AS resultado
FROM fir_outbox
UNION ALL
SELECT 'fir_inbox' AS tabla, COUNT(*) AS filas,
       CASE WHEN COUNT(*) = 0 THEN 'OK_SIN_FILAS' ELSE 'BLOQUEADO_HAY_FILAS' END AS resultado
FROM fir_inbox
UNION ALL
SELECT 'fir_inbox_error' AS tabla, COUNT(*) AS filas,
       CASE WHEN COUNT(*) = 0 THEN 'OK_SIN_FILAS' ELSE 'BLOQUEADO_HAY_FILAS' END AS resultado
FROM fir_inbox_error;

-- fir_bus_checkpoint: exactamente 1 fila (seed), con ultimo_evento_id
-- todavía en 0
SELECT
    COUNT(*) AS filas_presentes,
    MAX(ultimo_evento_id) AS ultimo_evento_id_maximo,
    CASE
        WHEN COUNT(*) <> 1 THEN 'BLOQUEADO_FILAS_INESPERADAS'
        WHEN MAX(ultimo_evento_id) <> 0 THEN 'BLOQUEADO_CHECKPOINT_YA_AVANZADO'
        ELSE 'OK_SOLO_SEED'
    END AS resultado
FROM fir_bus_checkpoint;

-- =============================================================================
-- VEREDICTO GLOBAL — revisar manualmente que fir_outbox/fir_inbox/
-- fir_inbox_error reporten OK_SIN_FILAS y que fir_bus_checkpoint reporte
-- OK_SOLO_SEED antes de ejecutar rollback-firmas-eda-outbox-inbox.sql. Si
-- alguna reporta BLOQUEADO_*, DETENER: el DROP TABLE destruye esos datos sin
-- posibilidad de reconciliación.
-- =============================================================================
-- FIN DEL ROLLBACK PRECHECK — fir_outbox / fir_bus_checkpoint / fir_inbox /
--                               fir_inbox_error
-- =============================================================================
