-- APROBADO COMO DISEÑO — DDL_READY, DDL_NOT_EXECUTED
-- NO EJECUTAR EN PRODUCCIÓN SIN BACKUP, PRECHECKS Y AUTORIZACIÓN
-- =============================================================================
-- ROLLBACK DE GRANTS — Cuentas SQL fijas de mínimo privilegio del bus central
-- Script: rollback-grants-firmas-eda-bus.sql
-- Trabajo: SPEC-FIRMAS-EDA-CLOSURE-001 (DEC-EDA-040bis)
--
-- Revoca exactamente lo que otorgó grants-firmas-eda-bus.sql, en orden
-- inverso, sin tocar la existencia de los usuarios MariaDB (que no fueron
-- creados por ese script — son responsabilidad del secret manager/pipeline,
-- fuera de este repositorio). NO incluye `DROP USER`.
--
-- Ejecutable de forma independiente al rollback de tablas
-- (alter-firmas-eda-closure-001-rollback.sql): revocar permisos no requiere
-- que bus_evento ya haya sido eliminada, pero por orden operativo se
-- ejecuta ANTES de ese rollback, para no dejar una ventana en la que el
-- objeto ya no exista pero el GRANT todavía lo referencie.
--
-- Fuente normativa: firma-eda-physical-model.md §11
-- Grant correspondiente: database/ddl/grants-firmas-eda-bus.sql
-- =============================================================================

-- =============================================================================
-- SECCIÓN 01 — bus_maintenance (orden inverso de la sección 05 de grants)
-- =============================================================================

REVOKE SELECT, DELETE ON sb_faltas_db.bus_evento FROM 'bus_maintenance'@'%';

-- =============================================================================
-- SECCIÓN 02 — bus_consumer_firmas (orden inverso de la sección 04 de grants)
-- =============================================================================

REVOKE SELECT ON sb_faltas_db.bus_evento FROM 'bus_consumer_firmas'@'%';

-- =============================================================================
-- SECCIÓN 03 — bus_consumer_faltas (orden inverso de la sección 03 de grants)
-- =============================================================================

REVOKE SELECT ON sb_faltas_db.bus_evento FROM 'bus_consumer_faltas'@'%';

-- =============================================================================
-- SECCIÓN 04 — bus_publisher_firmas (orden inverso de la sección 02 de grants)
--   REVOKE column-level exacto al GRANT otorgado (excluye bus_evento_id).
-- =============================================================================

REVOKE INSERT (
    evento_id, productor_codigo, evento_codigo, schema_version,
    agregado_codigo, agregado_id, agregado_version, correlation_id,
    causation_id, payload, ocurrido_en, publicado_en
) ON sb_faltas_db.bus_evento FROM 'bus_publisher_firmas'@'%';

-- =============================================================================
-- SECCIÓN 05 — bus_publisher_faltas (orden inverso de la sección 01 de grants)
--   REVOKE column-level exacto al GRANT otorgado (excluye bus_evento_id).
-- =============================================================================

REVOKE INSERT (
    evento_id, productor_codigo, evento_codigo, schema_version,
    agregado_codigo, agregado_id, agregado_version, correlation_id,
    causation_id, payload, ocurrido_en, publicado_en
) ON sb_faltas_db.bus_evento FROM 'bus_publisher_faltas'@'%';

-- Sin `FLUSH PRIVILEGES` (innecesario tras `REVOKE`; ver nota equivalente en
-- grants-firmas-eda-bus.sql).

-- =============================================================================
-- FIN DEL SCRIPT — rollback-grants-firmas-eda-bus.sql
-- =============================================================================
-- Revoca exactamente los 5 GRANT otorgados por grants-firmas-eda-bus.sql
-- (5 REVOKE en total, uno por usuario; los 2 REVOKE de publishers son
-- column-level exactos a su GRANT, excluyendo bus_evento_id). Sin DROP
-- USER: la baja de un usuario MariaDB es una decisión de aprovisionamiento
-- (secret manager/pipeline), no de este paquete de DDL de dominio.
-- =============================================================================
