package ar.gob.malvinas.faltas.core.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utilidades compartidas para los guardrails estáticos del DDL del dominio BOD Faltas.
 *
 * <p>No contiene tests. Solo helpers de lectura y parseo de archivos.
 */
final class DdlTestSupport {

    /** Conjunto de objetos protegidos del baseline (no deben aparecer en DDL ejecutable). */
    static final Set<String> OBJETOS_PROTEGIDOS = Set.of(
            "fal_informix_sync_error", "fal_informix_sync_run", "fal_rubro_version",
            "geo_bahra_asentamiento", "geo_calle_alturas_barrio", "geo_dataset_load_error",
            "geo_dataset_load_run", "geo_dataset_row_version", "geo_ign_departamento",
            "geo_ign_municipio", "geo_ign_provincia", "geo_indec_calles",
            "geo_indec_localidad", "geo_indec_localidad_censal",
            "geo_malv_calle_version", "geo_malv_localidad_version",
            "vw_fal_rubro_actual", "vw_geo_malv_calle_actual",
            "vw_geo_malv_localidad_actual", "vw_geo_municipio_departamento"
    );

    private DdlTestSupport() {}

    /**
     * Resuelve la ruta a {@code src/main/java} desde el working directory de Maven
     * ({@code backend/api-faltas-core/}). Lanza {@link IllegalStateException} si no existe.
     */
    static Path resolveSrcMainJava() {
        Path candidato = Paths.get("src", "main", "java").normalize();
        if (!Files.isDirectory(candidato)) {
            throw new IllegalStateException(
                    "No se encontro 'src/main/java/' desde el working directory Maven: "
                            + Paths.get("").toAbsolutePath());
        }
        return candidato;
    }

    /**
     * Resuelve la ruta al directorio {@code database/} desde el working directory de Maven
     * ({@code backend/api-faltas-core/}). Lanza {@link IllegalStateException} si no existe.
     */
    static Path resolveDbRoot() {
        Path candidato = Paths.get("..", "..", "database").normalize();
        if (!Files.isDirectory(candidato)) {
            throw new IllegalStateException(
                    "No se encontró 'database/' desde el working directory Maven: "
                            + Paths.get("").toAbsolutePath()
                            + ". Esperado en ../../database relativo a backend/api-faltas-core.");
        }
        return candidato;
    }

    /**
     * Resuelve la ruta a {@code docs/spec-as-source/} desde el working directory de Maven
     * ({@code backend/api-faltas-core/}). Lanza {@link IllegalStateException} si no existe.
     */
    static Path resolveDocsRoot() {
        Path candidato = Paths.get("docs", "spec-as-source").normalize();
        if (!Files.isDirectory(candidato)) {
            throw new IllegalStateException(
                    "No se encontró 'docs/spec-as-source/' desde el working directory Maven: "
                            + Paths.get("").toAbsolutePath());
        }
        return candidato;
    }

    /** Lee un archivo en UTF-8. */
    static String leer(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    /** Elimina líneas de comentario SQL (-- ...) del contenido dado. */
    static String quitarComentariosLinea(String sql) {
        return sql.lines()
                .filter(l -> !l.strip().startsWith("--"))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Extrae bloques CREATE TABLE del DDL canónico (sin comentarios de línea).
     * Cada bloque va desde CREATE TABLE hasta antes del siguiente CREATE TABLE.
     */
    static List<String> extraerBloquesDdl(String sql) {
        List<String> bloques = new ArrayList<>();
        String sinComentarios = quitarComentariosLinea(sql);
        String[] partes = sinComentarios.split("(?i)(?=CREATE\\s+TABLE\\s+)");
        for (String parte : partes) {
            if (parte.strip().toUpperCase().startsWith("CREATE TABLE")) {
                bloques.add(parte.strip());
            }
        }
        return bloques;
    }

    /** Extrae el nombre de tabla de un bloque CREATE TABLE. */
    static String extraerNombreTabla(String bloque) {
        Matcher m = Pattern.compile("(?i)CREATE\\s+TABLE\\s+(\\w+)").matcher(bloque);
        return m.find() ? m.group(1).toLowerCase() : "";
    }

    /**
     * Extrae el cuerpo entre el primer '(' y el ')' que precede a ENGINE.
     */
    static String extraerCuerpo(String bloque) {
        int open = bloque.indexOf('(');
        if (open < 0) return "";
        Matcher m = Pattern.compile("\\)\\s*\\r?\\nENGINE", Pattern.CASE_INSENSITIVE).matcher(bloque);
        if (m.find()) {
            return bloque.substring(open + 1, m.start());
        }
        int depth = 0;
        for (int i = open; i < bloque.length(); i++) {
            char c = bloque.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return bloque.substring(open + 1, i);
            }
        }
        return bloque.substring(open + 1);
    }

    /**
     * Cuenta columnas físicas en el cuerpo de un CREATE TABLE.
     * Una columna es una línea que, tras eliminar whitespace inicial,
     * comienza con letra minúscula (= nombre de columna en snake_case).
     */
    static long contarColumnas(String cuerpo) {
        return cuerpo.lines()
                .filter(l -> !l.strip().isEmpty())
                .filter(l -> !l.strip().startsWith("--"))
                .filter(l -> {
                    String s = l.strip();
                    return !s.isEmpty() && s.charAt(0) >= 'a' && s.charAt(0) <= 'z';
                })
                .count();
    }

    /**
     * Cuenta COMMENTs de columna en el cuerpo de un CREATE TABLE.
     *
     * <p>Los COMMENTs de columna pueden ser inline (al final de la línea de columna)
     * o en línea propia. Se cuentan las líneas de columna física que contienen COMMENT.
     */
    static long contarComentariosColumna(String cuerpo) {
        return cuerpo.lines()
                .filter(l -> !l.strip().isEmpty())
                .filter(l -> !l.strip().startsWith("--"))
                .filter(l -> {
                    String s = l.strip();
                    // Línea de columna: empieza con letra minúscula (snake_case)
                    return !s.isEmpty() && s.charAt(0) >= 'a' && s.charAt(0) <= 'z';
                })
                .filter(l -> {
                    String upper = l.toUpperCase();
                    return upper.contains("COMMENT '") || upper.contains("COMMENT \"");
                })
                .count();
    }

    /** Registra la info esencial de una fila del inventario canónico. */
    record FilaInventario(int numero, String nombre, String estado) {}

    /**
     * Parsea las filas del inventario canónico.
     * Formato: | N | `nombre` | ESTADO | ...
     */
    static List<FilaInventario> parsearInventario(String contenido) {
        List<FilaInventario> filas = new ArrayList<>();
        Pattern p = Pattern.compile(
                "\\|\\s*(\\d+)\\s*\\|\\s*`([^`]+)`\\s*\\|\\s*(PREEXISTING_CANONICAL_ADOPTED|TO_CREATE)\\s*\\|");
        Matcher m = p.matcher(contenido);
        while (m.find()) {
            filas.add(new FilaInventario(
                    Integer.parseInt(m.group(1)),
                    m.group(2),
                    m.group(3)));
        }
        return filas;
    }

    /**
     * Extrae nombres de tablas del modelo lógico MariaDB.
     * Formato canónico: #### `nombre_tabla`
     */
    static Set<String> parsearTablasModeloLogico(String contenido) {
        Set<String> tablas = new LinkedHashSet<>();
        Matcher m = Pattern.compile("^#### `([^`]+)`", Pattern.MULTILINE).matcher(contenido);
        while (m.find()) {
            tablas.add(m.group(1));
        }
        return tablas;
    }

    /** Par (tabla, columnas) normalizado: columnas en orden, separadas por coma sin espacios. */
    record UniqueForma(String tabla, String columnas) {

        /** Clave estable para comparar por identidad semántica (tabla + columnas + orden). */
        String clave() {
            return tabla + "|" + columnas;
        }
    }

    /**
     * Extrae las 33 declaraciones {@code UNIQUE KEY nombre (columnas)} de
     * {@code create-bod-faltas-domain.sql} (MARIADB-BASELINE-DIAGNOSTIC-FIX-001, guardrail DIAG-G21).
     *
     * <p>Recorre cada bloque {@code CREATE TABLE} y asocia cada UNIQUE KEY a la tabla que lo declara,
     * normalizando la lista de columnas (orden de aparición, sin espacios).
     */
    static List<UniqueForma> extraerUniqueKeysDdl(String ddlSinComentarios) {
        List<UniqueForma> resultado = new ArrayList<>();
        Pattern pUnique = Pattern.compile("UNIQUE\\s+KEY\\s+\\w+\\s*\\(([^)]+)\\)");
        for (String bloque : extraerBloquesDdl(ddlSinComentarios)) {
            String tabla = extraerNombreTabla(bloque);
            Matcher m = pUnique.matcher(bloque);
            while (m.find()) {
                String columnas = m.group(1).replaceAll("\\s+", "");
                resultado.add(new UniqueForma(tabla, columnas));
            }
        }
        return resultado;
    }

    /**
     * Extrae la lista de UNIQUE esperadas declarada en el CTE {@code esperadas} de la
     * Sección 10 de {@code verify-domain-schema.sql} (MARIADB-BASELINE-DIAGNOSTIC-FIX-001).
     *
     * <p>Formato de cada fila del CTE:
     * {@code SELECT 'tabla' AS tabla_esp, 'columnas' AS columnas_esp} o
     * {@code UNION ALL SELECT 'tabla', 'columnas'}. Se acota la búsqueda al bloque
     * {@code esperadas AS ( ... )} para no capturar otras listas de tablas del script.
     */
    static List<UniqueForma> extraerUniqueEsperadasDiagnostico(String diagnosticoSinComentarios) {
        int inicio = diagnosticoSinComentarios.indexOf("esperadas AS (");
        if (inicio < 0) {
            throw new IllegalStateException("No se encontró el CTE 'esperadas AS (' en verify-domain-schema.sql");
        }
        int finBloque = diagnosticoSinComentarios.indexOf("\nreales AS (", inicio);
        if (finBloque < 0) {
            throw new IllegalStateException("No se encontró el cierre del CTE 'esperadas' (antes de 'reales AS (')");
        }
        String bloque = diagnosticoSinComentarios.substring(inicio, finBloque);
        List<UniqueForma> resultado = new ArrayList<>();
        Matcher m = Pattern.compile("SELECT\\s+'([a-z0-9_]+)'(?:\\s+AS\\s+tabla_esp)?\\s*,\\s*'([a-z0-9_,]+)'")
                .matcher(bloque);
        while (m.find()) {
            resultado.add(new UniqueForma(m.group(1), m.group(2)));
        }
        return resultado;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHECK constraints (MARIADB-BASELINE-CHECK-DIAGNOSTIC-FIX-002)
    // ─────────────────────────────────────────────────────────────────────────

    /** Declaración cruda {@code CONSTRAINT nombre CHECK (expresión)} tal como aparece en el DDL. */
    record CheckForma(String tabla, String nombre, String clausula) {}

    /**
     * Extrae las declaraciones {@code CONSTRAINT nombre CHECK (...)} de
     * {@code create-bod-faltas-domain.sql}, asociando cada una a su tabla.
     *
     * <p>Usa un escáner de paréntesis balanceados (no una regex codiciosa/perezosa) porque
     * varias cláusulas del DDL contienen paréntesis internos (ej. {@code chk_num_talonario_nro},
     * {@code chk_dia_nc_ref_ext}); una regex simple {@code \((.*?)\)} cortaría en el primer
     * paréntesis de cierre interno, no en el que realmente cierra el CHECK. Omite el contenido
     * de literales de cadena simple-comilla al contar profundidad (irrelevante para el DDL
     * actual, pero evita falsos cierres si algún literal futuro contuviera paréntesis).
     */
    static List<CheckForma> extraerCheckConstraintsDdl(String ddlSinComentarios) {
        List<CheckForma> resultado = new ArrayList<>();
        Pattern pInicio = Pattern.compile("CONSTRAINT\\s+(\\w+)\\s+CHECK\\s*\\(");
        for (String bloque : extraerBloquesDdl(ddlSinComentarios)) {
            String tabla = extraerNombreTabla(bloque);
            Matcher m = pInicio.matcher(bloque);
            while (m.find()) {
                String nombre = m.group(1);
                int open = m.end() - 1;
                int depth = 0;
                boolean enLiteral = false;
                int cierre = -1;
                for (int i = open; i < bloque.length(); i++) {
                    char c = bloque.charAt(i);
                    if (enLiteral) {
                        if (c == '\'') {
                            if (i + 1 < bloque.length() && bloque.charAt(i + 1) == '\'') {
                                i++;
                            } else {
                                enLiteral = false;
                            }
                        }
                        continue;
                    }
                    if (c == '\'') {
                        enLiteral = true;
                    } else if (c == '(') {
                        depth++;
                    } else if (c == ')') {
                        depth--;
                        if (depth == 0) {
                            cierre = i;
                            break;
                        }
                    }
                }
                if (cierre < 0) {
                    throw new IllegalStateException(
                            "CHECK sin paréntesis de cierre balanceado para " + nombre
                                    + " en tabla " + tabla);
                }
                resultado.add(new CheckForma(tabla, nombre, bloque.substring(open + 1, cierre)));
            }
        }
        return resultado;
    }

    /**
     * Normaliza una cláusula CHECK para comparación de identidad semántica
     * (Sección 12 de {@code verify-domain-schema.sql}, MARIADB-BASELINE-CHECK-DIAGNOSTIC-FIX-002):
     *
     * <ul>
     *   <li>elimina backticks de identificadores;</li>
     *   <li>aplica minúsculas SOLO fuera de literales de cadena simple-comilla, preservando el
     *       case exacto de literales sensibles (ej. patrones REGEXP como {@code 'LC-...'});</li>
     *   <li>elimina el espacio posterior a una coma en listas {@code IN (...)} fuera de
     *       literales (MariaDB reconstruye {@code IN (1, 2)} del DDL como {@code in (1,2)} en
     *       {@code information_schema.check_constraints.check_clause}; sin esta normalización
     *       la cláusula extraída del DDL nunca igualaría a la cláusula real ni a la declarada
     *       en el CTE {@code esperados_check}, que ya usa la forma canónica sin espacio);</li>
     *   <li>elimina todos los paréntesis fuera de literales. MariaDB elimina paréntesis
     *       redundantes por precedencia al reconstruir {@code check_clause} — verificado
     *       materialmente contra sb_faltas_db para {@code chk_dia_nc_ref_ext}: el DDL declara
     *       {@code (origen = 1 AND referencia_externa IS NULL) OR (origen = 2 AND ...)} pero la
     *       cláusula real almacenada NO tiene esos paréntesis. Reproducir en Java el algoritmo
     *       exacto de eliminación de paréntesis redundantes por precedencia (respetando además
     *       la sintaxis {@code BETWEEN x AND y}, donde "AND" no es un conector booleano)
     *       requeriría un parser de expresiones booleanas completo. Para el propósito de este
     *       guardrail — detectar drift real de columna/operador/valor/constraint entre el DDL y
     *       el CTE {@code esperados_check}, no validar equivalencia formal de precedencia —
     *       basta con una clave de comparación que ignore paréntesis en ambos lados por igual.
     *       Esta función es solo una clave de igualdad interna para tests; no reproduce el
     *       texto exacto que devuelve MariaDB (para eso, ver la Sección 12 de
     *       verify-domain-schema.sql, que sí preserva paréntesis porque compara cláusula real
     *       contra cláusula real, sin necesidad de reconciliar el texto crudo del DDL).</li>
     *   <li>colapsa espacios/saltos de línea consecutivos a uno solo y recorta los extremos.</li>
     * </ul>
     *
     * <p>No es un parser SQL general: es un escáner mínimo y determinístico acotado a las
     * construcciones presentes en {@code create-bod-faltas-domain.sql} (identificadores,
     * operadores, {@code IN}/{@code BETWEEN}/{@code IS [NOT] NULL}/{@code REGEXP}, literales
     * simple-comilla con escape {@code ''}).
     */
    static String normalizarClausulaCheck(String clausula) {
        StringBuilder sb = new StringBuilder(clausula.length());
        boolean enLiteral = false;
        boolean saltarEspacioTrasComa = false;
        for (int i = 0; i < clausula.length(); i++) {
            char c = clausula.charAt(i);
            if (enLiteral) {
                sb.append(c);
                if (c == '\'') {
                    if (i + 1 < clausula.length() && clausula.charAt(i + 1) == '\'') {
                        sb.append('\'');
                        i++;
                    } else {
                        enLiteral = false;
                    }
                }
                continue;
            }
            if (c == '`' || c == '(' || c == ')') {
                continue;
            }
            if (c == '\'') {
                enLiteral = true;
                sb.append(c);
                saltarEspacioTrasComa = false;
                continue;
            }
            if (saltarEspacioTrasComa && Character.isWhitespace(c)) {
                continue;
            }
            saltarEspacioTrasComa = false;
            if (c == ',') {
                sb.append(',');
                saltarEspacioTrasComa = true;
                continue;
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * Extrae el listado de tablas incluidas en la cláusula
     * {@code WHERE tc.table_name IN (...)} del CTE {@code reales_check} de la Sección 12 de
     * {@code verify-domain-schema.sql} (MARIADB-BASELINE-CHECK-DIAGNOSTIC-FIX-002).
     *
     * <p>El DDL ({@code create-bod-faltas-domain.sql}, DDL-MARIADB-MANUAL-001-FULL-R1) declara 51
     * {@code CONSTRAINT ... CHECK} en 64 tablas, pero la Sección 12 solo verifica 36 de ellos en
     * un subconjunto acotado de tablas — alcance heredado y fuera del alcance de este slice
     * (que corrige el falso positivo {@code CLAUSULA_DIVERGENTE}, no amplía cobertura). Este
     * helper permite que los guardrails de paridad comparen el DDL contra el mismo subconjunto
     * que la Sección 12 realmente verifica, en vez de exigir cobertura total de las 51 CHECK.
     */
    static Set<String> extraerTablasIncluidasCheckDiagnostico(String diagnosticoSinComentarios) {
        int inicioCte = diagnosticoSinComentarios.indexOf("reales_check AS (");
        if (inicioCte < 0) {
            throw new IllegalStateException("No se encontró el CTE 'reales_check AS (' en verify-domain-schema.sql");
        }
        int inicioIn = diagnosticoSinComentarios.indexOf("tc.table_name IN (", inicioCte);
        if (inicioIn < 0) {
            throw new IllegalStateException(
                    "No se encontró 'tc.table_name IN (' dentro del CTE 'reales_check' en verify-domain-schema.sql");
        }
        int open = diagnosticoSinComentarios.indexOf('(', inicioIn);
        int close = diagnosticoSinComentarios.indexOf(')', open);
        if (open < 0 || close < 0) {
            throw new IllegalStateException("No se pudo delimitar la lista IN (...) de tc.table_name");
        }
        String listado = diagnosticoSinComentarios.substring(open + 1, close);
        Set<String> tablas = new LinkedHashSet<>();
        Matcher m = Pattern.compile("'([a-z0-9_]+)'").matcher(listado);
        while (m.find()) {
            tablas.add(m.group(1));
        }
        return tablas;
    }

    /** Fila esperada {@code (tabla, nombre, cláusula)} declarada en el CTE {@code esperados_check}. */
    record CheckEsperada(String tabla, String nombre, String clausula) {}

    /**
     * Extrae la lista de CHECK esperados declarada en el CTE {@code esperados_check} de la
     * Sección 12 de {@code verify-domain-schema.sql} (MARIADB-BASELINE-CHECK-DIAGNOSTIC-FIX-002).
     *
     * <p>Formato de cada fila del CTE: {@code SELECT 'tabla' AS tabla_esp, 'nombre' AS
     * nombre_esp, 'clausula' AS clausula_esp} o {@code UNION ALL SELECT 'tabla', 'nombre',
     * 'clausula'}. El literal de cláusula puede contener comillas simples escapadas ({@code ''})
     * para el patrón REGEXP de {@code chk_fal_lote_codigo}.
     */
    static List<CheckEsperada> extraerCheckEsperadosDiagnostico(String diagnosticoSinComentarios) {
        int inicio = diagnosticoSinComentarios.indexOf("esperados_check AS (");
        if (inicio < 0) {
            throw new IllegalStateException(
                    "No se encontró el CTE 'esperados_check AS (' en verify-domain-schema.sql");
        }
        int finBloque = diagnosticoSinComentarios.indexOf("\nreales_check AS (", inicio);
        if (finBloque < 0) {
            throw new IllegalStateException(
                    "No se encontró el cierre del CTE 'esperados_check' (antes de 'reales_check AS (')");
        }
        String bloque = diagnosticoSinComentarios.substring(inicio, finBloque);
        List<CheckEsperada> resultado = new ArrayList<>();
        Matcher m = Pattern.compile(
                        "SELECT\\s+'([a-z0-9_]+)'(?:\\s+AS\\s+tabla_esp)?\\s*,\\s*"
                                + "'([a-z0-9_]+)'(?:\\s+AS\\s+nombre_esp)?\\s*,\\s*"
                                + "'((?:[^']|'')*)'",
                        Pattern.DOTALL)
                .matcher(bloque);
        while (m.find()) {
            resultado.add(new CheckEsperada(m.group(1), m.group(2), m.group(3).replace("''", "'")));
        }
        return resultado;
    }
}
