-- verify-firmas-eda-closure-001-postcheck.sql
-- SOLO LECTURA — No altera ningún objeto. Solo SELECT sobre information_schema.
--
-- Propósito: verificar el resultado estructural DESPUÉS de ejecutar
--            alter-firmas-eda-closure-001-forward.sql.
-- Motor    : MariaDB 12.3.2, sb_faltas_db
-- Trabajo  : SPEC-FIRMAS-EDA-CLOSURE-001
-- Uso      : ejecutar manualmente desde HeidiSQL DESPUÉS de ejecutar el forward.
--
-- Modelo mínimo aprobado: sin fan-out central, sin bus_definer, sin rutinas
-- SQL SECURITY DEFINER, sin vistas diagnósticas. Además de existencia/
-- conteo, este postcheck VALIDA contenido y comportamiento, incluidos: los
-- CHECK de integridad de fal_outbox/bus_evento/fal_bus_checkpoint/fal_inbox/
-- fal_inbox_error, la FK fal_inbox_error->fal_inbox y la fila singleton de
-- fal_bus_checkpoint.
--
-- Este script es el POSTCHECK ESTRUCTURAL (se ejecuta inmediatamente
-- después del forward, antes de otorgar ningún GRANT; database/README.md
-- "Orden operativo"). La SECCIÓN 6 (permisos runtime de las 5 cuentas de
-- aplicación, directos sobre bus_evento) requiere que
-- database/ddl/grants-firmas-eda-bus.sql ya se haya ejecutado; si no lo fue,
-- la SECCIÓN 6 reporta BLOQUEADO_SIN_GRANTS en lugar de un falso DIVERGENCIA.
-- ============================================================================

-- ============================================================================
-- SECCIÓN 1 — fal_documento_firma_req: 14 columnas nuevas presentes
-- ============================================================================

SELECT
    c_esp.columna_esperada,
    CASE WHEN c.column_name IS NOT NULL THEN 'OK' ELSE 'FALTANTE' END AS resultado
FROM (
    SELECT 'version_row' AS columna_esperada
    UNION ALL SELECT 'solicitud_firma_id'
    UNION ALL SELECT 'revision_solicitud'
    UNION ALL SELECT 'fh_solicitud'
    UNION ALL SELECT 'fh_aceptacion'
    UNION ALL SELECT 'fh_inicio_firma'
    UNION ALL SELECT 'vence_en'
    UNION ALL SELECT 'fh_terminal'
    UNION ALL SELECT 'motivo_terminal_firma_req'
    UNION ALL SELECT 'detalle_terminal_firma_req'
    UNION ALL SELECT 'revision_artefacto_preparada'
    UNION ALL SELECT 'hash_entrada_preparado'
    UNION ALL SELECT 'content_length_preparado'
    UNION ALL SELECT 'fh_preparacion'
) c_esp
LEFT JOIN information_schema.columns c
    ON c.table_schema = DATABASE()
   AND c.table_name = 'fal_documento_firma_req'
   AND c.column_name = c_esp.columna_esperada;

-- 1b. Conteo esperado: 14 de 14 OK
SELECT
    COUNT(*) AS columnas_presentes,
    14       AS columnas_esperadas,
    CASE WHEN COUNT(*) = 14 THEN 'OK' ELSE 'DIVERGENCIA' END AS resultado
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'fal_documento_firma_req'
  AND column_name IN (
      'version_row','solicitud_firma_id','revision_solicitud','fh_solicitud',
      'fh_aceptacion','fh_inicio_firma','vence_en','fh_terminal',
      'motivo_terminal_firma_req','detalle_terminal_firma_req',
      'revision_artefacto_preparada','hash_entrada_preparado',
      'content_length_preparado','fh_preparacion');

-- 1c. Catálogo estado_firma_req extendido a 9 valores (comentario de columna)
SELECT
    column_comment,
    CASE WHEN column_comment LIKE '%9=RECHAZADO_CONTRATO%' THEN 'OK' ELSE 'DIVERGENCIA' END AS resultado
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'fal_documento_firma_req'
  AND column_name = 'estado_firma_req';

-- 1d. Constraints CHECK nuevas presentes (5, incluida la preparación DEC-EDA-030)
SELECT
    chk_esp.constraint_esperado,
    CASE WHEN tc.constraint_name IS NOT NULL THEN 'OK' ELSE 'FALTANTE' END AS resultado
FROM (
    SELECT 'chk_firma_req_estado_ext' AS constraint_esperado
    UNION ALL SELECT 'chk_firma_req_revision'
    UNION ALL SELECT 'chk_firma_req_preparacion'
    UNION ALL SELECT 'chk_firma_req_rev_preparada'
    UNION ALL SELECT 'chk_firma_req_content_length_preparado'
) chk_esp
LEFT JOIN information_schema.table_constraints tc
    ON tc.constraint_schema = DATABASE()
   AND tc.table_name = 'fal_documento_firma_req'
   AND tc.constraint_name = chk_esp.constraint_esperado;

-- 1e. Índices/UNIQUE nuevos presentes
SELECT
    ix_esp.indice_esperado,
    CASE WHEN COUNT(s.index_name) > 0 THEN 'OK' ELSE 'FALTANTE' END AS resultado
FROM (
    SELECT 'uq_firma_req_solicitud' AS indice_esperado
    UNION ALL SELECT 'ix_firma_req_estado_vence'
    UNION ALL SELECT 'ix_firma_req_solicitud_rev'
) ix_esp
LEFT JOIN information_schema.statistics s
    ON s.table_schema = DATABASE()
   AND s.table_name = 'fal_documento_firma_req'
   AND s.index_name = ix_esp.indice_esperado
GROUP BY ix_esp.indice_esperado;

-- 1f. Preparación persistida: NULL-o-todos-informados (chk_firma_req_preparacion
--     ya lo garantiza; esta consulta reporta la distribución observada)
SELECT
    CASE WHEN revision_artefacto_preparada IS NULL THEN 'SIN_PREPARACION' ELSE 'CON_PREPARACION' END AS estado,
    COUNT(*) AS filas
FROM fal_documento_firma_req
GROUP BY estado;

-- ============================================================================
-- SECCIÓN 2 — fal_documento_firma: 27 columnas nuevas presentes
-- ============================================================================

SELECT
    COUNT(*) AS columnas_presentes,
    27       AS columnas_esperadas,
    CASE WHEN COUNT(*) = 27 THEN 'OK' ELSE 'DIVERGENCIA' END AS resultado
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'fal_documento_firma'
  AND column_name IN (
      'hash_salida','revision_entrada','revision_salida',
      'actor_tecnico_tipo','actor_tecnico_id','actor_tecnico_app',
      'actor_tecnico_key_fingerprint','correlation_id','event_id','received_at',
      'perfil_firma','algoritmo_hash','resultado_firma_id','evidencia_id',
      'certificado_serial','certificado_thumbprint','certificado_subject',
      'certificado_issuer','certificado_vigencia_desde','certificado_vigencia_hasta',
      'resultado_cadena','fuente_revocacion','resultado_revocacion',
      'tsa_nombre','tsa_instante','resultado_validacion','version_validador');

-- 2b. Detalle columna por columna
SELECT
    c_esp.columna_esperada,
    CASE WHEN c.column_name IS NOT NULL THEN 'OK' ELSE 'FALTANTE' END AS resultado
FROM (
    SELECT 'hash_salida' AS columna_esperada
    UNION ALL SELECT 'revision_entrada'
    UNION ALL SELECT 'revision_salida'
    UNION ALL SELECT 'actor_tecnico_tipo'
    UNION ALL SELECT 'actor_tecnico_id'
    UNION ALL SELECT 'actor_tecnico_app'
    UNION ALL SELECT 'actor_tecnico_key_fingerprint'
    UNION ALL SELECT 'correlation_id'
    UNION ALL SELECT 'event_id'
    UNION ALL SELECT 'received_at'
    UNION ALL SELECT 'perfil_firma'
    UNION ALL SELECT 'algoritmo_hash'
    UNION ALL SELECT 'resultado_firma_id'
    UNION ALL SELECT 'evidencia_id'
    UNION ALL SELECT 'certificado_serial'
    UNION ALL SELECT 'certificado_thumbprint'
    UNION ALL SELECT 'certificado_subject'
    UNION ALL SELECT 'certificado_issuer'
    UNION ALL SELECT 'certificado_vigencia_desde'
    UNION ALL SELECT 'certificado_vigencia_hasta'
    UNION ALL SELECT 'resultado_cadena'
    UNION ALL SELECT 'fuente_revocacion'
    UNION ALL SELECT 'resultado_revocacion'
    UNION ALL SELECT 'tsa_nombre'
    UNION ALL SELECT 'tsa_instante'
    UNION ALL SELECT 'resultado_validacion'
    UNION ALL SELECT 'version_validador'
) c_esp
LEFT JOIN information_schema.columns c
    ON c.table_schema = DATABASE()
   AND c.table_name = 'fal_documento_firma'
   AND c.column_name = c_esp.columna_esperada;

-- 2c. algoritmo_hash es NULLABLE, sin DEFAULT (DEC-EDA-029; nunca se completa
--     retroactivamente un valor no demostrado para filas históricas). Nota
--     MariaDB 12.3.2: para una columna NULLABLE sin cláusula DEFAULT
--     explícita, information_schema.columns.column_default no es SQL NULL:
--     es el literal de 4 caracteres 'NULL' (verificado con
--     HEX(column_default) = 4E554C4C). Comparar contra ambas formas para no
--     reportar una divergencia falsa por este comportamiento del motor.
SELECT
    is_nullable, column_default,
    CASE WHEN is_nullable = 'YES' AND (column_default IS NULL OR column_default = 'NULL') THEN 'OK' ELSE 'DIVERGENCIA' END AS resultado
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'fal_documento_firma'
  AND column_name = 'algoritmo_hash';

-- 2d. Constraints CHECK nuevas presentes (10, incluidos chk_firma_eda_completo
--     y chk_firma_perfil_unico)
SELECT
    obj_esp.objeto_esperado,
    CASE WHEN tc.constraint_name IS NOT NULL THEN 'OK' ELSE 'FALTANTE' END AS resultado
FROM (
    SELECT 'chk_firma_hash_algoritmo' AS objeto_esperado
    UNION ALL SELECT 'chk_firma_perfil_unico'
    UNION ALL SELECT 'chk_firma_actor_tecnico_tipo'
    UNION ALL SELECT 'chk_firma_resultado_cadena'
    UNION ALL SELECT 'chk_firma_fuente_revocacion'
    UNION ALL SELECT 'chk_firma_resultado_revocacion'
    UNION ALL SELECT 'chk_firma_resultado_validacion'
    UNION ALL SELECT 'chk_firma_cert_vigencia'
    UNION ALL SELECT 'chk_firma_revision_salida'
    UNION ALL SELECT 'chk_firma_eda_completo'
) obj_esp
LEFT JOIN information_schema.table_constraints tc
    ON tc.constraint_schema = DATABASE()
   AND tc.table_name = 'fal_documento_firma'
   AND tc.constraint_name = obj_esp.objeto_esperado;

-- 2e. Índices/UNIQUE nuevos presentes
SELECT
    ix_esp.indice_esperado,
    CASE WHEN COUNT(s.index_name) > 0 THEN 'OK' ELSE 'FALTANTE' END AS resultado
FROM (
    SELECT 'ix_firma_correlation' AS indice_esperado
    UNION ALL SELECT 'ix_firma_event_id'
    UNION ALL SELECT 'uq_firma_resultado'
) ix_esp
LEFT JOIN information_schema.statistics s
    ON s.table_schema = DATABASE()
   AND s.table_name = 'fal_documento_firma'
   AND s.index_name = ix_esp.indice_esperado
GROUP BY ix_esp.indice_esperado;

-- 2f. fk_firma_resultado presente (FK diferida de la sección 04bis del forward).
--     Nota: information_schema.table_constraints en MariaDB 12.3.2 no expone
--     referenced_table_name (esa columna vive en key_column_usage /
--     referential_constraints); se usa key_column_usage para obtenerla junto
--     con la existencia del constraint.
SELECT
    constraint_name, referenced_table_name,
    CASE WHEN constraint_name IS NOT NULL THEN 'OK' ELSE 'FALTANTE' END AS resultado
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE()
  AND table_name = 'fal_documento_firma'
  AND constraint_name = 'fk_firma_resultado'
  AND referenced_table_name IS NOT NULL;

-- ============================================================================
-- SECCIÓN 2bis — fal_documento: revision_artefacto presente (DEC-EDA-024/031)
-- ============================================================================

SELECT
    is_nullable, column_default,
    CASE WHEN is_nullable = 'NO' AND column_default = '0' THEN 'OK' ELSE 'DIVERGENCIA' END AS resultado
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'fal_documento'
  AND column_name = 'revision_artefacto';

SELECT
    CASE WHEN constraint_name IS NOT NULL THEN 'OK' ELSE 'FALTANTE' END AS resultado
FROM information_schema.table_constraints
WHERE constraint_schema = DATABASE()
  AND table_name = 'fal_documento'
  AND constraint_name = 'chk_documento_revision_artefacto';

-- 2bis-b. Backfill correcto: ningún documento con storage_key/hash_docu
-- informado quedó en revision_artefacto = 0 (DEC-EDA-031)
SELECT
    COUNT(*) AS filas_backfill_incorrecto,
    CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'DIVERGENCIA_BACKFILL' END AS resultado
FROM fal_documento
WHERE (storage_key IS NOT NULL OR hash_docu IS NOT NULL)
  AND revision_artefacto = 0;

-- ============================================================================
-- SECCIÓN 3 — 6 tablas nuevas del bus presentes, motor/charset/collation
--              correctos. Modelo mínimo aprobado: bus_evento (único objeto
--              central) + Outbox/checkpoint/Inbox/Inbox error locales de
--              Faltas. Sin fan-out, sin entregas centrales, sin
--              suscripciones, sin permisos de publicación tabulares.
-- ============================================================================

SELECT
    table_name, engine, table_collation,
    CASE
        WHEN engine = 'InnoDB' AND table_collation = 'utf8mb4_uca1400_ai_ci' THEN 'OK'
        ELSE 'DIVERGENCIA'
    END AS resultado
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
      'fal_documento_firma_resultado', 'fal_outbox', 'bus_evento',
      'fal_bus_checkpoint', 'fal_inbox', 'fal_inbox_error');

-- 3b. Conteo esperado: 6 de 6 presentes
SELECT
    COUNT(*) AS tablas_presentes,
    6        AS tablas_esperadas,
    CASE WHEN COUNT(*) = 6 THEN 'OK' ELSE 'DIVERGENCIA' END AS resultado
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE'
  AND table_name IN (
      'fal_documento_firma_resultado', 'fal_outbox', 'bus_evento',
      'fal_bus_checkpoint', 'fal_inbox', 'fal_inbox_error');

-- 3c. Conteo de columnas por tabla nueva (paridad de cardinalidad)
SELECT
    esp.tabla, esp.columnas_esperadas,
    COUNT(c.column_name) AS columnas_presentes,
    CASE WHEN COUNT(c.column_name) = esp.columnas_esperadas THEN 'OK' ELSE 'DIVERGENCIA' END AS resultado
FROM (
    SELECT 'fal_documento_firma_resultado' AS tabla, 35 AS columnas_esperadas
    UNION ALL SELECT 'fal_outbox', 17
    UNION ALL SELECT 'bus_evento', 13
    UNION ALL SELECT 'fal_bus_checkpoint', 3
    UNION ALL SELECT 'fal_inbox', 18
    UNION ALL SELECT 'fal_inbox_error', 6
) esp
LEFT JOIN information_schema.columns c
    ON c.table_schema = DATABASE()
   AND c.table_name = esp.tabla
GROUP BY esp.tabla, esp.columnas_esperadas;

-- 3d. FKs presentes: fal_inbox_error -> fal_inbox,
--     fal_documento_firma_resultado -> fal_documento y fal_documento_firma_req.
--     bus_evento, fal_outbox y fal_bus_checkpoint no tienen FK entrante ni
--     saliente (modelo mínimo aprobado: sin fan-out central).
SELECT
    table_name, constraint_name, referenced_table_name,
    'OK' AS resultado
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE()
  AND table_name IN ('fal_inbox_error', 'fal_documento_firma_resultado')
  AND referenced_table_name IS NOT NULL
ORDER BY table_name, referenced_table_name;
-- Esperado: 3 filas (fal_inbox_error->fal_inbox,
-- fal_documento_firma_resultado->fal_documento,
-- fal_documento_firma_resultado->fal_documento_firma_req).

-- 3e. UNIQUE/PK claves de idempotencia presentes en las tablas nuevas del bus
SELECT
    tabla_esp.tabla, tabla_esp.indice_esperado,
    CASE WHEN COUNT(s.index_name) > 0 THEN 'OK' ELSE 'FALTANTE' END AS resultado
FROM (
    SELECT 'fal_documento_firma_resultado' AS tabla, 'uq_resultado_firma_id' AS indice_esperado
    UNION ALL SELECT 'fal_outbox', 'uq_fal_outbox_evento_id'
    UNION ALL SELECT 'bus_evento', 'uq_bus_evento_evento_id'
    UNION ALL SELECT 'fal_bus_checkpoint', 'PRIMARY'
    UNION ALL SELECT 'fal_inbox', 'PRIMARY'
    UNION ALL SELECT 'fal_inbox', 'uq_fal_inbox_evento_id'
    UNION ALL SELECT 'fal_inbox_error', 'uq_fal_inbox_error_attempt'
) tabla_esp
LEFT JOIN information_schema.statistics s
    ON s.table_schema = DATABASE()
   AND s.table_name = tabla_esp.tabla
   AND s.index_name = tabla_esp.indice_esperado
GROUP BY tabla_esp.tabla, tabla_esp.indice_esperado;

-- 3f. fal_inbox usa bus_evento_id como PRIMARY KEY (no un id técnico propio)
SELECT
    column_name,
    CASE WHEN column_key = 'PRI' THEN 'OK' ELSE 'DIVERGENCIA_PK_INCORRECTA' END AS resultado
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'fal_inbox'
  AND column_name = 'bus_evento_id';

-- 3g. fal_bus_checkpoint: fila singleton inicial presente (checkpoint_id=1,
--     ultimo_evento_id=0) y única fila de la tabla
SELECT
    COUNT(*) AS filas_presentes,
    1        AS filas_esperadas,
    CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'DIVERGENCIA_SEED_CHECKPOINT' END AS resultado
FROM fal_bus_checkpoint
WHERE checkpoint_id = 1;

-- 3h. si_error modelado como BOOLEAN con CHECK explícito IN (FALSE, TRUE),
--     en fal_outbox y fal_inbox
SELECT
    esp.tabla, esp.constraint_esperado,
    CASE WHEN tc.constraint_name IS NOT NULL THEN 'OK' ELSE 'FALTANTE' END AS resultado
FROM (
    SELECT 'fal_outbox' AS tabla, 'chk_fal_outbox_si_error' AS constraint_esperado
    UNION ALL SELECT 'fal_inbox', 'chk_fal_inbox_si_error'
) esp
LEFT JOIN information_schema.table_constraints tc
    ON tc.constraint_schema = DATABASE()
   AND tc.table_name = esp.tabla
   AND tc.constraint_name = esp.constraint_esperado;

-- 3i. CHECK de integridad en las tablas nuevas del bus: schema_version/
--     attempts/status por rango y singleton de fal_bus_checkpoint
SELECT
    esp.tabla, esp.constraint_esperado,
    CASE WHEN tc.constraint_name IS NOT NULL THEN 'OK' ELSE 'FALTANTE' END AS resultado
FROM (
    SELECT 'fal_outbox' AS tabla, 'chk_fal_outbox_status' AS constraint_esperado
    UNION ALL SELECT 'fal_outbox', 'chk_fal_outbox_schema_version'
    UNION ALL SELECT 'fal_outbox', 'chk_fal_outbox_attempts'
    UNION ALL SELECT 'bus_evento', 'chk_bus_evento_schema_version'
    UNION ALL SELECT 'fal_bus_checkpoint', 'chk_fal_bus_checkpoint_singleton'
    UNION ALL SELECT 'fal_inbox', 'chk_fal_inbox_status'
    UNION ALL SELECT 'fal_inbox', 'chk_fal_inbox_attempts'
) esp
LEFT JOIN information_schema.table_constraints tc
    ON tc.constraint_schema = DATABASE()
   AND tc.table_name = esp.tabla
   AND tc.constraint_name = esp.constraint_esperado;

SELECT
    COUNT(*) AS constraints_presentes,
    9        AS constraints_esperados,
    CASE WHEN COUNT(*) = 9 THEN 'OK' ELSE 'BLOQUEADO_FALTAN_CONSTRAINTS' END AS veredicto
FROM information_schema.table_constraints
WHERE constraint_schema = DATABASE()
  AND table_name IN ('fal_outbox', 'bus_evento', 'fal_bus_checkpoint', 'fal_inbox')
  AND constraint_name IN (
      'chk_fal_outbox_status', 'chk_fal_outbox_schema_version', 'chk_fal_outbox_attempts',
      'chk_bus_evento_schema_version', 'chk_fal_bus_checkpoint_singleton',
      'chk_fal_inbox_status', 'chk_fal_inbox_attempts',
      'chk_fal_outbox_si_error', 'chk_fal_inbox_si_error'
  );

-- ============================================================================
-- SECCIÓN 5 — Objetos del baseline protegido: intactos (no tocados por este script)
-- ============================================================================

SELECT
    COUNT(*) AS tablas_baseline_presentes,
    16       AS tablas_baseline_esperadas,
    CASE WHEN COUNT(*) = 16 THEN 'OK' ELSE 'DIVERGENCIA' END AS resultado
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE'
  AND table_name IN (
      'fal_informix_sync_error','fal_informix_sync_run','fal_rubro_version',
      'geo_bahra_asentamiento','geo_calle_alturas_barrio','geo_dataset_load_error',
      'geo_dataset_load_run','geo_dataset_row_version','geo_ign_departamento',
      'geo_ign_municipio','geo_ign_provincia','geo_indec_calles',
      'geo_indec_localidad','geo_indec_localidad_censal','geo_malv_calle_version',
      'geo_malv_localidad_version');

-- ============================================================================
-- SECCIÓN 6 — POSTCHECK RUNTIME DE SEGURIDAD Y PERMISOS (las 5 cuentas de
--              aplicación, con privilegios directos y mínimos sobre
--              bus_evento — INSERT column-level, sin bus_evento_id, para
--              los 2 publishers; SELECT/SELECT+DELETE de tabla completa
--              para consumers/maintenance; sin bus_definer, sin rutinas,
--              sin vistas). A
--              diferencia de las secciones 1-5 (postcheck ESTRUCTURAL,
--              ejecutable inmediatamente después del forward), esta sección
--              requiere que database/ddl/grants-firmas-eda-bus.sql ya se
--              haya ejecutado (database/README.md "Orden operativo"); antes
--              de eso, todas las filas de esta sección reportan 0
--              privilegios y el veredicto es BLOQUEADO_SIN_GRANTS, no una
--              divergencia real.
-- ============================================================================

-- 6a. Los consumers y bus_maintenance tienen EXACTAMENTE el privilegio
--     directo, a nivel de TABLA completa, documentado sobre bus_evento
--     (grants-firmas-eda-bus.sql secciones 03-05): SELECT para consumers,
--     SELECT+DELETE para maintenance. Los publishers NO aparecen aquí: su
--     grant es exclusivamente column-level (sección 6a-col abajo) y por
--     eso no genera fila en information_schema.table_privileges.
SELECT
    grantee, table_name AS tabla, privilege_type,
    'OK_PRESENTE' AS resultado
FROM information_schema.table_privileges
WHERE table_schema = DATABASE()
  AND grantee IN ("'bus_consumer_faltas'@'%'", "'bus_consumer_firmas'@'%'",
                  "'bus_maintenance'@'%'")
ORDER BY grantee, table_name, privilege_type;

SELECT
    esp.grantee, esp.privilegio,
    CASE WHEN tp.privilege_type IS NOT NULL THEN 'OK' ELSE 'FALTANTE' END AS resultado
FROM (
    SELECT "'bus_consumer_faltas'@'%'" AS grantee, 'SELECT' AS privilegio
    UNION ALL SELECT "'bus_consumer_firmas'@'%'", 'SELECT'
    UNION ALL SELECT "'bus_maintenance'@'%'", 'SELECT'
    UNION ALL SELECT "'bus_maintenance'@'%'", 'DELETE'
) esp
LEFT JOIN information_schema.table_privileges tp
    ON tp.table_schema = DATABASE()
   AND tp.table_name = 'bus_evento'
   AND tp.grantee = esp.grantee
   AND tp.privilege_type = esp.privilegio;

SELECT
    COUNT(*) AS privilegios_tabla_presentes,
    4        AS privilegios_tabla_esperados,
    CASE WHEN COUNT(*) = 4 THEN 'OK' ELSE 'BLOQUEADO_SIN_GRANTS' END AS resultado
FROM information_schema.table_privileges
WHERE table_schema = DATABASE()
  AND table_name = 'bus_evento'
  AND ((grantee = "'bus_consumer_faltas'@'%'" AND privilege_type = 'SELECT')
    OR (grantee = "'bus_consumer_firmas'@'%'" AND privilege_type = 'SELECT')
    OR (grantee = "'bus_maintenance'@'%'" AND privilege_type IN ('SELECT', 'DELETE')));

-- 6a-col. Los 2 publishers tienen EXACTAMENTE INSERT column-level sobre las
--     12 columnas de bus_evento distintas de bus_evento_id
--     (grants-firmas-eda-bus.sql secciones 01-02). El GRANT column-level
--     se registra en information_schema.column_privileges, nunca en
--     table_privileges.
SELECT
    grantee, column_name, privilege_type,
    'OK_PRESENTE' AS resultado
FROM information_schema.column_privileges
WHERE table_schema = DATABASE()
  AND table_name = 'bus_evento'
  AND grantee IN ("'bus_publisher_faltas'@'%'", "'bus_publisher_firmas'@'%'")
ORDER BY grantee, column_name;

SELECT
    esp.grantee, esp.columna,
    CASE WHEN cp.privilege_type IS NOT NULL THEN 'OK' ELSE 'FALTANTE' END AS resultado
FROM (
    SELECT grantee_esp.grantee, col_esp.columna
    FROM (
        SELECT "'bus_publisher_faltas'@'%'" AS grantee
        UNION ALL SELECT "'bus_publisher_firmas'@'%'"
    ) grantee_esp
    CROSS JOIN (
        SELECT 'evento_id' AS columna
        UNION ALL SELECT 'productor_codigo'
        UNION ALL SELECT 'evento_codigo'
        UNION ALL SELECT 'schema_version'
        UNION ALL SELECT 'agregado_codigo'
        UNION ALL SELECT 'agregado_id'
        UNION ALL SELECT 'agregado_version'
        UNION ALL SELECT 'correlation_id'
        UNION ALL SELECT 'causation_id'
        UNION ALL SELECT 'payload'
        UNION ALL SELECT 'ocurrido_en'
        UNION ALL SELECT 'publicado_en'
    ) col_esp
) esp
LEFT JOIN information_schema.column_privileges cp
    ON cp.table_schema = DATABASE()
   AND cp.table_name = 'bus_evento'
   AND cp.grantee = esp.grantee
   AND cp.column_name = esp.columna
   AND cp.privilege_type = 'INSERT';

SELECT
    COUNT(*) AS privilegios_columna_presentes,
    24       AS privilegios_columna_esperados,
    CASE WHEN COUNT(*) = 24 THEN 'OK' ELSE 'BLOQUEADO_SIN_GRANTS' END AS resultado
FROM information_schema.column_privileges
WHERE table_schema = DATABASE()
  AND table_name = 'bus_evento'
  AND grantee IN ("'bus_publisher_faltas'@'%'", "'bus_publisher_firmas'@'%'")
  AND privilege_type = 'INSERT'
  AND column_name <> 'bus_evento_id';

-- 6a-bis. Ningún publisher tiene INSERT (ni ningún otro privilegio) sobre
--     bus_evento_id: ese valor lo genera siempre AUTO_INCREMENT, nunca el
--     cliente.
SELECT
    grantee, column_name, privilege_type,
    'DIVERGENCIA_PRIVILEGIO_SOBRE_BUS_EVENTO_ID' AS resultado
FROM information_schema.column_privileges
WHERE table_schema = DATABASE()
  AND table_name = 'bus_evento'
  AND column_name = 'bus_evento_id'
  AND grantee IN ("'bus_publisher_faltas'@'%'", "'bus_publisher_firmas'@'%'");
-- Esperado: 0 filas.

-- 6b. Ninguna de las 5 cuentas tiene ningún privilegio (de tabla ni de
--     columna) sobre las tablas locales de Faltas (fal_documento*,
--     fal_outbox, fal_bus_checkpoint, fal_inbox, fal_inbox_error): son
--     locales de la aplicación Faltas, ajenas a las cuentas bus_*.
SELECT
    grantee, table_name, privilege_type,
    'DIVERGENCIA_PRIVILEGIO_NO_AUTORIZADO' AS resultado
FROM information_schema.table_privileges
WHERE table_schema = DATABASE()
  AND grantee IN ("'bus_publisher_faltas'@'%'", "'bus_publisher_firmas'@'%'",
                  "'bus_consumer_faltas'@'%'", "'bus_consumer_firmas'@'%'",
                  "'bus_maintenance'@'%'")
  AND (table_name LIKE 'fal_documento%'
       OR table_name IN ('fal_outbox', 'fal_bus_checkpoint', 'fal_inbox', 'fal_inbox_error'));
-- Esperado: 0 filas.

SELECT
    grantee, table_name, column_name, privilege_type,
    'DIVERGENCIA_PRIVILEGIO_COLUMNA_NO_AUTORIZADO' AS resultado
FROM information_schema.column_privileges
WHERE table_schema = DATABASE()
  AND grantee IN ("'bus_publisher_faltas'@'%'", "'bus_publisher_firmas'@'%'",
                  "'bus_consumer_faltas'@'%'", "'bus_consumer_firmas'@'%'",
                  "'bus_maintenance'@'%'")
  AND (table_name LIKE 'fal_documento%'
       OR table_name IN ('fal_outbox', 'fal_bus_checkpoint', 'fal_inbox', 'fal_inbox_error'));
-- Esperado: 0 filas.

-- 6c. Los publishers no tienen ningún privilegio de TABLA sobre bus_evento
--     (su único grant es column-level, sección 6a-col) ni ningún privilegio
--     de columna distinto de INSERT; los consumers no tienen
--     INSERT/UPDATE/DELETE (solo SELECT, de tabla completa).
SELECT
    grantee, privilege_type,
    'DIVERGENCIA_PRIVILEGIO_TABLA_EXCEDENTE' AS resultado
FROM information_schema.table_privileges
WHERE table_schema = DATABASE()
  AND table_name = 'bus_evento'
  AND grantee IN ("'bus_publisher_faltas'@'%'", "'bus_publisher_firmas'@'%'");
-- Esperado: 0 filas (los publishers no deben tener ningún privilegio a
-- nivel de tabla completa: todo su acceso es column-level).

SELECT
    grantee, column_name, privilege_type,
    'DIVERGENCIA_PRIVILEGIO_COLUMNA_EXCEDENTE' AS resultado
FROM information_schema.column_privileges
WHERE table_schema = DATABASE()
  AND table_name = 'bus_evento'
  AND grantee IN ("'bus_publisher_faltas'@'%'", "'bus_publisher_firmas'@'%'")
  AND privilege_type <> 'INSERT';
-- Esperado: 0 filas.

SELECT
    grantee, privilege_type,
    'DIVERGENCIA_PRIVILEGIO_EXCEDENTE' AS resultado
FROM information_schema.table_privileges
WHERE table_schema = DATABASE()
  AND table_name = 'bus_evento'
  AND grantee IN ("'bus_consumer_faltas'@'%'", "'bus_consumer_firmas'@'%'")
  AND privilege_type <> 'SELECT';
-- Esperado: 0 filas.

-- ============================================================================
-- FIN DEL POSTCHECK
-- ============================================================================
