-- verify-firmas-eda-bus-principals-precheck.sql
-- SOLO LECTURA — No altera ningún objeto. Solo SELECT sobre information_schema
-- y mysql.user/mysql.global_priv (metadatos de cuentas, ningún dato personal).
--
-- Propósito: verificar, ANTES de ejecutar database/ddl/grants-firmas-eda-bus.sql,
--            que los 5 usuarios MariaDB de mínimo privilegio del bus central ya
--            existen, aprovisionados fuera de este repositorio (secret
--            manager/pipeline de la plataforma), con host permitido y
--            REQUIRE SSL (DEC-EDA-040bis). Modelo mínimo aprobado: sin
--            bus_definer, sin rutinas SQL SECURITY DEFINER, sin vistas
--            diagnósticas centrales (database/README.md "Orden operativo").
-- Motor    : MariaDB 12.3.2, sb_faltas_db
-- Trabajo  : SPEC-FIRMAS-EDA-CLOSURE-001
-- Uso      : ejecutar manualmente desde HeidiSQL/mysql con un usuario con
--            privilegio SELECT sobre mysql.user (ej. root o un usuario con
--            SELECT en el esquema mysql). Si cualquier fila reporta
--            'BLOQUEADO_*' o 'FALTA_USUARIO', NO ejecutar
--            grants-firmas-eda-bus.sql hasta resolverlo.
--
-- Este script NUNCA crea, modifica ni elimina usuarios: la existencia de estos
-- 5 usuarios es responsabilidad exclusiva del secret manager/pipeline de la
-- plataforma, no de este paquete DDL.
--
-- grants-firmas-eda-bus.sql otorga privilegios explícitamente a la identidad
-- exacta `'usuario'@'%'` (ver ese script, secciones 01-05). Las secciones 1-3
-- de este precheck exigen esa MISMA identidad exacta (usuario Y host), no
-- solo el nombre de usuario: en MariaDB, `'bus_publisher_faltas'@'10.0.0.5'`
-- y `'bus_publisher_faltas'@'%'` son cuentas distintas con privilegios
-- independientes; verificar solo el nombre permitiría que una cuenta
-- existente en un host distinto de `%` pasara el precheck aunque el `GRANT`
-- real (dirigido a `%`) fuera a una cuenta que no existe.
-- ============================================================================

-- ============================================================================
-- SECCIÓN 1 — Las 5 cuentas MariaDB del bus central existen EXACTAMENTE como
--              'usuario'@'%' (identidad completa usuario+host)
-- ============================================================================

SELECT
    u_esp.usuario_esperado,
    '%' AS host_esperado,
    CASE WHEN u.user IS NOT NULL THEN 'OK_EXISTE' ELSE 'FALTA_USUARIO' END AS resultado
FROM (
    SELECT 'bus_publisher_faltas' AS usuario_esperado
    UNION ALL SELECT 'bus_publisher_firmas'
    UNION ALL SELECT 'bus_consumer_faltas'
    UNION ALL SELECT 'bus_consumer_firmas'
    UNION ALL SELECT 'bus_maintenance'
) u_esp
LEFT JOIN mysql.user u
    ON u.user = u_esp.usuario_esperado AND u.host = '%'
GROUP BY u_esp.usuario_esperado, u.user;

-- ============================================================================
-- SECCIÓN 2 — Informativo: TODOS los hosts existentes para estos nombres de
--              usuario, incluidos los que NO sean '%' (una cuenta con el
--              mismo nombre en otro host es ruido operativo a revisar
--              manualmente, no la cuenta que grants-firmas-eda-bus.sql usa)
-- ============================================================================

SELECT
    user AS usuario, host AS host_existente,
    CASE WHEN host = '%' THEN 'ES_LA_CUENTA_ESPERADA' ELSE 'INFORMATIVO_OTRO_HOST' END AS resultado
FROM mysql.user
WHERE user IN (
    'bus_publisher_faltas', 'bus_publisher_firmas',
    'bus_consumer_faltas', 'bus_consumer_firmas', 'bus_maintenance');

-- ============================================================================
-- SECCIÓN 3 — REQUIRE SSL configurado, evaluado exclusivamente sobre
--              'usuario'@'%' (la misma identidad exacta de la sección 1).
--              MariaDB 10.4+ mantiene mysql.user.ssl_type por compatibilidad
--              (vista sobre mysql.global_priv); 'NONE' o vacío indica que la
--              cuenta NO exige TLS. Si el servidor no expone ssl_type
--              (variante muy reciente sin la vista de compatibilidad),
--              consultar manualmente JSON_VALUE(Priv, '$.ssl_type') sobre
--              mysql.global_priv en su lugar; este script no asume cuál de
--              las dos formas aplica.
-- ============================================================================

SELECT
    user AS usuario, host AS host_permitido, ssl_type,
    CASE
        WHEN host <> '%' THEN 'IGNORADO_HOST_DISTINTO_DE_%'
        WHEN ssl_type IS NULL OR ssl_type = '' OR ssl_type = 'NONE' THEN 'BLOQUEADO_SIN_REQUIRE_SSL'
        ELSE 'OK_REQUIRE_SSL'
    END AS resultado
FROM mysql.user
WHERE user IN (
    'bus_publisher_faltas', 'bus_publisher_firmas',
    'bus_consumer_faltas', 'bus_consumer_firmas', 'bus_maintenance');

-- ============================================================================
-- SECCIÓN 4 — Ningún grant directo del bus otorgado todavía (idempotencia
--              de grants-firmas-eda-bus.sql; evita re-otorgar sobre un estado
--              ya conforme sin revisión)
-- ============================================================================

SELECT
    grantee, table_name AS objeto, privilege_type,
    'INFORMATIVO_GRANT_YA_PRESENTE' AS resultado
FROM information_schema.table_privileges
WHERE table_schema = DATABASE()
  AND grantee LIKE '%bus_%';

-- ============================================================================
-- SECCIÓN 5 — Precondición: bus_evento (única tabla central del bus) ya
--              existe (grants-firmas-eda-bus.sql otorga INSERT/SELECT/DELETE
--              directos sobre ella; sin objeto no hay grant válido). Modelo
--              mínimo aprobado: sin rutinas ni vistas que precondicionar.
-- ============================================================================

SELECT
    table_name,
    CASE WHEN table_name IS NOT NULL THEN 'OK' ELSE 'FALTANTE' END AS resultado
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'bus_evento';

SELECT
    COUNT(*) AS tabla_bus_evento_presente,
    1        AS tabla_bus_evento_esperada,
    CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE 'BLOQUEADO_FALTA_BUS_EVENTO' END AS resultado
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'bus_evento';

-- ============================================================================
-- SECCIÓN 6 — VEREDICTO GLOBAL ÚNICO (READY/BLOCKED)
--              Consolida las secciones 1, 3 y 5 en una única fila: si
--              cualquiera de las 5 cuentas 'usuario'@'%' falta, no exige
--              REQUIRE SSL, o si falta bus_evento, el veredicto es BLOCKED.
--              Esta es la única fila que debe leerse para decidir si
--              grants-firmas-eda-bus.sql puede ejecutarse; las secciones 1-5
--              quedan como detalle diagnóstico.
-- ============================================================================

SELECT
    (SELECT COUNT(*) FROM mysql.user
       WHERE user IN ('bus_publisher_faltas', 'bus_publisher_firmas',
                       'bus_consumer_faltas', 'bus_consumer_firmas', 'bus_maintenance')
         AND host = '%') AS cuentas_pct_existentes,
    5 AS cuentas_pct_esperadas,
    (SELECT COUNT(*) FROM mysql.user
       WHERE user IN ('bus_publisher_faltas', 'bus_publisher_firmas',
                       'bus_consumer_faltas', 'bus_consumer_firmas', 'bus_maintenance')
         AND host = '%'
         AND ssl_type IS NOT NULL AND ssl_type <> '' AND ssl_type <> 'NONE') AS cuentas_con_require_ssl,
    (SELECT COUNT(*) FROM information_schema.tables
       WHERE table_schema = DATABASE() AND table_name = 'bus_evento') AS tabla_bus_evento_presente,
    1 AS tabla_bus_evento_esperada,
    CASE WHEN
        (SELECT COUNT(*) FROM mysql.user
           WHERE user IN ('bus_publisher_faltas', 'bus_publisher_firmas',
                           'bus_consumer_faltas', 'bus_consumer_firmas', 'bus_maintenance')
             AND host = '%') = 5
        AND
        (SELECT COUNT(*) FROM mysql.user
           WHERE user IN ('bus_publisher_faltas', 'bus_publisher_firmas',
                           'bus_consumer_faltas', 'bus_consumer_firmas', 'bus_maintenance')
             AND host = '%'
             AND ssl_type IS NOT NULL AND ssl_type <> '' AND ssl_type <> 'NONE') = 5
        AND
        (SELECT COUNT(*) FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'bus_evento') = 1
    THEN 'READY' ELSE 'BLOCKED' END AS veredicto_global;

-- ============================================================================
-- FIN DEL PRECHECK — Principals del bus central
-- ============================================================================
