-- =============================================================================
-- ROLLBACK - fal_acta_evidencia: metadata de fotografias (Parte 3D)
-- Script: alter-fal-acta-evidencia-fotografias-001-rollback.sql
-- Trabajo: WAVE-04-PARTE-3D-EVIDENCIAS-FOTOGRAFICAS-20260806
-- Decision: DECISION_DDL-EVID-03
--
-- Revierte alter-fal-acta-evidencia-fotografias-001-forward.sql: baja los dos
-- UNIQUE nuevos, baja las diez columnas nuevas y restituye el comentario de
-- tabla anterior (DECISION_DDL-EVID-01). El forward no agrego ningun CHECK
-- nuevo (ver DECISION_DDL-SIN-CHECKS-CODIGOS-01 en el forward), de modo que
-- este rollback no tiene ningun CHECK que dar de baja.
--
-- ADVERTENCIA
-- Revertir elimina toda la metadata de fotografias incorporada por el pipeline
-- de la Parte 3D (orden, hash, dimensiones, MIME, EXIF, trazabilidad). Las
-- filas fal_acta_evidencia con tipo_evid=FOTO sobreviven (la fila en si no se
-- borra), pero pierden toda esa metadata. Los archivos fisicos en filesystem
-- NO los gestiona este script: su limpieza es responsabilidad operativa
-- separada si se revierte en un ambiente con fotografias reales incorporadas.
--
-- PRECHECK OBLIGATORIO Y BLOQUEANTE (cortafuegos, seccion 1 de este script):
-- la ventana de rollback se cierra con la primera fila FOTO que tenga alguna
-- de las columnas nuevas informada, porque en ese punto revertir borraria
-- informacion real sin backup logico previo. Con la ventana cerrada, extraer
-- primero un volcado logico de fal_acta_evidencia antes de continuar.
--
-- Motor: MariaDB 12.3.2 | InnoDB | utf8mb4 | utf8mb4_uca1400_ai_ci
-- =============================================================================

USE sb_faltas_db;

-- -----------------------------------------------------------------------------
-- 1. CORTAFUEGOS. Debe devolver 0 filas antes de continuar.
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS filas_foto_con_metadata_nueva
  FROM fal_acta_evidencia
 WHERE orden IS NOT NULL
    OR mime_tipo IS NOT NULL
    OR tamanio_bytes IS NOT NULL
    OR ancho_px IS NOT NULL
    OR alto_px IS NOT NULL
    OR origen_incorporacion IS NOT NULL
    OR bloque_al_incorporar IS NOT NULL;

-- -----------------------------------------------------------------------------
-- 2. Baja de los dos UNIQUE nuevos.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_acta_evidencia
    DROP INDEX uq_evidencia_acta_orden,
    DROP INDEX uq_evidencia_acta_hash;

-- -----------------------------------------------------------------------------
-- 3. Baja de las diez columnas nuevas.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_acta_evidencia
    DROP COLUMN orden,
    DROP COLUMN descripcion,
    DROP COLUMN nombre_original,
    DROP COLUMN mime_tipo,
    DROP COLUMN tamanio_bytes,
    DROP COLUMN ancho_px,
    DROP COLUMN alto_px,
    DROP COLUMN fecha_captura_exif,
    DROP COLUMN bloque_al_incorporar,
    DROP COLUMN origen_incorporacion;

-- -----------------------------------------------------------------------------
-- 4. Restitucion del comentario de tabla anterior (DECISION_DDL-EVID-01).
-- -----------------------------------------------------------------------------
ALTER TABLE fal_acta_evidencia
    COMMENT = 'Evidencias físicas del acta (firma ológrafa del infractor, etc.). Append-only. DECISION_DDL-EVID-01.';

-- -----------------------------------------------------------------------------
-- 5. Verificacion del retorno.
-- -----------------------------------------------------------------------------
SHOW CREATE TABLE fal_acta_evidencia;

-- =============================================================================
-- FIN DEL ROLLBACK - fal_acta_evidencia fotografias (Parte 3D)
-- =============================================================================
