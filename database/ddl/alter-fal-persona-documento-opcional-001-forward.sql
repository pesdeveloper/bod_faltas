-- =============================================================================
-- ALTER - fal_persona: documento opcional, canonico y estructurado
-- Script: alter-fal-persona-documento-opcional-001-forward.sql
-- Trabajo: JDBC-DOMINIO-EDA-WAVE-1A2-B1.5B0-B
-- Decision: DECISION_DDL-PERS-01 (unica autoridad vigente sobre el documento
--           de fal_persona), plan de migracion de 13 pasos.
--
-- MOTIVO
-- El modelo Java ya admite un estado que el motor rechaza: una persona sin
-- documento, exigida por SituacionIdentificacionInfractor.IDENTIFICADO_PARCIAL.
-- Contra el esquema ejecutado, tipo_documento y nro_doc son NOT NULL y nro_doc
-- es INT UNSIGNED, de modo que un pasaporte alfanumerico tampoco es
-- representable.
--
--   antes                                  despues
--   tipo_documento     SMALLINT NOT NULL   SMALLINT NULL
--   nro_doc            INT UNSIGNED NOT NULL  VARCHAR(12) NULL
--   prefijo_cuit_cuil  TINYINT UNSIGNED NULL  sin cambio de tipo; CHECK 10..99
--   digito_verificador TINYINT UNSIGNED NULL  sin cambio
--   doc_key            VARCHAR(20) STORED     VARCHAR(12) STORED, expresion nueva
--
-- doc_key deja de incluir el prefijo 'tipo_documento-' porque tipo_documento ya
-- es la primera columna del UNIQUE, y deja de usar COALESCE y LPAD: no se
-- rellenan componentes ausentes para producir una clave de apariencia completa.
-- Son los CHECK los que exigen los componentes antes de que la clave se genere.
--
-- Consecuencia deliberada del UNIQUE compuesto: los mismos once digitos pueden
-- existir una vez como CUIT y otra como CUIL, porque son dos tipos distintos.
--
-- Sin documento, doc_key es NULL y MariaDB admite ilimitadas filas con clave
-- NULL bajo un UNIQUE: asi coexisten las personas identificadas parcialmente.
--
-- FUERA DE ALCANCE
--   El modulo 11 del digito verificador de CUIT/CUIL no se exige: los CHECK
--   verifican estructura, no aritmetica. Deuda D-PERS-CUIT-DV-01, abierta.
--   chk_fal_persona_tipo, chk_fal_persona_digito, chk_fal_persona_id_suj,
--   chk_fal_persona_id_bie y chk_fal_persona_id_bie_suj no se tocan.
--   prefijo_cuit_cuil y digito_verificador NO se eliminan: la estructura de
--   tres componentes es justamente lo que la decision ratifica.
--
-- NOMBRE FISICO
--   El nombre canonico es tipo_documento. tipo_doc esta prohibido como nombre
--   de columna: no existe en el motor y este script no lo introduce.
--
-- Motor: MariaDB 12.3.2 | InnoDB | utf8mb4 | utf8mb4_uca1400_ai_ci
-- Precondicion: create-bod-faltas-domain.sql ya ejecutado (fal_persona existe).
--
-- PRECHECK OBLIGATORIO (paso 1 del plan)
--   database/diagnostics/verify-fal-persona-documento-opcional-001-precheck.sql
--   Los catorce controles deben devolver 0. Con una sola fila no migrable de
--   forma deterministica, DETENER: este script no infiere tipo documental,
--   prefijo, cuerpo ni digito.
--
-- BACKUP (paso 2 del plan)
--   La seccion 2 emite el volcado logico completo, incluido doc_key ya
--   materializado. Capturarlo y conservarlo antes de continuar; la restitucion
--   de datos, si hiciera falta, sale de ahi.
--
-- POSTCHECK (paso 13 del plan)
--   database/diagnostics/verify-fal-persona-documento-opcional-001-postcheck.sql
--
-- ROLLBACK
--   database/ddl/alter-fal-persona-documento-opcional-001-rollback.sql
--   Su ventana se cierra con la primera fila que tenga tipo_documento NULL o
--   nro_doc no numerico. Pertenece a la misma ventana de cambio que el forward.
-- =============================================================================

USE sb_faltas_db;

-- -----------------------------------------------------------------------------
-- 1. PRECHECK resumido. El detalle esta en el script de diagnostico.
--    Las tres columnas deben devolver 0 antes de continuar.
-- -----------------------------------------------------------------------------
SELECT
    COUNT(*)                                                             AS filas_totales,
    SUM(tipo_documento IS NULL OR nro_doc IS NULL)                       AS documento_incompleto,
    SUM(tipo_documento IN (2, 3)
        AND (prefijo_cuit_cuil IS NULL
             OR digito_verificador IS NULL
             OR prefijo_cuit_cuil NOT BETWEEN 10 AND 99
             OR CAST(nro_doc AS CHAR) NOT REGEXP '^[0-9]{8}$'))          AS cuit_cuil_no_migrable,
    SUM(tipo_documento = 1
        AND CAST(nro_doc AS CHAR) NOT REGEXP '^[1-9][0-9]{0,7}$')        AS dni_no_canonico
FROM fal_persona;

-- -----------------------------------------------------------------------------
-- 2. BACKUP LOGICO - volcado completo con doc_key materializado.
--    Capturar la salida antes de continuar. No crea objetos en la base: el
--    respaldo vive fuera del motor, junto a la evidencia del trabajo.
-- -----------------------------------------------------------------------------
SELECT id, tipo_persona, tipo_documento, prefijo_cuit_cuil, nro_doc,
       digito_verificador, doc_key, apellido, nombres, razon_social,
       nombre_mostrar, email_principal, telefono_principal, id_suj, id_bie,
       suj_bie_estado, fh_suj_bie_creacion, fh_alta, id_user_alta,
       fh_ult_mod, id_user_ult_mod
  FROM fal_persona
 ORDER BY id;

-- -----------------------------------------------------------------------------
-- 3. Baja del UNIQUE. Debe preceder a la baja de doc_key: el indice la usa.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_persona DROP INDEX uq_fal_persona_tipo_doc_key;

-- -----------------------------------------------------------------------------
-- 4. Baja de la columna generada. Es derivada: no hay perdida de informacion.
--    MariaDB no admite redefinir la expresion de una STORED GENERATED sobre
--    columnas cuyo tipo esta por cambiar, de modo que se baja y se recrea.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_persona DROP COLUMN doc_key;

-- -----------------------------------------------------------------------------
-- 5. Baja del CHECK numerico. Es un CHECK aritmetico sobre una columna que pasa
--    a ser texto. Su limite superior de ocho digitos se preserva para DNI en
--    chk_fal_persona_nro_doc_dni (paso 12).
-- -----------------------------------------------------------------------------
ALTER TABLE fal_persona DROP CONSTRAINT chk_fal_persona_nro_doc;

-- -----------------------------------------------------------------------------
-- 6. nro_doc: INT UNSIGNED NOT NULL -> VARCHAR(12) NULL.
--    La conversion es textualmente exacta para los datos numericos existentes:
--    MariaDB los renderiza en decimal sin ceros a la izquierda, y esos ceros
--    nunca existieron en INT UNSIGNED. Verificado en prueba controlada.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_persona
    MODIFY COLUMN nro_doc VARCHAR(12) NULL
        COMMENT 'CONTENIDO: Numero de documento en forma canonica, sin separadores y en mayusculas. USO: parte principal del documento; cuerpo central de 8 digitos en CUIT/CUIL. REGLA: 1 a 12 caracteres [0-9A-Z]; NULL cuando no hay documento; DNI sin ceros iniciales y hasta 8 digitos (DECISION_DDL-PERS-01).';

-- -----------------------------------------------------------------------------
-- 7. tipo_documento: SMALLINT NOT NULL -> SMALLINT NULL.
--    La persona sin documento pasa a ser representable.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_persona
    MODIFY COLUMN tipo_documento SMALLINT NULL
        COMMENT 'CONTENIDO: Tipo de documento de identidad. USO: determina la estructura del documento. VALORES: 1=DNI, 2=CUIT, 3=CUIL, 4=PASAPORTE, 5=DNI_EXTRANJERO, 9=OTRO (TipoDocumentoPersona enum). REGLA: NULL cuando la persona no tiene documento; se informa junto con nro_doc (DECISION_DDL-PERS-01).';

-- -----------------------------------------------------------------------------
-- 8. Normalizacion de los valores existentes a forma canonica.
--    Sobre datos provenientes de INT UNSIGNED es materialmente un no-op: no
--    pueden contener minusculas, separadores ni espacios. Se ejecuta igual
--    porque el plan no asume la tabla vacia y el paso debe existir.
--
--    NO se descompone aqui ningun CUIT/CUIL: el esquema anterior ya guardaba el
--    cuerpo central en nro_doc y los otros dos componentes en sus columnas. Un
--    CUIT cuyos once digitos estuvieran en nro_doc habria sido detectado por el
--    control P08 del precheck y habria bloqueado el forward, porque
--    reconstruirlo exigiria inferir prefijo y digito.
-- -----------------------------------------------------------------------------
UPDATE fal_persona
   SET nro_doc = UPPER(REGEXP_REPLACE(nro_doc, '[[:space:]./-]', ''))
 WHERE nro_doc IS NOT NULL
   AND nro_doc <> UPPER(REGEXP_REPLACE(nro_doc, '[[:space:]./-]', ''));

-- -----------------------------------------------------------------------------
-- 9. prefijo_cuit_cuil: el CHECK se estrecha de 0..99 a 10..99.
--    Dos digitos significativos es lo que permite componer doc_key sin relleno.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_persona DROP CONSTRAINT chk_fal_persona_prefijo;
ALTER TABLE fal_persona
    ADD CONSTRAINT chk_fal_persona_prefijo
        CHECK (prefijo_cuit_cuil IS NULL OR prefijo_cuit_cuil BETWEEN 10 AND 99);

ALTER TABLE fal_persona
    MODIFY COLUMN prefijo_cuit_cuil TINYINT UNSIGNED NULL
        COMMENT 'CONTENIDO: Prefijo de CUIT/CUIL (ej: 20, 23, 27, 30). USO: solo para CUIT/CUIL. REGLA: NULL para el resto de los tipos; BETWEEN 10 AND 99 cuando se informa, porque son dos digitos significativos (DECISION_DDL-PERS-01).';

-- -----------------------------------------------------------------------------
-- 10. doc_key nueva. Sin COALESCE, sin LPAD y sin el prefijo tipo_documento-.
--     AFTER digito_verificador conserva la posicion ordinal original.
--     Orden de clausulas exigido por MariaDB en una columna generada:
--     AS (...) STORED, luego COMMENT, y recien al final la posicion.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_persona
    ADD COLUMN doc_key VARCHAR(12) AS (
        CASE
            WHEN tipo_documento IS NULL   THEN NULL
            WHEN tipo_documento IN (2, 3) THEN CONCAT(prefijo_cuit_cuil, nro_doc, digito_verificador)
            ELSE nro_doc
        END) STORED
        COMMENT 'CONTENIDO: Clave funcional del documento. USO: segunda columna del UNIQUE de unicidad documental. REGLA: generada por la DB; NULL sin documento; CUIT/CUIL (2,3) concatena prefijo+nro_doc+digito sin relleno; el resto de los tipos reproduce nro_doc. Prohibido COALESCE y LPAD (DECISION_DDL-PERS-01).'
        AFTER digito_verificador;

-- -----------------------------------------------------------------------------
-- 11. UNIQUE compuesto. Falla si el paso 1 no detecto duplicados.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_persona
    ADD UNIQUE KEY uq_fal_persona_tipo_doc_key (tipo_documento, doc_key);

-- -----------------------------------------------------------------------------
-- 12. Los seis CHECK nuevos de la seccion 7 de DECISION_DDL-PERS-01.
--     Fallan si el paso 8 quedo incompleto.
--
--     Nota de semantica verificada en prueba controlada: bajo la collation
--     utf8mb4_uca1400_ai_ci, REGEXP es insensible a caso, de modo que
--     chk_fal_persona_nro_doc_canonico admite tambien letras minusculas. Es un
--     CHECK estructural de respaldo; la forma canonica en mayusculas la
--     garantiza DocumentoPersona en Java, que es la unica autoridad de
--     normalizacion. La insensibilidad a acentos NO se extiende a la clase de
--     caracteres: una vocal acentuada es rechazada.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_persona
    ADD CONSTRAINT chk_fal_persona_doc_coherente
        CHECK ((tipo_documento IS NULL) = (nro_doc IS NULL)),
    ADD CONSTRAINT chk_fal_persona_doc_tipo
        CHECK (tipo_documento IS NULL OR tipo_documento IN (1, 2, 3, 4, 5, 9)),
    ADD CONSTRAINT chk_fal_persona_doc_cuit
        CHECK (tipo_documento IS NULL
               OR tipo_documento NOT IN (2, 3)
               OR (prefijo_cuit_cuil IS NOT NULL
                   AND digito_verificador IS NOT NULL
                   AND nro_doc REGEXP '^[0-9]{8}$')),
    ADD CONSTRAINT chk_fal_persona_doc_no_cuit
        CHECK (tipo_documento IS NULL
               OR tipo_documento IN (2, 3)
               OR (prefijo_cuit_cuil IS NULL AND digito_verificador IS NULL)),
    ADD CONSTRAINT chk_fal_persona_nro_doc_canonico
        CHECK (nro_doc IS NULL OR nro_doc REGEXP '^[0-9A-Z]{1,12}$'),
    ADD CONSTRAINT chk_fal_persona_nro_doc_dni
        CHECK (tipo_documento IS NULL
               OR tipo_documento <> 1
               OR nro_doc REGEXP '^[1-9][0-9]{0,7}$');

-- -----------------------------------------------------------------------------
-- 13. Comentario de tabla: nro_doc ya no es INT UNSIGNED y el documento es
--     opcional (DECISION_DDL-COMMENT-01 exige comentarios exactos).
-- -----------------------------------------------------------------------------
ALTER TABLE fal_persona
    COMMENT = 'QUE ES: Maestro de personas fisicas y juridicas (infractores, presentantes). USO: identificacion en actas, notificaciones y domicilios. RAZON: modelo estructurado de documento evita duplicacion por formato. REGLAS CLAVE: el documento es opcional en conjunto; nro_doc es VARCHAR(12) en forma canonica; CUIT/CUIL se almacenan descompuestos y requieren prefijo y digito; doc_key garantiza unicidad junto con tipo_documento y es NULL sin documento.';

-- -----------------------------------------------------------------------------
-- 14. POSTCHECK resumido. El detalle esta en el script de diagnostico.
-- -----------------------------------------------------------------------------
SELECT column_name, column_type, is_nullable, extra, generation_expression
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name   = 'fal_persona'
   AND column_name IN ('tipo_documento', 'prefijo_cuit_cuil', 'nro_doc',
                       'digito_verificador', 'doc_key')
 ORDER BY ordinal_position;

-- =============================================================================
-- FIN DEL FORWARD - fal_persona documento opcional
-- =============================================================================
