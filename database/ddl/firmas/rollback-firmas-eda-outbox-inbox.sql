-- APROBADO COMO DISEÑO — DDL_READY, DDL_NOT_EXECUTED
-- NO EJECUTAR EN PRODUCCIÓN SIN BACKUP, PRECHECKS Y AUTORIZACIÓN
-- =============================================================================
-- ROLLBACK — fir_outbox / fir_bus_checkpoint / fir_inbox / fir_inbox_error
--            (bounded context Firmas)
-- Script: rollback-firmas-eda-outbox-inbox.sql
-- Trabajo: SPEC-FIRMAS-EDA-CLOSURE-001 (DEC-EDA-028)
--
-- Revierte create-firmas-eda-outbox-inbox.sql, en orden inverso al de
-- aplicación. Igual que el forward, NO ejecuta `USE <base>`: el operador
-- debe seleccionar la base de destino antes de ejecutar este archivo.
--
-- Orden de DROP respeta la única FK real del paquete: fir_inbox_error
-- referencia fir_inbox (ON DELETE RESTRICT), por lo que debe caer antes.
-- fir_outbox y fir_bus_checkpoint no tienen FK entrante ni saliente: pueden
-- caer en cualquier orden entre sí.
--
-- PRECONDICIÓN OBLIGATORIA — no ejecutar sin haber corrido antes:
--   database/diagnostics/firmas/verify-firmas-eda-outbox-inbox-rollback-precheck.sql
-- (verifica que no existan filas en fir_outbox/fir_inbox/fir_inbox_error, ni
-- que fir_bus_checkpoint haya avanzado más allá de su seed inicial; este
-- rollback destruye datos).
--
-- Fuente normativa (lado Faltas): backend/api-faltas-core/docs/spec-as-source/50-persistence/firma-eda-physical-model.md §9
-- Forward correspondiente: database/ddl/firmas/create-firmas-eda-outbox-inbox.sql
-- =============================================================================

SET NAMES utf8mb4;

DROP TABLE IF EXISTS fir_inbox_error;
DROP TABLE IF EXISTS fir_inbox;
DROP TABLE IF EXISTS fir_bus_checkpoint;
DROP TABLE IF EXISTS fir_outbox;

-- =============================================================================
-- FIN DEL SCRIPT DE ROLLBACK — fir_outbox / fir_bus_checkpoint / fir_inbox /
--                                fir_inbox_error (bounded context Firmas)
-- =============================================================================
-- Tablas eliminadas: 4 (fir_inbox_error, fir_inbox, fir_bus_checkpoint, fir_outbox)
-- =============================================================================
