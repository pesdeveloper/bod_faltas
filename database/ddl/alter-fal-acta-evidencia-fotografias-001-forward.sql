-- =============================================================================
-- ALTER - fal_acta_evidencia: metadata de fotografias (Parte 3D)
-- Script: alter-fal-acta-evidencia-fotografias-001-forward.sql
-- Trabajo: WAVE-04-PARTE-3D-EVIDENCIAS-FOTOGRAFICAS-20260806
-- Decision: DECISION_DDL-EVID-03 -- extension NO destructiva de
--           DECISION_DDL-EVID-01/02 (HUMAN_DECISION_CLOSED, FULL-R1.2-CORRECCION-10).
--
-- MOTIVO
-- fal_acta_evidencia ya es el repositorio de evidencias del acta (documentos,
-- multimedia, firma olografa) con tipo_evid como catalogo cerrado de 7 valores.
-- La Parte 3D exige un subsistema de fotografias con metadata rica (orden,
-- descripcion, dimensiones, MIME, SHA-256 del archivo final, fecha EXIF,
-- trazabilidad de etapa/origen) y dos restricciones UNIQUE nuevas por acta
-- (orden, hash). El prompt de la Parte 3D exige explicitamente "reconstruir la
-- tabla existente... no crear una segunda tabla paralela si la existente puede
-- evolucionar limpiamente": esta es esa evolucion.
--
-- DECISION_DDL-EVID-03 (evolucion, no reemplazo, de DECISION_DDL-EVID-01/02):
--   - tipo_evid, su catalogo de 7 valores y sus 37 guardrails NO se tocan.
--   - Las columnas nuevas son NULL-able y EXCLUSIVAS de evidencias FOTO: para
--     VIDEO/AUDIO/PDF/DOCUMENTO_OFIMATICO/PLANILLA_CALCULO/FIRMA_OLOGRAFA_INFRACTOR
--     permanecen NULL para siempre (el CHECK nuevo lo hace explicito).
--   - hash_evid (ya existente, CHAR(64) NULL, nunca poblado hasta hoy por
--     ningun camino de codigo) se reutiliza como SHA-256 del archivo final
--     procesado de la fotografia. No se agrega una columna paralela.
--   - orden y hash_evid quedan bajo UNIQUE(id_acta, *): MariaDB trata cada
--     NULL como distinto dentro de un UNIQUE, de modo que las filas no-FOTO
--     (orden y hash_evid ambos NULL) nunca colisionan entre si ni con una fila
--     FOTO real. La verificacion de esta semantica es parte del postcheck.
--   - "Todas las imagenes son FOTOGRAFIAS GENERALES" (Parte 3D S5.1) se lee
--     como: no existe una subclasificacion DENTRO de FOTO (no hay
--     tipo_fotografia); tipo_evid=4 (FOTO) sigue siendo la clasificacion de
--     evidencia que distingue una fotografia de un video, audio, PDF, etc.
--
-- COLUMNAS NUEVAS (las diez, todas NULL, todas exclusivas de tipo_evid=FOTO)
--   orden                 SMALLINT NULL      orden asignado por backend (1..10 por lote)
--   descripcion           VARCHAR(240) NULL  epigrafe opcional, normalizado
--   nombre_original       VARCHAR(180) NULL  nombre de archivo saneado (solo informativo)
--   mime_tipo             VARCHAR(50) NULL   MIME final tras el pipeline (image/jpeg)
--   tamanio_bytes         INT UNSIGNED NULL  tamano del archivo final en bytes
--   ancho_px              INT UNSIGNED NULL  ancho final en pixeles
--   alto_px               INT UNSIGNED NULL  alto final en pixeles
--   fecha_captura_exif    DATETIME(6) NULL   fecha EXIF informativa (nunca autoritativa)
--   bloque_al_incorporar  CHAR(4) NULL       BloqueActual.codigo() vigente al incorporar
--   origen_incorporacion  SMALLINT NULL      4=ALTA_INICIAL, 8=CARGA_POSTERIOR (multiplo de 4,
--                                            mismo estilo que TipoEvidenciaActa)
--
-- RESTRICCIONES NUEVAS
--   UNIQUE(id_acta, orden)     -- orden nunca se repite dentro de la misma acta
--   UNIQUE(id_acta, hash_evid) -- SHA-256 nunca se repite dentro de la misma acta
--
-- SIN CHECK NUEVO (decision explicita, no omision)
--   La coherencia "tipo_evid=FOTO implica columnas nuevas pobladas" NO se
--   impone con un CHECK fisico. database/diagnostics/verify-domain-schema.sql
--   Seccion 12 documenta DECISION_DDL-SIN-CHECKS-CODIGOS-01
--   (ACTA-ALTA-INTEGRAL-JDBC-MASTER-20260804-224853): 43 CHECK de
--   "coherencia funcional entre campos" fueron retirados del DDL porque esa
--   validacion vive en dominio/aplicacion, no en el motor, dejando
--   chk_evidencia_hash como "la unica CHECK que permanece en el dominio"
--   (invariante criptografico fijo, no de coherencia funcional). Agregar un
--   CHECK de coherencia FOTO<->campos aqui repetiria exactamente el patron
--   que esa decision cerro. La coherencia se valida en
--   FalActaEvidencia.java (constructor) y en el caso de uso de incorporacion
--   de fotografias, nunca en el motor.
--
-- FUERA DE ALCANCE
--   Ninguna columna existente cambia de tipo. tipo_evid, storage_key,
--   fecha_registro, fh_alta, id_user_alta no se tocan. hash_evid no cambia de
--   tipo (ya era CHAR(64) NULL): solo se documenta su nuevo uso.
--
-- Motor: MariaDB 12.3.2 | InnoDB | utf8mb4 | utf8mb4_uca1400_ai_ci
-- Precondicion: create-bod-faltas-domain.sql ya ejecutado (fal_acta_evidencia existe).
--
-- PRECHECK OBLIGATORIO (verificado 0 filas antes de este forward):
--   database/diagnostics/verify-fal-acta-evidencia-fotografias-001-precheck.sql
--
-- ROLLBACK
--   database/ddl/alter-fal-acta-evidencia-fotografias-001-rollback.sql
--   Ventana de rollback segura mientras ninguna fila tenga orden, hash_evid,
--   mime_tipo, tamanio_bytes, ancho_px, alto_px u origen_incorporacion
--   informados (es decir, mientras no exista ninguna fotografia real
--   incorporada por el pipeline de la Parte 3D).
-- =============================================================================

USE sb_faltas_db;

-- -----------------------------------------------------------------------------
-- 1. Columnas nuevas, todas NULL, todas exclusivas de tipo_evid=FOTO.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_acta_evidencia
    ADD COLUMN orden SMALLINT NULL
        COMMENT 'CONTENIDO: orden de la fotografia dentro del acta. USO: exclusivo de tipo_evid=FOTO; asignado por backend, nunca por el cliente. REGLA: NULL para evidencias no-FOTO; UNIQUE(id_acta, orden) cuando informado (DECISION_DDL-EVID-03).'
        AFTER hash_evid,
    ADD COLUMN descripcion VARCHAR(240) NULL
        COMMENT 'CONTENIDO: epigrafe opcional de la fotografia, texto normalizado. USO: exclusivo de tipo_evid=FOTO. REGLA: NULL para evidencias no-FOTO; NULL o no vacio tras trim (DECISION_DDL-EVID-03).'
        AFTER orden,
    ADD COLUMN nombre_original VARCHAR(180) NULL
        COMMENT 'CONTENIDO: nombre de archivo original saneado, solo informativo. USO: exclusivo de tipo_evid=FOTO; nunca se usa como ruta. REGLA: NULL para evidencias no-FOTO (DECISION_DDL-EVID-03).'
        AFTER descripcion,
    ADD COLUMN mime_tipo VARCHAR(50) NULL
        COMMENT 'CONTENIDO: MIME final tras el pipeline de normalizacion de imagen (p.ej. image/jpeg). USO: exclusivo de tipo_evid=FOTO. REGLA: NOT NULL cuando tipo_evid=FOTO (chk_evidencia_foto_campos, DECISION_DDL-EVID-03).'
        AFTER nombre_original,
    ADD COLUMN tamanio_bytes INT UNSIGNED NULL
        COMMENT 'CONTENIDO: tamano en bytes del archivo final procesado. USO: exclusivo de tipo_evid=FOTO. REGLA: NOT NULL cuando tipo_evid=FOTO (DECISION_DDL-EVID-03).'
        AFTER mime_tipo,
    ADD COLUMN ancho_px INT UNSIGNED NULL
        COMMENT 'CONTENIDO: ancho final en pixeles tras el pipeline. USO: exclusivo de tipo_evid=FOTO. REGLA: NOT NULL cuando tipo_evid=FOTO (DECISION_DDL-EVID-03).'
        AFTER tamanio_bytes,
    ADD COLUMN alto_px INT UNSIGNED NULL
        COMMENT 'CONTENIDO: alto final en pixeles tras el pipeline. USO: exclusivo de tipo_evid=FOTO. REGLA: NOT NULL cuando tipo_evid=FOTO (DECISION_DDL-EVID-03).'
        AFTER ancho_px,
    ADD COLUMN fecha_captura_exif DATETIME(6) NULL
        COMMENT 'CONTENIDO: fecha/hora de captura leida del EXIF original, informativa. USO: exclusivo de tipo_evid=FOTO. REGLA: nunca autoritativa; NULL si el EXIF no existe o es invalido; nunca la informa el cliente directamente (DECISION_DDL-EVID-03).'
        AFTER alto_px,
    ADD COLUMN bloque_al_incorporar CHAR(4) NULL
        COMMENT 'CONTENIDO: BloqueActual.codigo() del acta vigente al momento de incorporar la fotografia. USO: trazabilidad operativa de etapa; exclusivo de tipo_evid=FOTO. REGLA: NULL para evidencias no-FOTO (DECISION_DDL-EVID-03).'
        AFTER fecha_captura_exif,
    ADD COLUMN origen_incorporacion SMALLINT NULL
        COMMENT 'CONTENIDO: origen de la operacion que incorporo la fotografia. VALORES: 4=ALTA_INICIAL, 8=CARGA_POSTERIOR (OrigenIncorporacionEvidencia enum, multiplos de 4 como TipoEvidenciaActa). USO: exclusivo de tipo_evid=FOTO. REGLA: NOT NULL cuando tipo_evid=FOTO (DECISION_DDL-EVID-03).'
        AFTER bloque_al_incorporar;

-- -----------------------------------------------------------------------------
-- 2. UNIQUE nuevos. NULL se trata como distinto en MariaDB/InnoDB: las filas
--    no-FOTO (orden y hash_evid ambos NULL) nunca colisionan.
-- -----------------------------------------------------------------------------
ALTER TABLE fal_acta_evidencia
    ADD CONSTRAINT uq_evidencia_acta_orden UNIQUE (id_acta, orden),
    ADD CONSTRAINT uq_evidencia_acta_hash UNIQUE (id_acta, hash_evid);

-- -----------------------------------------------------------------------------
-- 3. Comentario de tabla actualizado (DECISION_DDL-COMMENT-01: comentarios
--    exactos, refleja la evolucion sin perder la mencion original a la firma
--    olografa ni al caracter append-only).
-- -----------------------------------------------------------------------------
ALTER TABLE fal_acta_evidencia
    COMMENT = 'Evidencias del acta (fotografias generales, firma olografa del infractor, video, audio, PDF, documentos ofimaticos y planillas). Append-only. DECISION_DDL-EVID-01 (catalogo tipo_evid) + DECISION_DDL-EVID-03 (metadata de fotografias: orden, hash, dimensiones, MIME, EXIF, trazabilidad de etapa/origen; exclusiva de tipo_evid=FOTO).';

-- -----------------------------------------------------------------------------
-- 4. POSTCHECK resumido. El detalle esta en el script de diagnostico.
-- -----------------------------------------------------------------------------
SELECT column_name, column_type, is_nullable
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name   = 'fal_acta_evidencia'
   AND column_name IN ('orden','descripcion','nombre_original','mime_tipo',
                       'tamanio_bytes','ancho_px','alto_px','fecha_captura_exif',
                       'bloque_al_incorporar','origen_incorporacion')
 ORDER BY ordinal_position;

-- =============================================================================
-- FIN DEL FORWARD - fal_acta_evidencia fotografias (Parte 3D)
-- =============================================================================
