package ar.gob.malvinas.faltas.core.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ar.gob.malvinas.faltas.core.application.DdlTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardrails del contrato del script DDL canónico.
 *
 * <p>Verifica sin conexión a base de datos que {@code database/ddl/create-bod-faltas-domain.sql}
 * cumple las decisiones cerradas del trabajo DDL-MARIADB-MANUAL-001-FULL-R1.
 *
 * <ul>
 *   <li>DDL-G2: 64 CREATE TABLE únicos, conjunto = TO_CREATE del inventario.</li>
 *   <li>DDL-G3: ENGINE/charset/collation y COMMENT de tabla por bloque.</li>
 *   <li>DDL-G4: sin ORM ni migraciones automáticas.</li>
 *   <li>DDL-G5: todas las columnas de cada bloque tienen COMMENT.</li>
 *   <li>DDL-G6: USE sb_faltas_db exactamente una vez.</li>
 *   <li>DDL-G7: estructura de directorios database/ completa.</li>
 *   <li>DDL-G9: ddl-full-scope.md existe y documenta el scope FULL-R1.</li>
 *   <li>DDL-G10: backend productivo sin clases ORM.</li>
 *   <li>DDL-G11: DATETIME siempre con precisión DATETIME(6).</li>
 *   <li>DDL-G13: release gate aprobado, ausencia de marcas WIP y database/ fuera del classpath.</li>
 *   <li>DDL-G14: ningún objeto protegido es destino de DDL ejecutable.</li>
 *   <li>DDL-G16: conjuntos exactos de enums persistidos declarados como CHECK.</li>
 *   <li>DDL-G17: README menciona 64 tablas y no contiene cadenas obsoletas de slices previos.</li>
 *   <li>DDL-G18: archivos en database/ sin BOM, sin CRLF, con LF final.</li>
 * </ul>
 *
 * <p>No usa Mockito, no usa base de datos. Solo lectura de archivos en {@code database/}.
 */
@DisplayName("Guardrails DDL: contrato del script DDL canónico FULL-R1")
class DdlScriptContractTest {

    private static Path dbRoot;

    @BeforeAll
    static void localizarDatabaseRoot() {
        dbRoot = resolveDbRoot();
    }

    private static Path ddlPath() {
        return dbRoot.resolve("ddl/create-bod-faltas-domain.sql");
    }

    private static List<String> bloquesDdl() throws IOException {
        return extraerBloquesDdl(leer(ddlPath()));
    }

    /** Lee el inventario y extrae las tablas TO_CREATE. */
    private Set<String> toCreateDesdeInventario() throws IOException {
        return parsearInventario(leer(dbRoot.resolve("design/canonical-table-inventory.md")))
                .stream()
                .filter(f -> "TO_CREATE".equals(f.estado()))
                .map(FilaInventario::nombre)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DDL-G2: 64 CREATE TABLE únicos, conjunto = TO_CREATE del inventario")
    class G2_TablasScript {

        @Test
        @DisplayName("create-bod-faltas-domain.sql existe")
        void scriptCanonicoExiste() {
            assertThat(ddlPath()).exists().isRegularFile();
        }

        @Test
        @DisplayName("el script declara exactamente 64 CREATE TABLE")
        void exactamente64CreateTable() throws IOException {
            List<String> bloques = bloquesDdl();
            assertThat(bloques)
                    .as("El script debe tener exactamente 64 CREATE TABLE")
                    .hasSize(64);
        }

        @Test
        @DisplayName("ninguna tabla aparece dos veces en CREATE TABLE")
        void cadaTablaExactamenteUnaVez() throws IOException {
            List<String> nombres = bloquesDdl().stream()
                    .map(DdlTestSupport::extraerNombreTabla)
                    .collect(Collectors.toList());
            Set<String> unicos = new HashSet<>(nombres);
            assertThat(unicos)
                    .as("Existen CREATE TABLE duplicados: " + nombres)
                    .hasSize(nombres.size());
        }

        @Test
        @DisplayName("conjunto de tablas en el script == TO_CREATE del inventario")
        void conjuntoIgualToCreate() throws IOException {
            Set<String> enScript = bloquesDdl().stream()
                    .map(DdlTestSupport::extraerNombreTabla)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Set<String> toCreate = toCreateDesdeInventario();
            assertThat(enScript)
                    .as("Tablas en script pero no en TO_CREATE del inventario: "
                            + enScript.stream().filter(t -> !toCreate.contains(t)).collect(Collectors.toSet())
                            + ". En TO_CREATE pero no en script: "
                            + toCreate.stream().filter(t -> !enScript.contains(t)).collect(Collectors.toSet()))
                    .isEqualTo(toCreate);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DDL-G3: ENGINE, charset, collation y COMMENT de tabla por bloque")
    class G3_MotorYComentario {

        @Test
        @DisplayName("cada bloque especifica ENGINE=InnoDB")
        void especificaInnoDB() throws IOException {
            for (String bloque : bloquesDdl()) {
                String nombre = extraerNombreTabla(bloque);
                String norm = bloque.replaceAll("\\s+", "").toUpperCase();
                assertThat(norm)
                        .as("Tabla " + nombre + " debe especificar ENGINE=InnoDB")
                        .contains("ENGINE=INNODB");
            }
        }

        @Test
        @DisplayName("cada bloque especifica CHARACTER SET=utf8mb4")
        void especificaCharacterSet() throws IOException {
            for (String bloque : bloquesDdl()) {
                String nombre = extraerNombreTabla(bloque);
                String norm = bloque.replaceAll("\\s+", "").toUpperCase();
                assertThat(norm)
                        .as("Tabla " + nombre + " debe especificar CHARACTER SET=utf8mb4")
                        .contains("CHARACTERSET=UTF8MB4");
            }
        }

        @Test
        @DisplayName("cada bloque especifica COLLATE=utf8mb4_uca1400_ai_ci")
        void especificaCollation() throws IOException {
            for (String bloque : bloquesDdl()) {
                String nombre = extraerNombreTabla(bloque);
                assertThat(bloque.toLowerCase())
                        .as("Tabla " + nombre + " debe especificar COLLATE=utf8mb4_uca1400_ai_ci")
                        .contains("utf8mb4_uca1400_ai_ci");
            }
        }

        @Test
        @DisplayName("cada bloque tiene exactamente un COMMENT de tabla no vacío")
        void tieneComentarioDeTabla() throws IOException {
            for (String bloque : bloquesDdl()) {
                String nombre = extraerNombreTabla(bloque);
                int enginePos = bloque.toUpperCase().indexOf("ENGINE");
                String opciones = enginePos >= 0 ? bloque.substring(enginePos) : bloque;
                long countComment = opciones.lines()
                        .filter(l -> l.replaceAll("\\s+", "").toUpperCase().startsWith("COMMENT="))
                        .count();
                assertThat(countComment)
                        .as("Tabla " + nombre + ": debe haber exactamente 1 COMMENT de tabla")
                        .isEqualTo(1);
                // El COMMENT no debe ser vacío (COMMENT = '')
                assertThat(opciones)
                        .as("Tabla " + nombre + ": el COMMENT de tabla no debe ser vacío (COMMENT = '')")
                        .doesNotContainPattern("(?i)COMMENT\\s*=\\s*''");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DDL-G4: sin ORM ni herramientas de migración automática")
    class G4_SinOrmNiMigracion {

        @Test
        @DisplayName("script canónico no usa Flyway ni Liquibase")
        void sinFlywayNiLiquibase() throws IOException {
            String sinComentarios = quitarComentariosLinea(leer(ddlPath())).toLowerCase();
            assertThat(sinComentarios).doesNotContain("flyway");
            assertThat(sinComentarios).doesNotContain("liquibase");
        }

        @Test
        @DisplayName("script canónico no contiene anotaciones JPA")
        void sinAnotacionesJpa() throws IOException {
            String contenido = leer(ddlPath());
            assertThat(contenido).doesNotContain("@Entity");
            assertThat(contenido).doesNotContain("@Table");
            assertThat(contenido).doesNotContain("@Column");
        }

        @Test
        @DisplayName("README del directorio database/ prohíbe ORM explícitamente")
        void readmeProhibeOrm() throws IOException {
            Path readme = dbRoot.resolve("README.md");
            assertThat(readme).exists();
            String contenido = leer(readme).toLowerCase();
            assertThat(contenido)
                    .as("El README de database/ debe mencionar la prohibición de ORM")
                    .containsAnyOf("no orm", "sin orm", "jpa", "hibernate");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DDL-G5: todas las columnas de cada bloque tienen COMMENT")
    class G5_CommentObligatorio {

        @Test
        @DisplayName("en cada bloque CREATE TABLE, columnas == COMMENTs de columna")
        void cadaColumnaFisicaTieneComment() throws IOException {
            for (String bloque : bloquesDdl()) {
                String nombre = extraerNombreTabla(bloque);
                String cuerpo = extraerCuerpo(bloque);
                long columnas = contarColumnas(cuerpo);
                long comentarios = contarComentariosColumna(cuerpo);
                assertThat(comentarios)
                        .as("Tabla " + nombre
                                + ": columnas=" + columnas
                                + " COMMENTs de columna=" + comentarios
                                + ". Cada columna física debe tener COMMENT.")
                        .isEqualTo(columnas);
                assertThat(columnas)
                        .as("Tabla " + nombre + ": debe tener al menos una columna")
                        .isGreaterThan(0);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DDL-G6: USE sb_faltas_db exactamente una vez")
    class G6_UseDeclarado {

        @Test
        @DisplayName("USE sb_faltas_db aparece exactamente una vez")
        void useDeclaradoUnaVez() throws IOException {
            String contenido = leer(ddlPath());
            long count = contenido.lines()
                    .filter(l -> l.strip().toUpperCase().matches("USE\\s.*"))
                    .count();
            assertThat(count)
                    .as("USE <database> debe aparecer exactamente una vez")
                    .isEqualTo(1);
            assertThat(contenido).containsIgnoringCase("USE sb_faltas_db");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DDL-G7: directorio database/ contiene los artefactos obligatorios")
    class G7_DirectorioEstructura {

        @Test
        @DisplayName("database/README.md existe")
        void readmeExiste() {
            assertThat(dbRoot.resolve("README.md")).exists().isRegularFile();
        }

        @Test
        @DisplayName("database/design/ existe")
        void designDirExiste() {
            assertThat(dbRoot.resolve("design")).exists().isDirectory();
        }

        @Test
        @DisplayName("database/diagnostics/ existe")
        void diagnosticsDirExiste() {
            assertThat(dbRoot.resolve("diagnostics")).exists().isDirectory();
        }

        @Test
        @DisplayName("database/ddl/ existe")
        void ddlDirExiste() {
            assertThat(dbRoot.resolve("ddl")).exists().isDirectory();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DDL-G9: ddl-full-scope.md existe y documenta el scope FULL-R1")
    class G9_FullScopeDoc {

        @Test
        @DisplayName("ddl-full-scope.md existe")
        void ddlFullScopeExiste() {
            assertThat(dbRoot.resolve("design/ddl-full-scope.md")).exists().isRegularFile();
        }

        @Test
        @DisplayName("ddl-full-scope.md menciona 64 tablas")
        void ddlFullScopeMenciona64Tablas() throws IOException {
            String contenido = leer(dbRoot.resolve("design/ddl-full-scope.md"));
            assertThat(contenido)
                    .as("ddl-full-scope.md debe documentar las 64 tablas del scope FULL-R1")
                    .containsAnyOf("64 tabla", "64 CREATE TABLE", "64 TO_CREATE");
        }

        @Test
        @DisplayName("ddl-full-scope.md documenta correcciones de fal_inspector")
        void ddlFullScopeMencionaCorreccionesInspector() throws IOException {
            String contenido = leer(dbRoot.resolve("design/ddl-full-scope.md"));
            assertThat(contenido)
                    .as("ddl-full-scope.md debe documentar la corrección de fal_inspector")
                    .contains("fal_inspector");
        }

        @Test
        @DisplayName("ddl-full-scope.md documenta correcciones de fal_persona")
        void ddlFullScopeMencionaCorreccionesPersona() throws IOException {
            String contenido = leer(dbRoot.resolve("design/ddl-full-scope.md"));
            assertThat(contenido)
                    .as("ddl-full-scope.md debe documentar la corrección de fal_persona")
                    .contains("fal_persona");
        }

        @Test
        @DisplayName("ddl-full-scope.md no referencia ddl-r1-scope (reemplazado)")
        void noReferenciaDdlR1Scope() {
            assertThat(dbRoot.resolve("design/ddl-r1-scope.md"))
                    .as("ddl-r1-scope.md fue reemplazado por ddl-full-scope.md y no debe existir")
                    .doesNotExist();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DDL-G10: backend productivo sin clases ORM")
    class G10_BackendSinOrm {

        private static final Path MAIN_JAVA = Paths.get(
                "src", "main", "java", "ar", "gob", "malvinas", "faltas");

        @Test
        @DisplayName("ninguna clase Java del backend importa javax.persistence o jakarta.persistence")
        void sinImportJpa() throws IOException {
            if (!Files.isDirectory(MAIN_JAVA)) return;
            try (var stream = Files.walk(MAIN_JAVA)) {
                List<Path> violadores = stream
                        .filter(p -> p.toString().endsWith(".java"))
                        .filter(p -> {
                            try {
                                String txt = leer(p);
                                return txt.contains("javax.persistence")
                                        || txt.contains("jakarta.persistence");
                            } catch (IOException e) {
                                return false;
                            }
                        }).toList();
                assertThat(violadores)
                        .as("Ninguna clase del backend debe importar javax/jakarta.persistence (ORM)")
                        .isEmpty();
            }
        }

        @Test
        @DisplayName("ningún archivo Java del backend usa @Entity o @Table de JPA")
        void sinAnnotationsJpaEntity() throws IOException {
            if (!Files.isDirectory(MAIN_JAVA)) return;
            try (var stream = Files.walk(MAIN_JAVA)) {
                List<Path> violadores = stream
                        .filter(p -> p.toString().endsWith(".java"))
                        .filter(p -> {
                            try {
                                String txt = leer(p);
                                return txt.contains("@Entity") || txt.contains("@Table(");
                            } catch (IOException e) {
                                return false;
                            }
                        }).toList();
                assertThat(violadores)
                        .as("Ninguna clase del backend debe usar @Entity o @Table (ORM prohibido)")
                        .isEmpty();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DDL-G11: DATETIME siempre con precisión DATETIME(6)")
    class G11_DatetimePrecision {

        @Test
        @DisplayName("script canónico no usa DATETIME sin precisión")
        void datetimeSiempreConPrecision() throws IOException {
            String contenido = leer(ddlPath());
            boolean sinPrecision = contenido.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> {
                        String upper = l.toUpperCase();
                        int idx = upper.indexOf("DATETIME");
                        if (idx < 0) return false;
                        String resto = upper.substring(idx + "DATETIME".length()).stripLeading();
                        return !resto.startsWith("(");
                    });
            assertThat(sinPrecision)
                    .as("El script DDL no debe usar DATETIME sin precisión; usar DATETIME(6)")
                    .isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DDL-G13: release gate aprobado y database/ fuera del classpath")
    class G13_ReleaseGateYUbicacion {

        @Test
        @DisplayName("el script canónico inicia con la marca APROBADO PARA EJECUCIÓN MANUAL CONTROLADA")
        void scriptIniciaConMarcaAprobada() throws IOException {
            String contenido = leer(ddlPath());
            assertThat(contenido)
                    .as("El script DDL debe iniciar con la marca de aprobación canónica")
                    .startsWith("-- APROBADO PARA EJECUCI\u00D3N MANUAL CONTROLADA");
        }

        @Test
        @DisplayName("el script contiene advertencia de producción (NO EJECUTAR EN PRODUCCIÓN SIN BACKUP)")
        void scriptContieneAdvertenciaProduccion() throws IOException {
            String contenido = leer(ddlPath());
            assertThat(contenido)
                    .as("El script DDL debe contener la advertencia de producción")
                    .contains("NO EJECUTAR EN PRODUCCI\u00D3N SIN BACKUP");
        }

        @Test
        @DisplayName("el script no contiene marcas WIP activas")
        void scriptSinMarcasWipActivas() throws IOException {
            String primerasLineas = leer(ddlPath()).lines().limit(20).collect(java.util.stream.Collectors.joining("\n"));
            assertThat(primerasLineas)
                    .as("El encabezado del script no debe contener marcas WIP activas")
                    .doesNotContainIgnoringCase("WORK IN PROGRESS")
                    .doesNotContainIgnoringCase("NO EJECUTAR TODAV\u00CDA")
                    .doesNotContainIgnoringCase("DDL CONTIN\u00DAA WIP")
                    .doesNotContainIgnoringCase("PENDIENTE DE AUDITOR\u00CDA");
        }

        @Test
        @DisplayName("database/ no está bajo src/main/resources")
        void databaseFueraDeResources() {
            Path resources = Paths.get("src", "main", "resources").toAbsolutePath();
            Path db = dbRoot.toAbsolutePath();
            assertThat(db.startsWith(resources))
                    .as("database/ no debe estar dentro de src/main/resources. "
                            + "dbRoot=" + db + ", resources=" + resources)
                    .isFalse();
        }

        @Test
        @DisplayName("no existe src/main/resources/db")
        void noExisteResourcesDb() {
            assertThat(Paths.get("src", "main", "resources", "db"))
                    .as("No debe existir src/main/resources/db (DDL no se auto-ejecuta)")
                    .doesNotExist();
        }

        @Test
        @DisplayName("no existen carpetas migration/ ni migrations/")
        void noExisteDirectorioMigration() {
            assertThat(Paths.get("migration")).doesNotExist();
            assertThat(Paths.get("migrations")).doesNotExist();
            assertThat(dbRoot.resolve("migration")).doesNotExist();
            assertThat(dbRoot.resolve("migrations")).doesNotExist();
        }

        @Test
        @DisplayName("no existen archivos de migración tipo V1__, V2__, etc.")
        void noExistenArchivosMigracion() throws IOException {
            Pattern patternMigracion = Pattern.compile("^V\\d+__.*\\.sql$", Pattern.CASE_INSENSITIVE);
            List<Path> violadores = new ArrayList<>();
            try (var stream = Files.walk(dbRoot)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> patternMigracion.matcher(p.getFileName().toString()).matches())
                        .forEach(violadores::add);
            }
            Path resources = Paths.get("src", "main", "resources");
            if (Files.isDirectory(resources)) {
                try (var stream = Files.walk(resources)) {
                    stream.filter(Files::isRegularFile)
                            .filter(p -> patternMigracion.matcher(p.getFileName().toString()).matches())
                            .forEach(violadores::add);
                }
            }
            assertThat(violadores)
                    .as("No deben existir archivos de migración automática tipo V1__.sql")
                    .isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DDL-G14: ningún objeto protegido es destino de DDL ejecutable")
    class G14_ObjetosProtegidos {

        @Test
        @DisplayName("el script DDL no aplica CREATE/ALTER/DROP TABLE|VIEW sobre objetos protegidos")
        void sinDdlSobreProtegidos() throws IOException {
            String sinComentarios = quitarComentariosLinea(leer(ddlPath()));
            // Verificar que no aparece ningún DDL que APUNTE al objeto protegido.
            // Patrón: (CREATE|ALTER|DROP)\s+TABLE\s+<obj> o (TRUNCATE TABLE <obj>).
            // NO detectar referencias válidas como REFERENCES obj o ON DELETE/UPDATE.
            for (String obj : OBJETOS_PROTEGIDOS) {
                java.util.regex.Pattern ddlTarget = java.util.regex.Pattern.compile(
                        "(?i)\\b(CREATE|ALTER|DROP)\\s+(TABLE|VIEW)\\s+" + java.util.regex.Pattern.quote(obj) + "\\b"
                );
                java.util.regex.Pattern truncateTarget = java.util.regex.Pattern.compile(
                        "(?i)\\bTRUNCATE\\s+(TABLE\\s+)?" + java.util.regex.Pattern.quote(obj) + "\\b"
                );
                boolean violacion = ddlTarget.matcher(sinComentarios).find()
                        || truncateTarget.matcher(sinComentarios).find();
                assertThat(violacion)
                        .as("El objeto protegido '" + obj + "' no debe ser destino de DDL ejecutable "
                                + "(CREATE/ALTER/DROP TABLE|VIEW ni TRUNCATE)")
                        .isFalse();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DDL-G16: conjuntos exactos de enums persistidos declarados como CHECK")
    class G16_EnumsExactos {

        private String bloqueParaTabla(String tabla) throws IOException {
            return bloquesDdl().stream()
                    .filter(b -> tabla.equals(extraerNombreTabla(b)))
                    .findFirst()
                    .orElse("");
        }

        @Test
        @DisplayName("num_politica tiene CHECK de ClaseNumeracion exactamente IN (1, 2)")
        void claseNumeracionCheckExacto() throws IOException {
            String bloque = bloqueParaTabla("num_politica");
            assertThat(bloque).isNotEmpty();
            assertThat(bloque.replaceAll("\\s+", " ").toUpperCase())
                    .as("num_politica debe tener CHECK (clase_numeracion IN (1, 2))")
                    .containsPattern("(?i)CLASE_NUMERACION\\s+IN\\s*\\(\\s*1\\s*,\\s*2\\s*\\)");
        }

        @Test
        @DisplayName("fal_dia_no_computable tiene CHECK de TipoDiaNoComputable exactamente IN (1, 2, 3)")
        void tipoDiaNoComputableCheckExacto() throws IOException {
            String bloque = bloqueParaTabla("fal_dia_no_computable");
            assertThat(bloque).isNotEmpty();
            assertThat(bloque)
                    .as("fal_dia_no_computable debe tener CHECK (tipo IN (1, 2, 3))")
                    .containsPattern("(?i)\\bTIPO\\s+IN\\s*\\(\\s*1\\s*,\\s*2\\s*,\\s*3\\s*\\)");
        }

        @Test
        @DisplayName("fal_dia_no_computable tiene CHECK de OrigenDiaNoComputable exactamente IN (1, 2)")
        void origenDiaNoComputableCheckExacto() throws IOException {
            String bloque = bloqueParaTabla("fal_dia_no_computable");
            assertThat(bloque).isNotEmpty();
            assertThat(bloque)
                    .as("fal_dia_no_computable debe tener CHECK (origen IN (1, 2))")
                    .containsPattern("(?i)\\bORIGEN\\s+IN\\s*\\(\\s*1\\s*,\\s*2\\s*\\)");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DDL-G17: README menciona 64 tablas; sin cadenas de slices previos")
    class G17_ReadmeConteo {

        @Test
        @DisplayName("README no contiene '6 tablas (R1)' (cadena de un slice previo obsoleto)")
        void readmeNoContiene6TablasR1() throws IOException {
            String contenido = leer(dbRoot.resolve("README.md"));
            assertThat(contenido)
                    .as("README no debe contener '6 tablas (R1)' — esa era la cadena del slice R1.3")
                    .doesNotContain("6 tablas (R1)");
        }

        @Test
        @DisplayName("README no contiene '7 tablas (R1)' (cadena obsoleta)")
        void readmeNoContiene7TablasR1() throws IOException {
            String contenido = leer(dbRoot.resolve("README.md"));
            assertThat(contenido)
                    .as("README no debe contener '7 tablas (R1)'")
                    .doesNotContain("7 tablas (R1)");
        }

        @Test
        @DisplayName("README menciona 64 tablas a crear")
        void readmeMenciona64Tablas() throws IOException {
            String contenido = leer(dbRoot.resolve("README.md"));
            assertThat(contenido)
                    .as("README debe mencionar las 64 tablas del scope FULL-R1")
                    .containsAnyOf("64 tabla", "64 a crear", "64 (FULL", "64 CREATE", "| 64 |");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DDL-G18: archivos en database/ sin BOM, sin CRLF, con LF final")
    class G18_FormatoArchivosBinario {

        @Test
        @DisplayName("ningún archivo en database/ tiene BOM, CRLF o CR aislado; todos terminan en LF")
        void archivosConLfPuro() throws IOException {
            try (var stream = Files.walk(dbRoot)) {
                List<java.nio.file.Path> archivos = stream.filter(Files::isRegularFile).toList();
                assertThat(archivos)
                        .as("database/ debe contener al menos un archivo")
                        .isNotEmpty();
                for (java.nio.file.Path archivo : archivos) {
                    byte[] bytes = Files.readAllBytes(archivo);
                    String nombre = dbRoot.relativize(archivo).toString();
                    boolean tieneBom = bytes.length >= 3
                            && (bytes[0] & 0xFF) == 0xEF
                            && (bytes[1] & 0xFF) == 0xBB
                            && (bytes[2] & 0xFF) == 0xBF;
                    assertThat(tieneBom)
                            .as("Archivo '" + nombre + "' no debe tener BOM UTF-8")
                            .isFalse();
                    for (int i = 0; i < bytes.length; i++) {
                        if (bytes[i] == (byte) '\r') {
                            assertThat(false)
                                    .as("Archivo '" + nombre + "' contiene CR (\\r) en posición " + i
                                            + " — debe usar LF puro")
                                    .isTrue();
                        }
                    }
                    assertThat(bytes.length > 0 && bytes[bytes.length - 1] == (byte) '\n')
                            .as("Archivo '" + nombre + "' debe terminar en LF")
                            .isTrue();
                }
            }
        }
    }
}
