-- =============================================================================
-- ROLLBACK - fal_persona: documento opcional, canonico y estructurado
-- Script: alter-fal-persona-documento-opcional-001-rollback.sql
-- Trabajo: JDBC-DOMINIO-EDA-WAVE-1A2-B1.5B0-B
-- Decision: DECISION_DDL-PERS-01, seccion 10, "Rollback"
--
-- Revierte alter-fal-persona-documento-opcional-001-forward.sql recorriendo el
-- orden inverso: bajar los CHECK nuevos, bajar el UNIQUE, bajar doc_key,
-- restituir nro_doc INT UNSIGNED NOT NULL y tipo_documento SMALLINT NOT NULL,
-- restituir chk_fal_persona_nro_doc, recrear doc_key con la expresion anterior
-- y recrear el UNIQUE anterior.
--
-- ADVERTENCIA
-- Revertir reintroduce exactamente la divergencia que el forward corrigio: el
-- modelo Java vuelve a admitir un estado que el motor rechaza. Una persona sin
-- documento y un documento alfanumerico dejan de ser persistibles, y
-- JdbcPersonaRepository deja de poder servir a IDENTIFICADO_PARCIAL. Usar solo
-- para restaurar el estado anterior ante un incidente, no como configuracion de
-- destino.
--
-- PRECHECK OBLIGATORIO Y BLOQUEANTE
--   database/diagnostics/verify-fal-persona-documento-opcional-001-rollback-precheck.sql
--
-- La ventana de rollback es real y se cierra sola. El retorno es seguro
-- unicamente mientras ninguna fila tenga tipo_documento NULL, nro_doc NULL,
-- nro_doc con un caracter no numerico, nro_doc con cero inicial o nro_doc fuera
-- de 1..99999999. La primera fila que incumpla cualquiera de esas condiciones
-- cierra la ventana: INT UNSIGNED NOT NULL no puede representarla.
--
-- Por eso el rollback pertenece a la misma ventana de cambio que el forward y
-- no a una posterior.
--
-- La seccion 1 de este script es un cortafuegos: si detecta una fila fuera de
-- la ventana, la conversion no debe ejecutarse. MariaDB no aborta un script por
-- si mismo ante un resultado distinto de cero, de modo que la verificacion es
-- del operador. Con la ventana cerrada, el retorno exige restituir desde el
-- backup logico del paso 2 del forward, no ejecutar este script.
--
-- No se trunca ni se convierte nada en silencio.
--
-- Motor: MariaDB 12.3.2 | InnoDB | utf8mb4 | utf8mb4_uca1400_ai_ci
-- =============================================================================

USE sb_faltas_db;

-- -----------------------------------------------------------------------------
-- 1. CORTAFUEGOS. Las cinco columnas deben devolver 0. Si alguna no lo hace,
--    DETENER: la ventana esta cerrada y este script perderia informacion.
-- -----------------------------------------------------------------------------
SELECT
    COUNT(*)                                                        AS filas_totales,
    SUM(tipo_documento IS NULL)                                     AS sin_tipo_documento,
    SUM(nro_doc IS NULL)                                            AS sin_nro_doc,
    SUM(nro_doc IS NOT NULL AND nro_doc NOT REGEXP '^[0-9]+$')      AS nro_doc_alfanumerico,
    SUM(nro_doc REGEXP '^0')                                        AS nro_doc_con_cero_inicial
FROM fal_persona;

-- -----------------------------------------------------------------------------
-- 2. Baja de los seis CHECK nuevos.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_persona
    DROP CONSTRAINT chk_fal_persona_doc_coherente,
    DROP CONSTRAINT chk_fal_persona_doc_tipo,
    DROP CONSTRAINT chk_fal_persona_doc_cuit,
    DROP CONSTRAINT chk_fal_persona_doc_no_cuit,
    DROP CONSTRAINT chk_fal_persona_nro_doc_canonico,
    DROP CONSTRAINT chk_fal_persona_nro_doc_dni;

-- -----------------------------------------------------------------------------
-- 3. Baja del UNIQUE y de la columna generada nueva.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_persona DROP INDEX uq_fal_persona_tipo_doc_key;
ALTER TABLE fal_persona DROP COLUMN doc_key;

-- -----------------------------------------------------------------------------
-- 4. Restitucion de los tipos anteriores.
--    tipo_documento primero: el NOT NULL de nro_doc no depende de el, pero
--    dejar la tabla el menor tiempo posible en un estado mixto es preferible.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_persona
    MODIFY COLUMN tipo_documento SMALLINT NOT NULL
        COMMENT 'CONTENIDO: Tipo de documento de identidad. USO: determina estructura del documento. VALORES: 1=DNI, 2=CUIT, 3=CUIL, 4=PASAPORTE, 5=DNI_EXTRANJERO, 9=OTRO (TipoDocumentoPersona enum).';

ALTER TABLE fal_persona
    MODIFY COLUMN nro_doc INT UNSIGNED NOT NULL
        COMMENT 'CONTENIDO: Numero de documento sin prefijo ni digito. USO: parte principal del documento. REGLA: BETWEEN 1 AND 99999999.';

-- -----------------------------------------------------------------------------
-- 5. Restitucion del CHECK numerico y del rango antiguo del prefijo.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_persona
    ADD CONSTRAINT chk_fal_persona_nro_doc CHECK (nro_doc BETWEEN 1 AND 99999999);

ALTER TABLE fal_persona DROP CONSTRAINT chk_fal_persona_prefijo;
ALTER TABLE fal_persona
    ADD CONSTRAINT chk_fal_persona_prefijo
        CHECK (prefijo_cuit_cuil IS NULL OR prefijo_cuit_cuil BETWEEN 0 AND 99);

ALTER TABLE fal_persona
    MODIFY COLUMN prefijo_cuit_cuil TINYINT UNSIGNED NULL
        COMMENT 'CONTENIDO: Prefijo de CUIT/CUIL (ej: 20, 23, 27, 30). USO: solo para CUIT/CUIL. REGLA: NULL para documentos simples; BETWEEN 0 AND 99 cuando informado.';

-- -----------------------------------------------------------------------------
-- 6. Recreacion de doc_key con la expresion anterior y del UNIQUE anterior.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_persona
    ADD COLUMN doc_key VARCHAR(20) AS (
        CASE WHEN tipo_documento IN (2, 3)
               THEN CONCAT(LPAD(COALESCE(prefijo_cuit_cuil, 0), 2, 0),
                           LPAD(nro_doc, 8, 0),
                           COALESCE(digito_verificador, 0))
             ELSE CONCAT(tipo_documento, '-', nro_doc)
        END) STORED
        COMMENT 'CONTENIDO: Clave funcional normalizada para unicidad de documento. USO: UNIQUE constraint. REGLA: generada por DB. TIPOS CUIT/CUIL (2,3): concatena prefijo+nro_doc+digito; otros tipos: tipo_documento-nro_doc.'
        AFTER digito_verificador;

ALTER TABLE fal_persona
    ADD UNIQUE KEY uq_fal_persona_tipo_doc_key (tipo_documento, doc_key);

-- -----------------------------------------------------------------------------
-- 7. Restitucion del comentario de tabla anterior.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_persona
    COMMENT = 'QUE ES: Maestro de personas fisicas y juridicas (infractores, presentantes). USO: identificacion en actas, notificaciones y domicilios. RAZON: modelo estructurado de documento evita duplicacion por formato. REGLAS CLAVE: doc_key garantiza unicidad; nro_doc es INT UNSIGNED; CUIT/CUIL require prefijo y digito.';

-- -----------------------------------------------------------------------------
-- 8. Verificacion del retorno.
-- -----------------------------------------------------------------------------
SELECT column_name, column_type, is_nullable, extra, generation_expression
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name   = 'fal_persona'
   AND column_name IN ('tipo_documento', 'prefijo_cuit_cuil', 'nro_doc',
                       'digito_verificador', 'doc_key')
 ORDER BY ordinal_position;

-- =============================================================================
-- FIN DEL ROLLBACK - fal_persona documento opcional
-- =============================================================================
