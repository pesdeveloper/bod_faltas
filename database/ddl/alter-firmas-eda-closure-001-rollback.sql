-- APROBADO COMO DISEÑO — DDL_READY, DDL_NOT_EXECUTED
-- NO EJECUTAR EN PRODUCCIÓN SIN BACKUP, PRECHECKS Y AUTORIZACIÓN
-- =============================================================================
-- ROLLBACK — Integración EDA Faltas ↔ Firmas
-- Script: alter-firmas-eda-closure-001-rollback.sql
-- Trabajo: SPEC-FIRMAS-EDA-CLOSURE-001
--
-- Revierte íntegramente alter-firmas-eda-closure-001-forward.sql, en orden
-- inverso al de aplicación (firma-eda-physical-model.md §12/§14). Modelo
-- mínimo aprobado: sin vistas, sin rutinas SQL SECURITY DEFINER, sin fan-out
-- central; el bus se reduce a bus_evento + Outbox/checkpoint/Inbox/Inbox
-- error locales de Faltas:
--   1. DROP TABLE fal_inbox_error   (FK a fal_inbox; debe caer antes)
--   2. DROP TABLE fal_inbox
--   3. DROP TABLE fal_bus_checkpoint
--   4. DROP TABLE bus_evento
--   5. DROP TABLE fal_outbox
--   6. ALTER TABLE fal_documento_firma DROP CONSTRAINT fk_firma_resultado
--      (FK hacia fal_documento_firma_resultado; debe caer antes del DROP TABLE)
--   7. DROP TABLE fal_documento_firma_resultado  (FK a fal_documento y
--      fal_documento_firma_req; cae antes de tocar esas dos tablas)
--   8. ALTER TABLE fal_documento              DROP COLUMN revision_artefacto
--   9. ALTER TABLE fal_documento_firma        DROP COLUMN ... (orden inverso
--      de creación) + DROP de sus constraints/índices nuevos
--  10. ALTER TABLE fal_documento_firma_req    DROP COLUMN / DROP CONSTRAINT /
--      DROP INDEX (orden inverso de creación) y MODIFY COLUMN
--      estado_firma_req para restaurar su comentario original de 5 valores.
--
-- Este script NO usa SET FOREIGN_KEY_CHECKS = 0/1: el orden de DROP respeta
-- las dependencias de FK reales (fal_inbox_error antes de fal_inbox;
-- fk_firma_resultado antes de fal_documento_firma_resultado;
-- fal_documento_firma_resultado antes de tocar fal_documento y
-- fal_documento_firma_req). bus_evento, fal_outbox y fal_bus_checkpoint no
-- tienen FK entrante ni saliente: pueden caer en cualquier orden entre sí.
-- Cualquier error de FK debe interrumpir el script de forma visible.
--
-- PRECONDICIÓN OBLIGATORIA — no ejecutar sin haber corrido antes:
--   database/diagnostics/verify-firmas-eda-closure-001-rollback-precheck.sql
-- Ese precheck ABORTA (reporta BLOQUEADO) si existen filas nuevas en
-- cualquiera de las tablas/columnas creadas por el forward, salvo la fila
-- singleton de fal_bus_checkpoint (seed, exenta del check de filas nuevas —
-- firma-eda-physical-model.md §14). Este rollback DESTRUYE datos en las
-- columnas/tablas eliminadas: nunca se declara completo de forma
-- incondicional.
--
-- El rollback de grants (database/ddl/rollback-grants-firmas-eda-bus.sql) se
-- ejecuta ANTES que este script (revoca INSERT column-level/SELECT/DELETE
-- directos sobre bus_evento que este script luego elimina).
--
-- SEGURIDAD DEL ROLLBACK:
--   Solo es seguro si no se ejecutó código productivo que ya dependa de las
--   columnas/tablas nuevas (este cierre es documental; IMPLEMENTATION_NOT_STARTED).
--
-- Fuente normativa: backend/api-faltas-core/docs/spec-as-source/50-persistence/firma-eda-physical-model.md §12/§14
-- Forward correspondiente: database/ddl/alter-firmas-eda-closure-001-forward.sql
-- Precheck de rollback: database/diagnostics/verify-firmas-eda-closure-001-rollback-precheck.sql
-- Rollback de grants (ejecutar antes de este script): database/ddl/rollback-grants-firmas-eda-bus.sql
-- Recuperación de ejecución parcial: firma-eda-physical-model.md §14 (punto 3)
-- =============================================================================

USE sb_faltas_db;

SET NAMES utf8mb4;

-- =============================================================================
-- SECCIÓN 01 — Tablas nuevas del bus, en orden inverso al forward §5:
--   fal_inbox_error, fal_inbox, fal_bus_checkpoint, bus_evento, fal_outbox
-- =============================================================================

DROP TABLE IF EXISTS fal_inbox_error;
DROP TABLE IF EXISTS fal_inbox;
DROP TABLE IF EXISTS fal_bus_checkpoint;
DROP TABLE IF EXISTS bus_evento;
DROP TABLE IF EXISTS fal_outbox;

-- =============================================================================
-- SECCIÓN 02 — fal_documento_firma: revertir fk_firma_resultado (DEC-EDA-034)
-- antes de eliminar fal_documento_firma_resultado
-- =============================================================================

ALTER TABLE fal_documento_firma
    DROP CONSTRAINT fk_firma_resultado;

-- =============================================================================
-- SECCIÓN 03 — fal_documento_firma_resultado: staging temporal (DEC-EDA-024/032/033)
-- =============================================================================

DROP TABLE IF EXISTS fal_documento_firma_resultado;

-- =============================================================================
-- SECCIÓN 04 — fal_documento: revertir revision_artefacto (DEC-EDA-024/031)
-- =============================================================================

ALTER TABLE fal_documento
    DROP CONSTRAINT chk_documento_revision_artefacto;

ALTER TABLE fal_documento
    DROP COLUMN revision_artefacto;

-- =============================================================================
-- SECCIÓN 05 — fal_documento_firma: revertir columnas/constraints/índices
--               nuevos (DEC-EDA-013/021/023/029/034/035)
-- =============================================================================

ALTER TABLE fal_documento_firma
    DROP CONSTRAINT chk_firma_eda_completo,
    DROP CONSTRAINT chk_firma_revision_salida,
    DROP CONSTRAINT chk_firma_cert_vigencia,
    DROP CONSTRAINT chk_firma_resultado_validacion,
    DROP CONSTRAINT chk_firma_resultado_revocacion,
    DROP CONSTRAINT chk_firma_fuente_revocacion,
    DROP CONSTRAINT chk_firma_resultado_cadena,
    DROP CONSTRAINT chk_firma_actor_tecnico_tipo,
    DROP CONSTRAINT chk_firma_perfil_unico,
    DROP CONSTRAINT chk_firma_hash_algoritmo,
    DROP INDEX uq_firma_resultado,
    DROP INDEX ix_firma_event_id,
    DROP INDEX ix_firma_correlation;

ALTER TABLE fal_documento_firma
    DROP COLUMN version_validador,
    DROP COLUMN resultado_validacion,
    DROP COLUMN tsa_instante,
    DROP COLUMN tsa_nombre,
    DROP COLUMN resultado_revocacion,
    DROP COLUMN fuente_revocacion,
    DROP COLUMN resultado_cadena,
    DROP COLUMN certificado_vigencia_hasta,
    DROP COLUMN certificado_vigencia_desde,
    DROP COLUMN certificado_issuer,
    DROP COLUMN certificado_subject,
    DROP COLUMN certificado_thumbprint,
    DROP COLUMN certificado_serial,
    DROP COLUMN evidencia_id,
    DROP COLUMN resultado_firma_id,
    DROP COLUMN algoritmo_hash,
    DROP COLUMN perfil_firma,
    DROP COLUMN received_at,
    DROP COLUMN event_id,
    DROP COLUMN correlation_id,
    DROP COLUMN actor_tecnico_key_fingerprint,
    DROP COLUMN actor_tecnico_app,
    DROP COLUMN actor_tecnico_id,
    DROP COLUMN actor_tecnico_tipo,
    DROP COLUMN revision_salida,
    DROP COLUMN revision_entrada,
    DROP COLUMN hash_salida;

-- =============================================================================
-- SECCIÓN 06 — fal_documento_firma_req: revertir columnas/constraints/índices
--               nuevos (incluida la preparación persistida DEC-EDA-030) y el
--               catálogo estado_firma_req extendido (DEC-EDA-016/020)
-- =============================================================================

ALTER TABLE fal_documento_firma_req
    DROP INDEX ix_firma_req_solicitud_rev,
    DROP INDEX ix_firma_req_estado_vence,
    DROP INDEX uq_firma_req_solicitud,
    DROP CONSTRAINT chk_firma_req_content_length_preparado,
    DROP CONSTRAINT chk_firma_req_rev_preparada,
    DROP CONSTRAINT chk_firma_req_preparacion,
    DROP CONSTRAINT chk_firma_req_revision,
    DROP CONSTRAINT chk_firma_req_estado_ext;

ALTER TABLE fal_documento_firma_req
    MODIFY COLUMN estado_firma_req SMALLINT NOT NULL DEFAULT 1
        COMMENT '1=PENDIENTE, 2=FIRMADO, 3=ANULADO, 4=VENCIDO, 5=REEMPLAZADO (EstadoFirmaReq enum)';

ALTER TABLE fal_documento_firma_req
    DROP COLUMN fh_preparacion,
    DROP COLUMN content_length_preparado,
    DROP COLUMN hash_entrada_preparado,
    DROP COLUMN revision_artefacto_preparada,
    DROP COLUMN detalle_terminal_firma_req,
    DROP COLUMN motivo_terminal_firma_req,
    DROP COLUMN fh_terminal,
    DROP COLUMN vence_en,
    DROP COLUMN fh_inicio_firma,
    DROP COLUMN fh_aceptacion,
    DROP COLUMN fh_solicitud,
    DROP COLUMN revision_solicitud,
    DROP COLUMN solicitud_firma_id,
    DROP COLUMN version_row;

-- NOTA: si alguna fila de estado_firma_req quedó con un valor 6-9 (nuevos
-- estados) antes de ejecutar este rollback, el precheck de rollback
-- (verify-firmas-eda-closure-001-rollback-precheck.sql) debe haber
-- BLOQUEADO la ejecución de este script. Este rollback NO reescribe
-- estado_firma_req en las filas (no inventa una reclasificación).

-- =============================================================================
-- FIN DEL SCRIPT DE ROLLBACK — SPEC-FIRMAS-EDA-CLOSURE-001
-- =============================================================================
-- Vistas eliminadas: 0 (modelo mínimo aprobado, sin vistas diagnósticas)
-- Rutinas eliminadas: 0 (modelo mínimo aprobado, sin rutinas SQL SECURITY DEFINER)
-- Tablas eliminadas: 6 (fal_inbox_error, fal_inbox, fal_bus_checkpoint,
--                       bus_evento, fal_outbox, fal_documento_firma_resultado)
-- Tablas revertidas: 3 (fal_documento, fal_documento_firma, fal_documento_firma_req)
-- =============================================================================
