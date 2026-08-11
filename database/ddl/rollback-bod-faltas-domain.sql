-- APROBADO PARA EJECUCIÓN AUTOMATIZADA EN SANDBOX (sb_faltas_db)
-- NO EJECUTAR EN PRODUCCIÓN SIN BACKUP, PRECHECKS Y AUTORIZACIÓN
-- =============================================================================
-- ROLLBACK — DOMINIO BOD FALTAS — MariaDB
-- Script: rollback-bod-faltas-domain.sql
-- Trabajo: MARIADB-BASELINE-EXEC-001
--
-- Revierte íntegramente create-bod-faltas-domain.sql: elimina exclusivamente
-- las 64 tablas TO_CREATE de la allowlist, en el orden inverso exacto de
-- database/design/dependency-order.md (G12 → G1; dentro de cada grupo, orden
-- inverso al de creación). Los 21 triggers definidos sobre estas tablas por
-- el forward caen automáticamente junto con su tabla (DROP TABLE elimina los
-- triggers definidos sobre ella); no requieren DROP TRIGGER explícito.
--
-- Contenido taxativo de este script: USE, control de FOREIGN_KEY_CHECKS y
-- DROP TABLE IF EXISTS exclusivamente sobre las 64 tablas autorizadas.
-- Ninguna otra sentencia. Prohibido mencionar, bajo cualquier forma, la tabla
-- adoptada sin alteración ni ninguno de los objetos protegidos preexistentes
-- del baseline (ver ddl-decisions.md DECISION_DDL-BASELINE-01).
--
-- Este script es la única vía de reversión usada tanto ante un forward
-- parcial (recuperación segura, DROP TABLE IF EXISTS tolera creación
-- incompleta) como en el rollback rehearsal de una instalación completa.
--
-- Fuente normativa: database/design/dependency-order.md
-- Forward correspondiente: database/ddl/create-bod-faltas-domain.sql
-- =============================================================================

USE sb_faltas_db;

SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- G12 (1 tabla) — reverso de G12
-- =============================================================================
DROP TABLE IF EXISTS fal_acta_economia_proyeccion;

-- =============================================================================
-- G11 (1 tabla) — reverso de G11
-- =============================================================================
DROP TABLE IF EXISTS fal_acta_pago_movimiento;

-- =============================================================================
-- G10 (1 tabla) — reverso de G10
-- =============================================================================
DROP TABLE IF EXISTS fal_acta_plan_pago_ref;

-- =============================================================================
-- G9 (3 tablas) — reverso de G9
-- =============================================================================
DROP TABLE IF EXISTS fal_acta_forma_pago;
DROP TABLE IF EXISTS fal_acta_apelacion_documento;
DROP TABLE IF EXISTS fal_acta_snapshot;

-- =============================================================================
-- G8 (6 tablas) — reverso de G8
-- =============================================================================
DROP TABLE IF EXISTS fal_acta_obligacion_pago;
DROP TABLE IF EXISTS fal_acta_apelacion;
DROP TABLE IF EXISTS fal_acta_evento;
DROP TABLE IF EXISTS fal_notificacion_acuse;
DROP TABLE IF EXISTS fal_notificacion_intento;
DROP TABLE IF EXISTS fal_documento_firma;

-- =============================================================================
-- G7 (6 tablas) — reverso de G7
-- =============================================================================
DROP TABLE IF EXISTS fal_acta_fallo;
DROP TABLE IF EXISTS num_talonario_movimiento;
DROP TABLE IF EXISTS fal_notificacion;
DROP TABLE IF EXISTS fal_documento_redaccion;
DROP TABLE IF EXISTS fal_documento_firma_req;
DROP TABLE IF EXISTS fal_acta_documento;

-- =============================================================================
-- G6 (3 tablas) — reverso de G6
-- =============================================================================
DROP TABLE IF EXISTS fal_documento;
DROP TABLE IF EXISTS fal_acta_valorizacion_item;
DROP TABLE IF EXISTS fal_acta_medida_preventiva;

-- =============================================================================
-- G5 (14 tablas) — reverso de G5
-- =============================================================================
DROP TABLE IF EXISTS fal_acta_bloqueante_cierre_material;
DROP TABLE IF EXISTS fal_acta_valorizacion;
DROP TABLE IF EXISTS fal_acta_gestion_externa;
DROP TABLE IF EXISTS fal_acta_qr_acceso;
DROP TABLE IF EXISTS fal_acta_articulo_infringido;
DROP TABLE IF EXISTS fal_acta_archivo;
DROP TABLE IF EXISTS fal_acta_paralizacion;
DROP TABLE IF EXISTS fal_acta_sustancias_alimenticias;
DROP TABLE IF EXISTS fal_acta_contravencion;
DROP TABLE IF EXISTS fal_acta_vehiculo;
DROP TABLE IF EXISTS fal_acta_transito_alcoholemia;
DROP TABLE IF EXISTS fal_acta_transito;
DROP TABLE IF EXISTS fal_observacion;
DROP TABLE IF EXISTS fal_acta_evidencia;

-- =============================================================================
-- G4 (2 tablas) — reverso de G4
-- =============================================================================
DROP TABLE IF EXISTS fal_acta;
DROP TABLE IF EXISTS fal_persona_domicilio;

-- =============================================================================
-- G3 (5 tablas) — reverso de G3
-- =============================================================================
DROP TABLE IF EXISTS num_talonario_inspector;
DROP TABLE IF EXISTS num_talonario_ambito;
DROP TABLE IF EXISTS fal_articulo_medida_preventiva;
DROP TABLE IF EXISTS fal_tarifario_unidad_faltas;
DROP TABLE IF EXISTS fal_firmante_version_habilitacion;

-- =============================================================================
-- G2 (10 tablas) — reverso de G2
-- =============================================================================
DROP TABLE IF EXISTS fal_documento_plantilla_default;
DROP TABLE IF EXISTS fal_documento_plantilla_contenido;
DROP TABLE IF EXISTS fal_documento_plantilla_firma_req;
DROP TABLE IF EXISTS fal_articulo_normativa_faltas;
DROP TABLE IF EXISTS fal_dependencia_normativa;
DROP TABLE IF EXISTS num_talonario;
DROP TABLE IF EXISTS fal_vehiculo_modelo;
DROP TABLE IF EXISTS fal_firmante_version;
DROP TABLE IF EXISTS fal_inspector_version;
DROP TABLE IF EXISTS fal_dependencia_version;

-- =============================================================================
-- G1 (13 tablas) — reverso de G1 (incluye fal_valor_unidad_faltas, G1-13,
-- agregada en Wave 4 PARTE-2 S7.2)
-- =============================================================================
DROP TABLE IF EXISTS fal_valor_unidad_faltas;
DROP TABLE IF EXISTS fal_documento_plantilla;
DROP TABLE IF EXISTS fal_lote_correo;
DROP TABLE IF EXISTS fal_medida_preventiva;
DROP TABLE IF EXISTS fal_normativa_faltas;
DROP TABLE IF EXISTS fal_motivo_archivo;
DROP TABLE IF EXISTS fal_dia_no_computable;
DROP TABLE IF EXISTS num_politica;
DROP TABLE IF EXISTS fal_vehiculo_marca;
DROP TABLE IF EXISTS fal_persona;
DROP TABLE IF EXISTS fal_firmante;
DROP TABLE IF EXISTS fal_inspector;
DROP TABLE IF EXISTS fal_dependencia;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- FIN DEL SCRIPT DE ROLLBACK — DOMINIO BOD FALTAS
-- =============================================================================
-- Tablas eliminadas: 65 (las mismas 65 TO_CREATE de create-bod-faltas-domain.sql, incl. fal_valor_unidad_faltas)
-- Triggers eliminados: 21 (caen automáticamente con su tabla host; sin DROP
--                          TRIGGER explícito en este script)
-- Vistas eliminadas: 0
-- Rutinas eliminadas: 0
-- =============================================================================
