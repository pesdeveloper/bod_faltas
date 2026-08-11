-- verify-firmas-eda-closure-001-precheck.sql
-- SOLO LECTURA — No altera ningún objeto. Solo SELECT sobre information_schema
-- y sobre las tablas base (conteos/consistencia, sin escritura).
--
-- Propósito: verificar precondiciones ANTES de ejecutar
--            alter-firmas-eda-closure-001-forward.sql.
-- Motor    : MariaDB 12.3.2, sb_faltas_db
-- Trabajo  : SPEC-FIRMAS-EDA-CLOSURE-001
-- Uso      : ejecutar manualmente desde HeidiSQL ANTES de ejecutar el forward.
-- ============================================================================
--
-- Advertencia: si cualquier verificación de esta sección resulta en
-- DIVERGENCIA/PRESENTE indebido o BLOQUEADO, NO ejecutar el forward hasta
-- resolverlo.
--
-- ============================================================================

-- ============================================================================
-- SECCIÓN 0 — Entorno de conexión, motor y compatibilidad de tipos/collation
-- ============================================================================

-- 0a. Base de datos conectada
SELECT
    DATABASE() AS base_actual,
    CASE WHEN DATABASE() = 'sb_faltas_db' THEN 'OK' ELSE 'BLOQUEADO_BASE_INCORRECTA' END AS resultado;

-- 0b. Versión de MariaDB >= 11.7 — mínimo final de este paquete, no el
--     mínimo genérico de JSON_NORMALIZE en aislamiento (10.7+): cubre además
--     utf8mb4_uca1400_ai_ci. Mismo mínimo exigido en
--     verify-firmas-eda-bus-principals-precheck.sql y en
--     firma-eda-physical-model.md/el gate. Destino real de despliegue:
--     MariaDB 12.3.2, estrictamente por encima de este mínimo.
SELECT
    VERSION() AS version_motor,
    CASE
        WHEN CAST(SUBSTRING_INDEX(VERSION(), '.', 1) AS UNSIGNED) > 11 THEN 'OK'
        WHEN CAST(SUBSTRING_INDEX(VERSION(), '.', 1) AS UNSIGNED) = 11
             AND CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(VERSION(), '.', 2), '.', -1) AS UNSIGNED) >= 7 THEN 'OK'
        ELSE 'BLOQUEADO_VERSION_INSUFICIENTE_MINIMO_11_7'
    END AS resultado;

-- 0c. Motor InnoDB y charset/collation por defecto de las 3 tablas base a
--     alterar (deben coincidir con utf8mb4/utf8mb4_uca1400_ai_ci, igual que
--     las tablas nuevas de este forward, para que las columnas agregadas sin
--     COLLATE explícito hereden un valor compatible con las FK nuevas)
SELECT
    table_name, engine, table_collation,
    CASE WHEN engine = 'InnoDB' AND table_collation = 'utf8mb4_uca1400_ai_ci' THEN 'OK'
         ELSE 'DIVERGENCIA_MOTOR_O_COLLATION'
    END AS resultado
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('fal_documento_firma_req', 'fal_documento_firma', 'fal_documento');

-- 0d. Colación de columnas string existentes usadas como ancla de las nuevas
--     FK: fal_documento_firma.resultado_firma_id (nueva, CHAR(36) sin COLLATE
--     explícito, hereda el de la tabla) debe ser compatible con
--     fal_documento_firma_resultado.resultado_firma_id (nueva tabla,
--     utf8mb4_uca1400_ai_ci) para que fk_firma_resultado (sección 04bis) no
--     falle por colación incompatible.
SELECT
    table_name, table_collation,
    CASE WHEN table_collation = 'utf8mb4_uca1400_ai_ci' THEN 'OK' ELSE 'DIVERGENCIA_COLLATION_FK' END AS resultado
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'fal_documento_firma';

-- 0e. Tipo y signedness de las columnas PK/FK ancla de las 3 tablas base
--     (deben ser BIGINT con signo, igual que las columnas FK que declaran
--     las 6 tablas nuevas hacia estas 3)
SELECT
    table_name, column_name, data_type, column_type,
    CASE WHEN data_type = 'bigint' AND column_type NOT LIKE '%unsigned%' THEN 'OK'
         ELSE 'DIVERGENCIA_TIPO_O_SIGNEDNESS'
    END AS resultado
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND ((table_name = 'fal_documento' AND column_name = 'id')
    OR (table_name = 'fal_documento_firma_req' AND column_name IN ('id', 'id_documento'))
    OR (table_name = 'fal_documento_firma' AND column_name IN ('id', 'id_documento')));

-- 0f. Columnas ancla de los tres ALTER TABLE, usadas en la primera cláusula
--     AFTER de cada bloque (las demás cláusulas AFTER de este forward
--     encadenan columnas agregadas en el mismo ALTER TABLE, no requieren
--     verificación de preexistencia)
SELECT
    esp.tabla, esp.columna_ancla,
    CASE WHEN c.column_name IS NOT NULL THEN 'OK' ELSE 'FALTA_COLUMNA_ANCLA' END AS resultado
FROM (
    SELECT 'fal_documento_firma_req' AS tabla, 'id_user_alta' AS columna_ancla
    UNION ALL SELECT 'fal_documento_firma', 'hash_firma'
    UNION ALL SELECT 'fal_documento', 'hash_docu'
) esp
LEFT JOIN information_schema.columns c
    ON c.table_schema = DATABASE() AND c.table_name = esp.tabla AND c.column_name = esp.columna_ancla;

-- ============================================================================
-- SECCIÓN 1 — Las 3 tablas base ya existen (baseline create-bod-faltas-domain.sql)
-- ============================================================================

SELECT
    t_esp.tabla_esperada                AS tabla_base_requerida,
    CASE WHEN t.table_name IS NOT NULL THEN 'OK_PRESENTE' ELSE 'FALTA_BASELINE' END AS resultado
FROM (
    SELECT 'fal_documento_firma_req' AS tabla_esperada
    UNION ALL SELECT 'fal_documento_firma'
    UNION ALL SELECT 'fal_documento'
) t_esp
LEFT JOIN information_schema.tables t
    ON t.table_schema = DATABASE()
   AND t.table_name = t_esp.tabla_esperada
   AND t.table_type = 'BASE TABLE';

-- ============================================================================
-- SECCIÓN 2 — Ninguna de las columnas nuevas existe todavía (idempotencia)
--              fal_documento_firma_req: 14 columnas (10 DEC-EDA-020 + 4 DEC-EDA-030)
--              fal_documento_firma: 27 columnas
--              fal_documento: 1 columna
-- ============================================================================

SELECT
    table_name, column_name,
    'DIVERGENCIA_YA_EXISTE' AS resultado
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND (
       (table_name = 'fal_documento_firma_req' AND column_name IN (
           'version_row','solicitud_firma_id','revision_solicitud','fh_solicitud',
           'fh_aceptacion','fh_inicio_firma','vence_en','fh_terminal',
           'motivo_terminal_firma_req','detalle_terminal_firma_req',
           'revision_artefacto_preparada','hash_entrada_preparado',
           'content_length_preparado','fh_preparacion'))
    OR (table_name = 'fal_documento_firma' AND column_name IN (
           'hash_salida','revision_entrada','revision_salida',
           'actor_tecnico_tipo','actor_tecnico_id','actor_tecnico_app',
           'actor_tecnico_key_fingerprint','correlation_id','event_id','received_at',
           'perfil_firma','algoritmo_hash','resultado_firma_id','evidencia_id',
           'certificado_serial','certificado_thumbprint','certificado_subject',
           'certificado_issuer','certificado_vigencia_desde','certificado_vigencia_hasta',
           'resultado_cadena','fuente_revocacion','resultado_revocacion',
           'tsa_nombre','tsa_instante','resultado_validacion','version_validador'))
    OR (table_name = 'fal_documento' AND column_name = 'revision_artefacto')
  );
-- Esperado: 0 filas. Si hay filas, el forward ya fue aplicado parcial o
-- totalmente; no volver a ejecutarlo (no es idempotente; usar postcheck/parity
-- en su lugar, o firma-eda-physical-model.md §14 para recuperación parcial).

-- ============================================================================
-- SECCIÓN 3 — Ninguna de las 7 tablas nuevas existe todavía (idempotencia).
--              Modelo mínimo aprobado: bus_evento (único objeto central) +
--              Outbox/checkpoint/Inbox/Inbox error locales de Faltas. Sin
--              rutinas SQL SECURITY DEFINER ni vistas diagnósticas centrales
--              que precondicionar (no existen en este diseño).
-- ============================================================================

SELECT
    table_name AS tabla_nueva_ya_presente,
    'DIVERGENCIA_YA_EXISTE' AS resultado
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE'
  AND table_name IN (
      'fal_documento_firma_resultado', 'fal_outbox', 'bus_evento',
      'fal_bus_checkpoint', 'fal_inbox', 'fal_inbox_error');
-- Esperado: 0 filas.

-- ============================================================================
-- SECCIÓN 4 — Catálogo estado_firma_req todavía en su forma original (5 valores)
-- ============================================================================

SELECT
    column_comment,
    CASE
        WHEN column_comment LIKE '%CANCELACION_SOLICITADA%' THEN 'DIVERGENCIA_YA_EXTENDIDO'
        ELSE 'OK_NO_EXTENDIDO'
    END AS resultado
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'fal_documento_firma_req'
  AND column_name = 'estado_firma_req';

-- ============================================================================
-- SECCIÓN 5 — Conteo de filas existentes en las tablas a alterar (informativo,
--              para dimensionar el impacto del backfill; sin backup implícito)
-- ============================================================================

SELECT 'fal_documento_firma_req' AS tabla, COUNT(*) AS filas_existentes FROM fal_documento_firma_req
UNION ALL
SELECT 'fal_documento_firma' AS tabla, COUNT(*) AS filas_existentes FROM fal_documento_firma
UNION ALL
SELECT 'fal_documento' AS tabla, COUNT(*) AS filas_existentes FROM fal_documento;

-- ============================================================================
-- SECCIÓN 6 — Consistencia storage_key/hash_docu en fal_documento, base del
--              backfill condicional de revision_artefacto (DEC-EDA-031). El
--              forward se DETIENE si esta sección reporta alguna fila
--              INCONSISTENTE: el backfill nunca decide por sí solo cuál de
--              los dos campos es el correcto.
-- ============================================================================

SELECT
    id, storage_key, hash_docu,
    CASE
        WHEN (storage_key IS NOT NULL) = (hash_docu IS NOT NULL) THEN 'OK_CONSISTENTE'
        ELSE 'BLOQUEADO_INCONSISTENTE_STORAGE_KEY_HASH_DOCU'
    END AS resultado
FROM fal_documento
WHERE (storage_key IS NOT NULL) <> (hash_docu IS NOT NULL);
-- Esperado: 0 filas. Cualquier fila aquí BLOQUEA la ejecución del forward
-- hasta resolución manual (inconsistencia preexistente, ajena a este cierre).

-- ============================================================================
-- SECCIÓN 7 — VEREDICTO GLOBAL ÚNICO (READY/BLOCKED)
--              Consolida las secciones 0 a 6 en una única fila. READY exige,
--              además de las condiciones de las secciones 1-6: conexión a
--              sb_faltas_db, versión MariaDB >= 11.7 (mínimo final del
--              paquete: JSON_NORMALIZE + SESSION_USER() estable + collation), motor
--              InnoDB y collation utf8mb4_uca1400_ai_ci en las 3 tablas base,
--              tipo BIGINT con signo en sus columnas PK/FK ancla y presencia
--              de las 3 columnas ancla usadas en AFTER (sección 0). Esta es
--              la única fila que debe leerse para decidir si
--              alter-firmas-eda-closure-001-forward.sql puede ejecutarse;
--              las secciones 0-6 quedan como detalle diagnóstico.
-- ============================================================================

SELECT
    CASE WHEN DATABASE() = 'sb_faltas_db' THEN 'OK' ELSE 'BLOQUEADO' END AS base_datos_correcta,
    CASE
        WHEN CAST(SUBSTRING_INDEX(VERSION(), '.', 1) AS UNSIGNED) > 11 THEN 'OK'
        WHEN CAST(SUBSTRING_INDEX(VERSION(), '.', 1) AS UNSIGNED) = 11
             AND CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(VERSION(), '.', 2), '.', -1) AS UNSIGNED) >= 7 THEN 'OK'
        ELSE 'BLOQUEADO'
    END AS version_motor_suficiente,
    (SELECT COUNT(*) FROM information_schema.tables
       WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
         AND table_name IN ('fal_documento_firma_req', 'fal_documento_firma', 'fal_documento')
         AND engine = 'InnoDB' AND table_collation = 'utf8mb4_uca1400_ai_ci') AS tablas_base_motor_collation_ok,
    (SELECT COUNT(*) FROM information_schema.columns
       WHERE table_schema = DATABASE()
         AND ((table_name = 'fal_documento' AND column_name = 'id')
           OR (table_name = 'fal_documento_firma_req' AND column_name IN ('id', 'id_documento'))
           OR (table_name = 'fal_documento_firma' AND column_name IN ('id', 'id_documento')))
         AND data_type = 'bigint' AND column_type NOT LIKE '%unsigned%') AS columnas_ancla_tipo_ok,
    (SELECT COUNT(*) FROM information_schema.columns
       WHERE table_schema = DATABASE()
         AND ((table_name = 'fal_documento_firma_req' AND column_name = 'id_user_alta')
           OR (table_name = 'fal_documento_firma' AND column_name = 'hash_firma')
           OR (table_name = 'fal_documento' AND column_name = 'hash_docu'))) AS columnas_ancla_after_presentes,
    (SELECT COUNT(*) FROM information_schema.tables
       WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
         AND table_name IN ('fal_documento_firma_req', 'fal_documento_firma', 'fal_documento')) AS tablas_base_presentes,
    3 AS tablas_base_esperadas,
    (SELECT COUNT(*) FROM information_schema.columns
       WHERE table_schema = DATABASE()
         AND ((table_name = 'fal_documento_firma_req' AND column_name IN (
                 'version_row','solicitud_firma_id','revision_solicitud','fh_solicitud',
                 'fh_aceptacion','fh_inicio_firma','vence_en','fh_terminal',
                 'motivo_terminal_firma_req','detalle_terminal_firma_req',
                 'revision_artefacto_preparada','hash_entrada_preparado',
                 'content_length_preparado','fh_preparacion'))
           OR (table_name = 'fal_documento_firma' AND column_name IN (
                 'hash_salida','revision_entrada','revision_salida',
                 'actor_tecnico_tipo','actor_tecnico_id','actor_tecnico_app',
                 'actor_tecnico_key_fingerprint','correlation_id','event_id','received_at',
                 'perfil_firma','algoritmo_hash','resultado_firma_id','evidencia_id',
                 'certificado_serial','certificado_thumbprint','certificado_subject',
                 'certificado_issuer','certificado_vigencia_desde','certificado_vigencia_hasta',
                 'resultado_cadena','fuente_revocacion','resultado_revocacion',
                 'tsa_nombre','tsa_instante','resultado_validacion','version_validador'))
           OR (table_name = 'fal_documento' AND column_name = 'revision_artefacto'))) AS columnas_nuevas_ya_presentes,
    (SELECT COUNT(*) FROM information_schema.tables
       WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
         AND table_name IN ('fal_documento_firma_resultado', 'fal_outbox', 'bus_evento',
             'fal_bus_checkpoint', 'fal_inbox', 'fal_inbox_error')) AS tablas_nuevas_ya_presentes,
    (SELECT CASE WHEN column_comment LIKE '%CANCELACION_SOLICITADA%' THEN 1 ELSE 0 END
       FROM information_schema.columns
       WHERE table_schema = DATABASE() AND table_name = 'fal_documento_firma_req'
         AND column_name = 'estado_firma_req') AS catalogo_ya_extendido,
    (SELECT COUNT(*) FROM fal_documento
       WHERE (storage_key IS NOT NULL) <> (hash_docu IS NOT NULL)) AS filas_storage_hash_inconsistentes,
    CASE WHEN
        DATABASE() = 'sb_faltas_db'
        AND (CAST(SUBSTRING_INDEX(VERSION(), '.', 1) AS UNSIGNED) > 11
             OR (CAST(SUBSTRING_INDEX(VERSION(), '.', 1) AS UNSIGNED) = 11
                 AND CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(VERSION(), '.', 2), '.', -1) AS UNSIGNED) >= 7))
        AND (SELECT COUNT(*) FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
                 AND table_name IN ('fal_documento_firma_req', 'fal_documento_firma', 'fal_documento')
                 AND engine = 'InnoDB' AND table_collation = 'utf8mb4_uca1400_ai_ci') = 3
        AND (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND ((table_name = 'fal_documento' AND column_name = 'id')
                   OR (table_name = 'fal_documento_firma_req' AND column_name IN ('id', 'id_documento'))
                   OR (table_name = 'fal_documento_firma' AND column_name IN ('id', 'id_documento')))
                 AND data_type = 'bigint' AND column_type NOT LIKE '%unsigned%') = 5
        AND (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND ((table_name = 'fal_documento_firma_req' AND column_name = 'id_user_alta')
                   OR (table_name = 'fal_documento_firma' AND column_name = 'hash_firma')
                   OR (table_name = 'fal_documento' AND column_name = 'hash_docu'))) = 3
        AND (SELECT COUNT(*) FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
             AND table_name IN ('fal_documento_firma_req', 'fal_documento_firma', 'fal_documento')) = 3
        AND (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND ((table_name = 'fal_documento_firma_req' AND column_name IN (
                         'version_row','solicitud_firma_id','revision_solicitud','fh_solicitud',
                         'fh_aceptacion','fh_inicio_firma','vence_en','fh_terminal',
                         'motivo_terminal_firma_req','detalle_terminal_firma_req',
                         'revision_artefacto_preparada','hash_entrada_preparado',
                         'content_length_preparado','fh_preparacion'))
                   OR (table_name = 'fal_documento_firma' AND column_name IN (
                         'hash_salida','revision_entrada','revision_salida',
                         'actor_tecnico_tipo','actor_tecnico_id','actor_tecnico_app',
                         'actor_tecnico_key_fingerprint','correlation_id','event_id','received_at',
                         'perfil_firma','algoritmo_hash','resultado_firma_id','evidencia_id',
                         'certificado_serial','certificado_thumbprint','certificado_subject',
                         'certificado_issuer','certificado_vigencia_desde','certificado_vigencia_hasta',
                         'resultado_cadena','fuente_revocacion','resultado_revocacion',
                         'tsa_nombre','tsa_instante','resultado_validacion','version_validador'))
                   OR (table_name = 'fal_documento' AND column_name = 'revision_artefacto'))) = 0
        AND (SELECT COUNT(*) FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
                 AND table_name IN ('fal_documento_firma_resultado', 'fal_outbox', 'bus_evento',
                     'fal_bus_checkpoint', 'fal_inbox', 'fal_inbox_error')) = 0
        AND COALESCE((SELECT CASE WHEN column_comment LIKE '%CANCELACION_SOLICITADA%' THEN 1 ELSE 0 END
               FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'fal_documento_firma_req'
                 AND column_name = 'estado_firma_req'), 0) = 0
        AND (SELECT COUNT(*) FROM fal_documento
               WHERE (storage_key IS NOT NULL) <> (hash_docu IS NOT NULL)) = 0
    THEN 'READY' ELSE 'BLOCKED' END AS veredicto_global;

-- ============================================================================
-- FIN DEL PRECHECK
-- ============================================================================
