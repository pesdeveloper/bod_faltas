-- verify-firmas-eda-closure-001-parity.sql
-- SOLO LECTURA — No altera ningún objeto. Solo SELECT sobre information_schema.
--
-- Propósito: verificar paridad tipo-a-tipo entre el modelo físico documentado en
--            backend/api-faltas-core/docs/spec-as-source/50-persistence/
--            firma-eda-physical-model.md y el esquema real de MariaDB, tras
--            ejecutar alter-firmas-eda-closure-001-forward.sql.
-- Motor    : MariaDB 12.3.2, sb_faltas_db
-- Trabajo  : SPEC-FIRMAS-EDA-CLOSURE-001
-- Uso      : ejecutar manualmente desde HeidiSQL DESPUÉS del forward y del
--            postcheck estructural (verify-firmas-eda-closure-001-postcheck.sql).
-- ============================================================================
--
-- Este script reporta divergencias; nunca las oculta ni las corrige. Toda fila
-- con resultado <> 'OK' debe resolverse contra firma-eda-physical-model.md
-- antes de considerar el paquete DDL conforme.
--
-- ============================================================================

-- ============================================================================
-- SECCIÓN 1 — fal_documento_firma_req: tipo/nullable por columna nueva
-- ============================================================================

SELECT
    esp.columna, esp.tipo_esperado, esp.nullable_esperado,
    c.data_type, c.is_nullable,
    CASE
        WHEN c.column_name IS NULL THEN 'FALTANTE'
        WHEN c.is_nullable <> esp.nullable_esperado THEN 'DIVERGENCIA_NULLABLE'
        WHEN c.data_type <> esp.tipo_esperado THEN 'DIVERGENCIA_TIPO'
        ELSE 'OK'
    END AS resultado
FROM (
    SELECT 'version_row'                    AS columna, 'int'      AS tipo_esperado, 'NO'  AS nullable_esperado
    UNION ALL SELECT 'solicitud_firma_id',            'char',     'YES'
    UNION ALL SELECT 'revision_solicitud',            'smallint', 'NO'
    UNION ALL SELECT 'fh_solicitud',                  'datetime', 'YES'
    UNION ALL SELECT 'fh_aceptacion',                 'datetime', 'YES'
    UNION ALL SELECT 'fh_inicio_firma',               'datetime', 'YES'
    UNION ALL SELECT 'vence_en',                      'datetime', 'YES'
    UNION ALL SELECT 'fh_terminal',                   'datetime', 'YES'
    UNION ALL SELECT 'motivo_terminal_firma_req',     'smallint', 'YES'
    UNION ALL SELECT 'detalle_terminal_firma_req',    'varchar',  'YES'
    UNION ALL SELECT 'revision_artefacto_preparada',  'int',      'YES'
    UNION ALL SELECT 'hash_entrada_preparado',        'varchar',  'YES'
    UNION ALL SELECT 'content_length_preparado',      'bigint',   'YES'
    UNION ALL SELECT 'fh_preparacion',                'datetime', 'YES'
) esp
LEFT JOIN information_schema.columns c
    ON c.table_schema = DATABASE()
   AND c.table_name = 'fal_documento_firma_req'
   AND c.column_name = esp.columna;

-- 1b. Precisión DATETIME(6) en columnas temporales nuevas
SELECT
    column_name,
    datetime_precision,
    CASE WHEN datetime_precision = 6 THEN 'OK' ELSE 'DIVERGENCIA_PRECISION' END AS resultado
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'fal_documento_firma_req'
  AND column_name IN ('fh_solicitud','fh_aceptacion','fh_inicio_firma','vence_en','fh_terminal','fh_preparacion');

-- 1c. CHAR(36) exacto para solicitud_firma_id (UUID)
SELECT
    character_maximum_length,
    CASE WHEN character_maximum_length = 36 THEN 'OK' ELSE 'DIVERGENCIA_LONGITUD' END AS resultado
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'fal_documento_firma_req'
  AND column_name = 'solicitud_firma_id';

-- ============================================================================
-- SECCIÓN 2 — fal_documento_firma: tipo/nullable por columna nueva
-- ============================================================================

SELECT
    esp.columna, esp.tipo_esperado, esp.nullable_esperado,
    c.data_type, c.is_nullable,
    CASE
        WHEN c.column_name IS NULL THEN 'FALTANTE'
        WHEN c.is_nullable <> esp.nullable_esperado THEN 'DIVERGENCIA_NULLABLE'
        WHEN c.data_type <> esp.tipo_esperado THEN 'DIVERGENCIA_TIPO'
        ELSE 'OK'
    END AS resultado
FROM (
    SELECT 'hash_salida'                    AS columna, 'varchar'  AS tipo_esperado, 'YES' AS nullable_esperado
    UNION ALL SELECT 'revision_entrada',              'smallint', 'YES'
    UNION ALL SELECT 'revision_salida',                'smallint', 'YES'
    UNION ALL SELECT 'actor_tecnico_tipo',              'smallint', 'YES'
    UNION ALL SELECT 'actor_tecnico_id',                'varchar',  'YES'
    UNION ALL SELECT 'actor_tecnico_app',               'varchar',  'YES'
    UNION ALL SELECT 'actor_tecnico_key_fingerprint',   'varchar',  'YES'
    UNION ALL SELECT 'correlation_id',                  'char',     'YES'
    UNION ALL SELECT 'event_id',                        'char',     'YES'
    UNION ALL SELECT 'received_at',                      'datetime', 'YES'
    UNION ALL SELECT 'perfil_firma',                      'varchar',  'YES'
    UNION ALL SELECT 'algoritmo_hash',                    'varchar',  'YES'
    UNION ALL SELECT 'resultado_firma_id',                'char',     'YES'
    UNION ALL SELECT 'evidencia_id',                      'varchar',  'YES'
    UNION ALL SELECT 'certificado_serial',                'varchar',  'YES'
    UNION ALL SELECT 'certificado_thumbprint',            'varchar',  'YES'
    UNION ALL SELECT 'certificado_subject',               'varchar',  'YES'
    UNION ALL SELECT 'certificado_issuer',                'varchar',  'YES'
    UNION ALL SELECT 'certificado_vigencia_desde',        'datetime', 'YES'
    UNION ALL SELECT 'certificado_vigencia_hasta',        'datetime', 'YES'
    UNION ALL SELECT 'resultado_cadena',                  'smallint', 'YES'
    UNION ALL SELECT 'fuente_revocacion',                 'smallint', 'YES'
    UNION ALL SELECT 'resultado_revocacion',               'smallint', 'YES'
    UNION ALL SELECT 'tsa_nombre',                        'varchar',  'YES'
    UNION ALL SELECT 'tsa_instante',                      'datetime', 'YES'
    UNION ALL SELECT 'resultado_validacion',               'smallint', 'YES'
    UNION ALL SELECT 'version_validador',                  'varchar',  'YES'
) esp
LEFT JOIN information_schema.columns c
    ON c.table_schema = DATABASE()
   AND c.table_name = 'fal_documento_firma'
   AND c.column_name = esp.columna;

-- 2b. algoritmo_hash: nullable, sin DEFAULT (DEC-EDA-029; nunca se completa
--     retroactivamente un valor no demostrado para filas históricas). Nota
--     MariaDB 12.3.2: para una columna NULLABLE sin cláusula DEFAULT
--     explícita, information_schema.columns.column_default no es SQL NULL:
--     es el literal de 4 caracteres 'NULL' (verificado con
--     HEX(column_default) = 4E554C4C). Comparar contra ambas formas para no
--     reportar una divergencia falsa por este comportamiento del motor.
SELECT
    column_default,
    CASE WHEN column_default IS NULL OR column_default = 'NULL' THEN 'OK' ELSE 'DIVERGENCIA_DEFAULT' END AS resultado
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'fal_documento_firma'
  AND column_name = 'algoritmo_hash';

-- 2c. perfil_firma: único valor no nulo admitido PAdES-B-LT (chk_firma_perfil_unico)
SELECT
    check_clause,
    CASE WHEN check_clause LIKE '%PAdES-B-LT%' THEN 'OK' ELSE 'DIVERGENCIA' END AS resultado
FROM information_schema.check_constraints
WHERE constraint_schema = DATABASE()
  AND table_name = 'fal_documento_firma'
  AND constraint_name = 'chk_firma_perfil_unico';

-- ============================================================================
-- SECCIÓN 2bis — fal_documento: revision_artefacto (DEC-EDA-024)
-- ============================================================================

SELECT
    esp.columna, esp.tipo_esperado, esp.nullable_esperado, esp.default_esperado,
    c.data_type, c.is_nullable, c.column_default,
    CASE
        WHEN c.column_name IS NULL THEN 'FALTANTE'
        WHEN c.is_nullable <> esp.nullable_esperado THEN 'DIVERGENCIA_NULLABLE'
        WHEN c.data_type <> esp.tipo_esperado THEN 'DIVERGENCIA_TIPO'
        WHEN c.column_default <> esp.default_esperado THEN 'DIVERGENCIA_DEFAULT'
        ELSE 'OK'
    END AS resultado
FROM (
    SELECT 'revision_artefacto' AS columna, 'int' AS tipo_esperado, 'NO' AS nullable_esperado, '0' AS default_esperado
) esp
LEFT JOIN information_schema.columns c
    ON c.table_schema = DATABASE()
   AND c.table_name = 'fal_documento'
   AND c.column_name = esp.columna;

-- ============================================================================
-- SECCIÓN 3 — Tablas nuevas del bus: conteo de columnas por tabla (paridad
--              de cardinalidad; el detalle tipo-a-tipo está en el postcheck
--              estructural, sección 3c). Modelo mínimo aprobado: bus_evento
--              (único objeto central) + Outbox/checkpoint/Inbox/Inbox error
--              locales de Faltas.
-- ============================================================================

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

-- 3b. payload es JSON nativo (no LONGTEXT con CHECK), en fal_outbox,
--     bus_evento y fal_inbox
SELECT
    table_name, column_name, data_type,
    CASE WHEN data_type = 'longtext' THEN 'OK_MARIADB_ALIAS_JSON' ELSE data_type END AS detalle
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('fal_outbox', 'bus_evento', 'fal_inbox')
  AND column_name = 'payload';
-- Nota: en MariaDB, JSON es un alias de LONGTEXT con un CHECK implícito de
-- validez JSON; 'longtext' en data_type con json_valid() en generation_expression
-- o CHECK es el resultado esperado, no una divergencia (ver documentación de
-- MariaDB 12.3.2 sobre el tipo JSON).

-- 3c. evento_id (bus_evento, fal_outbox, fal_inbox): BINARY(16), NOT NULL
--     (UUID lógico estable, idéntico en las 3 tablas; base de la
--     idempotencia de publicación por UNIQUE(evento_id) en bus_evento/
--     fal_outbox, y de la idempotencia de ingestión por
--     uq_fal_inbox_evento_id en fal_inbox). bus_evento_id, en cambio, es un
--     cursor técnico AUTO_INCREMENT local a bus_evento, sin significado de
--     identidad lógica.
SELECT
    table_name, data_type, character_maximum_length, is_nullable,
    CASE
        WHEN data_type = 'binary' AND character_maximum_length = 16 AND is_nullable = 'NO' THEN 'OK'
        ELSE 'DIVERGENCIA'
    END AS resultado
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('bus_evento', 'fal_outbox', 'fal_inbox')
  AND column_name = 'evento_id';

-- 3c-envelope. Columnas de envelope agregadas a fal_inbox (evento_id,
--     productor_codigo, schema_version, correlation_id, causation_id,
--     ocurrido_en): mismo tipo que su columna homóloga en bus_evento, para
--     que el reader pueda copiar el envelope completo sin transformación.
SELECT
    esp.columna, esp.tipo_esperado, esp.nullable_esperado,
    c.data_type, c.is_nullable,
    CASE
        WHEN c.column_name IS NULL THEN 'FALTANTE'
        WHEN c.is_nullable <> esp.nullable_esperado THEN 'DIVERGENCIA_NULLABLE'
        WHEN c.data_type <> esp.tipo_esperado THEN 'DIVERGENCIA_TIPO'
        ELSE 'OK'
    END AS resultado
FROM (
    SELECT 'productor_codigo' AS columna, 'varchar'  AS tipo_esperado, 'NO'  AS nullable_esperado
    UNION ALL SELECT 'schema_version',    'smallint', 'NO'
    UNION ALL SELECT 'correlation_id',    'binary',   'YES'
    UNION ALL SELECT 'causation_id',      'binary',   'YES'
    UNION ALL SELECT 'ocurrido_en',       'datetime', 'NO'
) esp
LEFT JOIN information_schema.columns c
    ON c.table_schema = DATABASE()
   AND c.table_name = 'fal_inbox'
   AND c.column_name = esp.columna;

-- 3c-unique. uq_fal_inbox_evento_id existe y es UNIQUE sobre evento_id
--     (idempotencia de ingestión: un evento_id se inserta a lo sumo una vez
--     en fal_inbox, sin importar cuántas veces el reader relea el mismo
--     rango de bus_evento_id)
SELECT
    index_name, non_unique,
    CASE WHEN non_unique = 0 THEN 'OK' ELSE 'DIVERGENCIA_NO_ES_UNIQUE' END AS resultado
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'fal_inbox'
  AND index_name = 'uq_fal_inbox_evento_id'
  AND seq_in_index = 1;

-- 3c-bis. productor_codigo (bus_evento): VARCHAR(8), NOT NULL, sin CHECK IN
--         cerrado (firma-eda-physical-model.md §3: la validación de valores
--         vigentes vive en el enum/constante de cada dominio, no en el
--         esquema, para no requerir un ALTER TABLE por cada aplicación
--         municipal futura)
SELECT
    data_type, character_maximum_length, is_nullable,
    CASE
        WHEN data_type = 'varchar' AND character_maximum_length = 8 AND is_nullable = 'NO' THEN 'OK'
        ELSE 'DIVERGENCIA'
    END AS resultado
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'bus_evento'
  AND column_name = 'productor_codigo';

-- 3d. Motor/charset/collation de las 6 tablas nuevas del bus coincide con el
--     baseline
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

-- ============================================================================
-- SECCIÓN 4 — Ninguna columna preexistente de fal_documento_firma_req /
--              fal_documento_firma / fal_documento cambió de tipo
--              (paridad de no-regresión)
-- ============================================================================

SELECT
    esp.columna, esp.tabla, esp.tipo_esperado,
    c.data_type,
    CASE WHEN c.data_type = esp.tipo_esperado THEN 'OK' ELSE 'DIVERGENCIA_REGRESION' END AS resultado
FROM (
    SELECT 'fal_documento_firma_req' AS tabla, 'id' AS columna, 'bigint' AS tipo_esperado
    UNION ALL SELECT 'fal_documento_firma_req', 'id_documento', 'bigint'
    UNION ALL SELECT 'fal_documento_firma_req', 'seq_firma_req', 'smallint'
    UNION ALL SELECT 'fal_documento_firma_req', 'estado_firma_req', 'smallint'
    UNION ALL SELECT 'fal_documento_firma', 'id', 'bigint'
    UNION ALL SELECT 'fal_documento_firma', 'id_documento', 'bigint'
    UNION ALL SELECT 'fal_documento_firma', 'hash_firma', 'varchar'
    UNION ALL SELECT 'fal_documento_firma', 'estado_firma', 'smallint'
    UNION ALL SELECT 'fal_documento', 'id', 'bigint'
    UNION ALL SELECT 'fal_documento', 'hash_docu', 'varchar'
) esp
LEFT JOIN information_schema.columns c
    ON c.table_schema = DATABASE()
   AND c.table_name = esp.tabla
   AND c.column_name = esp.columna;

-- ============================================================================
-- SECCIÓN 5 — Sin rutinas SQL SECURITY DEFINER ni vistas diagnósticas
--              centrales que verificar (modelo mínimo aprobado). Confirma
--              explícitamente su ausencia, para detectar una reintroducción
--              accidental de la arquitectura eliminada.
-- ============================================================================

SELECT
    COUNT(*) AS rutinas_bus_presentes,
    0        AS rutinas_bus_esperadas,
    CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'DIVERGENCIA_RUTINA_REINTRODUCIDA' END AS resultado
FROM information_schema.routines
WHERE routine_schema = DATABASE()
  AND (routine_name LIKE 'sp\_bus\_%' OR routine_name = 'sp_bus_admin_suscripcion');

SELECT
    COUNT(*) AS vistas_bus_presentes,
    0        AS vistas_bus_esperadas,
    CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'DIVERGENCIA_VISTA_REINTRODUCIDA' END AS resultado
FROM information_schema.views
WHERE table_schema = DATABASE()
  AND table_name LIKE 'v\_bus\_%';

-- ============================================================================
-- SECCIÓN 6 — Sin tablas de fan-out central (bus_evento_entrega,
--              bus_evento_entrega_historial, bus_suscripcion,
--              bus_publicacion_permiso) que verificar (modelo mínimo
--              aprobado). Confirma explícitamente su ausencia, para detectar
--              una reintroducción accidental de la arquitectura eliminada.
-- ============================================================================

SELECT
    table_name AS tabla_eliminada_reintroducida,
    'DIVERGENCIA_TABLA_REINTRODUCIDA' AS resultado
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
      'bus_evento_entrega', 'bus_evento_entrega_historial',
      'bus_suscripcion', 'bus_publicacion_permiso');
-- Esperado: 0 filas.

-- ============================================================================
-- FIN DEL PARITY CHECK
-- ============================================================================
