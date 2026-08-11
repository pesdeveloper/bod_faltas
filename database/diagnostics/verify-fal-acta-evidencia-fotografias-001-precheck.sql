-- =============================================================================
-- PRECHECK - fal_acta_evidencia: columnas de fotografia (Parte 3D)
-- Script: verify-fal-acta-evidencia-fotografias-001-precheck.sql
-- Trabajo: WAVE-04-PARTE-3D-EVIDENCIAS-FOTOGRAFICAS
-- Decision: DECISION_DDL-EVID-03 (extension no destructiva de DECISION_DDL-EVID-01)
--
-- Todas las filas devueltas deben ser 0 antes de ejecutar el forward. La tabla
-- fal_acta_evidencia es append-only y transaccional (no maestro): un conteo no
-- cero de filas no bloquea el forward por si mismo (las columnas nuevas son
-- todas NULL-able), pero se registra para no migrar "a ciegas".
-- =============================================================================

USE sb_faltas_db;

SELECT
    COUNT(*)                                            AS filas_totales,
    SUM(tipo_evid = 4)                                  AS filas_foto_existentes,
    SUM(hash_evid IS NOT NULL)                           AS filas_con_hash
FROM fal_acta_evidencia;

-- Las columnas nuevas no deben existir todavia (idempotencia del precheck).
SELECT column_name
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name   = 'fal_acta_evidencia'
   AND column_name IN ('orden','descripcion','nombre_original','mime_tipo',
                       'tamanio_bytes','ancho_px','alto_px','fecha_captura_exif',
                       'bloque_al_incorporar','origen_incorporacion');
