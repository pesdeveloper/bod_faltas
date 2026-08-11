-- APROBADO COMO DISEÑO — DDL_READY, DDL_NOT_EXECUTED
-- NO EJECUTAR EN PRODUCCIÓN SIN BACKUP, PRECHECKS Y AUTORIZACIÓN
-- =============================================================================
-- GRANTS — Cuentas SQL fijas de mínimo privilegio del bus central de eventos
-- Script: grants-firmas-eda-bus.sql
-- Trabajo: SPEC-FIRMAS-EDA-CLOSURE-001 (DEC-EDA-027/040bis)
--
-- Modelo mínimo aprobado: sin rutinas SQL SECURITY DEFINER, sin vistas
-- diagnósticas, sin `bus_definer`. Cada cuenta recibe un privilegio directo
-- y mínimo sobre la única tabla central `bus_evento`:
--   publisher/relay (bus_publisher_faltas, bus_publisher_firmas): INSERT
--     column-level, sobre todas las columnas EXCEPTO bus_evento_id (ese
--     valor lo genera siempre la base con AUTO_INCREMENT; ninguna
--     aplicación productora puede escribirlo, ni por error).
--   consumer/reader (bus_consumer_faltas, bus_consumer_firmas): SELECT
--     (tabla completa; necesita leer todas las columnas para copiar el
--     envelope a su Inbox local).
--   maintenance (bus_maintenance): SELECT y DELETE (tabla completa),
--     exclusivamente para retención operativa autorizada (fuera de la ruta
--     de publicación/consumo; ver database/README.md).
--
-- Ninguna cuenta del bus tiene acceso a fal_documento*, fal_outbox,
-- fal_bus_checkpoint, fal_inbox ni fal_inbox_error: esas tablas son locales
-- de la aplicación Faltas y se acceden con el datasource/usuario de
-- aplicación ya existente, no con una cuenta bus_*. fir_outbox/fir_inbox/
-- fir_bus_checkpoint/fir_inbox_error son igualmente locales de Firmas.
--
-- PRECONDICIONES (en este orden):
--   1. alter-firmas-eda-closure-001-forward.sql ya ejecutado (existe
--      bus_evento).
--   2. database/diagnostics/verify-firmas-eda-bus-principals-precheck.sql
--      confirma que los 5 usuarios MariaDB bus_* ya existen, con host
--      permitido y REQUIRE SSL. Si ese precheck no está en OK, NO ejecutar
--      este script.
--
-- SEGURIDAD — SIN CONTRASEÑAS NI CREATE USER:
--   Este script deliberadamente NO contiene CREATE USER, IDENTIFIED BY ni
--   ningún placeholder de contraseña. El aprovisionamiento de los 5 usuarios
--   MariaDB (nombre, host permitido, REQUIRE SSL, credencial) es
--   responsabilidad exclusiva del secret manager/pipeline de la plataforma,
--   fuera de este repositorio (DEC-EDA-040bis).
--
-- `X-Api-Key` (DEC-EDA-018) NO se usa para el bus central; se usa
-- exclusivamente en los 3 endpoints HTTP de transferencia binaria
-- (30-integrations/firma-binary-transfer.md). Este script no crea ni
-- modifica ninguna credencial HTTP.
--
-- Rollback: database/ddl/rollback-grants-firmas-eda-bus.sql (REVOKE
-- simétrico exacto, sin DROP USER — la baja de un usuario MariaDB es una
-- decisión de aprovisionamiento, no de este paquete de DDL de dominio).
--
-- Fuente normativa: firma-eda-physical-model.md §11
-- =============================================================================

-- =============================================================================
-- SECCIÓN 01 — bus_publisher_faltas
--   INSERT column-level sobre bus_evento, excluyendo bus_evento_id (columna
--   generada siempre por AUTO_INCREMENT; ningún cliente puede escribirla).
--   El relay de Faltas reutiliza el mismo evento_id en cada reintento
--   (idempotencia por UNIQUE(evento_id)).
-- =============================================================================

GRANT INSERT (
    evento_id, productor_codigo, evento_codigo, schema_version,
    agregado_codigo, agregado_id, agregado_version, correlation_id,
    causation_id, payload, ocurrido_en, publicado_en
) ON sb_faltas_db.bus_evento TO 'bus_publisher_faltas'@'%';

-- =============================================================================
-- SECCIÓN 02 — bus_publisher_firmas
--   Mismo privilegio column-level que bus_publisher_faltas. Usuario
--   separado: nunca comparte credencial con Faltas.
-- =============================================================================

GRANT INSERT (
    evento_id, productor_codigo, evento_codigo, schema_version,
    agregado_codigo, agregado_id, agregado_version, correlation_id,
    causation_id, payload, ocurrido_en, publicado_en
) ON sb_faltas_db.bus_evento TO 'bus_publisher_firmas'@'%';

-- =============================================================================
-- SECCIÓN 03 — bus_consumer_faltas
--   SELECT directo sobre bus_evento. El lector local de Faltas filtra por
--   bus_evento_id > su checkpoint local (fal_bus_checkpoint) y por
--   evento_codigo; el bus central no sabe qué consumidor leyó qué.
-- =============================================================================

GRANT SELECT ON sb_faltas_db.bus_evento TO 'bus_consumer_faltas'@'%';

-- =============================================================================
-- SECCIÓN 04 — bus_consumer_firmas
--   Mismo privilegio que bus_consumer_faltas, sobre su propio checkpoint
--   local (fir_bus_checkpoint).
-- =============================================================================

GRANT SELECT ON sb_faltas_db.bus_evento TO 'bus_consumer_firmas'@'%';

-- =============================================================================
-- SECCIÓN 05 — bus_maintenance
--   SELECT y DELETE directos sobre bus_evento, exclusivamente para
--   retención operativa autorizada. Ningún procedimiento de purga vive en
--   MariaDB (sin sp_bus_purgar): el DELETE de mantenimiento es una tarea
--   operativa externa, fuera de la ruta de publicación/consumo, que respeta
--   la ventana de retención documentada (database/README.md).
-- =============================================================================

GRANT SELECT, DELETE ON sb_faltas_db.bus_evento TO 'bus_maintenance'@'%';

-- Sin `FLUSH PRIVILEGES`: en MariaDB moderno, `GRANT`/`REVOKE` sobre
-- privilegios ya aplican en memoria de inmediato; `FLUSH PRIVILEGES` solo es
-- necesario tras editar las tablas de privilegios directamente
-- (`INSERT`/`UPDATE`/`DELETE` en `mysql.user`, etc.), lo que este script
-- nunca hace.

-- =============================================================================
-- FIN DEL SCRIPT — grants-firmas-eda-bus.sql
-- =============================================================================
-- Usuarios alcanzados: 5 (bus_publisher_faltas, bus_publisher_firmas,
--                         bus_consumer_faltas, bus_consumer_firmas, bus_maintenance)
-- Privilegios otorgados: 5 GRANT directos sobre bus_evento (2 INSERT
--   column-level publishers, sin bus_evento_id + 2 SELECT consumers +
--   1 GRANT SELECT,DELETE maintenance)
-- Sin rutinas, sin vistas, sin bus_definer.
-- Sin CREATE USER, sin IDENTIFIED BY, sin placeholders de contraseña.
-- Rollback: database/ddl/rollback-grants-firmas-eda-bus.sql
-- =============================================================================
