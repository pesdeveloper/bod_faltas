-- =============================================================================
-- ROLLBACK — Unificacion del tipo fisico de coordenadas geograficas
-- Script: alter-coord-type-unify-001-rollback.sql
-- Trabajo: MARIADB-COORD-TYPE-UNIFY-001
--
-- Revierte alter-coord-type-unify-001-forward.sql: devuelve las cuatro columnas
-- de coordenadas de fal_acta a DECIMAL(12,8), su tipo previo.
--
-- ADVERTENCIA
-- Revertir reintroduce la divergencia que el forward corrigio: fal_acta volveria
-- a DECIMAL(12,8) mientras fal_persona_domicilio permanece en DECIMAL(10,7), y el
-- guardrail PHY de coordenadas volveria a estar en rojo. Usar solo para restaurar
-- el estado anterior ante un incidente, no como configuracion de destino.
--
-- La conversion inversa es ampliatoria y no trunca datos: DECIMAL(12,8) contiene
-- a DECIMAL(10,7).
--
-- precision_captura_m no se toca: el forward tampoco lo modifico.
-- =============================================================================

USE sb_faltas_db;

ALTER TABLE fal_acta
    MODIFY COLUMN lat_captura DECIMAL(12,8) NULL
        COMMENT 'Latitud del punto de captura (grados decimales)',
    MODIFY COLUMN lon_captura DECIMAL(12,8) NULL
        COMMENT 'Longitud del punto de captura (grados decimales)',
    MODIFY COLUMN lat_infr DECIMAL(12,8) NULL
        COMMENT 'Latitud final del lugar del hecho (grados decimales)',
    MODIFY COLUMN lon_infr DECIMAL(12,8) NULL
        COMMENT 'Longitud final del lugar del hecho (grados decimales)';

SELECT table_name, column_name, column_type
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'fal_acta'
   AND column_name IN ('lat_captura','lon_captura','lat_infr','lon_infr')
 ORDER BY column_name;
