-- DIAGNOSTICO READ-ONLY - no modifica datos ni esquema
-- =============================================================================
-- PRECHECK - fal_persona: documento opcional, canonico y estructurado
-- Script: verify-fal-persona-documento-opcional-001-precheck.sql
-- Trabajo: JDBC-DOMINIO-EDA-WAVE-1A2-B1.5B0-B
-- Decision: DECISION_DDL-PERS-01, paso 1 del plan de migracion de 13 pasos
--
-- Se ejecuta ANTES de
-- database/ddl/alter-fal-persona-documento-opcional-001-forward.sql.
--
-- El plan NO asume la tabla vacia: que hoy tenga cero filas es un hecho
-- coyuntural verificado, no un contrato. Estas consultas deben ser correctas
-- con filas.
--
-- Uso: revisar manualmente el resultado. Si `estado` <> 'LISTO_PARA_FORWARD'
-- en la seccion 3, DETENER y remediar los datos antes de ejecutar el forward.
--
-- Codigos CUIT/CUIL: 2 y 3. Verificados contra TipoDocumentoPersona.java
-- (CUIT((short) 2), CUIL((short) 3)) y contra DECISION_DDL-PERS-01 seccion 4.
-- Prohibido derivarlos de ordinal().
--
-- Ninguna consulta expone un documento en claro: se informan cantidades,
-- longitudes y PK tecnicas.
-- =============================================================================

USE sb_faltas_db;

-- -----------------------------------------------------------------------------
-- 1. Identidad del entorno. Debe ser sb_faltas_db.
-- -----------------------------------------------------------------------------
SELECT
    DATABASE()                                                    AS base_actual,
    VERSION()                                                     AS version_motor,
    CASE WHEN DATABASE() = 'sb_faltas_db' THEN 'SI' ELSE 'NO' END AS base_esperada;

-- -----------------------------------------------------------------------------
-- 2. Forma fisica de partida y volumen.
-- -----------------------------------------------------------------------------
SELECT column_name, column_type, is_nullable, extra
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name   = 'fal_persona'
   AND column_name IN ('tipo_documento', 'prefijo_cuit_cuil', 'nro_doc',
                       'digito_verificador', 'doc_key')
 ORDER BY ordinal_position;

SELECT 'fal_persona'                                 AS tabla, COUNT(*) AS filas FROM fal_persona
UNION ALL SELECT 'fal_persona_domicilio (hija)',              COUNT(*) FROM fal_persona_domicilio
UNION ALL SELECT 'fal_acta (hija)',                           COUNT(*) FROM fal_acta
UNION ALL SELECT 'fal_acta_obligacion_pago (hija)',           COUNT(*) FROM fal_acta_obligacion_pago;

-- -----------------------------------------------------------------------------
-- 3. Controles bloqueantes. Toda fila debe devolver cantidad = 0.
-- -----------------------------------------------------------------------------
SELECT control, cantidad,
       CASE WHEN cantidad = 0 THEN 'LISTO_PARA_FORWARD' ELSE 'BLOQUEADO' END AS estado
FROM (
    SELECT 'P01 tipo_documento NULL' AS control, COUNT(*) AS cantidad
      FROM fal_persona WHERE tipo_documento IS NULL
    UNION ALL SELECT 'P02 nro_doc NULL', COUNT(*)
      FROM fal_persona WHERE nro_doc IS NULL
    UNION ALL SELECT 'P03 nro_doc fuera de 1..99999999', COUNT(*)
      FROM fal_persona WHERE nro_doc IS NOT NULL AND (nro_doc < 1 OR nro_doc > 99999999)
    UNION ALL SELECT 'P04 nro_doc en texto supera 12 caracteres', COUNT(*)
      FROM fal_persona WHERE nro_doc IS NOT NULL AND CHAR_LENGTH(CAST(nro_doc AS CHAR)) > 12
    UNION ALL SELECT 'P05 prefijo fuera del rango nuevo 10..99', COUNT(*)
      FROM fal_persona WHERE prefijo_cuit_cuil IS NOT NULL
                         AND prefijo_cuit_cuil NOT BETWEEN 10 AND 99
    UNION ALL SELECT 'P06 digito fuera de 0..9', COUNT(*)
      FROM fal_persona WHERE digito_verificador IS NOT NULL
                         AND digito_verificador NOT BETWEEN 0 AND 9
    UNION ALL SELECT 'P07 CUIT/CUIL sin prefijo o sin digito', COUNT(*)
      FROM fal_persona WHERE tipo_documento IN (2, 3)
                         AND (prefijo_cuit_cuil IS NULL OR digito_verificador IS NULL)
    UNION ALL SELECT 'P08 CUIT/CUIL cuyo cuerpo no rinde 8 digitos', COUNT(*)
      FROM fal_persona WHERE tipo_documento IN (2, 3)
                         AND CAST(nro_doc AS CHAR) NOT REGEXP '^[0-9]{8}$'
    UNION ALL SELECT 'P09 componentes CUIT/CUIL en otro tipo', COUNT(*)
      FROM fal_persona WHERE tipo_documento IS NOT NULL AND tipo_documento NOT IN (2, 3)
                         AND (prefijo_cuit_cuil IS NOT NULL OR digito_verificador IS NOT NULL)
    UNION ALL SELECT 'P10 tipo_documento fuera de (1,2,3,4,5,9)', COUNT(*)
      FROM fal_persona WHERE tipo_documento IS NOT NULL
                         AND tipo_documento NOT IN (1, 2, 3, 4, 5, 9)
    UNION ALL SELECT 'P11 DNI que violaria el CHECK canonico', COUNT(*)
      FROM fal_persona WHERE tipo_documento = 1
                         AND CAST(nro_doc AS CHAR) NOT REGEXP '^[1-9][0-9]{0,7}$'
    UNION ALL SELECT 'P12 nro_doc que violaria chk_nro_doc_canonico', COUNT(*)
      FROM fal_persona WHERE nro_doc IS NOT NULL
                         AND CAST(nro_doc AS CHAR) NOT REGEXP '^[0-9A-Z]{1,12}$'
    UNION ALL SELECT 'P13 doc_key nueva con mas de 12 caracteres', COUNT(*)
      FROM fal_persona
     WHERE CHAR_LENGTH(CASE WHEN tipo_documento IS NULL THEN NULL
                            WHEN tipo_documento IN (2, 3)
                              THEN CONCAT(prefijo_cuit_cuil, CAST(nro_doc AS CHAR), digito_verificador)
                            ELSE CAST(nro_doc AS CHAR) END) > 12
    UNION ALL SELECT 'P14 duplicados bajo la nueva (tipo_documento, doc_key)', COUNT(*)
      FROM (SELECT tipo_documento,
                   CASE WHEN tipo_documento IN (2, 3)
                          THEN CONCAT(prefijo_cuit_cuil, CAST(nro_doc AS CHAR), digito_verificador)
                        ELSE CAST(nro_doc AS CHAR) END AS nueva_doc_key
              FROM fal_persona
             WHERE tipo_documento IS NOT NULL
             GROUP BY 1, 2 HAVING COUNT(*) > 1) duplicados
) controles
ORDER BY control;

-- -----------------------------------------------------------------------------
-- 4. PK tecnicas de las filas no migrables de forma deterministica.
--    Debe devolver cero filas. No expone ningun documento en claro.
-- -----------------------------------------------------------------------------
SELECT id                                             AS pk_tecnica,
       tipo_documento,
       CHAR_LENGTH(CAST(nro_doc AS CHAR))             AS longitud_nro_doc,
       (prefijo_cuit_cuil IS NOT NULL)                AS tiene_prefijo,
       (digito_verificador IS NOT NULL)               AS tiene_digito
  FROM fal_persona
 WHERE tipo_documento IS NULL
    OR nro_doc IS NULL
    OR tipo_documento NOT IN (1, 2, 3, 4, 5, 9)
    OR (tipo_documento IN (2, 3)
        AND (prefijo_cuit_cuil IS NULL
             OR digito_verificador IS NULL
             OR prefijo_cuit_cuil NOT BETWEEN 10 AND 99
             OR CAST(nro_doc AS CHAR) NOT REGEXP '^[0-9]{8}$'))
    OR (tipo_documento NOT IN (2, 3)
        AND (prefijo_cuit_cuil IS NOT NULL OR digito_verificador IS NOT NULL))
    OR (tipo_documento = 1 AND CAST(nro_doc AS CHAR) NOT REGEXP '^[1-9][0-9]{0,7}$')
 ORDER BY id;

-- =============================================================================
-- FIN DEL PRECHECK - fal_persona documento opcional
-- =============================================================================
