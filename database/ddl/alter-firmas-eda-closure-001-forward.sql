-- APROBADO COMO DISEÑO — DDL_READY, DDL_NOT_EXECUTED
-- NO EJECUTAR EN PRODUCCIÓN SIN BACKUP, PRECHECKS Y AUTORIZACIÓN
-- =============================================================================
-- ALTER/CREATE — Integración EDA Faltas ↔ Firmas
-- Script: alter-firmas-eda-closure-001-forward.sql
-- Trabajo: SPEC-FIRMAS-EDA-CLOSURE-001
--
-- Extiende el baseline DDL-MARIADB-MANUAL-001-FULL-R1 (create-bod-faltas-domain.sql,
-- 65 tablas canónicas) con:
--   1. ALTER TABLE fal_documento_firma_req      (14 columnas nuevas: 10 DEC-EDA-020 +
--      4 DEC-EDA-030 preparación persistida + catálogo estado_firma_req extendido de
--      5 a 9 valores + 5 CHECK + 3 índices/UNIQUE)
--   2. ALTER TABLE fal_documento_firma          (27 columnas nuevas: 3 hashes/revisiones +
--      7 actor técnico + 17 metadata PKI + chk_firma_eda_completo integral (DEC-EDA-034) +
--      chk_firma_perfil_unico PAdES-B-LT sin excepción (DEC-EDA-035) + 8 CHECK
--      adicionales + 3 índices/UNIQUE; fk_firma_resultado diferida a la sección 04bis)
--   3. ALTER TABLE fal_documento                (1 columna nueva: revision_artefacto +
--      1 CHECK + backfill seguro por consistencia storage_key/hash_docu, DEC-EDA-031)
--   4. CREATE TABLE fal_documento_firma_resultado (staging temporal del candidato firmado +
--      swap de storage recuperable tras crash, DEC-EDA-032 + binding criptográfico del
--      recibo, DEC-EDA-033; 2 FK + 1 UNIQUE + 5 índices + 6 CHECK)
--   4bis. ALTER TABLE fal_documento_firma ADD fk_firma_resultado (después de 4)
--   5. CREATE TABLE bus_evento (bus central append-only, único; sin fan-out, sin
--      suscripciones, sin rutinas SECURITY DEFINER, sin vistas), fal_outbox (Outbox
--      local de Faltas), fal_bus_checkpoint (checkpoint local de lectura del bus,
--      singleton + seed), fal_inbox (Inbox local, PK = bus_evento_id +
--      UNIQUE(evento_id), envelope completo), fal_inbox_error
--      (errores de Inbox, tabla local separada) — 5 tablas nuevas, propiedad Faltas
--      (DEC-EDA-011/019/022/025/026)
--
-- Estado: DISEÑO CERRADO — DDL_READY — DDL_NOT_EXECUTED — IMPLEMENTATION_NOT_STARTED
-- Motor: InnoDB | Charset: utf8mb4 | Collation: utf8mb4_uca1400_ai_ci
-- Precondición de ejecución: create-bod-faltas-domain.sql ya ejecutado
-- (fal_documento, fal_documento_firma_req y fal_documento_firma ya existen).
--
-- INSTRUCCIONES DE EJECUCIÓN (cuando se autorice):
--   1. Ejecutar database/diagnostics/verify-firmas-eda-closure-001-precheck.sql.
--   2. Conectarse a la base de datos de destino (USE sb_faltas_db).
--   3. Ejecutar este script íntegro con un cliente MariaDB (mysql / HeidiSQL).
--   4. Ejecutar database/diagnostics/verify-firmas-eda-closure-001-postcheck.sql
--      y verify-firmas-eda-closure-001-parity.sql.
--   5. Ejecutar database/diagnostics/verify-firmas-eda-bus-principals-precheck.sql
--      (confirma que los 5 usuarios bus_* ya existen, aprovisionados fuera de este
--      repositorio, con host permitido y REQUIRE SSL).
--   6. Ejecutar database/ddl/grants-firmas-eda-bus.sql (INSERT column-level sin
--      bus_evento_id para publishers / SELECT / SELECT, DELETE de mínimo
--      privilegio, directo sobre bus_evento; sin contraseñas ni CREATE USER).
--
-- ADVERTENCIA:
--   NO ejecutar sin haber ejecutado antes create-bod-faltas-domain.sql.
--   NO ejecutar en producción sin revisión manual previa.
--   Esta migración es aditiva (ADD COLUMN / ADD CONSTRAINT / ADD INDEX / CREATE TABLE)
--   sobre fal_documento_firma_req, fal_documento_firma y fal_documento — ningún DROP
--   sobre objetos preexistentes. Contiene un único MODIFY COLUMN (sección 01.2,
--   comentario de estado_firma_req; no cambia tipo, nullable ni default de esa
--   columna, solo el texto documental del catálogo).
--   Deliberadamente NO usa SET FOREIGN_KEY_CHECKS=0: cualquier error de FK debe
--   interrumpir el script de forma visible, nunca ocultarse.
--
-- Fuente normativa: backend/api-faltas-core/docs/spec-as-source/
-- Modelo físico completo: 50-persistence/firma-eda-physical-model.md
-- Decisiones físicas: 50-persistence/ddl-decisions.md (DECISION_DDL-FIRMAREQ-01/-02,
--   DECISION_DDL-FIRMA-02/-03/-04, DECISION_DDL-DOC-02/-03, DECISION_DDL-RESULTADO-01
--   a -04, DECISION_DDL-BUS-01/-02/-03)
-- Orden de ejecución y dependencias: firma-eda-physical-model.md §12
-- Rollback: database/ddl/alter-firmas-eda-closure-001-rollback.sql
-- Recuperación de ejecución parcial: firma-eda-physical-model.md §14
-- =============================================================================

USE sb_faltas_db;

SET NAMES utf8mb4;

-- =============================================================================
-- SECCIÓN 01 — fal_documento_firma_req: columnas nuevas (DEC-EDA-016/020/030)
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 1.1 Columnas nuevas (todas ADD COLUMN; ninguna columna existente cambia) — 10 columnas
-- ----------------------------------------------------------------------------
ALTER TABLE fal_documento_firma_req
    ADD COLUMN version_row INT NOT NULL DEFAULT 0
        COMMENT 'CONTENIDO: Control de concurrencia optimista. USO: OCC para la regla de carrera firma/cancelacion. REGLA: incrementar en cada modificacion; ausente hasta este cierre (DEC-EDA-016/020).'
        AFTER id_user_alta,
    ADD COLUMN solicitud_firma_id CHAR(36) NULL
        COMMENT 'CONTENIDO: UUID de correlacion EDA de la solicitud de firma. USO: idempotencia y correlacion con FirmaSolicitada.solicitudFirmaId. REGLA: NULL para filas anteriores a esta migracion (DEC-EDA-020).'
        AFTER version_row,
    ADD COLUMN revision_solicitud SMALLINT NOT NULL DEFAULT 0
        COMMENT 'CONTENIDO: Revision de la solicitud EDA. USO: base de la resolucion de carrera y de duplicados. REGLA: >= 0 (DEC-EDA-020).'
        AFTER solicitud_firma_id,
    ADD COLUMN fh_solicitud DATETIME(6) NULL
        COMMENT 'CONTENIDO: Momento de publicacion del evento FirmaSolicitada. USO: auditoria del lifecycle EDA. REGLA: NULL hasta que se publique la solicitud (DEC-EDA-016/020).'
        AFTER revision_solicitud,
    ADD COLUMN fh_aceptacion DATETIME(6) NULL
        COMMENT 'CONTENIDO: Momento del evento SolicitudFirmaAceptada. USO: auditoria del lifecycle EDA. REGLA: NULL hasta la aceptacion tecnica de Firmas (DEC-EDA-016/020).'
        AFTER fh_solicitud,
    ADD COLUMN fh_inicio_firma DATETIME(6) NULL
        COMMENT 'CONTENIDO: Momento en que Firmas confirma que comienza a procesar. USO: informativo; no crea un estado fisico nuevo. REGLA: NULL si no informado (DEC-EDA-016/020).'
        AFTER fh_aceptacion,
    ADD COLUMN vence_en DATETIME(6) NULL
        COMMENT 'CONTENIDO: Vencimiento de la solicitud, en UTC. USO: autoridad de vencimiento exclusiva de Faltas. REGLA: NULL si no aplica vencimiento (DEC-EDA-016 seccion 6.4).'
        AFTER fh_inicio_firma,
    ADD COLUMN fh_terminal DATETIME(6) NULL
        COMMENT 'CONTENIDO: Momento de la transicion terminal (FIRMADO/ANULADO/VENCIDO/REEMPLAZADO/CANCELADO/RECHAZADO/RECHAZADO_CONTRATO). USO: auditoria de cierre del lifecycle. REGLA: NULL mientras el requisito este en un estado no terminal (DEC-EDA-016/020).'
        AFTER vence_en,
    ADD COLUMN motivo_terminal_firma_req SMALLINT NULL
        COMMENT 'CONTENIDO: Motivo de la transicion terminal. USO: significado condicionado por estado_firma_req (ver 10-domain/firma-notificacion-fallo.md, Lifecycle unico de solicitud/requisito de firma). REGLA: NULL si estado_firma_req no es terminal (DEC-EDA-016/020).'
        AFTER fh_terminal,
    ADD COLUMN detalle_terminal_firma_req VARCHAR(255) NULL
        COMMENT 'CONTENIDO: Complemento descriptivo opcional del motivo terminal. USO: diagnostico; nunca clasificacion unica. REGLA: NULL admitido siempre (DEC-EDA-016/020).'
        AFTER motivo_terminal_firma_req;

-- ----------------------------------------------------------------------------
-- 1.1bis Preparación persistida e idempotente por requisito (DEC-EDA-030) — 4 columnas
--        firma-eda-physical-model.md §1.1bis
-- ----------------------------------------------------------------------------
ALTER TABLE fal_documento_firma_req
    ADD COLUMN revision_artefacto_preparada INT NULL
        COMMENT 'CONTENIDO: revisionArtefacto fijada por la preparacion mas reciente de este requisito. USO: verificar GET .../artefacto y validar PUT .../resultados contra la preparacion exacta, no contra fal_documento.revision_artefacto directamente. REGLA: NULL (nunca preparado) o >= 1; nunca 0 (DEC-EDA-030).'
        AFTER detalle_terminal_firma_req,
    ADD COLUMN hash_entrada_preparado VARCHAR(128) NULL
        COMMENT 'CONTENIDO: Hash SHA-256 (o algoritmo declarado) del PDF exacto expuesto a Firmas en la preparacion mas reciente. USO: idempotencia verificable contra base, no solo contra memoria del proceso HTTP. REGLA: NULL si nunca preparado; informado junto con las otras 3 columnas de esta subseccion (DEC-EDA-030).'
        AFTER revision_artefacto_preparada,
    ADD COLUMN content_length_preparado BIGINT NULL
        COMMENT 'CONTENIDO: Tamanio exacto del PDF expuesto en la preparacion mas reciente. USO: validar PUT .../resultados contra el tamanio realmente preparado. REGLA: NULL si nunca preparado; > 0 cuando informado (DEC-EDA-030).'
        AFTER hash_entrada_preparado,
    ADD COLUMN fh_preparacion DATETIME(6) NULL
        COMMENT 'CONTENIDO: Momento de la preparacion mas reciente de este requisito. USO: auditoria de la preparacion persistida. REGLA: NULL si nunca preparado (DEC-EDA-030).'
        AFTER content_length_preparado;

-- ----------------------------------------------------------------------------
-- 1.2 Catálogo estado_firma_req extendido (aditivo, sin renombrar valores existentes;
--     unico MODIFY de este script, exclusivamente sobre el texto documental del
--     comentario; tipo/nullable/default de la columna no cambian)
--     Valores existentes sin cambios: 1=PENDIENTE, 2=FIRMADO, 3=ANULADO, 4=VENCIDO, 5=REEMPLAZADO
--     Valores nuevos DEC-EDA-016: 6=CANCELACION_SOLICITADA, 7=CANCELADO, 8=RECHAZADO, 9=RECHAZADO_CONTRATO
-- ----------------------------------------------------------------------------
ALTER TABLE fal_documento_firma_req
    MODIFY COLUMN estado_firma_req SMALLINT NOT NULL DEFAULT 1
        COMMENT '1=PENDIENTE, 2=FIRMADO, 3=ANULADO, 4=VENCIDO, 5=REEMPLAZADO, 6=CANCELACION_SOLICITADA, 7=CANCELADO, 8=RECHAZADO, 9=RECHAZADO_CONTRATO (EstadoFirmaReq enum extendido DEC-EDA-016)';

-- ----------------------------------------------------------------------------
-- 1.3 Constraints e índices nuevos
-- ----------------------------------------------------------------------------
ALTER TABLE fal_documento_firma_req
    ADD CONSTRAINT chk_firma_req_estado_ext CHECK (estado_firma_req BETWEEN 1 AND 9),
    ADD CONSTRAINT chk_firma_req_revision CHECK (revision_solicitud >= 0),
    ADD CONSTRAINT chk_firma_req_preparacion CHECK (
        (revision_artefacto_preparada IS NULL AND hash_entrada_preparado IS NULL
            AND content_length_preparado IS NULL AND fh_preparacion IS NULL)
        OR (revision_artefacto_preparada IS NOT NULL AND hash_entrada_preparado IS NOT NULL
            AND content_length_preparado IS NOT NULL AND fh_preparacion IS NOT NULL)
    ),
    ADD CONSTRAINT chk_firma_req_rev_preparada CHECK (revision_artefacto_preparada IS NULL OR revision_artefacto_preparada >= 1),
    ADD CONSTRAINT chk_firma_req_content_length_preparado CHECK (content_length_preparado IS NULL OR content_length_preparado > 0),
    ADD UNIQUE KEY uq_firma_req_solicitud (solicitud_firma_id),
    ADD INDEX ix_firma_req_estado_vence (estado_firma_req, vence_en),
    ADD INDEX ix_firma_req_solicitud_rev (solicitud_firma_id, revision_solicitud);

-- ----------------------------------------------------------------------------
-- 1.4 Backfill de filas existentes (sin datos jurídicos que inventar)
-- ----------------------------------------------------------------------------
UPDATE fal_documento_firma_req
   SET version_row = 0,
       revision_solicitud = 0
 WHERE version_row IS NULL OR revision_solicitud IS NULL;
-- solicitud_firma_id, fh_solicitud, fh_aceptacion, fh_inicio_firma, vence_en,
-- fh_terminal, motivo_terminal_firma_req, detalle_terminal_firma_req,
-- revision_artefacto_preparada, hash_entrada_preparado, content_length_preparado,
-- fh_preparacion quedan NULL por DEFAULT de ADD COLUMN; no se inventa dato
-- retroactivo (firma-eda-physical-model.md §1/§1.1bis). Ningun requisito anterior
-- a esta migracion tiene una preparacion EDA persistida bajo este circuito; la
-- rama "todos NULL" de chk_firma_req_preparacion cubre exactamente ese caso.

-- =============================================================================
-- SECCIÓN 02 — fal_documento_firma: columnas nuevas (DEC-EDA-013/021/023/029) — 27 columnas
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 2.1 Hashes y revisiones (DEC-EDA-013) — 3 columnas
-- ----------------------------------------------------------------------------
ALTER TABLE fal_documento_firma
    ADD COLUMN hash_salida VARCHAR(128) NULL
        COMMENT 'CONTENIDO: Hash SHA-256 del PDF vigente despues de aplicar esta firma. USO: cadena de integridad hashSalida/hashEntrada (firmaN.hashSalida == firmaN+1.hashEntrada). REGLA: NULL para firmas historicas anteriores a esta migracion (DEC-EDA-013/021).'
        AFTER hash_firma,
    ADD COLUMN revision_entrada SMALLINT NULL
        COMMENT 'CONTENIDO: Revision del PDF vigente al momento de firmar. USO: trazabilidad de la cadena de revisiones del unico PDF vigente. REGLA: NULL para firmas historicas (DEC-EDA-013/021).'
        AFTER hash_salida,
    ADD COLUMN revision_salida SMALLINT NULL
        COMMENT 'CONTENIDO: Revision del PDF vigente resultante tras aplicar esta firma. USO: trazabilidad de la cadena de revisiones. REGLA: NULL para firmas historicas (DEC-EDA-013/021).'
        AFTER revision_entrada;

-- ----------------------------------------------------------------------------
-- 2.2 Actor técnico M2M (DEC-EDA-021) — 7 columnas
-- ----------------------------------------------------------------------------
ALTER TABLE fal_documento_firma
    ADD COLUMN actor_tecnico_tipo SMALLINT NULL
        COMMENT 'CONTENIDO: Catalogo ActorTecnicoTipo (1=APLICACION). USO: distingue el actor tecnico M2M del actor humano id_user_firma. REGLA: NULL si la fila no proviene del consumidor EDA (DEC-EDA-021).'
        AFTER fh_firma,
    ADD COLUMN actor_tecnico_id VARCHAR(64) NULL
        COMMENT 'CONTENIDO: Identificador tecnico del consumidor/instancia (ej. firmas-consumer-01). USO: auditoria tecnica del actor M2M. REGLA: NULL si no aplica (DEC-EDA-021).'
        AFTER actor_tecnico_tipo,
    ADD COLUMN actor_tecnico_app VARCHAR(32) NULL
        COMMENT 'CONTENIDO: Aplicacion origen del actor tecnico (firmas). USO: auditoria del origen M2M. REGLA: NULL si no aplica (DEC-EDA-021).'
        AFTER actor_tecnico_id,
    ADD COLUMN actor_tecnico_key_fingerprint VARCHAR(64) NULL
        COMMENT 'CONTENIDO: Fingerprint (hash) de la X-Api-Key usada. USO: auditoria de la credencial M2M sin exponerla. REGLA: nunca almacenar la clave completa; NULL si no aplica (DEC-EDA-018/021).'
        AFTER actor_tecnico_app,
    ADD COLUMN correlation_id CHAR(36) NULL
        COMMENT 'CONTENIDO: UUID de correlacion EDA de punta a punta. USO: trazabilidad distribuida. REGLA: NULL para firmas historicas o via firmar-real sincrono sin evento (DEC-EDA-021).'
        AFTER actor_tecnico_key_fingerprint,
    ADD COLUMN event_id CHAR(36) NULL
        COMMENT 'CONTENIDO: eventoId del evento DocumentoFirmado que origino esta fila. USO: idempotencia y trazabilidad del consumidor EDA. REGLA: NULL cuando la firma llega por el callback sincrono firmar-real (DEC-EDA-021).'
        AFTER correlation_id,
    ADD COLUMN received_at DATETIME(6) NULL
        COMMENT 'CONTENIDO: Momento en que Faltas recibio el resultado/evento. USO: auditoria de latencia EDA. REGLA: NULL si no aplica (DEC-EDA-021).'
        AFTER event_id;

-- ----------------------------------------------------------------------------
-- 2.3 Metadata PKI mínima (DEC-EDA-023/029) — 17 columnas
-- ----------------------------------------------------------------------------
ALTER TABLE fal_documento_firma
    ADD COLUMN perfil_firma VARCHAR(32) NULL
        COMMENT 'CONTENIDO: Perfil de firma PAdES aplicado. USO: auditoria del perfil PKI. REGLA: unico valor no nulo admitido: PAdES-B-LT (chk_firma_perfil_unico, DEC-EDA-035); NULL para firmas historicas.'
        AFTER received_at,
    ADD COLUMN algoritmo_hash VARCHAR(16) NULL
        COMMENT 'CONTENIDO: Algoritmo de hash usado (SHA-256/SHA-384/SHA-512). USO: auditoria criptografica. REGLA: nullable; nunca se completa retroactivamente un valor no demostrado para filas historicas (DEC-EDA-029). Obligatorio de hecho para filas nuevas via chk_firma_eda_completo (seccion 2.5).'
        AFTER perfil_firma,
    ADD COLUMN resultado_firma_id CHAR(36) NULL
        COMMENT 'CONTENIDO: Referencia al resultadoFirmaId de firma-binary-transfer.md / fal_documento_firma_resultado. USO: correlacion con la carga del resultado firmado; FK fisica hacia fal_documento_firma_resultado.resultado_firma_id (seccion 04bis). REGLA: NULL si no aplica; informado dispara la obligatoriedad condicional de chk_firma_eda_completo (DEC-EDA-017/023/024/034).'
        AFTER algoritmo_hash,
    ADD COLUMN evidencia_id VARCHAR(128) NULL
        COMMENT 'CONTENIDO: Referencia opaca a la evidencia criptografica completa conservada por Firmas. USO: trazabilidad de evidencia PKI externa. REGLA: NULL si no aplica (DEC-EDA-023).'
        AFTER resultado_firma_id,
    ADD COLUMN certificado_serial VARCHAR(64) NULL
        COMMENT 'CONTENIDO: Serial del certificado X.509 usado para firmar. USO: auditoria PKI minima verificable localmente. REGLA: NULL para firmas historicas (DEC-EDA-023).'
        AFTER evidencia_id,
    ADD COLUMN certificado_thumbprint VARCHAR(64) NULL
        COMMENT 'CONTENIDO: Thumbprint SHA-256 del certificado. USO: auditoria PKI. REGLA: NULL para firmas historicas (DEC-EDA-023).'
        AFTER certificado_serial,
    ADD COLUMN certificado_subject VARCHAR(255) NULL
        COMMENT 'CONTENIDO: Subject del certificado X.509. USO: auditoria PKI. REGLA: NULL para firmas historicas (DEC-EDA-023).'
        AFTER certificado_thumbprint,
    ADD COLUMN certificado_issuer VARCHAR(255) NULL
        COMMENT 'CONTENIDO: Issuer del certificado X.509. USO: auditoria PKI. REGLA: NULL para firmas historicas (DEC-EDA-023).'
        AFTER certificado_subject,
    ADD COLUMN certificado_vigencia_desde DATETIME(6) NULL
        COMMENT 'CONTENIDO: Inicio de vigencia del certificado al momento de firmar. USO: auditoria PKI. REGLA: NULL para firmas historicas (DEC-EDA-023).'
        AFTER certificado_issuer,
    ADD COLUMN certificado_vigencia_hasta DATETIME(6) NULL
        COMMENT 'CONTENIDO: Fin de vigencia del certificado al momento de firmar. USO: auditoria PKI. REGLA: NULL para firmas historicas; certificado_vigencia_desde <= certificado_vigencia_hasta cuando ambas informadas (chk_firma_cert_vigencia, DEC-EDA-023).'
        AFTER certificado_vigencia_desde,
    ADD COLUMN resultado_cadena SMALLINT NULL
        COMMENT 'CONTENIDO: Catalogo ResultadoValidacionCadena (1=VALIDA, 2=INVALIDA, 3=NO_VERIFICADA). USO: resultado de validacion de cadena de confianza X.509; debe ser VALIDA para aplicar dominio (DEC-EDA-023). REGLA: NULL para firmas historicas.'
        AFTER certificado_vigencia_hasta,
    ADD COLUMN fuente_revocacion SMALLINT NULL
        COMMENT 'CONTENIDO: Catalogo FuenteRevocacion (1=OCSP, 2=CRL, 3=NO_DISPONIBLE). USO: fuente de la verificacion de revocacion. REGLA: NULL para firmas historicas (DEC-EDA-023).'
        AFTER resultado_cadena,
    ADD COLUMN resultado_revocacion SMALLINT NULL
        COMMENT 'CONTENIDO: Catalogo ResultadoRevocacion (1=NO_REVOCADO, 2=REVOCADO, 3=DESCONOCIDO). USO: resultado de la verificacion de revocacion; debe ser NO_REVOCADO para aplicar dominio (DEC-EDA-023). REGLA: NULL para firmas historicas.'
        AFTER fuente_revocacion,
    ADD COLUMN tsa_nombre VARCHAR(128) NULL
        COMMENT 'CONTENIDO: Autoridad de sello de tiempo (TSA) usada. USO: auditoria del sello de tiempo. REGLA: NULL para firmas historicas (DEC-EDA-023).'
        AFTER resultado_revocacion,
    ADD COLUMN tsa_instante DATETIME(6) NULL
        COMMENT 'CONTENIDO: Instante del sello de tiempo. USO: auditoria del sello de tiempo; debe estar informado y el certificado debe estar vigente a este instante para aplicar dominio (DEC-EDA-023). REGLA: NULL para firmas historicas.'
        AFTER tsa_nombre,
    ADD COLUMN resultado_validacion SMALLINT NULL
        COMMENT 'CONTENIDO: Catalogo ResultadoValidacionFirma (1=VALIDO, 2=INVALIDO, 3=VALIDO_CON_OBSERVACIONES_PERMITIDAS). USO: resultado final de la validacion PKI reportada por Firmas; INVALIDO nunca aplica dominio (DEC-EDA-023). REGLA: NULL para firmas historicas.'
        AFTER tsa_instante,
    ADD COLUMN version_validador VARCHAR(32) NULL
        COMMENT 'CONTENIDO: Version del validador PKI usado por Firmas. USO: auditoria de reproducibilidad. REGLA: NULL para firmas historicas (DEC-EDA-023).'
        AFTER resultado_validacion;

-- ----------------------------------------------------------------------------
-- 2.4 Constraints e índices nuevos (consolidado; fk_firma_resultado diferida a 04bis)
-- ----------------------------------------------------------------------------
ALTER TABLE fal_documento_firma
    ADD CONSTRAINT chk_firma_hash_algoritmo CHECK (algoritmo_hash IS NULL OR algoritmo_hash IN ('SHA-256', 'SHA-384', 'SHA-512')),
    ADD CONSTRAINT chk_firma_perfil_unico CHECK (perfil_firma IS NULL OR perfil_firma = 'PAdES-B-LT'),
    ADD CONSTRAINT chk_firma_actor_tecnico_tipo CHECK (actor_tecnico_tipo IS NULL OR actor_tecnico_tipo = 1),
    ADD CONSTRAINT chk_firma_resultado_cadena CHECK (resultado_cadena IS NULL OR resultado_cadena BETWEEN 1 AND 3),
    ADD CONSTRAINT chk_firma_fuente_revocacion CHECK (fuente_revocacion IS NULL OR fuente_revocacion BETWEEN 1 AND 3),
    ADD CONSTRAINT chk_firma_resultado_revocacion CHECK (resultado_revocacion IS NULL OR resultado_revocacion BETWEEN 1 AND 3),
    ADD CONSTRAINT chk_firma_resultado_validacion CHECK (resultado_validacion IS NULL OR resultado_validacion BETWEEN 1 AND 3),
    ADD CONSTRAINT chk_firma_cert_vigencia CHECK (certificado_vigencia_desde IS NULL OR certificado_vigencia_hasta IS NULL OR certificado_vigencia_desde <= certificado_vigencia_hasta),
    ADD CONSTRAINT chk_firma_revision_salida CHECK (revision_salida IS NULL OR revision_entrada IS NULL OR revision_salida = revision_entrada + 1),
    ADD INDEX ix_firma_correlation (correlation_id),
    ADD INDEX ix_firma_event_id (event_id),
    ADD UNIQUE KEY uq_firma_resultado (resultado_firma_id);

-- ----------------------------------------------------------------------------
-- 2.5 CHECK integral de completitud EDA (DEC-EDA-029/034/035/036;
--     firma-eda-physical-model.md §2.4/§2.4bis)
-- ----------------------------------------------------------------------------
ALTER TABLE fal_documento_firma
    ADD CONSTRAINT chk_firma_eda_completo CHECK (
        resultado_firma_id IS NULL
        OR (
            hash_firma IS NOT NULL
            AND hash_salida IS NOT NULL
            AND revision_entrada IS NOT NULL
            AND revision_salida IS NOT NULL
            AND revision_salida = revision_entrada + 1
            AND revision_entrada >= 1
            AND algoritmo_hash IS NOT NULL
            AND perfil_firma = 'PAdES-B-LT'
            AND evidencia_id IS NOT NULL
            AND actor_tecnico_tipo = 1
            AND actor_tecnico_id IS NOT NULL
            AND actor_tecnico_app IS NOT NULL
            AND actor_tecnico_key_fingerprint IS NOT NULL
            AND correlation_id IS NOT NULL
            AND event_id IS NOT NULL
            AND received_at IS NOT NULL
            AND certificado_serial IS NOT NULL
            AND certificado_thumbprint IS NOT NULL
            AND certificado_subject IS NOT NULL
            AND certificado_issuer IS NOT NULL
            AND certificado_vigencia_desde IS NOT NULL
            AND certificado_vigencia_hasta IS NOT NULL
            AND resultado_cadena = 1
            AND fuente_revocacion IN (1, 2)
            AND resultado_revocacion = 1
            AND tsa_nombre IS NOT NULL
            AND tsa_instante IS NOT NULL
            AND resultado_validacion IN (1, 3)
            AND version_validador IS NOT NULL
        )
    );
-- fuente_revocacion IN (1,2) excluye 3=NO_DISPONIBLE: una firma que aplico dominio
-- siempre resolvio la revocacion por OCSP o CRL. resultado_validacion IN (1,3)
-- excluye 2=INVALIDO: una fila con resultado_firma_id informado ya supero la
-- validacion PKI (DEC-EDA-036); una PKI invalida nunca llega a crear esta fila.

-- Backfill: todas las columnas de esta sección quedan NULL para filas existentes;
-- ninguna toma un DEFAULT distinto de NULL (no se inventa ningún algoritmo
-- retroactivo). Filas con resultado_firma_id IS NULL quedan exentas del CHECK
-- de la sección 2.5 (firma-eda-physical-model.md §2, backfill).

-- =============================================================================
-- SECCIÓN 03 — fal_documento: columna nueva revision_artefacto (DEC-EDA-024/031)
-- =============================================================================

ALTER TABLE fal_documento
    ADD COLUMN revision_artefacto INT NOT NULL DEFAULT 0
        COMMENT 'CONTENIDO: Revision fisica del unico PDF vigente. USO: base de revisionArtefacto/revisionEntrada/revisionSalida (firma-binary-transfer.md, firma-event-contracts.md). REGLA: 0=sin artefacto preparado; +1 en cada preparacion inicial y en cada firma valida aplicada (DEC-EDA-024).'
        AFTER hash_docu;

ALTER TABLE fal_documento
    ADD CONSTRAINT chk_documento_revision_artefacto CHECK (revision_artefacto >= 0);

-- ----------------------------------------------------------------------------
-- Backfill definitivo (DEC-EDA-031): no fijar 1 indiscriminadamente ni dejar el
-- DEFAULT 0 del ADD COLUMN para documentos que ya tienen contenido fisico.
-- PRECHECK OBLIGATORIO — database/diagnostics/verify-firmas-eda-closure-001-precheck.sql
-- §6: antes de ejecutar este UPDATE, ese precheck reporta y este script se
-- DETIENE si existe alguna fila con exactamente uno de storage_key/hash_docu
-- informado (inconsistencia preexistente, ajena a este cierre); el backfill
-- nunca decide por si solo cual de los dos campos es el correcto ni sobrescribe
-- o re-renderiza el PDF existente. Si el esquema esta vacio (sin filas
-- productivas, §13), este UPDATE no tiene efecto observable.
-- ----------------------------------------------------------------------------
UPDATE fal_documento
   SET revision_artefacto = CASE
       WHEN storage_key IS NOT NULL OR hash_docu IS NOT NULL THEN 1
       ELSE 0
   END
 WHERE revision_artefacto = 0;

-- =============================================================================
-- SECCIÓN 04 — fal_documento_firma_resultado: staging temporal, swap de storage
-- recuperable tras crash y binding criptográfico del recibo
-- (DEC-EDA-024/032/033)
-- =============================================================================

CREATE TABLE fal_documento_firma_resultado (
    id                       BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    resultado_firma_id       CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: UUID de negocio de la carga. USO: clave de idempotencia del PUT resultados/{resultadoFirmaId}. REGLA: unico; agotado permanentemente tras un rechazo por PKI invalida (DEC-EDA-036).',
    solicitud_firma_id       CHAR(36)       NOT NULL               COMMENT 'CONTENIDO: Correlacion con fal_documento_firma_req.solicitud_firma_id. USO: trazabilidad y busqueda por solicitud. REGLA: no vacio.',
    id_documento             BIGINT         NOT NULL               COMMENT 'CONTENIDO: FK al documento. USO: vinculo con fal_documento. REGLA: referencia fal_documento(id).',
    id_firma_req             BIGINT         NOT NULL               COMMENT 'CONTENIDO: FK al requisito de firma. USO: vinculo con fal_documento_firma_req. REGLA: referencia fal_documento_firma_req(id).',
    seq_firma_req            SMALLINT       NOT NULL               COMMENT 'CONTENIDO: Posicion del requisito. USO: igual que en fal_documento_firma_req. REGLA: positivo.',
    revision_solicitud       INT            NOT NULL               COMMENT 'CONTENIDO: Revision de la solicitud EDA al momento de la carga. USO: idempotencia y correlacion. REGLA: >= 0.',
    revision_entrada         INT            NOT NULL               COMMENT 'CONTENIDO: revisionArtefacto vigente al momento de la carga. USO: validada contra fal_documento_firma_req.revision_artefacto_preparada (DEC-EDA-030), no contra fal_documento directamente. REGLA: >= 1.',
    revision_salida          INT            NOT NULL               COMMENT 'CONTENIDO: Revision resultante que se aplicara si el evento se consume con exito. USO: verificacion revisionSalida == revisionEntrada + 1. REGLA: = revision_entrada + 1.',
    hash_algoritmo           VARCHAR(16)    NOT NULL               COMMENT 'CONTENIDO: Algoritmo declarado en la carga. USO: obligatorio en este contrato HTTP (distinto de fal_documento_firma.algoritmo_hash, que es nullable por compatibilidad historica). REGLA: SHA-256/SHA-384/SHA-512.',
    hash_entrada             VARCHAR(128)   NOT NULL               COMMENT 'CONTENIDO: Hash de entrada declarado. USO: debe coincidir con fal_documento_firma_req.hash_entrada_preparado vigente (validado en la carga). REGLA: no vacio.',
    hash_salida              VARCHAR(128)   NOT NULL               COMMENT 'CONTENIDO: Hash del PDF resultante declarado por Firmas. USO: idempotencia (mismo id + distinto hash = 409). REGLA: no vacio.',
    content_type             VARCHAR(64)    NOT NULL               COMMENT 'CONTENIDO: MIME real verificado. USO: validacion de la carga. REGLA: application/pdf.',
    content_length           BIGINT         NOT NULL               COMMENT 'CONTENIDO: Tamanio exacto del binario recibido. USO: validacion de limite (100 MiB). REGLA: entre 1 y 104857600.',
    storage_key_candidato    VARCHAR(196)   NULL                   COMMENT 'CONTENIDO: Ubicacion fisica interna, definitiva e inmutable del candidato firmado, ya escrita por el PUT (nunca una ruta temporal que requiera rename/copy posterior). USO: base del swap atomico de fal_documento.storage_key. REGLA: inactiva (no referenciada por fal_documento.storage_key) hasta el consumo; NULL solo tras CONSUMIDO exitoso (una vez promovida) o tras EXPIRADO/RECHAZADO (purga; nunca se conserva como historial) (DEC-EDA-032).',
    storage_key_anterior     VARCHAR(196)   NULL                   COMMENT 'CONTENIDO: Copia del fal_documento.storage_key vigente inmediatamente antes del swap, tomada en la misma transaccion que lo reemplaza. USO: recuperacion tras crash y limpieza asincrona del PDF anterior. REGLA: NULL antes del consumo; NULL de nuevo tras cleanup_status=COMPLETADO (DEC-EDA-032).',
    cleanup_status           SMALLINT       NOT NULL DEFAULT 1     COMMENT 'CONTENIDO: Catalogo CleanupStatus: 1=NO_APLICA, 2=PENDIENTE, 3=COMPLETADO, 4=RETRY, 5=QUARANTINED. USO: gobierna la eliminacion asincrona, posterior al commit, del PDF anterior referenciado por storage_key_anterior. REGLA: NO_APLICA por defecto; pasa a PENDIENTE unicamente en el mismo commit del swap si storage_key_anterior difiere del candidato (DEC-EDA-032).',
    cleanup_attempts         SMALLINT       NOT NULL DEFAULT 0     COMMENT 'CONTENIDO: Intentos de limpieza realizados por el job posterior al commit. USO: control de reintentos y backoff (10 intentos maximos, mismo esquema que eventing-operations.md §5). REGLA: incrementa en cada intento fallido.',
    cleanup_next_attempt_at  DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Proximo instante habilitado para reintentar la limpieza. USO: backoff exponencial del job de limpieza. REGLA: NULL si cleanup_status no requiere reintento.',
    cleanup_last_error       VARCHAR(512)   NULL                   COMMENT 'CONTENIDO: Detalle del ultimo error de limpieza. USO: diagnostico. REGLA: NULL si ningun intento fallo.',
    cleanup_completed_at     DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Momento en que cleanup_status paso a COMPLETADO. USO: auditoria del cierre de la limpieza asincrona. REGLA: NULL hasta la limpieza exitosa.',
    referencia_firma_ext     VARCHAR(64)    NOT NULL               COMMENT 'CONTENIDO: Igual a referenciaFirmaExt de la carga. USO: debe coincidir exactamente con DocumentoFirmado.referenciaFirmaExt al consumir el evento; una divergencia es rechazo permanente (DEC-EDA-033).',
    evidencia_id             VARCHAR(128)   NOT NULL               COMMENT 'CONTENIDO: Igual a evidenciaId de la carga. USO: debe coincidir exactamente con DocumentoFirmado.evidenciaId al consumir el evento; una divergencia es rechazo permanente (DEC-EDA-033).',
    upload_actor_id          VARCHAR(64)    NOT NULL               COMMENT 'CONTENIDO: Identificador tecnico del actor autenticado que ejecuto el PUT, derivado de la X-Api-Key. USO: auditoria y binding criptografico del recibo. REGLA: nunca derivado del JSON enviado por Firmas (DEC-EDA-033).',
    upload_actor_app         VARCHAR(32)    NOT NULL               COMMENT 'CONTENIDO: Aplicacion origen del actor autenticado del PUT (firmas), derivada de la credencial. USO: auditoria del binding criptografico del recibo. REGLA: nunca derivado del JSON enviado por Firmas (DEC-EDA-033).',
    upload_key_fingerprint   VARCHAR(64)    NOT NULL               COMMENT 'CONTENIDO: Fingerprint (hash) de la X-Api-Key usada en el PUT. USO: auditoria de la credencial M2M sin exponerla. REGLA: nunca la clave completa (DEC-EDA-033).',
    estado_resultado         SMALLINT       NOT NULL DEFAULT 1     COMMENT 'CONTENIDO: Catalogo EstadoResultadoFirma: 1=RECIBIDO, 2=VALIDADO, 3=CONSUMIDO, 4=EXPIRADO, 5=RECHAZADO. USO: transiciones exactas en firma-eda-physical-model.md §4.',
    received_at              DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: Momento de la carga HTTP. USO: base del TTL. REGLA: suministrado por FaltasClock.',
    validated_at             DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Momento en que paso a VALIDADO. USO: auditoria. REGLA: NULL si no aplica.',
    consumed_at              DATETIME(6)    NULL                   COMMENT 'CONTENIDO: Momento en que el consumidor de DocumentoFirmado marco esta fila CONSUMIDO. USO: auditoria de latencia. REGLA: NULL hasta el consumo.',
    expires_at               DATETIME(6)    NOT NULL               COMMENT 'CONTENIDO: received_at + 24 horas. USO: base del job de purga. REGLA: suministrado por FaltasClock al crear la fila.',
    event_id                 CHAR(36)       NULL                   COMMENT 'CONTENIDO: eventoId de DocumentoFirmado que consumio (o rechazo, DEC-EDA-036) esta fila. USO: trazabilidad. REGLA: NULL hasta esa resolucion.',
    last_error_code          VARCHAR(64)    NULL                   COMMENT 'CONTENIDO: Codigo de rechazo (carga invalida o PKI invalida al consumir, DEC-EDA-036), si estado_resultado = RECHAZADO. USO: diagnostico. REGLA: NULL si nunca fue rechazado.',
    last_error_detail        VARCHAR(512)   NULL                   COMMENT 'CONTENIDO: Detalle del rechazo. USO: diagnostico. REGLA: NULL si nunca fue rechazado.',
    version_row              INT            NOT NULL DEFAULT 0     COMMENT 'CONTENIDO: Control de concurrencia optimista. USO: OCC. REGLA: incrementar en cada modificacion.',
    PRIMARY KEY (id),
    UNIQUE KEY uq_resultado_firma_id (resultado_firma_id),
    CONSTRAINT fk_resultado_documento FOREIGN KEY (id_documento)
        REFERENCES fal_documento (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_resultado_firma_req FOREIGN KEY (id_firma_req)
        REFERENCES fal_documento_firma_req (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_resultado_expires (estado_resultado, expires_at),
    INDEX ix_resultado_solicitud (solicitud_firma_id, revision_solicitud),
    INDEX ix_resultado_firma_req (id_firma_req, seq_firma_req),
    INDEX ix_resultado_estado (estado_resultado),
    INDEX ix_resultado_cleanup (cleanup_status, cleanup_next_attempt_at),
    CONSTRAINT chk_resultado_estado CHECK (estado_resultado BETWEEN 1 AND 5),
    CONSTRAINT chk_resultado_cleanup_status CHECK (cleanup_status BETWEEN 1 AND 5),
    CONSTRAINT chk_resultado_revisiones CHECK (revision_entrada >= 1 AND revision_salida = revision_entrada + 1),
    CONSTRAINT chk_resultado_content_length CHECK (content_length BETWEEN 1 AND 104857600),
    CONSTRAINT chk_resultado_content_type CHECK (content_type = 'application/pdf'),
    CONSTRAINT chk_resultado_hash_algoritmo CHECK (hash_algoritmo IN ('SHA-256', 'SHA-384', 'SHA-512'))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Staging temporal e idempotente del candidato firmado cargado por PUT .../resultados/{resultadoFirmaId}. USO: puente entre la carga HTTP y el consumo del evento DocumentoFirmado; nunca aplica efecto de dominio por si misma; persiste ademas el binding criptografico del recibo (DEC-EDA-033) y el estado durable del swap de storage (DEC-EDA-032). REGLAS CLAVE: unico punto de aplicacion de dominio es el consumidor de DocumentoFirmado (DEC-EDA-024); storage_key_candidato se limpia solo tras cleanup_status=COMPLETADO; TTL 24 horas; un rechazo por PKI invalida agota resultado_firma_id de forma permanente (DEC-EDA-036).';

-- ----------------------------------------------------------------------------
-- 04bis — fal_documento_firma: FK diferida hacia fal_documento_firma_resultado
--         (DEC-EDA-034; creada aquí porque la tabla referenciada recién existe)
-- ----------------------------------------------------------------------------
ALTER TABLE fal_documento_firma
    ADD CONSTRAINT fk_firma_resultado FOREIGN KEY (resultado_firma_id)
        REFERENCES fal_documento_firma_resultado (resultado_firma_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT;

-- =============================================================================
-- SECCIÓN 05 — Tabla nueva: bus_evento (bus central append-only, único)
-- (DEC-EDA-011/019/022/027; DECISION_DDL-BUS-01)
--
-- Modelo mínimo aprobado: bus_evento (§5.2) es la única tabla central del
-- bus. No hay fan-out, no hay entregas centrales, no hay suscripciones, no
-- hay rutinas SQL SECURITY DEFINER ni vistas diagnósticas. El productor
-- escribe primero en su Outbox local (§5.1); un relay de la aplicación
-- inserta el evento en bus_evento; cada consumidor lee por bus_evento_id
-- ascendente con su propio checkpoint local (§5.3) y copia lo que le
-- interesa a su Inbox local (§5.4), cuyos errores de procesamiento quedan en
-- su tabla local separada (§5.5). Un evento puede tener cero consumidores;
-- eso es válido y no requiere ninguna escritura adicional. bus_evento nunca
-- se actualiza tras el INSERT.
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 5.1 fal_outbox
--     Outbox local de Faltas, reducida a lo necesario (DECISION_DDL-BUS-02).
--     Se escribe junto con el cambio de dominio, en la misma transacción
--     local. El relay reutiliza el mismo evento_id en cada reintento
--     (idempotencia de publicación por UNIQUE(evento_id) en bus_evento).
--     status: 0=PENDIENTE, 1=PUBLICADO, 2=REINTENTO, 3=ERROR_DEFINITIVO.
--     si_error indica que el último intento de publicación falló; el detalle
--     técnico completo (stack trace) queda en logs correlacionados por
--     evento_id, nunca en esta tabla.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_outbox (
    outbox_id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico local. USO: PK. REGLA: generado por DB.',
    evento_id          BINARY(16)       NOT NULL                COMMENT 'CONTENIDO: UUID logico del evento (identidad estable de negocio), en binario. USO: idempotencia de publicacion; se reutiliza en cada reintento. REGLA: unico; generado por Faltas al crear la fila.',
    evento_codigo      VARCHAR(16)      NOT NULL                COMMENT 'CONTENIDO: Codigo compacto del tipo de evento (ver catalogo de codigos en firma-eda-physical-model.md §3). USO: enrutamiento; se modela como enum en Java. REGLA: coincide con el catalogo vigente de codigos de evento.',
    schema_version     SMALLINT UNSIGNED NOT NULL DEFAULT 1     COMMENT 'CONTENIDO: Version de esquema del payload. USO: compatibilidad de contrato. REGLA: entero >= 1.',
    agregado_codigo    VARCHAR(12)      NULL                    COMMENT 'CONTENIDO: Codigo compacto del tipo de agregado emisor, cuando aplica. USO: indexacion secundaria opcional. REGLA: NULL si el agregado no tiene una identidad numerica unica indexable; la identidad completa vive siempre en payload.',
    agregado_id        BIGINT           NULL                    COMMENT 'CONTENIDO: Identificador numerico del agregado emisor, cuando es representable como BIGINT. USO: indexacion secundaria opcional. REGLA: NULL si la clave del agregado no es un unico valor numerico; nunca la unica fuente de identidad (esa vive en payload).',
    agregado_version   BIGINT UNSIGNED  NULL                    COMMENT 'CONTENIDO: Version/secuencia del agregado al momento de publicar. USO: orden logico por agregado. REGLA: NULL si no aplica; monotonicamente creciente por agregado cuando se informa.',
    correlation_id     BINARY(16)       NULL                    COMMENT 'CONTENIDO: UUID de correlacion end-to-end, en binario. USO: trazabilidad distribuida. REGLA: NULL si no aplica.',
    causation_id       BINARY(16)       NULL                    COMMENT 'CONTENIDO: UUID del evento/comando causante inmediato, en binario. USO: trazabilidad causal. REGLA: NULL si no hay causante directo.',
    payload            JSON             NOT NULL                COMMENT 'CONTENIDO: Cuerpo del evento segun el contrato de 40-api/firma-event-contracts.md. USO: dato transportado. REGLA: JSON valido; nunca se actualiza tras el insert.',
    status             TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'CONTENIDO: Estado del relay Outbox->bus_evento. VALORES: 0=PENDIENTE, 1=PUBLICADO, 2=REINTENTO, 3=ERROR_DEFINITIVO.',
    si_error           BOOLEAN          NOT NULL DEFAULT FALSE  COMMENT 'CONTENIDO: TRUE si el ultimo intento de publicacion fallo. USO: senal simple de error activo, sin cuarentena central. REGLA: el detalle tecnico completo queda en logs correlacionados por evento_id.',
    attempts           SMALLINT UNSIGNED NOT NULL DEFAULT 0     COMMENT 'CONTENIDO: Cantidad de intentos de publicacion realizados. USO: control de reintentos. REGLA: incrementa en cada intento fallido.',
    next_attempt_at    DATETIME(6)      NULL                    COMMENT 'CONTENIDO: Proximo instante habilitado para reintentar. USO: backoff simple de aplicacion. REGLA: NULL si status no requiere reintento.',
    ocurrido_en        DATETIME(6)      NOT NULL                COMMENT 'CONTENIDO: Momento de ocurrencia del hecho de dominio. USO: linea de tiempo de negocio. REGLA: suministrado por FaltasClock.',
    creado_en          DATETIME(6)      NOT NULL                COMMENT 'CONTENIDO: Momento de insercion en el Outbox. USO: auditoria tecnica.',
    publicado_en       DATETIME(6)      NULL                    COMMENT 'CONTENIDO: Momento de publicacion exitosa en bus_evento. USO: auditoria de latencia del relay. REGLA: NULL mientras status <> PUBLICADO.',
    PRIMARY KEY (outbox_id),
    UNIQUE KEY uq_fal_outbox_evento_id (evento_id),
    INDEX ix_fal_outbox_status (status, next_attempt_at),
    INDEX ix_fal_outbox_agregado (agregado_codigo, agregado_id, agregado_version),
    CONSTRAINT chk_fal_outbox_status CHECK (status BETWEEN 0 AND 3),
    CONSTRAINT chk_fal_outbox_schema_version CHECK (schema_version >= 1),
    CONSTRAINT chk_fal_outbox_attempts CHECK (attempts >= 0),
    CONSTRAINT chk_fal_outbox_si_error CHECK (si_error IN (FALSE, TRUE))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Outbox local de Faltas para el bus de eventos municipal. USO: publicacion atomica con el commit de dominio (patron Outbox); el relay reutiliza el mismo evento_id en cada reintento. REGLAS CLAVE: payload inmutable; sin cuarentena central; el detalle tecnico de error vive en logs, no en la fila (DEC-EDA-011/019/022).';

-- ----------------------------------------------------------------------------
-- 5.2 bus_evento
--     Bus central de eventos municipal. Tabla única, append-only,
--     inmutable tras el INSERT (DECISION_DDL-BUS-01). Sin fan-out, sin
--     entregas centrales, sin suscripciones, sin fingerprint central, sin
--     rutinas SQL SECURITY DEFINER: la escritura es un INSERT directo del
--     relay de cada aplicación, autorizado por grants mínimos
--     (database/ddl/grants-firmas-eda-bus.sql). La idempotencia de
--     publicación se basa únicamente en UNIQUE(evento_id): el relay
--     reutiliza el mismo evento_id en cada reintento.
-- ----------------------------------------------------------------------------
CREATE TABLE bus_evento (
    bus_evento_id      BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico secuencial. USO: PK; base de lectura por checkpoint (bus_evento_id > :ultimo_evento_id). REGLA: generado por DB; nunca se reutiliza.',
    evento_id          BINARY(16)       NOT NULL                COMMENT 'CONTENIDO: UUID logico del evento, igual al evento_id de origen en el Outbox del productor. USO: identidad estable de negocio; base de la idempotencia de publicacion. REGLA: unico; el relay reutiliza el mismo valor en cada reintento (UNIQUE(evento_id)).',
    productor_codigo   VARCHAR(8)       NOT NULL                COMMENT 'CONTENIDO: Codigo compacto de la aplicacion productora (FAL=Faltas, FIR=Firmas; hasta 8 caracteres para aplicaciones municipales futuras). USO: auditoria de origen y lectura selectiva. REGLA: sin CHECK IN cerrado; la validacion de valores vigentes vive en el enum/constante de cada dominio, no en el esquema (firma-eda-physical-model.md §3).',
    evento_codigo      VARCHAR(16)      NOT NULL                COMMENT 'CONTENIDO: Codigo compacto del tipo de evento (ver catalogo de los 12 contratos en firma-eda-physical-model.md §3). USO: enrutamiento y filtro de lectura por cada consumidor. REGLA: coincide con el catalogo vigente; se modela como enum en cada dominio.',
    schema_version     SMALLINT UNSIGNED NOT NULL DEFAULT 1     COMMENT 'CONTENIDO: Version de esquema del payload. USO: compatibilidad de contrato. REGLA: entero >= 1.',
    agregado_codigo    VARCHAR(12)      NULL                    COMMENT 'CONTENIDO: Codigo compacto del tipo de agregado emisor, cuando aplica. USO: indexacion secundaria opcional para diagnostico por agregado. REGLA: NULL si la identidad del agregado no es un unico valor numerico indexable; la identidad completa siempre vive en payload (coherente con 40-api/firma-event-contracts.md).',
    agregado_id        BIGINT           NULL                    COMMENT 'CONTENIDO: Identificador numerico del agregado emisor, cuando es representable como BIGINT. USO: indexacion secundaria opcional. REGLA: NULL si la clave del agregado no es un unico valor numerico; nunca la unica fuente de identidad.',
    agregado_version   BIGINT UNSIGNED  NULL                    COMMENT 'CONTENIDO: Version/secuencia del agregado al momento de publicar, cuando el contrato la informa. USO: orden logico por agregado (no orden fisico de la tabla). REGLA: NULL si no aplica; monotonicamente creciente por agregado cuando se informa.',
    correlation_id     BINARY(16)       NULL                    COMMENT 'CONTENIDO: UUID de correlacion end-to-end, en binario. USO: trazabilidad distribuida. REGLA: NULL si no aplica; heredado del Outbox de origen cuando se informa.',
    causation_id       BINARY(16)       NULL                    COMMENT 'CONTENIDO: UUID del evento/comando causante inmediato, en binario. USO: trazabilidad causal. REGLA: NULL si no aplica.',
    payload            JSON             NOT NULL                COMMENT 'CONTENIDO: Cuerpo del evento segun el contrato de 40-api/firma-event-contracts.md. USO: dato transportado a todos los consumidores. REGLA: JSON valido; nunca se actualiza tras el insert (bus_evento es inmutable).',
    ocurrido_en        DATETIME(6)      NOT NULL                COMMENT 'CONTENIDO: Momento de ocurrencia del hecho de dominio en el productor. USO: linea de tiempo de negocio. REGLA: heredado del Outbox de origen.',
    publicado_en       DATETIME(6)      NOT NULL                COMMENT 'CONTENIDO: Momento de insercion en el bus central. USO: auditoria de latencia del relay. REGLA: suministrado por el relay al insertar.',
    PRIMARY KEY (bus_evento_id),
    UNIQUE KEY uq_bus_evento_evento_id (evento_id),
    INDEX ix_bus_evento_codigo (evento_codigo, bus_evento_id),
    INDEX ix_bus_evento_productor (productor_codigo, bus_evento_id),
    INDEX ix_bus_evento_agregado (agregado_codigo, agregado_id, bus_evento_id),
    CONSTRAINT chk_bus_evento_schema_version CHECK (schema_version >= 1)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Bus central de eventos municipal, tabla unica append-only e inmutable tras el insert. USO: distribucion de eventos EDA entre aplicaciones municipales (Faltas, Firmas y futuras); cada consumidor lee por bus_evento_id ascendente con su propio checkpoint local. REGLAS CLAVE: sin fan-out, sin entregas centrales, sin suscripciones, sin rutinas SQL SECURITY DEFINER; idempotencia de publicacion por UNIQUE(evento_id); un evento sin consumidores es valido y no requiere ninguna escritura adicional; grants minimos (INSERT publisher, SELECT consumer, SELECT+DELETE mantenimiento) en database/ddl/grants-firmas-eda-bus.sql (DEC-EDA-011/019/022/027).';

-- ----------------------------------------------------------------------------
-- 5.3 fal_bus_checkpoint
--     Checkpoint local de lectura del bus para el consumidor Faltas.
--     Tabla singleton (checkpoint_id = 1 fijo por CHECK). ultimo_evento_id
--     es el ultimo bus_evento_id que Faltas terminó de examinar/copiar a su
--     Inbox local. El avance del checkpoint y el INSERT en fal_inbox deben
--     quedar coordinados en la misma transaccion local para que nunca se
--     avance el checkpoint antes de copiar el evento relevante.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_bus_checkpoint (
    checkpoint_id      TINYINT UNSIGNED NOT NULL              COMMENT 'CONTENIDO: Identificador fijo de fila singleton. USO: PK. REGLA: siempre 1 (chk_fal_bus_checkpoint_singleton); nunca hay una segunda fila.',
    ultimo_evento_id   BIGINT UNSIGNED  NOT NULL DEFAULT 0     COMMENT 'CONTENIDO: Ultimo bus_evento_id examinado/copiado por el lector local de Faltas. USO: base de la consulta bus_evento WHERE bus_evento_id > ultimo_evento_id ORDER BY bus_evento_id. REGLA: nunca disminuye; se actualiza en la misma transaccion local que el INSERT en fal_inbox de los eventos relevantes del lote.',
    actualizado_en     DATETIME(6)      NOT NULL              COMMENT 'CONTENIDO: Momento de la ultima actualizacion del checkpoint. USO: auditoria tecnica y diagnostico de lectores detenidos. REGLA: suministrado por FaltasClock.',
    PRIMARY KEY (checkpoint_id),
    CONSTRAINT chk_fal_bus_checkpoint_singleton CHECK (checkpoint_id = 1)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Checkpoint local, singleton, del lector del bus de eventos municipal para Faltas. USO: recordar hasta que bus_evento_id ya se examino, sin usar MAX(bus_evento_id) de fal_inbox (la Inbox solo contiene eventos relevantes, no todos los examinados). REGLAS CLAVE: fila unica (checkpoint_id=1); avanza junto con el INSERT en fal_inbox del mismo lote, en la misma transaccion local (DEC-EDA-011/019).';

-- ----------------------------------------------------------------------------
-- 5.3.1 Seed de fal_bus_checkpoint — fila singleton inicial en 0
-- ----------------------------------------------------------------------------
INSERT INTO fal_bus_checkpoint (checkpoint_id, ultimo_evento_id, actualizado_en)
VALUES (1, 0, UTC_TIMESTAMP(6));

-- ----------------------------------------------------------------------------
-- 5.4 fal_inbox
--     Inbox local del consumidor Faltas. Separa la lectura del bus
--     (fal_bus_checkpoint) del procesamiento funcional. PK = bus_evento_id
--     (cursor tecnico, no un id propio): la Inbox solo contiene eventos
--     relevantes para Faltas, copiados desde bus_evento por el lector
--     local (transaccion de ingestion, independiente del procesamiento;
--     eventing-operations.md §3bis.1/§3bis.2). UNIQUE(evento_id) es una
--     defensa adicional de idempotencia por identidad logica estable
--     (nunca UNIQUE(consumer, event_id): no hay columna consumer aqui).
--     status: 0=PENDIENTE, 1=PROCESANDO, 2=PROCESADO, 3=REINTENTO,
--     4=ERROR_DEFINITIVO, 5=IGNORADO. Un evento con error tecnico no
--     detiene el procesamiento de otras filas PENDIENTE/REINTENTO; un
--     rechazo funcional de dominio cierra en PROCESADO, si_error=FALSE,
--     sin fila en fal_inbox_error (DEC-EDA-019/036).
-- ----------------------------------------------------------------------------
CREATE TABLE fal_inbox (
    bus_evento_id      BIGINT UNSIGNED  NOT NULL              COMMENT 'CONTENIDO: bus_evento_id del evento copiado desde el bus central. USO: PK operativa principal; idempotencia de copia local. REGLA: coincide con bus_evento.bus_evento_id del evento origen; nunca se genera un id tecnico propio.',
    evento_id          BINARY(16)       NOT NULL              COMMENT 'CONTENIDO: UUID logico del evento, igual al evento_id de bus_evento/Outbox de origen. USO: identidad estable de negocio, independiente del cursor tecnico bus_evento_id. REGLA: unico (uq_fal_inbox_evento_id); nunca se usa UNIQUE(consumer, event_id) porque esta tabla no tiene columna consumer (ambito unico Faltas).',
    productor_codigo   VARCHAR(8)       NOT NULL              COMMENT 'CONTENIDO: Codigo compacto de la aplicacion productora del evento. USO: auditoria de origen. REGLA: copiado de bus_evento.productor_codigo al momento de la ingestion.',
    evento_codigo      VARCHAR(16)      NOT NULL              COMMENT 'CONTENIDO: Codigo compacto del tipo de evento consumido. USO: enrutamiento al handler de dominio local. REGLA: coincide con bus_evento.evento_codigo del evento origen.',
    schema_version     SMALLINT UNSIGNED NOT NULL             COMMENT 'CONTENIDO: Version de esquema del payload copiado. USO: compatibilidad de contrato en el procesamiento local. REGLA: copiado de bus_evento.schema_version al momento de la ingestion.',
    agregado_codigo    VARCHAR(12)      NULL                  COMMENT 'CONTENIDO: Codigo compacto del tipo de agregado local afectado, cuando aplica. USO: diagnostico y trazabilidad del efecto. REGLA: NULL si no aplica; la identidad completa vive en payload.',
    agregado_id        BIGINT           NULL                  COMMENT 'CONTENIDO: Identificador numerico del agregado local afectado, cuando es representable como BIGINT. USO: diagnostico y trazabilidad del efecto. REGLA: NULL si no aplica.',
    agregado_version   BIGINT UNSIGNED  NULL                  COMMENT 'CONTENIDO: Version/secuencia del agregado local tras aplicar el efecto, cuando se informa. USO: orden logico local. REGLA: NULL si no aplica.',
    correlation_id     BINARY(16)       NULL                  COMMENT 'CONTENIDO: UUID de correlacion end-to-end, copiado del evento origen. USO: trazabilidad distribuida en el procesamiento local. REGLA: NULL si no aplica.',
    causation_id       BINARY(16)       NULL                  COMMENT 'CONTENIDO: UUID del evento/comando causante inmediato, copiado del evento origen. USO: trazabilidad causal. REGLA: NULL si no aplica.',
    payload            JSON             NOT NULL              COMMENT 'CONTENIDO: Copia del payload del evento, tal como llego en bus_evento. USO: dato de entrada para el procesamiento local; evita relecturas del bus. REGLA: JSON valido; nunca se actualiza tras el insert.',
    status             TINYINT UNSIGNED NOT NULL DEFAULT 0    COMMENT 'CONTENIDO: Progreso del procesamiento local. VALORES: 0=PENDIENTE, 1=PROCESANDO, 2=PROCESADO, 3=REINTENTO, 4=ERROR_DEFINITIVO, 5=IGNORADO. Un rechazo funcional de dominio cierra en PROCESADO (si_error=FALSE), nunca en REINTENTO/ERROR_DEFINITIVO.',
    si_error           BOOLEAN          NOT NULL DEFAULT FALSE COMMENT 'CONTENIDO: TRUE si el ultimo intento de procesamiento fallo por un error tecnico. USO: senal simple de error activo, sin texto de error en esta fila. REGLA: un rechazo funcional (ej. PKI invalida) deja si_error=FALSE y status=PROCESADO; el detalle de cada fallo tecnico se inserta en fal_inbox_error.',
    attempts           SMALLINT UNSIGNED NOT NULL DEFAULT 0   COMMENT 'CONTENIDO: Cantidad de intentos de procesamiento local realizados. USO: diagnostico de reintentos. REGLA: incrementa en cada intento fallido; corresponde a attempt_number de la ultima fila en fal_inbox_error.',
    next_attempt_at    DATETIME(6)      NULL                  COMMENT 'CONTENIDO: Proximo instante habilitado para reintentar el procesamiento. USO: backoff simple de aplicacion. REGLA: NULL si status no requiere reintento.',
    ocurrido_en        DATETIME(6)      NOT NULL              COMMENT 'CONTENIDO: Momento de ocurrencia del hecho de dominio en el productor, copiado del evento origen. USO: linea de tiempo de negocio en el procesamiento local. REGLA: copiado de bus_evento.ocurrido_en al momento de la ingestion.',
    recibido_en        DATETIME(6)      NOT NULL              COMMENT 'CONTENIDO: Momento en que el lector local copio el evento a la Inbox. USO: auditoria de latencia. REGLA: suministrado por FaltasClock.',
    procesado_en       DATETIME(6)      NULL                  COMMENT 'CONTENIDO: Momento en que el worker local termino de aplicar el efecto con exito. USO: auditoria de latencia de procesamiento. REGLA: NULL mientras status <> PROCESADO.',
    PRIMARY KEY (bus_evento_id),
    UNIQUE KEY uq_fal_inbox_evento_id (evento_id),
    INDEX ix_fal_inbox_status (status, next_attempt_at),
    INDEX ix_fal_inbox_evento_codigo (evento_codigo, bus_evento_id),
    INDEX ix_fal_inbox_agregado (agregado_codigo, agregado_id, agregado_version),
    CONSTRAINT chk_fal_inbox_status CHECK (status BETWEEN 0 AND 5),
    CONSTRAINT chk_fal_inbox_attempts CHECK (attempts >= 0),
    CONSTRAINT chk_fal_inbox_si_error CHECK (si_error IN (FALSE, TRUE))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Inbox local del consumidor Faltas. USO: separa la lectura del bus (fal_bus_checkpoint) del procesamiento funcional; garantiza que cada evento relevante se procesa una sola vez. REGLAS CLAVE: PK = bus_evento_id (cursor tecnico, no un id propio) mas UNIQUE(evento_id) (identidad logica estable, nunca UNIQUE(consumer, event_id)); envelope completo copiado en la ingestion (evento_id/productor_codigo/schema_version/correlation_id/causation_id/ocurrido_en); un error tecnico en una fila no detiene el procesamiento de otras filas PENDIENTE/REINTENTO; un rechazo funcional cierra en PROCESADO sin fila en fal_inbox_error; detalle de fallos tecnicos en fal_inbox_error, nunca texto largo en esta tabla (DEC-EDA-019/022). fir_outbox/fir_inbox/fir_bus_checkpoint/fir_inbox_error tienen su propio paquete DDL ejecutable en database/ddl/firmas/ (bounded context Firmas).';

-- ----------------------------------------------------------------------------
-- 5.5 fal_inbox_error
--     Tabla local de errores de procesamiento de fal_inbox. No hay tabla
--     central de errores: cada consumidor guarda su propio historial.
--     Se inserta una fila solo cuando ocurre un error; el stack trace
--     completo queda en logs correlacionados por bus_evento_id, nunca aqui.
-- ----------------------------------------------------------------------------
CREATE TABLE fal_inbox_error (
    inbox_error_id     BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'CONTENIDO: Identificador tecnico. USO: PK. REGLA: generado por DB.',
    bus_evento_id       BIGINT UNSIGNED  NOT NULL                COMMENT 'CONTENIDO: FK a la fila de fal_inbox sobre la que ocurrio el error. USO: vinculo error-evento. REGLA: referencia fal_inbox(bus_evento_id).',
    attempt_number      SMALLINT UNSIGNED NOT NULL               COMMENT 'CONTENIDO: Numero de intento en el que ocurrio este error. USO: correlacion con fal_inbox.attempts. REGLA: unico junto a bus_evento_id (uq_fal_inbox_error_attempt); coincide con el valor de attempts en fal_inbox al momento del fallo.',
    error_code          SMALLINT UNSIGNED NOT NULL               COMMENT 'CONTENIDO: Codigo numerico de error, modelado como enum local de Faltas. USO: diagnostico agregable sin texto libre. REGLA: no vacio.',
    error_detail        VARCHAR(255)     NULL                    COMMENT 'CONTENIDO: Resumen breve del error, maximo 255 caracteres. USO: diagnostico rapido. REGLA: NULL admitido; nunca un stack trace (el stack trace completo vive en logs correlacionados por bus_evento_id).',
    ocurrido_en         DATETIME(6)      NOT NULL                COMMENT 'CONTENIDO: Momento del error. USO: auditoria temporal. REGLA: suministrado por FaltasClock.',
    PRIMARY KEY (inbox_error_id),
    UNIQUE KEY uq_fal_inbox_error_attempt (bus_evento_id, attempt_number),
    INDEX ix_fal_inbox_error_evento (bus_evento_id, ocurrido_en),
    CONSTRAINT fk_fal_inbox_error_inbox FOREIGN KEY (bus_evento_id)
        REFERENCES fal_inbox (bus_evento_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci
  COMMENT = 'QUE ES: Historial local de errores de procesamiento de fal_inbox (tabla local, no central). USO: diagnostico de fallos de procesamiento sin guardar texto largo en fal_inbox. REGLAS CLAVE: una fila por intento fallido (UNIQUE(bus_evento_id, attempt_number)); error_detail acotado a 255 caracteres, nunca un stack trace; FK RESTRICT a fal_inbox (DEC-EDA-019/022).';

-- =============================================================================
-- FIN DEL SCRIPT — SPEC-FIRMAS-EDA-CLOSURE-001
-- =============================================================================
-- Tablas alteradas: 3 (fal_documento_firma_req, fal_documento_firma, fal_documento)
-- Tablas creadas: 6 (fal_documento_firma_resultado, bus_evento, fal_outbox,
--                    fal_bus_checkpoint, fal_inbox, fal_inbox_error)
-- Rutinas creadas: 0 (sin rutinas SQL SECURITY DEFINER; el relay/lector es
--                     codigo de aplicacion, no un procedimiento almacenado)
-- Vistas creadas: 0 (sin vistas diagnosticas centrales)
-- Filas de seed insertadas: 1 (fal_bus_checkpoint, fila singleton checkpoint_id=1)
-- Rollback: database/ddl/alter-firmas-eda-closure-001-rollback.sql
-- Orden operativo completo: database/README.md.
-- Precheck de principals de aplicación (bus_publisher_faltas/firmas,
-- bus_consumer_faltas/firmas, bus_maintenance; sin bus_definer):
-- database/diagnostics/verify-firmas-eda-bus-principals-precheck.sql
-- Grants de aplicación, mínimos y directos sobre bus_evento (aparte, tras
-- este script y su postcheck estructural, y tras el precheck de principals):
-- database/ddl/grants-firmas-eda-bus.sql
-- =============================================================================
