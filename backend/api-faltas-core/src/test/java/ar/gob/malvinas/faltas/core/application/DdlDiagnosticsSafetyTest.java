package ar.gob.malvinas.faltas.core.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static ar.gob.malvinas.faltas.core.application.DdlTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardrails de seguridad de los scripts de diagnóstico DDL.
 *
 * <p>Verifica sin conexión a base de datos que los scripts bajo
 * {@code database/diagnostics/} son de solo lectura y que
 * {@code verify-protected-baseline.sql} usa las consultas de metadata correctas.
 *
 * <ul>
 *   <li>DIAG-G1: los tres scripts SQL de diagnóstico existen.</li>
 *   <li>DIAG-G15: los tres scripts de diagnóstico son de solo lectura.</li>
 *   <li>DIAG-G19: metadata MariaDB exacta en verify-protected-baseline.sql
 *                 y verify-domain-schema.sql.</li>
 * </ul>
 *
 * <p>No usa Mockito, no usa base de datos. Solo lectura de archivos en {@code database/}.
 */
@DisplayName("Guardrails DIAG: seguridad y estructura de scripts de diagnóstico")
class DdlDiagnosticsSafetyTest {

    private static Path dbRoot;

    @BeforeAll
    static void localizarDatabaseRoot() {
        dbRoot = resolveDbRoot();
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DIAG-G1: scripts SQL de diagnóstico presentes")
    class G1_DiagnosticScriptsExisten {

        @Test
        @DisplayName("verify-protected-baseline.sql existe")
        void verifyProtectedBaselineExiste() {
            assertThat(dbRoot.resolve("diagnostics/verify-protected-baseline.sql"))
                    .exists().isRegularFile();
        }

        @Test
        @DisplayName("verify-canonical-table-inventory.sql existe")
        void verifyCanonicalInventoryExiste() {
            assertThat(dbRoot.resolve("diagnostics/verify-canonical-table-inventory.sql"))
                    .exists().isRegularFile();
        }

        @Test
        @DisplayName("verify-domain-schema.sql existe")
        void verifyDomainSchemaExiste() {
            assertThat(dbRoot.resolve("diagnostics/verify-domain-schema.sql"))
                    .exists().isRegularFile();
        }

        @Test
        @DisplayName("verify-ddl-r1.sql no existe (reemplazado por verify-domain-schema.sql)")
        void verifyDdlR1NoExiste() {
            assertThat(dbRoot.resolve("diagnostics/verify-ddl-r1.sql"))
                    .as("verify-ddl-r1.sql fue reemplazado por verify-domain-schema.sql y no debe existir")
                    .doesNotExist();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DIAG-G15: scripts de diagnóstico son de solo lectura")
    class G15_DiagnosticosSoloLectura {

        private static final String[] VERBOS_PROHIBIDOS = {
                "INSERT", "UPDATE", "DELETE", "CREATE", "ALTER", "DROP",
                "TRUNCATE", "REPLACE", "CALL", "EXECUTE"
        };

        @Test
        @DisplayName("verify-protected-baseline.sql es de solo lectura")
        void verifyProtectedBaselineSoloLectura() throws IOException {
            verificarSoloLectura("verify-protected-baseline.sql");
        }

        @Test
        @DisplayName("verify-canonical-table-inventory.sql es de solo lectura")
        void verifyCanonicalInventorySoloLectura() throws IOException {
            verificarSoloLectura("verify-canonical-table-inventory.sql");
        }

        @Test
        @DisplayName("verify-domain-schema.sql es de solo lectura")
        void verifyDomainSchemaSoloLectura() throws IOException {
            verificarSoloLectura("verify-domain-schema.sql");
        }

        private void verificarSoloLectura(String archivo) throws IOException {
            String sql = quitarComentariosLinea(
                    leer(dbRoot.resolve("diagnostics/" + archivo)));
            for (String stmt : sql.split(";")) {
                String trimmed = stmt.strip().toUpperCase();
                if (trimmed.isEmpty()) continue;
                for (String verbo : VERBOS_PROHIBIDOS) {
                    boolean iniciaCon = trimmed.startsWith(verbo + " ")
                            || trimmed.startsWith(verbo + "\n")
                            || trimmed.startsWith(verbo + "\r")
                            || trimmed.equals(verbo);
                    assertThat(iniciaCon)
                            .as("Diagnóstico " + archivo + " contiene sentencia prohibida: " + verbo)
                            .isFalse();
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DIAG-G19: metadata MariaDB correcta en scripts de diagnóstico")
    class G19_MetadataMariaDb {

        private String sqlBaseline() throws IOException {
            return quitarComentariosLinea(
                    leer(dbRoot.resolve("diagnostics/verify-protected-baseline.sql")));
        }

        private String sqlDomain() throws IOException {
            return quitarComentariosLinea(
                    leer(dbRoot.resolve("diagnostics/verify-domain-schema.sql")));
        }

        @Test
        @DisplayName("verify-protected-baseline.sql no usa information_schema.views en SQL ejecutable")
        void verifyProtectedBaselineNoUsaViews() throws IOException {
            assertThat(sqlBaseline().toLowerCase())
                    .as("verify-protected-baseline.sql no debe referenciar information_schema.views; "
                            + "usar information_schema.tables con table_type='VIEW'")
                    .doesNotContain("information_schema.views");
        }

        @Test
        @DisplayName("verify-protected-baseline.sql usa information_schema.tables con table_type='VIEW'")
        void verifyProtectedBaselineUsaTablesConTableTypeView() throws IOException {
            String sqlLower = sqlBaseline().toLowerCase();
            assertThat(sqlLower)
                    .as("verify-protected-baseline.sql debe usar information_schema.tables")
                    .contains("information_schema.tables");
            assertThat(sqlLower)
                    .as("verify-protected-baseline.sql debe filtrar vistas con table_type = 'VIEW'")
                    .contains("table_type = 'view'");
        }

        @Test
        @DisplayName("verify-protected-baseline.sql exige collation exacta utf8mb4_uca1400_ai_ci")
        void verifyProtectedBaselineExigeCollationExacta() throws IOException {
            String sqlLower = sqlBaseline().toLowerCase();
            assertThat(sqlLower)
                    .as("verify-protected-baseline.sql no debe usar LIKE 'utf8mb4%' para validar collation")
                    .doesNotContain("like 'utf8mb4");
            assertThat(sqlLower)
                    .as("verify-protected-baseline.sql debe exigir el literal exacto utf8mb4_uca1400_ai_ci")
                    .contains("utf8mb4_uca1400_ai_ci");
        }

        @Test
        @DisplayName("verify-domain-schema.sql sección PK usa GROUP_CONCAT para comparar lista exacta")
        void domainSchemaPkUsaGroupConcat() throws IOException {
            String sqlLower = sqlDomain().toLowerCase();
            assertThat(sqlLower)
                    .as("La sección PK de verify-domain-schema.sql debe usar GROUP_CONCAT")
                    .contains("group_concat");
            assertThat(sqlLower)
                    .as("La sección PK debe ordenar por ordinal_position")
                    .contains("ordinal_position");
        }

        @Test
        @DisplayName("verify-domain-schema.sql reporta UNIQUE_INESPERADA")
        void domainSchemaReportaUniqueInesperadas() throws IOException {
            assertThat(sqlDomain())
                    .as("verify-domain-schema.sql debe reportar UNIQUE_INESPERADA para detectar UNIQUEs inesperadas")
                    .contains("UNIQUE_INESPERADA");
        }

        @Test
        @DisplayName("verify-domain-schema.sql reporta FK_INESPERADA")
        void domainSchemaReportaFkInesperadas() throws IOException {
            assertThat(sqlDomain())
                    .as("verify-domain-schema.sql debe reportar FK_INESPERADA para detectar FKs inesperadas")
                    .contains("FK_INESPERADA");
        }

        @Test
        @DisplayName("verify-domain-schema.sql sección CHECK no usa validación débil por tokens")
        void domainSchemaCheckNoUsaTokens() throws IOException {
            String sql = sqlDomain();
            assertThat(sql)
                    .as("verify-domain-schema.sql no debe usar 'token_semantico' (validación débil)")
                    .doesNotContain("token_semantico");
            assertThat(sql)
                    .as("verify-domain-schema.sql no debe usar REGEXP '\\\\b' (word-boundary token)")
                    .doesNotContain("REGEXP '\\\\b");
        }

        @Test
        @DisplayName("verify-domain-schema.sql sección CHECK compara cláusula normalizada (LOWER)")
        void domainSchemaCheckUsaClausulaNormalizada() throws IOException {
            String sqlLower = sqlDomain().toLowerCase();
            assertThat(sqlLower)
                    .as("verify-domain-schema.sql debe usar LOWER() para normalizar cláusulas CHECK")
                    .contains("lower(");
            assertThat(sqlLower)
                    .as("verify-domain-schema.sql debe reportar CLAUSULA_DIVERGENTE cuando no coincide")
                    .contains("clausula_divergente");
        }

        @Test
        @DisplayName("verify-domain-schema.sql usa collation exacta utf8mb4_uca1400_ai_ci")
        void domainSchemaUsaCollationExacta() throws IOException {
            String sqlLower = sqlDomain().toLowerCase();
            assertThat(sqlLower)
                    .as("verify-domain-schema.sql debe exigir la collation exacta utf8mb4_uca1400_ai_ci")
                    .contains("utf8mb4_uca1400_ai_ci");
        }

        @Test
        @DisplayName("verify-domain-schema.sql cubre fal_acta en sus verificaciones")
        void domainSchemaCubreFalActa() throws IOException {
            String sql = sqlDomain();
            assertThat(sql)
                    .as("verify-domain-schema.sql debe cubrir la tabla central fal_acta")
                    .contains("fal_acta");
        }

        @Test
        @DisplayName("verify-domain-schema.sql sección 13 usa LEFT JOIN (expected-vs-actual)")
        void domainSchema13UsaLeftJoin() throws IOException {
            assertThat(sqlDomain())
                    .as("verify-domain-schema.sql sección 13 debe usar LEFT JOIN para expected-vs-actual")
                    .contains("LEFT JOIN");
        }

        @Test
        @DisplayName("verify-domain-schema.sql sección 13 expone columnas_hija_esperadas y columnas_padre_esperadas")
        void domainSchema13ExponeColumnasEsperadas() throws IOException {
            String sql = sqlDomain();
            assertThat(sql)
                    .as("verify-domain-schema.sql sección 13 debe exponer alias columnas_hija_esperadas")
                    .contains("columnas_hija_esperadas");
            assertThat(sql)
                    .as("verify-domain-schema.sql sección 13 debe exponer alias columnas_padre_esperadas")
                    .contains("columnas_padre_esperadas");
        }

        @Test
        @DisplayName("verify-domain-schema.sql sección 13a reporta COLUMNA_AUSENTE")
        void domainSchema13ReportaColumnaAusente() throws IOException {
            assertThat(sqlDomain())
                    .as("verify-domain-schema.sql sección 13a debe reportar COLUMNA_AUSENTE cuando la columna no existe")
                    .contains("COLUMNA_AUSENTE");
        }

        @Test
        @DisplayName("verify-domain-schema.sql sección 13 fija constraint_name esperado explícito")
        void domainSchema13ConstraintNameEsperado() throws IOException {
            String sql = sqlDomain();
            assertThat(sql)
                    .as("verify-domain-schema.sql sección 13 debe fijar fk_acta_dep_ver como constraint esperado")
                    .contains("fk_acta_dep_ver");
            assertThat(sql)
                    .as("verify-domain-schema.sql sección 13 debe fijar fk_acta_ctv_rubro como constraint esperado")
                    .contains("fk_acta_ctv_rubro");
            assertThat(sql)
                    .as("verify-domain-schema.sql sección 13 debe fijar fk_pers_dom_loc_malv como constraint esperado")
                    .contains("fk_pers_dom_loc_malv");
        }
    }

    @Nested
    @DisplayName("DIAG-G20: verify-canonical-table-inventory.sql sin referencias obsoletas R-PERS — CORRECCION-09")
    class G20_DiagnosticFalPersonaFullR1 {

        private String sqlInventario() throws IOException {
            return leer(dbRoot.resolve("diagnostics/verify-canonical-table-inventory.sql"));
        }

        @Test
        @DisplayName("diagnóstico no declara 'fal_persona NO está en R1'")
        void sinFalPersonaNoEstaEnR1() throws IOException {
            assertThat(sqlInventario())
                    .as("El diagnóstico no debe declarar 'fal_persona NO está en R1' — gap cerrado en FULL-R1")
                    .doesNotContain("fal_persona NO está en R1");
        }

        @Test
        @DisplayName("diagnóstico no asigna slice R-PERS a ninguna tabla")
        void sinSliceRPers() throws IOException {
            assertThat(sqlInventario())
                    .as("El diagnóstico no debe contener 'R-PERS' — fal_persona fue incorporada al FULL-R1")
                    .doesNotContain("R-PERS");
        }

        @Test
        @DisplayName("diagnóstico no declara 'Esperado post-R1: 6 filas' (sección parcial eliminada)")
        void sinEsperado6Filas() throws IOException {
            assertThat(sqlInventario())
                    .as("El diagnóstico no debe declarar 'Esperado post-R1: 6 filas' — sección parcial obsoleta fue corregida")
                    .doesNotContain("Esperado post-R1: 6 filas");
        }

        @Test
        @DisplayName("diagnóstico no menciona 'slice R-PERS futuro'")
        void sinSliceRPersFuturo() throws IOException {
            assertThat(sqlInventario())
                    .as("El diagnóstico no debe mencionar 'slice R-PERS futuro'")
                    .doesNotContain("slice R-PERS futuro");
        }

        @Test
        @DisplayName("diagnóstico incluye 'fal_persona' como tabla canónica inventariada")
        void falPersonaEnInventarioDiagnostico() throws IOException {
            assertThat(sqlInventario())
                    .as("verify-canonical-table-inventory.sql debe incluir 'fal_persona' entre las tablas canónicas")
                    .contains("'fal_persona'");
        }

        @Test
        @DisplayName("diagnóstico no contiene 'reconciliación física pendiente'")
        void sinReconciliacionPendiente() throws IOException {
            assertThat(sqlInventario())
                    .as("El diagnóstico no debe mencionar 'reconciliación física pendiente'")
                    .doesNotContain("reconciliación física pendiente");
        }
    }
}
