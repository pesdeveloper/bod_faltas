package ar.gob.malvinas.faltas.core.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.gob.malvinas.faltas.core.application.DdlTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardrails de paridad física del DDL con las decisiones humanas cerradas.
 *
 * <p>Verifica sin conexión a base de datos que {@code database/ddl/create-bod-faltas-domain.sql}
 * cumple los contratos de tipo, longitud y FK declarados como HUMAN_DECISION_CLOSED.
 *
 * <ul>
 *   <li>PHY-G1: tipos y longitudes de las seis tablas R1.3 restauradas.</li>
 *   <li>PHY-G2: contratos humanos de tablas maestro (dependencia, inspector, persona, firmante).</li>
 *   <li>PHY-G3: FK al baseline protegido usan los nombres de columna correctos.</li>
 *   <li>PHY-G4: columnas OCC/versionRow presentes donde la spec lo exige.</li>
 *   <li>PHY-G5: columnas de auditoría DATETIME(6) y CHAR(36) presentes en tablas clave.</li>
 *   <li>PHY-G9: plantilla documental y contenido versionado FULL-R1.2-CORRECCION-04.</li>
 * </ul>
 *
 * <p>No usa Mockito, no usa base de datos. Solo lectura de archivos en {@code database/}.
 */
@DisplayName("Guardrails PHY: paridad física con decisiones humanas cerradas")
class DdlPhysicalParityTest {

    private static Path dbRoot;
    private static Map<String, String> bloquesPorTabla;

    @BeforeAll
    static void cargarDdl() throws IOException {
        dbRoot = resolveDbRoot();
        List<String> bloques = extraerBloquesDdl(leer(dbRoot.resolve("ddl/create-bod-faltas-domain.sql")));
        bloquesPorTabla = bloques.stream()
                .collect(Collectors.toMap(DdlTestSupport::extraerNombreTabla, b -> b));
    }

    private String cuerpo(String tabla) {
        String bloque = bloquesPorTabla.get(tabla);
        assertThat(bloque).as("Bloque DDL de la tabla '%s' debe existir", tabla).isNotNull();
        return extraerCuerpo(bloque);
    }

    /**
     * Verifica que alguna línea de columna del cuerpo contenga el fragmento dado.
     * Ignora líneas de comentario SQL (-- ...).
     */
    private void assertColumnContains(String tabla, String fragment, String descripcion) {
        String cuerpo = cuerpo(tabla);
        boolean found = cuerpo.lines()
                .filter(l -> !l.strip().startsWith("--"))
                .anyMatch(l -> l.contains(fragment));
        assertThat(found)
                .as("Tabla '%s': se esperaba fragmento '%s' (%s)", tabla, fragment, descripcion)
                .isTrue();
    }

    /**
     * Verifica que ninguna línea de columna del cuerpo comience con el nombre de columna dado.
     * Ignora líneas de comentario SQL.
     */
    private void assertColumnAbsente(String tabla, String columnName, String descripcion) {
        String cuerpo = cuerpo(tabla);
        boolean presente = cuerpo.lines()
                .filter(l -> !l.strip().startsWith("--"))
                .filter(l -> {
                    String s = l.strip();
                    return s.startsWith(columnName + " ") || s.startsWith(columnName + "\t");
                })
                .findAny().isPresent();
        assertThat(presente)
                .as("Tabla '%s': columna '%s' debe estar ausente (%s)", tabla, columnName, descripcion)
                .isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PHY-G1: Seis tablas R1.3 restauradas — tipos y longitudes críticos")
    class G1_R13Restauradas {

        @Test
        @DisplayName("fal_firmante: nom_firmante VARCHAR(48)")
        void falFirmante_nomFirmante() {
            assertColumnContains("fal_firmante", "VARCHAR(48)", "nom_firmante 48 chars");
        }

        @Test
        @DisplayName("fal_vehiculo_marca: codigo VARCHAR(12), nombre VARCHAR(24)")
        void falVehiculoMarca_codigos() {
            assertColumnContains("fal_vehiculo_marca", "codigo          VARCHAR(12)", "R1.3 codigo VARCHAR(12)");
            assertColumnContains("fal_vehiculo_marca", "nombre          VARCHAR(24)", "R1.3 nombre VARCHAR(24)");
        }

        @Test
        @DisplayName("num_politica: codigo VARCHAR(8), apodo VARCHAR(20), descripcion VARCHAR(64)")
        void numPolitica_campos() {
            assertColumnContains("num_politica", "codigo              VARCHAR(8)", "HUMAN_DECISION_CLOSED codigo 8");
            assertColumnContains("num_politica", "apodo               VARCHAR(20)", "HUMAN_DECISION_CLOSED apodo 20");
            assertColumnContains("num_politica", "descripcion         VARCHAR(64)", "HUMAN_DECISION_CLOSED descripcion 64");
        }

        @Test
        @DisplayName("fal_dia_no_computable: tipo SMALLINT, origen SMALLINT — no tipo_dia ni origen_dia")
        void falDiaNoComputable_nombres() {
            String cuerpo = cuerpo("fal_dia_no_computable");
            assertThat(cuerpo).contains("tipo                SMALLINT");
            assertThat(cuerpo).contains("origen              SMALLINT");
            assertThat(cuerpo).doesNotContain("tipo_dia").doesNotContain("origen_dia");
        }

        @Test
        @DisplayName("fal_dia_no_computable: CHECK tipo IN (1, 2, 3) y origen IN (1, 2)")
        void falDiaNoComputable_checks() {
            String bloque = bloquesPorTabla.get("fal_dia_no_computable");
            // Constraint may have extra spaces: tipo   IN (1, 2, 3)
            assertThat(bloque).containsPattern("tipo\\s+IN\\s*\\(1,\\s*2,\\s*3\\)");
            assertThat(bloque).containsPattern("origen\\s+IN\\s*\\(1,\\s*2\\)");
        }

        @Test
        @DisplayName("fal_motivo_archivo: cod_motivo_archivo VARCHAR(8)")
        void falMotivoArchivo_codigo() {
            assertColumnContains("fal_motivo_archivo", "VARCHAR(8)", "R1.3 codigo 8");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PHY-G2: Contratos humanos de maestros — dependencia, inspector, persona, firmante")
    class G2_MaestrosHumanos {

        @Test
        @DisplayName("fal_dependencia: nom_dep VARCHAR(48), cod_dep VARCHAR(8)")
        void falDependencia_campos() {
            // Nombres canónicos cerrados: nom_dep, cod_dep (no nombre, no sigla, no codigo_dependencia)
            assertColumnContains("fal_dependencia", "nom_dep         VARCHAR(48)", "nom_dep 48");
            assertColumnContains("fal_dependencia", "cod_dep         VARCHAR(8)", "cod_dep 8");
            assertColumnAbsente("fal_dependencia", "nombre", "nombre no canónico");
            assertColumnAbsente("fal_dependencia", "sigla", "sigla no canónica");
        }

        @Test
        @DisplayName("fal_dependencia_version: nom_dep VARCHAR(48), PK (id_dep, ver_dep)")
        void falDependenciaVersion_campos() {
            assertColumnContains("fal_dependencia_version", "nom_dep", "nom_dep presente");
            assertColumnContains("fal_dependencia_version", "id_dep", "id_dep parte de PK");
            assertColumnContains("fal_dependencia_version", "ver_dep", "ver_dep parte de PK");
        }

        @Test
        @DisplayName("fal_inspector: legajo_insp INT, nom_insp VARCHAR(36)")
        void falInspector_campos() {
            // Check column names and types; spacing may vary per table alignment
            String cuerpo = cuerpo("fal_inspector");
            assertThat(cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("legajo_insp") && l.contains("INT")))
                    .as("fal_inspector: legajo_insp INT debe estar presente")
                    .isTrue();
            assertThat(cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("nom_insp") && l.contains("VARCHAR(36)")))
                    .as("fal_inspector: nom_insp VARCHAR(36) debe estar presente")
                    .isTrue();
            assertColumnAbsente("fal_inspector", "nombre_completo", "nombre_completo no canónico");
            assertColumnAbsente("fal_inspector", "num_legajo", "num_legajo no canónico");
        }

        @Test
        @DisplayName("fal_inspector_version: id_insp, ver_insp como PK compuesta")
        void falInspectorVersion_pk() {
            assertColumnContains("fal_inspector_version", "id_insp", "id_insp en PK");
            assertColumnContains("fal_inspector_version", "ver_insp", "ver_insp en PK");
        }

        @Test
        @DisplayName("fal_persona: tipo_documento SMALLINT, nro_doc INT UNSIGNED — no tipo_doc")
        void falPersona_documentoEstructurado() {
            assertColumnContains("fal_persona", "tipo_documento          SMALLINT", "documento estructurado SMALLINT");
            assertColumnContains("fal_persona", "nro_doc                 INT UNSIGNED", "nro_doc INT UNSIGNED");
            assertColumnAbsente("fal_persona", "tipo_doc", "tipo_doc fue renombrado (HUMAN_DECISION_CLOSED)");
        }

        @Test
        @DisplayName("fal_persona: apellido VARCHAR(24), nombres VARCHAR(36), razon_social VARCHAR(64)")
        void falPersona_nombres() {
            assertColumnContains("fal_persona", "apellido                VARCHAR(24)", "apellido 24");
            assertColumnContains("fal_persona", "nombres                 VARCHAR(36)", "nombres 36");
            assertColumnContains("fal_persona", "razon_social            VARCHAR(64)", "razon_social 64");
        }

        @Test
        @DisplayName("fal_firmante_version: nom_firmante VARCHAR(48), rol_firmante presente, cargo_firmante VARCHAR(64)")
        void falFirmanteVersion_campos() {
            assertColumnContains("fal_firmante_version", "nom_firmante    VARCHAR(48)", "nom_firmante 48");
            assertColumnContains("fal_firmante_version", "rol_firmante", "rol_firmante presente");
            assertColumnContains("fal_firmante_version", "cargo_firmante  VARCHAR(64)", "cargo_firmante 64");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PHY-G3: FK al baseline protegido — nombres de columna correctos")
    class G3_FkBaseline {

        @Test
        @DisplayName("FK a fal_dependencia_version usa (id_dep, ver_dep)")
        void fkDependenciaVersion_columnas() {
            String ddl = bloquesPorTabla.values().stream().collect(Collectors.joining("\n"));
            assertThat(ddl).contains("REFERENCES fal_dependencia_version (id_dep, ver_dep)");
            assertThat(ddl).doesNotContain("REFERENCES fal_dependencia_version (id_dependencia");
        }

        @Test
        @DisplayName("FK a fal_inspector_version usa (id_insp, ver_insp)")
        void fkInspectorVersion_columnas() {
            String ddl = bloquesPorTabla.values().stream().collect(Collectors.joining("\n"));
            assertThat(ddl).contains("REFERENCES fal_inspector_version (id_insp, ver_insp)");
            assertThat(ddl).doesNotContain("REFERENCES fal_inspector_version (id_inspector");
        }

        @Test
        @DisplayName("FK a fal_rubro_version está presente (baseline adoptado)")
        void fkRubroVersion_presente() {
            String ddl = bloquesPorTabla.values().stream().collect(Collectors.joining("\n"));
            assertThat(ddl).contains("REFERENCES fal_rubro_version");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PHY-G4: OCC / versionRow presente en tablas que lo requieren")
    class G4_Occ {

        @Test
        @DisplayName("fal_acta tiene version_row INT NOT NULL DEFAULT 0")
        void falActa_versionRow() {
            assertColumnContains("fal_acta", "version_row", "OCC version_row");
        }

        @Test
        @DisplayName("num_talonario tiene version_row INT NOT NULL DEFAULT 0")
        void numTalonario_versionRow() {
            assertColumnContains("num_talonario", "version_row", "OCC version_row");
        }

        @Test
        @DisplayName("fal_persona NO tiene version_row (DECISION_DDL-PERS-01 CERRADA)")
        void falPersona_sinVersionRow() {
            assertColumnAbsente("fal_persona", "version_row", "DECISION_DDL-PERS-01 CERRADA: sin OCC");
        }

        @Test
        @DisplayName("fal_dependencia NO tiene version_row (maestro simple, no versiona row)")
        void falDependencia_sinVersionRow() {
            assertColumnAbsente("fal_dependencia", "version_row", "fal_dependencia no tiene OCC en row");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PHY-G6: Delta FULL-R1.2-CORRECCION-01 — FK versiones, Rubros y GEO")
    class G6_DeltaFkRubrosGeo {

        @Test
        @DisplayName("fal_acta.ver_dep es SMALLINT (tipo exacto de fal_dependencia_version.ver_dep)")
        void falActa_verDepEsSmallint() {
            String cuerpo = cuerpo("fal_acta");
            boolean esSmallint = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("ver_dep") && l.contains("SMALLINT"));
            boolean esInt = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> {
                        String s = l.strip();
                        return s.startsWith("ver_dep") && s.contains("INT") && !s.contains("SMALLINT");
                    });
            assertThat(esSmallint)
                    .as("fal_acta.ver_dep debe ser SMALLINT (FK exacta con fal_dependencia_version.ver_dep)")
                    .isTrue();
            assertThat(esInt)
                    .as("fal_acta.ver_dep NO debe ser INT — tipo incorrecto para la FK")
                    .isFalse();
        }

        @Test
        @DisplayName("fal_acta.ver_insp es SMALLINT (tipo exacto de fal_inspector_version.ver_insp)")
        void falActa_verInspEsSmallint() {
            String cuerpo = cuerpo("fal_acta");
            boolean esSmallint = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("ver_insp") && l.contains("SMALLINT"));
            boolean esInt = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> {
                        String s = l.strip();
                        return s.startsWith("ver_insp") && s.contains("INT") && !s.contains("SMALLINT");
                    });
            assertThat(esSmallint)
                    .as("fal_acta.ver_insp debe ser SMALLINT (FK exacta con fal_inspector_version.ver_insp)")
                    .isTrue();
            assertThat(esInt)
                    .as("fal_acta.ver_insp NO debe ser INT — tipo incorrecto para la FK")
                    .isFalse();
        }

        @Test
        @DisplayName("FK a fal_rubro_version apunta a rubro_id — no a id")
        void fkRubroVersion_apuntaARubroId() {
            String ddl = bloquesPorTabla.values().stream().collect(Collectors.joining("\n"));
            assertThat(ddl)
                    .as("FK a fal_rubro_version debe usar columna referenciada 'rubro_id'")
                    .contains("REFERENCES fal_rubro_version (rubro_id)");
            assertThat(ddl)
                    .as("FK a fal_rubro_version NO debe usar columna referenciada 'id' (incorrecta)")
                    .doesNotContain("REFERENCES fal_rubro_version (id)");
        }

        @Test
        @DisplayName("fal_acta_contravencion: cláusula completa FK → fal_rubro_version (rubro_id)")
        void falActaContravencion_fkRubroId() {
            String bloque = bloquesPorTabla.get("fal_acta_contravencion");
            assertThat(bloque).as("Bloque DDL de fal_acta_contravencion debe existir").isNotNull();
            assertThat(bloque)
                    .as("fal_acta_contravencion: FK debe referenciar fal_rubro_version (rubro_id)")
                    .contains("REFERENCES fal_rubro_version (rubro_id)");
            assertThat(bloque)
                    .as("fal_acta_contravencion: cláusula completa FK con ON DELETE RESTRICT ON UPDATE RESTRICT")
                    .containsPattern(
                            "fk_acta_ctv_rubro\\s+FOREIGN KEY\\s+\\(rubro_id\\)" +
                            "\\s+REFERENCES\\s+fal_rubro_version\\s+\\(rubro_id\\)" +
                            "\\s+ON DELETE RESTRICT\\s+ON UPDATE RESTRICT");
        }

        @Test
        @DisplayName("fal_acta_sustancias_alimenticias: cláusula completa FK → fal_rubro_version (rubro_id)")
        void falActaSustanciasAlimenticias_fkRubroId() {
            String bloque = bloquesPorTabla.get("fal_acta_sustancias_alimenticias");
            assertThat(bloque).as("Bloque DDL de fal_acta_sustancias_alimenticias debe existir").isNotNull();
            assertThat(bloque)
                    .as("fal_acta_sustancias_alimenticias: FK debe referenciar fal_rubro_version (rubro_id)")
                    .contains("REFERENCES fal_rubro_version (rubro_id)");
            assertThat(bloque)
                    .as("fal_acta_sustancias_alimenticias: cláusula completa FK con ON DELETE RESTRICT ON UPDATE RESTRICT")
                    .containsPattern(
                            "fk_acta_sus_alim_rubro\\s+FOREIGN KEY\\s+\\(rubro_id\\)" +
                            "\\s+REFERENCES\\s+fal_rubro_version\\s+\\(rubro_id\\)" +
                            "\\s+ON DELETE RESTRICT\\s+ON UPDATE RESTRICT");
        }

        @Test
        @DisplayName("FK GEO fal_persona_domicilio → geo_malv_localidad_version: cláusula completa")
        void falPersonaDomicilio_fkLocMalv() {
            String ddl = bloquesPorTabla.values().stream().collect(Collectors.joining("\n"));
            assertThat(ddl)
                    .as("fk_pers_dom_loc_malv: cláusula completa FOREIGN KEY ... REFERENCES ... ON DELETE RESTRICT ON UPDATE RESTRICT")
                    .containsPattern(
                            "fk_pers_dom_loc_malv\\s+FOREIGN KEY\\s+\\(localidad_malvinas_version_id\\)" +
                            "\\s+REFERENCES\\s+geo_malv_localidad_version\\s+\\(localidad_version_id\\)" +
                            "\\s+ON DELETE RESTRICT\\s+ON UPDATE RESTRICT");
        }

        @Test
        @DisplayName("FK GEO fal_persona_domicilio → geo_malv_calle_version: cláusula completa")
        void falPersonaDomicilio_fkCalleMalv() {
            String ddl = bloquesPorTabla.values().stream().collect(Collectors.joining("\n"));
            assertThat(ddl)
                    .as("fk_pers_dom_calle_malv: cláusula completa FOREIGN KEY ... REFERENCES ... ON DELETE RESTRICT ON UPDATE RESTRICT")
                    .containsPattern(
                            "fk_pers_dom_calle_malv\\s+FOREIGN KEY\\s+\\(calle_malvinas_version_id\\)" +
                            "\\s+REFERENCES\\s+geo_malv_calle_version\\s+\\(calle_version_id\\)" +
                            "\\s+ON DELETE RESTRICT\\s+ON UPDATE RESTRICT");
        }

        @Test
        @DisplayName("FK GEO fal_acta → geo_malv_localidad_version: cláusula completa (lugar del hecho)")
        void falActa_fkLocInfrMalv() {
            String ddl = bloquesPorTabla.values().stream().collect(Collectors.joining("\n"));
            assertThat(ddl)
                    .as("fk_acta_loc_infr_malv: cláusula completa FOREIGN KEY ... REFERENCES ... ON DELETE RESTRICT ON UPDATE RESTRICT")
                    .containsPattern(
                            "fk_acta_loc_infr_malv\\s+FOREIGN KEY\\s+\\(localidad_infr_malvinas_version_id\\)" +
                            "\\s+REFERENCES\\s+geo_malv_localidad_version\\s+\\(localidad_version_id\\)" +
                            "\\s+ON DELETE RESTRICT\\s+ON UPDATE RESTRICT");
        }

        @Test
        @DisplayName("FK GEO fal_acta → geo_malv_calle_version: cláusula completa (lugar del hecho)")
        void falActa_fkCalleInfrMalv() {
            String ddl = bloquesPorTabla.values().stream().collect(Collectors.joining("\n"));
            assertThat(ddl)
                    .as("fk_acta_calle_infr_malv: cláusula completa FOREIGN KEY ... REFERENCES ... ON DELETE RESTRICT ON UPDATE RESTRICT")
                    .containsPattern(
                            "fk_acta_calle_infr_malv\\s+FOREIGN KEY\\s+\\(calle_infr_malvinas_version_id\\)" +
                            "\\s+REFERENCES\\s+geo_malv_calle_version\\s+\\(calle_version_id\\)" +
                            "\\s+ON DELETE RESTRICT\\s+ON UPDATE RESTRICT");
        }

        @Test
        @DisplayName("verify-domain-schema.sql sección 13: expected-vs-actual completo (FK GEO, Rubros, versiones)")
        void diagnosticoContieneSeccion13() {
            String diagnostico;
            try {
                diagnostico = DdlTestSupport.leer(dbRoot.resolve("diagnostics/verify-domain-schema.sql"));
            } catch (java.io.IOException e) {
                throw new RuntimeException("No se pudo leer verify-domain-schema.sql", e);
            }
            assertThat(diagnostico)
                    .as("verify-domain-schema.sql debe contener sección 13 del delta FK GEO/Rubros/versiones")
                    .contains("SECCIÓN 13")
                    .contains("LEFT JOIN")
                    .contains("columnas_hija_esperadas")
                    .contains("columnas_padre_esperadas")
                    .contains("fk_acta_dep_ver")
                    .contains("fk_acta_insp_ver")
                    .contains("fk_acta_ctv_rubro")
                    .contains("fk_pers_dom_loc_malv")
                    .contains("fk_pers_dom_calle_malv")
                    .contains("fk_acta_loc_infr_malv")
                    .contains("fk_acta_calle_infr_malv")
                    .contains("rubro_id")
                    .contains("COLUMNA_AUSENTE")
                    .contains("FK_AUSENTE")
                    .contains("FK_DIVERGENTE");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PHY-G8: Delta FULL-R1.2-CORRECCION-03 — fal_documento: storage_key VARCHAR(196), sin descripcion")
    class G8_DeltaDocumentoCorreccion03 {

        /** Lee FalDocumento.java desde el working directory Maven (backend/api-faltas-core/). */
        private String leerFalDocumentoJava() {
            try {
                return DdlTestSupport.leer(Paths.get(
                        "src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalDocumento.java"));
            } catch (java.io.IOException e) {
                throw new RuntimeException("No se pudo leer FalDocumento.java", e);
            }
        }

        @Test
        @DisplayName("fal_documento.storage_key es VARCHAR(196) NULL — no VARCHAR(500), no NOT NULL")
        void falDocumento_storageKey_varchar196() {
            String cuerpo = cuerpo("fal_documento");
            boolean esVar196 = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("storage_key") && l.contains("VARCHAR(196)"));
            assertThat(esVar196)
                    .as("fal_documento.storage_key debe ser VARCHAR(196) — HUMAN_DECISION_CLOSED")
                    .isTrue();
            boolean esVar500 = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("storage_key") && l.contains("VARCHAR(500)"));
            assertThat(esVar500)
                    .as("fal_documento.storage_key NO debe ser VARCHAR(500) — longitud obsoleta")
                    .isFalse();
            // Verificar nullability explícita: la línea de storage_key VARCHAR(196) no debe contener NOT NULL
            String lineaStorageKey = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .filter(l -> l.contains("storage_key") && l.contains("VARCHAR(196)"))
                    .findFirst()
                    .orElse(null);
            assertThat(lineaStorageKey)
                    .as("Debe existir línea de storage_key VARCHAR(196) en fal_documento (sin comentarios)")
                    .isNotNull();
            assertThat(lineaStorageKey)
                    .as("storage_key VARCHAR(196) debe ser NULL (nullable) — HUMAN_DECISION_CLOSED: no NOT NULL")
                    .doesNotContain("NOT NULL");
        }

        @Test
        @DisplayName("fal_documento.descripcion está ausente — HUMAN_DECISION_CLOSED eliminado")
        void falDocumento_descripcionAusente() {
            assertColumnAbsente("fal_documento", "descripcion",
                    "HUMAN_DECISION_CLOSED: descripcion eliminado de fal_documento en FULL-R1.2-CORRECCION-03");
        }

        @Test
        @DisplayName("FalDocumento.java: sin campo descripcion, sin getter/setter, sin this.descripcion, sin @Deprecated para descripcion")
        void falDocumento_javaNoTieneDescripcion() {
            String java = leerFalDocumentoJava();
            assertThat(java)
                    .as("FalDocumento.java no debe declarar 'private String descripcion;'")
                    .doesNotContain("private String descripcion;");
            assertThat(java)
                    .as("FalDocumento.java no debe tener parámetro 'String descripcion'")
                    .doesNotContain("String descripcion");
            assertThat(java)
                    .as("FalDocumento.java no debe tener método 'getDescripcion('")
                    .doesNotContain("getDescripcion(");
            assertThat(java)
                    .as("FalDocumento.java no debe tener método 'setDescripcion('")
                    .doesNotContain("setDescripcion(");
            assertThat(java)
                    .as("FalDocumento.java no debe referenciar 'this.descripcion'")
                    .doesNotContain("this.descripcion");
            assertThat(java)
                    .as("FalDocumento.java no debe tener @Deprecated asociado a descripcion")
                    .doesNotContainPattern("@Deprecated[\\s\\S]{0,60}descripcion");
        }

        @Test
        @DisplayName("FalDocumento.java: Javadoc de storageKey menciona 196, clave técnica interna, no URL pública, nullable hasta emisión")
        void falDocumento_javadocStorageKeyCompleto() {
            String java = leerFalDocumentoJava();
            assertThat(java)
                    .as("Javadoc de storageKey en FalDocumento.java debe mencionar '196'")
                    .contains("196");
            assertThat(java)
                    .as("Javadoc de storageKey debe mencionar 'clave técnica interna' (o variante)")
                    .containsIgnoringCase("clave técnica interna");
            assertThat(java)
                    .as("Javadoc de storageKey debe indicar que no es URL pública")
                    .containsIgnoringCase("no es URL pública");
            assertThat(java)
                    .as("Javadoc de storageKey debe indicar que es nullable hasta emisión")
                    .containsIgnoringCase("nullable hasta");
        }

        @Test
        @DisplayName("verify-domain-schema.sql sección 15 contiene expectativas FULL-R1.2-CORRECCION-03 (expected-vs-actual completo)")
        void diagnosticoContieneSeccion15() {
            String diagnostico;
            try {
                diagnostico = DdlTestSupport.leer(dbRoot.resolve("diagnostics/verify-domain-schema.sql"));
            } catch (java.io.IOException e) {
                throw new RuntimeException("No se pudo leer verify-domain-schema.sql", e);
            }
            assertThat(diagnostico)
                    .as("verify-domain-schema.sql debe contener sección 15 del delta fal_documento")
                    .contains("SECCIÓN 15")
                    // Tabla y columna positiva
                    .contains("fal_documento")
                    .contains("storage_key")
                    .contains("varchar")
                    .contains("196")
                    .contains("YES")
                    // Columna de ausencia
                    .contains("descripcion")
                    // Diagnósticos de la expectativa positiva
                    .contains("COLUMNA_AUSENTE")
                    .contains("TIPO_DIVERGENTE")
                    .contains("LONGITUD_DIVERGENTE")
                    .contains("NULLABILITY_DIVERGENTE")
                    // Diagnóstico de la expectativa de ausencia
                    .contains("COLUMNA_INESPERADA")
                    // Patrón expected-vs-actual
                    .contains("LEFT JOIN");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PHY-G9: FULL-R1.2-CORRECCION-04 — plantilla documental y contenido versionado")
    class G9_PlantillaDocumentoCorreccion04 {

        private String leerFuente(String ruta) {
            try {
                return DdlTestSupport.leer(Paths.get(ruta));
            } catch (java.io.IOException e) {
                throw new RuntimeException("No se pudo leer " + ruta, e);
            }
        }

        private String leerJava(String nombreClase) {
            return leerFuente("src/main/java/ar/gob/malvinas/faltas/core/domain/model/"
                    + nombreClase + ".java");
        }

        @Test
        @DisplayName("fal_documento_plantilla mantiene codigo VARCHAR(12), nombre VARCHAR(64) y no contiene descripcion")
        void plantillaColumnasCanonicas() {
            assertColumnContains("fal_documento_plantilla", "codigo                  VARCHAR(12)",
                    "codigo canónico de 12 caracteres");
            assertColumnContains("fal_documento_plantilla", "nombre                  VARCHAR(64)",
                    "nombre canónico de 64 caracteres");
            assertColumnAbsente("fal_documento_plantilla", "descripcion",
                    "descripcion fue eliminada en FULL-R1.2-CORRECCION-04");
        }

        @Test
        @DisplayName("fal_documento_plantilla_contenido usa titulo VARCHAR(64), no VARCHAR(200)")
        void contenidoTituloVarchar64() {
            String cuerpo = cuerpo("fal_documento_plantilla_contenido");
            assertThat(cuerpo)
                    .contains("titulo                      VARCHAR(64)")
                    .doesNotContain("titulo                      VARCHAR(200)");
        }

        @Test
        @DisplayName("variables_declaradas_json es JSON NOT NULL y su COMMENT expresa metadata tipada/requerida/etiquetada")
        void contenidoVariablesMetadata() {
            String cuerpo = cuerpo("fal_documento_plantilla_contenido");
            String linea = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .filter(l -> l.contains("variables_declaradas_json"))
                    .findFirst()
                    .orElse(null);
            assertThat(linea).isNotNull();
            assertThat(linea)
                    .contains("JSON")
                    .contains("NOT NULL")
                    .containsIgnoringCase("tipados")
                    .containsIgnoringCase("requeridos")
                    .containsIgnoringCase("etiquetados")
                    .contains("namespace")
                    .contains("campo")
                    .contains("tipoDato")
                    .contains("requerida")
                    .contains("etiqueta");
        }

        @Test
        @DisplayName("FalDocumentoPlantilla.java no conserva descripcion ni compatibilidad @Deprecated")
        void plantillaJavaSinDescripcion() {
            String java = leerJava("FalDocumentoPlantilla");
            assertThat(java)
                    .doesNotContain("private String descripcion")
                    .doesNotContain("String descripcion")
                    .doesNotContain("getDescripcion(")
                    .doesNotContain("setDescripcion(")
                    .doesNotContain("this.descripcion")
                    .doesNotContainPattern("@Deprecated[\\s\\S]{0,80}descripcion");
        }

        @Test
        @DisplayName("Command, request, response, controller y service no transportan descripcion de plantilla")
        void contratosJavaSinDescripcion() {
            List<String> fuentes = List.of(
                    "src/main/java/ar/gob/malvinas/faltas/core/application/command/CrearDocumentoPlantillaCommand.java",
                    "src/main/java/ar/gob/malvinas/faltas/core/web/dto/CrearDocumentoPlantillaRequest.java",
                    "src/main/java/ar/gob/malvinas/faltas/core/web/dto/DocumentoPlantillaResponse.java",
                    "src/main/java/ar/gob/malvinas/faltas/core/web/DocumentoPlantillaController.java",
                    "src/main/java/ar/gob/malvinas/faltas/core/application/service/DocumentoPlantillaService.java");
            for (String fuente : fuentes) {
                assertThat(leerFuente(fuente))
                        .as("%s no debe exponer ni transportar descripcion de plantilla", fuente)
                        .doesNotContain("descripcion");
            }
        }

        @Test
        @DisplayName("FalDocumentoPlantillaContenido.java expresa titulo máximo 64 y metadata obligatoria tipada/etiquetada")
        void contenidoJavaContrato() {
            String java = leerJava("FalDocumentoPlantillaContenido");
            assertThat(java)
                    .contains("MAX_TITULO_LENGTH = 64")
                    .contains("titulo.length() > MAX_TITULO_LENGTH")
                    .contains("variablesDeclaradasJson")
                    .contains("namespace")
                    .contains("campo")
                    .contains("tipoDato")
                    .contains("requerida")
                    .contains("etiqueta")
                    .contains("[]");
        }

        @Test
        @DisplayName("verify-domain-schema.sql sección 16 cubre expected-vs-actual de ambas tablas")
        void diagnosticoContieneSeccion16() {
            String diagnostico;
            try {
                diagnostico = DdlTestSupport.leer(dbRoot.resolve("diagnostics/verify-domain-schema.sql"));
            } catch (java.io.IOException e) {
                throw new RuntimeException("No se pudo leer verify-domain-schema.sql", e);
            }
            assertThat(diagnostico)
                    .contains("SECCIÓN 16")
                    .contains("FULL-R1.2-CORRECCION-04")
                    .contains("fal_documento_plantilla")
                    .contains("fal_documento_plantilla_contenido")
                    .contains("variables_declaradas_json")
                    .contains("COMMENT_DIVERGENTE")
                    .contains("COLUMNA_INESPERADA")
                    .contains("LONGITUD_DIVERGENTE")
                    .contains("LEFT JOIN");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PHY-G5: Auditoría DATETIME(6) + CHAR(36) en tablas clave")
    class G5_TiposAuditoria {

        private void assertFhAltaDatetime6(String tabla) {
            String cuerpo = cuerpo(tabla);
            boolean fhAltaDatetime6 = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("fh_alta") && l.contains("DATETIME(6)"));
            assertThat(fhAltaDatetime6)
                    .as("Tabla '%s': fh_alta debe ser DATETIME(6) — no DATETIME sin precision", tabla)
                    .isTrue();
        }

        private void assertIdUserAltaChar36(String tabla) {
            String cuerpo = cuerpo(tabla);
            boolean found = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("id_user_alta") && l.contains("CHAR(36)"));
            assertThat(found)
                    .as("Tabla '%s': id_user_alta debe ser CHAR(36)", tabla)
                    .isTrue();
        }

        @Test @DisplayName("fal_acta: fh_alta DATETIME(6) + id_user_alta CHAR(36)")
        void falActa_auditoria() {
            assertFhAltaDatetime6("fal_acta");
            assertIdUserAltaChar36("fal_acta");
        }

        @Test @DisplayName("fal_persona: fh_alta DATETIME(6) + id_user_alta CHAR(36)")
        void falPersona_auditoria() {
            assertFhAltaDatetime6("fal_persona");
            assertIdUserAltaChar36("fal_persona");
        }

        @Test @DisplayName("fal_dependencia: fh_alta DATETIME(6) + id_user_alta CHAR(36)")
        void falDependencia_auditoria() {
            assertFhAltaDatetime6("fal_dependencia");
            assertIdUserAltaChar36("fal_dependencia");
        }

        @Test @DisplayName("fal_inspector: fh_alta DATETIME(6) + id_user_alta CHAR(36)")
        void falInspector_auditoria() {
            assertFhAltaDatetime6("fal_inspector");
            assertIdUserAltaChar36("fal_inspector");
        }

        @Test @DisplayName("fal_observacion: fh_alta DATETIME(6) + id_user_alta CHAR(36)")
        void falObservacion_auditoria() {
            assertFhAltaDatetime6("fal_observacion");
            assertIdUserAltaChar36("fal_observacion");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PHY-G7: Delta FULL-R1.2-CORRECCION-02 — domicilios, acta y QR")
    class G7_DomiciliosActaQr {

        @Test
        @DisplayName("fal_persona_domicilio.calle_txt es VARCHAR(48)")
        void domicilio_calleTxt_varchar48() {
            assertColumnContains("fal_persona_domicilio", "calle_txt", "calle_txt presente");
            String cuerpo = cuerpo("fal_persona_domicilio");
            boolean esVar48 = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("calle_txt") && l.contains("VARCHAR(48)"));
            assertThat(esVar48)
                    .as("fal_persona_domicilio.calle_txt debe ser VARCHAR(48) — contrato HUMAN_DECISION_CLOSED")
                    .isTrue();
        }

        @Test
        @DisplayName("fal_persona_domicilio.domicilio_txt es VARCHAR(196)")
        void domicilio_domicilioTxt_varchar196() {
            String cuerpo = cuerpo("fal_persona_domicilio");
            boolean esVar196 = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("domicilio_txt") && l.contains("VARCHAR(196)"));
            assertThat(esVar196)
                    .as("fal_persona_domicilio.domicilio_txt debe ser VARCHAR(196) — contrato HUMAN_DECISION_CLOSED")
                    .isTrue();
        }

        @Test
        @DisplayName("fal_persona_domicilio.validacion_domicilio NO existe en DDL ni en Java")
        void domicilio_sinValidacionDomicilio() {
            assertColumnAbsente("fal_persona_domicilio", "validacion_domicilio",
                    "ELIMINADO: no reemplazar por otro texto ambiguo");
            // Verifica ausencia en el bloque DDL (cobertura de string en el DDL completo)
            String bloque = bloquesPorTabla.get("fal_persona_domicilio");
            assertThat(bloque)
                    .as("El bloque DDL de fal_persona_domicilio no debe contener 'validacion_domicilio'")
                    .doesNotContain("validacion_domicilio");
        }

        @Test
        @DisplayName("fal_acta.domicilio_hecho es VARCHAR(196)")
        void acta_domicilioHecho_varchar196() {
            String cuerpo = cuerpo("fal_acta");
            boolean esVar196 = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("domicilio_hecho") && l.contains("VARCHAR(196)"));
            assertThat(esVar196)
                    .as("fal_acta.domicilio_hecho debe ser VARCHAR(196) — contrato HUMAN_DECISION_CLOSED")
                    .isTrue();
        }

        @Test
        @DisplayName("fal_acta.dom_txt_infr es VARCHAR(196)")
        void acta_domTxtInfr_varchar196() {
            String cuerpo = cuerpo("fal_acta");
            boolean esVar196 = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("dom_txt_infr") && l.contains("VARCHAR(196)"));
            assertThat(esVar196)
                    .as("fal_acta.dom_txt_infr debe ser VARCHAR(196) — contrato HUMAN_DECISION_CLOSED")
                    .isTrue();
        }

        @Test
        @DisplayName("fal_acta.codigo_qr es VARCHAR(128) — token firmado, no payload masivo")
        void acta_codigoQr_varchar128() {
            String cuerpo = cuerpo("fal_acta");
            boolean esVar128 = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("codigo_qr") && l.contains("VARCHAR(128)"));
            assertThat(esVar128)
                    .as("fal_acta.codigo_qr debe ser VARCHAR(128) — token firmado formato QR0.<uuid>.<ver>.<hmac>")
                    .isTrue();
            boolean esVar512 = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("codigo_qr") && l.contains("VARCHAR(512)"));
            assertThat(esVar512)
                    .as("fal_acta.codigo_qr NO debe ser VARCHAR(512) — contrato HUMAN_DECISION_CLOSED es 128")
                    .isFalse();
        }

        @Test
        @DisplayName("fal_acta.qr_payload_version está presente")
        void acta_qrPayloadVersion_presente() {
            assertColumnContains("fal_acta", "qr_payload_version", "qr_payload_version requerido");
        }

        @Test
        @DisplayName("verify-domain-schema.sql sección 14 contiene expectativas FULL-R1.2-CORRECCION-02")
        void diagnosticoContieneSeccion14() {
            String diagnostico;
            try {
                diagnostico = DdlTestSupport.leer(dbRoot.resolve("diagnostics/verify-domain-schema.sql"));
            } catch (java.io.IOException e) {
                throw new RuntimeException("No se pudo leer verify-domain-schema.sql", e);
            }
            assertThat(diagnostico)
                    .as("verify-domain-schema.sql debe contener sección 14 del delta domicilios/acta/QR")
                    .contains("SECCIÓN 14")
                    .contains("calle_txt")
                    .contains("domicilio_txt")
                    .contains("domicilio_hecho")
                    .contains("dom_txt_infr")
                    .contains("codigo_qr")
                    .contains("validacion_domicilio")
                    .contains("COLUMNA_AUSENTE")
                    .contains("COLUMNA_INESPERADA")
                    .contains("LONGITUD_DIVERGENTE")
                    .contains("NULLABILITY_DIVERGENTE");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PHY-G10: Delta FULL-R1.2-CORRECCION-05 — correcciones integrales DDL")
    class G10_DeltaCorreccion05 {

        @Test
        @DisplayName("fal_documento_firma: id_firmante nullable, id_user_firma presente, rol_firmante presente, nombre_firmante VARCHAR(48), mensaje_error VARCHAR(512)")
        void falDocumentoFirma_camposCompletos() {
            String cuerpo = cuerpo("fal_documento_firma");
            // id_firmante debe ser BIGINT NULL (nullable — Long en Java)
            assertThat(cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("id_firmante") && l.contains("BIGINT") && !l.contains("NOT NULL")))
                    .as("fal_documento_firma.id_firmante debe ser BIGINT NULL (nullable - firma externa sin firmante en catalogo)")
                    .isTrue();
            assertColumnContains("fal_documento_firma", "id_user_firma", "id_user_firma requerido (JAVA: idUserFirma)");
            assertColumnContains("fal_documento_firma", "rol_firmante", "rol_firmante requerido (JAVA: rolFirmante)");
            assertColumnContains("fal_documento_firma", "nombre_firmante", "nombre_firmante requerido snapshot del firmante");
            assertThat(cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("nombre_firmante") && l.contains("VARCHAR(48)")))
                    .as("fal_documento_firma.nombre_firmante debe ser VARCHAR(48) — mismo ancho que fal_firmante.nom_firmante")
                    .isTrue();
            assertColumnContains("fal_documento_firma", "mensaje_error", "mensaje_error requerido (JAVA: mensajeError)");
            assertThat(cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("mensaje_error") && l.contains("VARCHAR(512)")))
                    .as("fal_documento_firma.mensaje_error debe ser VARCHAR(512)")
                    .isTrue();
        }

        @Test
        @DisplayName("fal_notificacion_acuse: canal NOT NULL, estado_acuse NOT NULL, intento_id presente, fh_acuse nullable")
        void falNotificacionAcuse_contratos() {
            String cuerpo = cuerpo("fal_notificacion_acuse");
            assertThat(cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("canal") && l.contains("SMALLINT") && l.contains("NOT NULL")))
                    .as("fal_notificacion_acuse.canal debe ser SMALLINT NOT NULL (SPEC-MODEL-DDL-CLOSURE-001)")
                    .isTrue();
            assertThat(cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("estado_acuse") && l.contains("SMALLINT") && l.contains("NOT NULL")))
                    .as("fal_notificacion_acuse.estado_acuse debe ser SMALLINT NOT NULL (SPEC-MODEL-DDL-CLOSURE-001)")
                    .isTrue();
            assertColumnContains("fal_notificacion_acuse", "intento_id", "intento_id FK a fal_notificacion_intento");
            assertThat(cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("fh_acuse") && l.contains("DATETIME(6)") && !l.contains("NOT NULL")))
                    .as("fal_notificacion_acuse.fh_acuse debe ser DATETIME(6) nullable (SPEC-MODEL-DDL-CLOSURE-001)")
                    .isTrue();
            // Campos eliminados
            assertColumnAbsente("fal_notificacion_acuse", "guid_acuse", "guid_acuse eliminado (SPEC-MODEL-DDL-CLOSURE-001)");
            assertColumnAbsente("fal_notificacion_acuse", "detalle", "detalle eliminado — usar fal_observacion(NOTIFICACION)");
        }

        @Test
        @DisplayName("fal_acta_pago_movimiento: motivo_aplicacion_pago_anterior ausente (HUMAN_DECISION_CLOSED)")
        void falActaPagoMovimiento_motivoAplicacionAusente() {
            assertColumnAbsente("fal_acta_pago_movimiento", "motivo_aplicacion_pago_anterior",
                    "HUMAN_DECISION_CLOSED: texto libre eliminado; idempotencia via movimiento_origen_id");
        }

        @Test
        @DisplayName("fal_notificacion_intento: canal_notif SMALLINT, domicilio_notif_id, destino_digital, lote_id, fh_intento presentes")
        void falNotificacionIntento_camposCompletos() {
            assertThat(cuerpo("fal_notificacion_intento").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("canal_notif") && l.contains("SMALLINT")))
                    .as("fal_notificacion_intento.canal_notif debe ser SMALLINT (SPEC-MODEL-DDL-CLOSURE-001: no VARCHAR)")
                    .isTrue();
            assertColumnContains("fal_notificacion_intento", "domicilio_notif_id", "domicilio_notif_id FK nullable");
            assertColumnContains("fal_notificacion_intento", "destino_digital", "destino_digital canal digital");
            assertColumnContains("fal_notificacion_intento", "lote_id", "lote_id FK a fal_lote_correo");
            assertColumnContains("fal_notificacion_intento", "fh_intento", "fh_intento timestamp del intento");
        }

        @Test
        @DisplayName("fal_normativa_faltas: tipo_norma, numero_norma, anio_norma presentes; fh_vig_desde DATE nullable")
        void falNormativaFaltas_camposCompletos() {
            assertColumnContains("fal_normativa_faltas", "tipo_norma", "tipo_norma SMALLINT NOT NULL");
            assertColumnContains("fal_normativa_faltas", "numero_norma", "numero_norma INT NULL");
            assertColumnContains("fal_normativa_faltas", "anio_norma", "anio_norma SMALLINT NULL");
            assertThat(cuerpo("fal_normativa_faltas").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("fh_vig_desde") && l.contains("DATE") && !l.contains("NOT NULL")))
                    .as("fal_normativa_faltas.fh_vig_desde debe ser DATE nullable")
                    .isTrue();
        }

        @Test
        @DisplayName("fal_articulo_normativa_faltas: codigo_articulo VARCHAR(16), nombre_articulo VARCHAR(64), tipo_infraccion NOT NULL")
        void falArticuloNormativaFaltas_contratos() {
            assertThat(cuerpo("fal_articulo_normativa_faltas").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("codigo_articulo") && l.contains("VARCHAR(16)")))
                    .as("fal_articulo_normativa_faltas.codigo_articulo debe ser VARCHAR(16) (HUMAN_DECISION_CLOSED)")
                    .isTrue();
            assertThat(cuerpo("fal_articulo_normativa_faltas").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("nombre_articulo") && l.contains("VARCHAR(64)")))
                    .as("fal_articulo_normativa_faltas.nombre_articulo debe ser VARCHAR(64) (HUMAN_DECISION_CLOSED)")
                    .isTrue();
            assertThat(cuerpo("fal_articulo_normativa_faltas").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("tipo_infraccion") && l.contains("SMALLINT") && l.contains("NOT NULL")))
                    .as("fal_articulo_normativa_faltas.tipo_infraccion debe ser SMALLINT NOT NULL")
                    .isTrue();
        }

        @Test
        @DisplayName("fal_vehiculo_modelo: codigo VARCHAR(12), nombre VARCHAR(24) — no nombre VARCHAR(64)")
        void falVehiculoModelo_contratos() {
            assertThat(cuerpo("fal_vehiculo_modelo").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("codigo") && l.contains("VARCHAR(12)")))
                    .as("fal_vehiculo_modelo.codigo debe ser VARCHAR(12)")
                    .isTrue();
            assertThat(cuerpo("fal_vehiculo_modelo").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("nombre") && l.contains("VARCHAR(24)")))
                    .as("fal_vehiculo_modelo.nombre debe ser VARCHAR(24) (no 64)")
                    .isTrue();
            assertThat(cuerpo("fal_vehiculo_modelo").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .noneMatch(l -> l.contains("nombre") && l.contains("VARCHAR(64)")))
                    .as("fal_vehiculo_modelo.nombre NO debe ser VARCHAR(64)")
                    .isTrue();
        }

        @Test
        @DisplayName("fal_persona_domicilio: lat/lon son DECIMAL(10,7) — no DECIMAL(12,8)")
        void falPersonaDomicilio_latLonDecimal10x7() {
            String cuerpo = cuerpo("fal_persona_domicilio");
            assertThat(cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("lat") && l.contains("DECIMAL(10,7)")))
                    .as("fal_persona_domicilio.lat debe ser DECIMAL(10,7) (HUMAN_DECISION_CLOSED)")
                    .isTrue();
            assertThat(cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("lon") && l.contains("DECIMAL(10,7)")))
                    .as("fal_persona_domicilio.lon debe ser DECIMAL(10,7) (HUMAN_DECISION_CLOSED)")
                    .isTrue();
        }

        @Test
        @DisplayName("num_talonario_inspector: estado_asignacion SMALLINT con enum ENTREGADO=1/DEVUELTO=2/CERRADO=3/OBSERVADO=4")
        void numTalonarioInspector_estadoAsignacion() {
            String bloque = bloquesPorTabla.get("num_talonario_inspector");
            assertThat(bloque)
                    .as("num_talonario_inspector.estado_asignacion debe tener ENTREGADO=1/DEVUELTO=2/CERRADO=3/OBSERVADO=4")
                    .contains("ENTREGADO")
                    .contains("DEVUELTO")
                    .contains("CERRADO")
                    .contains("OBSERVADO");
            assertThat(bloque)
                    .containsPattern("estado_asignacion\\s+BETWEEN\\s+1\\s+AND\\s+4");
        }
    }

    // ==========================================================
    // PHY-G11: Delta FULL-R1.2-CORRECCION-06 — OCC fal_documento_redaccion
    // ==========================================================

    @Nested
    @DisplayName("PHY-G11: Delta FULL-R1.2-CORRECCION-06 — OCC fal_documento_redaccion y drift documental")
    class PHY_G11 {

        @Test
        @DisplayName("fal_documento_redaccion: version_row INT NOT NULL DEFAULT 0 presente")
        void falDocumentoRedaccion_versionRow() {
            String cuerpo = cuerpo("fal_documento_redaccion");
            assertThat(cuerpo)
                    .as("fal_documento_redaccion debe tener version_row INT NOT NULL DEFAULT 0")
                    .containsPattern("version_row\\s+INT\\s+NOT NULL\\s+DEFAULT\\s+0");
        }

        @Test
        @DisplayName("fal_acta_fallo: fh_dictado presente — nombre canonico (no fecha_dictado)")
        void falActaFallo_fhDictado() {
            String cuerpo = cuerpo("fal_acta_fallo");
            assertThat(cuerpo)
                    .as("fal_acta_fallo debe usar fh_dictado, no fecha_dictado")
                    .contains("fh_dictado");
            assertColumnAbsente("fal_acta_fallo", "fecha_dictado", "fecha_dictado es nombre historico; canonico es fh_dictado");
        }

        @Test
        @DisplayName("fal_acta_fallo: fh_notificacion presente — nombre canonico (no fecha_notificacion)")
        void falActaFallo_fhNotificacion() {
            String cuerpo = cuerpo("fal_acta_fallo");
            assertThat(cuerpo)
                    .as("fal_acta_fallo debe usar fh_notificacion, no fecha_notificacion")
                    .contains("fh_notificacion");
            assertColumnAbsente("fal_acta_fallo", "fecha_notificacion", "fecha_notificacion es nombre historico; canonico es fh_notificacion");
        }

        @Test
        @DisplayName("fal_acta_fallo: fh_vto_apelacion es DATE (no DATETIME)")
        void falActaFallo_fhVtoApelacionDate() {
            String cuerpo = cuerpo("fal_acta_fallo");
            assertThat(cuerpo.lines()
                    .filter(l -> l.contains("fh_vto_apelacion"))
                    .anyMatch(l -> l.contains("DATE") && !l.contains("DATETIME")))
                    .as("fal_acta_fallo.fh_vto_apelacion debe ser DATE NULL (Java: LocalDate)")
                    .isTrue();
        }

        @Test
        @DisplayName("fal_acta_fallo: si_vigente presente")
        void falActaFallo_siVigente() {
            assertThat(cuerpo("fal_acta_fallo")).contains("si_vigente");
        }

        @Test
        @DisplayName("fal_acta: id_tecnico presente — nombre canonico (no uuid_tecnico)")
        void falActa_idTecnico() {
            String cuerpo = cuerpo("fal_acta");
            assertThat(cuerpo)
                    .as("fal_acta debe usar id_tecnico, no uuid_tecnico")
                    .contains("id_tecnico");
            assertColumnAbsente("fal_acta", "uuid_tecnico", "uuid_tecnico es nombre historico; canonico es id_tecnico");
        }

        @Test
        @DisplayName("fal_notificacion: canal SMALLINT NULL (nullable en PENDIENTE_ENVIO)")
        void falNotificacion_canalNullable() {
            String cuerpo = cuerpo("fal_notificacion");
            // Eliminar la parte COMMENT de la linea antes de verificar NOT NULL,
            // porque el comentario explica "NOT NULL tras iniciarEnvio" pero la
            // definicion de columna es NULL.
            assertThat(cuerpo.lines()
                    .filter(l -> l.strip().startsWith("canal"))
                    .anyMatch(l -> {
                        String def = l.contains("COMMENT") ? l.substring(0, l.indexOf("COMMENT")) : l;
                        return def.contains("SMALLINT") && !def.contains("NOT NULL");
                    }))
                    .as("fal_notificacion.canal debe ser SMALLINT NULL en la definicion (no NOT NULL)")
                    .isTrue();
        }
    }


    // ==========================================================
    // PHY-G12: Hallazgos auditoria externa CORRECCION-07
    // ==========================================================

    @Nested
    @DisplayName("PHY-G12: Hallazgos auditoria externa FULL-R1.2-CORRECCION-07 — F1..F6")
    class PHY_G12 {

        private String leerJava(String clase) {
            try {
                return DdlTestSupport.leer(java.nio.file.Paths.get(
                        "src/main/java/ar/gob/malvinas/faltas/core/domain/model/" + clase + ".java"));
            } catch (java.io.IOException e) {
                throw new RuntimeException("No se pudo leer " + clase + ".java", e);
            }
        }

        // ---- F1: doc_key usa codigos 2 y 3 para CUIT/CUIL ----

        @Test
        @DisplayName("F1: fal_persona.doc_key usa tipo_documento IN (2, 3) — CUIT=2, CUIL=3")
        void falPersona_docKey_usaCodigosCuitCuil_2_3() {
            String cuerpo = cuerpo("fal_persona");
            assertThat(cuerpo.lines()
                    .filter(l -> l.contains("doc_key"))
                    .anyMatch(l -> l.contains("tipo_documento IN (2, 3)")))
                    .as("fal_persona.doc_key debe usar tipo_documento IN (2, 3) — CUIT=2, CUIL=3 (TipoDocumentoPersona)")
                    .isTrue();
        }

        @Test
        @DisplayName("F1: fal_persona.doc_key NO usa (4, 5) — codigos incorrectos para CUIT/CUIL")
        void falPersona_docKey_noUsaCodigosErroneos_4_5() {
            String cuerpo = cuerpo("fal_persona");
            assertThat(cuerpo.lines()
                    .filter(l -> l.contains("doc_key"))
                    .anyMatch(l -> l.contains("tipo_documento IN (4, 5)")))
                    .as("fal_persona.doc_key NO debe usar tipo_documento IN (4, 5) — codigos PASAPORTE y DNI_EXTRANJERO, no CUIT/CUIL")
                    .isFalse();
        }

        @Test
        @DisplayName("F1: fal_persona UNIQUE (tipo_documento, doc_key) — no solo (doc_key)")
        void falPersona_uniqueCompuesta_tipoDocumento_docKey() {
            String cuerpo = cuerpo("fal_persona");
            assertThat(cuerpo)
                    .as("fal_persona debe tener UNIQUE KEY compuesta (tipo_documento, doc_key)")
                    .containsPattern("UNIQUE KEY[^(]+\\(tipo_documento, doc_key\\)");
        }

        @Test
        @DisplayName("F1: fal_persona no tiene UNIQUE simple solo sobre doc_key")
        void falPersona_noUniqueSimpleDocKey() {
            String cuerpo = cuerpo("fal_persona");
            boolean unicaSimple = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("UNIQUE") && l.contains("(doc_key)") && !l.contains("tipo_documento"));
                    assertThat(unicaSimple)
                    .as("fal_persona NO debe tener UNIQUE simple solo sobre doc_key — debe ser compuesta con tipo_documento")
                    .isFalse();
        }

        // ---- F2: CHECK autorreferencial en fal_dependencia ----

        @Test
        @DisplayName("F2: fal_dependencia tiene CONSTRAINT chk_dep_no_self CHECK (id_dep_padre IS NULL OR id_dep_padre <> id)")
        void falDependencia_checkAutoReferencial() {
            String cuerpo = cuerpo("fal_dependencia");
            assertThat(cuerpo)
                    .as("fal_dependencia debe tener CHECK autorreferencial chk_dep_no_self")
                    .contains("chk_dep_no_self")
                    .containsPattern("id_dep_padre IS NULL OR id_dep_padre <> id");
        }

        // ---- F3: fh_ult_mod NULL en las 7 tablas ----

        private void assertFhUltModNullable(String tabla) {
            String cuerpo = cuerpo(tabla);
            boolean esNull = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .filter(l -> l.contains("fh_ult_mod"))
                    .anyMatch(l -> {
                        String def = l.contains("COMMENT") ? l.substring(0, l.indexOf("COMMENT")) : l;
                        return def.contains("DATETIME(6)") && !def.contains("NOT NULL");
                    });
                    assertThat(esNull)
                    .as("Tabla %s: fh_ult_mod debe ser DATETIME(6) NULL (HUMAN_DECISION_CLOSED CORRECCION-07)", tabla)
                    .isTrue();
        }

        @Test @DisplayName("F3: fal_persona.fh_ult_mod es NULL") void fhUltMod_falPersona() { assertFhUltModNullable("fal_persona"); }
        @Test @DisplayName("F3: num_politica.fh_ult_mod es NULL") void fhUltMod_numPolitica() { assertFhUltModNullable("num_politica"); }
        @Test @DisplayName("F3: fal_motivo_archivo.fh_ult_mod es NULL") void fhUltMod_falMotivoArchivo() { assertFhUltModNullable("fal_motivo_archivo"); }
        @Test @DisplayName("F3: fal_persona_domicilio.fh_ult_mod es NULL") void fhUltMod_falPersonaDomicilio() { assertFhUltModNullable("fal_persona_domicilio"); }
        @Test @DisplayName("F3: fal_acta.fh_ult_mod es NULL") void fhUltMod_falActa() { assertFhUltModNullable("fal_acta"); }
        @Test @DisplayName("F3: fal_notificacion.fh_ult_mod es NULL") void fhUltMod_falNotificacion() { assertFhUltModNullable("fal_notificacion"); }
        @Test @DisplayName("F3: fal_notificacion_intento.fh_ult_mod es NULL") void fhUltMod_falNotificacionIntento() { assertFhUltModNullable("fal_notificacion_intento"); }

        // ---- F4: cuatro campos DDL en fal_documento_redaccion ----

        @Test
        @DisplayName("F4 DDL: fal_documento_redaccion tiene nro_revision SMALLINT NOT NULL")
        void falDocumentoRedaccion_nroRevision() {
            String cuerpo = cuerpo("fal_documento_redaccion");
            assertThat(cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("nro_revision") && l.contains("SMALLINT") && l.contains("NOT NULL")))
                    .as("fal_documento_redaccion debe tener nro_revision SMALLINT NOT NULL")
                    .isTrue();
        }

        @Test
        @DisplayName("F4 DDL: fal_documento_redaccion tiene redaccion_origen_id BIGINT NULL")
        void falDocumentoRedaccion_redaccionOrigenId() {
            String cuerpo = cuerpo("fal_documento_redaccion");
            assertThat(cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("redaccion_origen_id") && l.contains("BIGINT")))
                    .as("fal_documento_redaccion debe tener redaccion_origen_id BIGINT NULL")
                    .isTrue();
        }

        @Test
        @DisplayName("F4 DDL: fal_documento_redaccion tiene fh_anulacion DATETIME(6) NULL")
        void falDocumentoRedaccion_fhAnulacion() {
            assertThat(cuerpo("fal_documento_redaccion"))
                    .as("fal_documento_redaccion debe tener fh_anulacion")
                    .contains("fh_anulacion");
        }

        @Test
        @DisplayName("F4 DDL: fal_documento_redaccion tiene id_user_anulacion CHAR(36) NULL")
        void falDocumentoRedaccion_idUserAnulacion() {
            assertThat(cuerpo("fal_documento_redaccion"))
                    .as("fal_documento_redaccion debe tener id_user_anulacion")
                    .contains("id_user_anulacion");
        }

        @Test
        @DisplayName("F4 Java: FalDocumentoRedaccion tiene los cuatro campos de F4")
        void falDocumentoRedaccionJava_cuatroCampos() {
            String java = leerJava("FalDocumentoRedaccion");
            assertThat(java)
                    .as("FalDocumentoRedaccion debe declarar nroRevision")
                    .contains("nroRevision");
            assertThat(java)
                    .as("FalDocumentoRedaccion debe declarar redaccionOrigenId")
                    .contains("redaccionOrigenId");
            assertThat(java)
                    .as("FalDocumentoRedaccion debe declarar fhAnulacion")
                    .contains("fhAnulacion");
            assertThat(java)
                    .as("FalDocumentoRedaccion debe declarar idUserAnulacion")
                    .contains("idUserAnulacion");
        }

        @Test
        @DisplayName("F4 Java: FalDocumentoRedaccion valida nroRevision >= 1")
        void falDocumentoRedaccionJava_validaNroRevision() {
            String java = leerJava("FalDocumentoRedaccion");
            assertThat(java)
                    .as("FalDocumentoRedaccion debe validar nroRevision < 1")
                    .contains("nroRevision < 1");
        }

        @Test
        @DisplayName("F4 Java: FalDocumentoRedaccion tiene metodo anular()")
        void falDocumentoRedaccionJava_tieneAnular() {
            String java = leerJava("FalDocumentoRedaccion");
            assertThat(java)
                    .as("FalDocumentoRedaccion debe tener metodo anular()")
                    .contains("public void anular(");
        }

        // ---- F4 Reglas de revision/anulacion ----

        @Test
        @DisplayName("F4 regla: anular requiere fhAnulacion — fallo si null")
        void falDocumentoRedaccion_anularRequiereFecha() {
            var r = new ar.gob.malvinas.faltas.core.domain.model.FalDocumentoRedaccion(
                    1L, 100L, 10L, (short) 1, null,
                    ar.gob.malvinas.faltas.core.domain.enums.EstadoRedaccionDocumento.BORRADOR, "Contenido.",
                    "{}", null, null,
                    java.time.LocalDateTime.now(), "usr",
                    null, null, null, null);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> r.anular(null, "usr", "Motivo."))
                    .isInstanceOf(ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException.class);
        }

        // ---- F5: canal_notif en fal_notificacion_intento ----

        @Test
        @DisplayName("F5: fal_notificacion_intento usa canal_notif (no canal) como nombre de columna")
        void falNotificacionIntento_usaCanalNotif() {
            assertThat(cuerpo("fal_notificacion_intento").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.strip().startsWith("canal_notif") && l.contains("SMALLINT")))
                    .as("fal_notificacion_intento debe usar canal_notif SMALLINT (nombre canonico SPEC-MODEL-DDL-CLOSURE-001)")
                    .isTrue();
        }

        @Test
        @DisplayName("F5: fal_notificacion_intento NO tiene columna llamada solo canal")
        void falNotificacionIntento_noCanalSimple() {
            boolean tieneCanal = cuerpo("fal_notificacion_intento").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> {
                        String s = l.strip();
                        return (s.startsWith("canal ") || s.startsWith("canal\t")) && !s.startsWith("canal_notif");
                    });
                    assertThat(tieneCanal)
                    .as("fal_notificacion_intento NO debe tener columna llamada canal (solo canal_notif)")
                    .isFalse();
        }

        // ---- F6: longitudes de num_politica ----

        @Test
        @DisplayName("F6: num_politica.prefijo es VARCHAR(10) — no VARCHAR(8)")
        void numPolitica_prefijo_varchar10() {
            String cuerpo = cuerpo("num_politica");
            assertThat(cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("prefijo") && l.contains("VARCHAR(10)")))
                    .as("num_politica.prefijo debe ser VARCHAR(10) (HUMAN_DECISION_CLOSED CORRECCION-07)")
                    .isTrue();
            assertThat(cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("prefijo") && l.contains("VARCHAR(8)")))
                    .as("num_politica.prefijo NO debe ser VARCHAR(8) — longitud incorrecta")
                    .isFalse();
        }

        @Test
        @DisplayName("F6: num_politica.formato_visible es VARCHAR(60) — no VARCHAR(64)")
        void numPolitica_formatoVisible_varchar60() {
            String cuerpo = cuerpo("num_politica");
            assertThat(cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("formato_visible") && l.contains("VARCHAR(60)")))
                    .as("num_politica.formato_visible debe ser VARCHAR(60) (HUMAN_DECISION_CLOSED CORRECCION-07)")
                    .isTrue();
            assertThat(cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .anyMatch(l -> l.contains("formato_visible") && l.contains("VARCHAR(64)")))
                    .as("num_politica.formato_visible NO debe ser VARCHAR(64) — longitud incorrecta")
                    .isFalse();
        }
    }
}
