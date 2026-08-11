-- DIAGNÓSTICO READ-ONLY — no modifica datos ni esquema
-- =============================================================================
-- PRECHECK — fir_outbox / fir_bus_checkpoint / fir_inbox / fir_inbox_error
--            (bounded context Firmas)
-- Script: verify-firmas-eda-outbox-inbox-precheck.sql
-- Trabajo: SPEC-FIRMAS-EDA-CLOSURE-001 (DEC-EDA-028)
--
-- Se ejecuta ANTES de database/ddl/firmas/create-firmas-eda-outbox-inbox.sql.
-- Verifica:
--   1. que el cliente seleccionó explícitamente una base de datos
--      (DATABASE() IS NOT NULL) — este paquete deliberadamente no ejecuta
--      USE, porque el nombre de la base de Firmas no es un objeto lógico de
--      Bod-Faltas;
--   2. que fir_outbox, fir_bus_checkpoint, fir_inbox y fir_inbox_error NO
--      existen todavía (evita un CREATE TABLE fallido sobre un objeto ya
--      presente).
--
-- Uso: revisar manualmente el resultado antes de continuar. Si
-- `base_seleccionada` = 'NO' o `estado` <> 'LISTO_PARA_CREATE' para alguna
-- fila, DETENER — no ejecutar el forward.
-- =============================================================================

SELECT
    DATABASE() AS base_actual,
    CASE WHEN DATABASE() IS NULL THEN 'NO' ELSE 'SI' END AS base_seleccionada;

SELECT
    t_esp.tabla,
    CASE
        WHEN DATABASE() IS NULL THEN 'BLOQUEADO_SIN_BASE_SELECCIONADA'
        WHEN EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = DATABASE() AND table_name = t_esp.tabla
        ) THEN 'BLOQUEADO_YA_EXISTE'
        ELSE 'LISTO_PARA_CREATE'
    END AS estado
FROM (
    SELECT 'fir_outbox' AS tabla
    UNION ALL SELECT 'fir_bus_checkpoint'
    UNION ALL SELECT 'fir_inbox'
    UNION ALL SELECT 'fir_inbox_error'
) t_esp;

-- =============================================================================
-- FIN DEL PRECHECK — fir_outbox / fir_bus_checkpoint / fir_inbox / fir_inbox_error
-- =============================================================================
