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
}
