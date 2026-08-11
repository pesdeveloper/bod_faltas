package ar.gob.malvinas.faltas.core.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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
        @DisplayName("verify-domain-schema.sql Sección 12 fue retirada (DECISION_DDL-SIN-CHECKS-CODIGOS-01)")
        void domainSchemaCheckUsaClausulaNormalizada() throws IOException {
            String sql = sqlDomain();
            String sqlLower = sql.toLowerCase();
            assertThat(sql)
                    .as("La Sección 12 debe declarar su marca de retiro")
                    .contains("SECCION_12_RETIRADA_DECISION_DDL_SIN_CHECKS_CODIGOS_01");
            assertThat(sqlLower)
                    .as("No debe quedar la comparación por cláusula normalizada de la Sección 12 retirada")
                    .doesNotContain("clausula_divergente")
                    .doesNotContain("replace(cc.check_clause");
        }

        @Test
        @DisplayName("verify-domain-schema.sql sección CHECK ya NO exige que la cláusula contenga el nombre físico del constraint (falso positivo cerrado)")
        void domainSchemaCheckYaNoExigeNombreEnClausula() throws IOException {
            String sqlLower = sqlDomain().toLowerCase();
            assertThat(sqlLower)
                    .as("El criterio anterior LIKE CONCAT('%', lower(tc.constraint_name), '%') generaba falsos "
                            + "positivos: el nombre de un CHECK normalmente no forma parte del texto de su "
                            + "expresión (MARIADB-BASELINE-CHECK-DIAGNOSTIC-FIX-002)")
                    .doesNotContain("like concat('%', lower(tc.constraint_name)");
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

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * MARIADB-BASELINE-DIAGNOSTIC-FIX-001: corrección del falso positivo {@code UNIQUE_INESPERADA}
     * de la Sección 10. El criterio anterior exigía {@code uq_<nombre_completo_de_tabla>%} y fallaba
     * en 25 de 33 UNIQUE reales porque la convención física del DDL es {@code uq_<abreviatura>_*}.
     * El criterio nuevo compara identidad semántica (tabla + columnas + orden), no nombre.
     */
    @Nested
    @DisplayName("DIAG-G21: Sección 10 UNIQUE por identidad semántica, no por nombre — MARIADB-BASELINE-DIAGNOSTIC-FIX-001")
    class G21_UniqueSemanticaNoNombre {

        private String sqlDomainSinComentarios() throws IOException {
            return quitarComentariosLinea(
                    leer(dbRoot.resolve("diagnostics/verify-domain-schema.sql")));
        }

        private String ddlSinComentarios() throws IOException {
            return quitarComentariosLinea(
                    leer(dbRoot.resolve("ddl/create-bod-faltas-domain.sql")));
        }

        @Test
        @DisplayName("Sección 10 ya NO exige el patrón de nombre uq_<tabla_completa>% (causa del falso positivo)")
        void seccion10YaNoUsaPatronDeNombrePorTablaCompleta() throws IOException {
            String sql = sqlDomainSinComentarios();
            assertThat(sql)
                    .as("El patrón LIKE CONCAT('uq_', tc.table_name, ...) generaba falsos positivos: "
                            + "la convención real del DDL es uq_<abreviatura>_*, no uq_<tabla_completa>_*")
                    .doesNotContain("LIKE CONCAT('uq_', tc.table_name");
        }

        @Test
        @DisplayName("Sección 10 sigue detectando UNIQUE faltante y UNIQUE inesperada")
        void seccion10SigueDetectandoFaltanteEInesperada() throws IOException {
            String sql = sqlDomainSinComentarios();
            assertThat(sql).contains("UNIQUE_FALTANTE");
            assertThat(sql).contains("UNIQUE_INESPERADA");
        }

        @Test
        @DisplayName("Sección 10 detecta UNIQUE duplicada (misma tabla+columnas) con ROW_NUMBER")
        void seccion10DetectaDuplicadasConRowNumber() throws IOException {
            String sql = sqlDomainSinComentarios();
            assertThat(sql)
                    .as("La detección de UNIQUE duplicada (misma tabla+columnas, distinto nombre) "
                            + "requiere ROW_NUMBER() OVER particionado por tabla+columnas")
                    .contains("ROW_NUMBER() OVER");
        }

        @Test
        @DisplayName("Sección 10 compara por columnas (identidad semántica), no reintroduce comparación por nombre de tabla")
        void seccion10ComparaPorColumnas() throws IOException {
            String sqlLower = sqlDomainSinComentarios().toLowerCase();
            assertThat(sqlLower)
                    .as("El join expected-vs-actual debe comparar columnas_esp contra columnas_real")
                    .contains("columnas_esp = rr.columnas_real");
        }

        @Test
        @DisplayName("Nombres físicos abreviados previamente rechazados (uq_fal_dep_cod, uq_firma_req_doc_seq) siguen presentes y trazables")
        void nombresAbreviadosSiguenPresentes() throws IOException {
            String sql = sqlDomainSinComentarios();
            // Antes del fix: 'fal_dependencia' con constraint 'uq_fal_dep_cod' era UNIQUE_INESPERADA
            // porque uq_fal_dep_cod no empieza con uq_fal_dependencia. Debe seguir declarado en la
            // lista esperada por columnas (cod_dep), no por nombre.
            assertThat(sql).containsPattern("'fal_dependencia'\\s*(?:AS\\s+tabla_esp)?,\\s*'cod_dep'");
            assertThat(sql).contains("'fal_documento_firma_req', 'id_documento,seq_firma_req'");
        }

        @Test
        @DisplayName("Paridad exacta: UNIQUE esperadas en el diagnóstico == UNIQUE KEY declaradas en create-bod-faltas-domain.sql")
        void paridadExactaEntreDiagnosticoYDdl() throws IOException {
            List<DdlTestSupport.UniqueForma> esperadasDiagnostico =
                    extraerUniqueEsperadasDiagnostico(sqlDomainSinComentarios());
            List<DdlTestSupport.UniqueForma> declaradasDdl =
                    extraerUniqueKeysDdl(ddlSinComentarios());

            java.util.Set<String> clavesDiagnostico = esperadasDiagnostico.stream()
                    .map(DdlTestSupport.UniqueForma::clave)
                    .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
            java.util.Set<String> clavesDdl = declaradasDdl.stream()
                    .map(DdlTestSupport.UniqueForma::clave)
                    .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));

            assertThat(clavesDdl)
                    .as("create-bod-faltas-domain.sql debe declarar exactamente 41 UNIQUE KEY (33 del baseline "
                            + "MARIADB-BASELINE-EXEC-001 / MARIADB-BASELINE-DIAGNOSTIC-FIX-001 + 7 agregadas en "
                            + "Wave 4 PARTE-2 por el rediseño de normativa/articulo/tarifario/valorizacion + 1 "
                            + "agregada en Wave 4 PARTE-3B S7.2: fal_acta_medida_preventiva(acta_id,"
                            + "medida_preventiva_id), duplicado exacto de medida por acta)")
                    .hasSize(41);
            assertThat(clavesDiagnostico)
                    .as("La lista 'esperadas' de la Sección 10 debe tener exactamente las mismas 41 formas "
                            + "(tabla+columnas) que las UNIQUE KEY reales del DDL — sin faltantes ni sobrantes")
                    .isEqualTo(clavesDdl);
        }
    }

    /**
     * MARIADB-BASELINE-CHECK-DIAGNOSTIC-FIX-002 cerró un falso positivo histórico de la
     * Sección 12 (comparación por identidad semántica en lugar de por nombre). La Sección 12
     * en sí quedó retirada por {@code DECISION_DDL-SIN-CHECKS-CODIGOS-01}
     * ({@code ACTA-ALTA-INTEGRAL-JDBC-MASTER-20260804-224853}): las 43 CHECK que verificaba
     * fueron eliminadas del DDL porque son de código/enum/estado o de coherencia funcional, no
     * invariantes técnicos, y la validación ya vive en dominio/aplicación. No queda ninguna
     * CHECK física que verificar en esas 15 tablas; la sección se retira en vez de dejar una
     * consulta vacía.
     */
    @Nested
    @DisplayName("DIAG-G22: Sección 12 retirada — DECISION_DDL-SIN-CHECKS-CODIGOS-01")
    class G22_CheckSemanticaNoNombre {

        private String sqlDomainSinComentarios() throws IOException {
            return quitarComentariosLinea(
                    leer(dbRoot.resolve("diagnostics/verify-domain-schema.sql")));
        }

        @Test
        @DisplayName("Sección 12 fue retirada: no quedan CTE ni CHECK_FALTANTE/INESPERADO/DUPLICADO huérfanos")
        void seccion12FueRetiradaSinArtefactosHuerfanos() throws IOException {
            String sql = sqlDomainSinComentarios();
            assertThat(sql)
                    .as("La Sección 12 debe declarar su marca de retiro")
                    .contains("SECCION_12_RETIRADA_DECISION_DDL_SIN_CHECKS_CODIGOS_01");
            assertThat(sql)
                    .as("No debe quedar el CTE esperados_check huérfano de la Sección 12 retirada")
                    .doesNotContain("esperados_check AS (")
                    .doesNotContain("CHECK_FALTANTE")
                    .doesNotContain("CHECK_INESPERADO")
                    .doesNotContain("CHECK_DUPLICADO");
        }

        @Test
        @DisplayName("Ninguna de las 43 CHECK retiradas de la Sección 12 sigue declarada en el DDL canónico")
        void ningunaDeLas43ChecksRetiradasSigueEnElDdl() throws IOException {
            String ddl = quitarComentariosLinea(leer(dbRoot.resolve("ddl/create-bod-faltas-domain.sql")));
            for (String constraint : List.of(
                    "chk_fal_insp_legajo", "chk_fal_persona_tipo", "chk_fal_persona_doc_coherente",
                    "chk_fal_persona_doc_tipo", "chk_fal_persona_doc_cuit", "chk_fal_persona_doc_no_cuit",
                    "chk_fal_persona_nro_doc_canonico", "chk_fal_persona_nro_doc_dni",
                    "chk_fal_persona_prefijo", "chk_fal_persona_digito", "chk_fal_persona_id_suj",
                    "chk_fal_persona_id_bie", "chk_fal_persona_id_bie_suj",
                    "chk_num_pol_clase", "chk_num_pol_longitud",
                    "chk_dia_nc_tipo", "chk_dia_nc_origen", "chk_dia_nc_ref_ext",
                    "chk_acta_tipo", "chk_acta_qr_ver", "chk_acta_situacion_infractor",
                    "chk_acta_infractor_coherente",
                    "chk_oblig_tipo", "chk_oblig_origen", "chk_oblig_estado", "chk_oblig_monto",
                    "chk_forma_pago_tipo", "chk_forma_pago_nro", "chk_forma_pago_monto",
                    "chk_fallo_estado", "chk_fallo_tipo", "chk_fallo_firmeza",
                    "chk_apelacion_estado",
                    "chk_pago_mov_tipo", "chk_pago_mov_clasificacion", "chk_pago_mov_motivo_anulacion",
                    "chk_num_talonario_nro", "chk_fal_lote_codigo", "chk_fal_lote_estado",
                    "chk_alcoholemia_orden", "chk_noti_intento_nro", "chk_tal_insp_estado",
                    "chk_tal_mov_nro")) {
                assertThat(ddl)
                        .as("%s fue retirado por DECISION_DDL-SIN-CHECKS-CODIGOS-01", constraint)
                        .doesNotContain(constraint);
            }
        }
    }
}
