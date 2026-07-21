package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenNomenclatura;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;
import ar.gob.malvinas.faltas.core.domain.model.FalActaContravencion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.gob.malvinas.faltas.core.application.DdlTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardrails DECISION_DDL-PERS-02 — familia id_suj / id_bie.
 *
 * <p>Verifica tipos físicos en DDL, rangos, ausencia de catálogo cerrado y
 * uso de {@code Integer} en Java para los tres artefactos afectados.
 * HUMAN_DECISION_CLOSED — FULL-R1.2-CORRECCION-10
 */
@DisplayName("Guardrails familia id_suj/id_bie (DECISION_DDL-PERS-02 / FULL-R1.2-CORRECCION-10)")
class IdSujBieGuardrailTest {

    private static Map<String, String> bloquesPorTabla;
    private static String sqlCompleto;

    @BeforeAll
    static void cargarDdl() throws IOException {
        Path dbRoot = resolveDbRoot();
        sqlCompleto = leer(dbRoot.resolve("ddl/create-bod-faltas-domain.sql"));
        bloquesPorTabla = extraerBloquesDdl(sqlCompleto)
                .stream()
                .collect(Collectors.toMap(DdlTestSupport::extraerNombreTabla, b -> b));
    }

    private String cuerpo(String tabla) {
        String bloque = bloquesPorTabla.get(tabla);
        assertThat(bloque).as("Bloque DDL de '%s' debe existir", tabla).isNotNull();
        return extraerCuerpo(bloque);
    }

    private boolean cuerpoContiene(String tabla, String fragmento) {
        return cuerpo(tabla).lines()
                .filter(l -> !l.strip().startsWith("--"))
                .anyMatch(l -> l.contains(fragmento));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tipos físicos DDL — fal_persona
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DDL fal_persona — id_suj TINYINT UNSIGNED / id_bie MEDIUMINT UNSIGNED")
    class FalPersonaDdl {

        @Test
        @DisplayName("id_suj es TINYINT UNSIGNED NULL")
        void id_suj_tinyint_unsigned() {
            assertThat(cuerpoContiene("fal_persona", "id_suj"))
                    .as("fal_persona debe tener columna id_suj").isTrue();
            assertThat(cuerpo("fal_persona").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .filter(l -> l.contains("id_suj"))
                    .anyMatch(l -> l.contains("TINYINT UNSIGNED")))
                    .as("fal_persona.id_suj debe ser TINYINT UNSIGNED").isTrue();
            assertThat(cuerpo("fal_persona").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .filter(l -> l.contains("id_suj") && !l.contains("id_suj_"))
                    .anyMatch(l -> l.contains("BIGINT")))
                    .as("fal_persona.id_suj NO debe ser BIGINT").isFalse();
        }

        @Test
        @DisplayName("id_bie es MEDIUMINT UNSIGNED NULL")
        void id_bie_mediumint_unsigned() {
            assertThat(cuerpoContiene("fal_persona", "id_bie"))
                    .as("fal_persona debe tener columna id_bie").isTrue();
            assertThat(cuerpo("fal_persona").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .filter(l -> l.contains("id_bie") && !l.contains("id_bie_"))
                    .anyMatch(l -> l.contains("MEDIUMINT UNSIGNED")))
                    .as("fal_persona.id_bie debe ser MEDIUMINT UNSIGNED").isTrue();
            assertThat(cuerpo("fal_persona").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .filter(l -> l.contains("id_bie") && !l.contains("id_bie_"))
                    .anyMatch(l -> l.contains("BIGINT")))
                    .as("fal_persona.id_bie NO debe ser BIGINT").isFalse();
        }

        @Test
        @DisplayName("id_suj tiene CHECK BETWEEN 1 AND 255 (rango abierto aprobado)")
        void check_id_suj_rango() {
            assertThat(cuerpo("fal_persona")).contains("id_suj BETWEEN 1 AND 255");
        }

        @Test
        @DisplayName("id_bie tiene CHECK BETWEEN 1 AND 9999999")
        void check_id_bie_rango() {
            assertThat(cuerpo("fal_persona")).contains("id_bie BETWEEN 1 AND 9999999");
        }

        @Test
        @DisplayName("id_bie requiere id_suj (CHECK id_bie IS NULL OR id_suj IS NOT NULL)")
        void check_id_bie_requiere_id_suj() {
            assertThat(cuerpo("fal_persona"))
                    .contains("id_bie IS NULL OR id_suj IS NOT NULL");
        }

        @Test
        @DisplayName("Ausencia de catálogo cerrado IN (1, 2, 3, 18, 20, 99) para id_suj")
        void sin_catalogo_cerrado() {
            String cuerpo = cuerpo("fal_persona");
            assertThat(cuerpo).doesNotContainPattern("id_suj\\s+IN\\s*\\(");
            assertThat(cuerpo).doesNotContain("IN (1, 2, 3, 18, 20, 99)");
            assertThat(cuerpo).doesNotContain("IN (1,2,3,18,20,99)");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tipos físicos DDL — fal_acta_contravencion
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DDL fal_acta_contravencion — id_suj_i/c TINYINT UNSIGNED / id_bie_i/c MEDIUMINT UNSIGNED")
    class FalActaContravencionDdl {

        @Test
        @DisplayName("id_suj_i es TINYINT UNSIGNED NULL")
        void id_suj_i_tinyint_unsigned() {
            assertThat(cuerpo("fal_acta_contravencion").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .filter(l -> l.strip().startsWith("id_suj_i"))
                    .anyMatch(l -> l.contains("TINYINT UNSIGNED")))
                    .as("fal_acta_contravencion.id_suj_i debe ser TINYINT UNSIGNED").isTrue();
        }

        @Test
        @DisplayName("id_suj_c es TINYINT UNSIGNED NULL")
        void id_suj_c_tinyint_unsigned() {
            assertThat(cuerpo("fal_acta_contravencion").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .filter(l -> l.strip().startsWith("id_suj_c"))
                    .anyMatch(l -> l.contains("TINYINT UNSIGNED")))
                    .as("fal_acta_contravencion.id_suj_c debe ser TINYINT UNSIGNED").isTrue();
        }

        @Test
        @DisplayName("id_bie_i es MEDIUMINT UNSIGNED NULL")
        void id_bie_i_mediumint_unsigned() {
            assertThat(cuerpo("fal_acta_contravencion").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .filter(l -> l.strip().startsWith("id_bie_i"))
                    .anyMatch(l -> l.contains("MEDIUMINT UNSIGNED")))
                    .as("fal_acta_contravencion.id_bie_i debe ser MEDIUMINT UNSIGNED").isTrue();
        }

        @Test
        @DisplayName("id_bie_c es MEDIUMINT UNSIGNED NULL")
        void id_bie_c_mediumint_unsigned() {
            assertThat(cuerpo("fal_acta_contravencion").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .filter(l -> l.strip().startsWith("id_bie_c"))
                    .anyMatch(l -> l.contains("MEDIUMINT UNSIGNED")))
                    .as("fal_acta_contravencion.id_bie_c debe ser MEDIUMINT UNSIGNED").isTrue();
        }

        @Test
        @DisplayName("id_suj_i CHECK BETWEEN 1 AND 255 (antes BETWEEN 1 AND 99)")
        void check_id_suj_i_255() {
            assertThat(cuerpo("fal_acta_contravencion"))
                    .as("id_suj_i debe tener BETWEEN 1 AND 255 (no BETWEEN 1 AND 99)")
                    .contains("id_suj_i BETWEEN 1 AND 255");
            assertThat(cuerpo("fal_acta_contravencion"))
                    .doesNotContain("id_suj_i BETWEEN 1 AND 99");
        }

        @Test
        @DisplayName("id_suj_c CHECK BETWEEN 1 AND 255 (antes BETWEEN 1 AND 99)")
        void check_id_suj_c_255() {
            assertThat(cuerpo("fal_acta_contravencion"))
                    .as("id_suj_c debe tener BETWEEN 1 AND 255 (no BETWEEN 1 AND 99)")
                    .contains("id_suj_c BETWEEN 1 AND 255");
            assertThat(cuerpo("fal_acta_contravencion"))
                    .doesNotContain("id_suj_c BETWEEN 1 AND 99");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tipos físicos DDL — fal_acta_snapshot
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DDL fal_acta_snapshot — id_bie_i/c MEDIUMINT UNSIGNED")
    class FalActaSnapshotDdl {

        @Test
        @DisplayName("id_bie_i es MEDIUMINT UNSIGNED NULL (antes BIGINT)")
        void id_bie_i_mediumint_unsigned() {
            assertThat(cuerpo("fal_acta_snapshot").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .filter(l -> l.strip().startsWith("id_bie_i"))
                    .anyMatch(l -> l.contains("MEDIUMINT UNSIGNED")))
                    .as("fal_acta_snapshot.id_bie_i debe ser MEDIUMINT UNSIGNED").isTrue();
            assertThat(cuerpo("fal_acta_snapshot").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .filter(l -> l.strip().startsWith("id_bie_i"))
                    .anyMatch(l -> l.contains("BIGINT")))
                    .as("fal_acta_snapshot.id_bie_i NO debe ser BIGINT").isFalse();
        }

        @Test
        @DisplayName("id_bie_c es MEDIUMINT UNSIGNED NULL (antes BIGINT)")
        void id_bie_c_mediumint_unsigned() {
            assertThat(cuerpo("fal_acta_snapshot").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .filter(l -> l.strip().startsWith("id_bie_c"))
                    .anyMatch(l -> l.contains("MEDIUMINT UNSIGNED")))
                    .as("fal_acta_snapshot.id_bie_c debe ser MEDIUMINT UNSIGNED").isTrue();
            assertThat(cuerpo("fal_acta_snapshot").lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .filter(l -> l.strip().startsWith("id_bie_c"))
                    .anyMatch(l -> l.contains("BIGINT")))
                    .as("fal_acta_snapshot.id_bie_c NO debe ser BIGINT").isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Java usa Integer — FalPersona
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Java FalPersona — idSuj/idBie son Integer")
    class FalPersonaJava {

        @Test
        @DisplayName("FalPersona.idSuj es Integer (no Long)")
        void idSuj_es_Integer() throws Exception {
            Field f = FalPersona.class.getDeclaredField("idSuj");
            assertThat(f.getType()).isEqualTo(Integer.class);
        }

        @Test
        @DisplayName("FalPersona.idBie es Integer (no Long)")
        void idBie_es_Integer() throws Exception {
            Field f = FalPersona.class.getDeclaredField("idBie");
            assertThat(f.getType()).isEqualTo(Integer.class);
        }

        @Test
        @DisplayName("setIdSuj acepta Integer")
        void setIdSuj_acepta_Integer() throws Exception {
            var setter = FalPersona.class.getMethod("setIdSuj", Integer.class);
            assertThat(setter).isNotNull();
        }

        @Test
        @DisplayName("setIdBie acepta Integer")
        void setIdBie_acepta_Integer() throws Exception {
            var setter = FalPersona.class.getMethod("setIdBie", Integer.class);
            assertThat(setter).isNotNull();
        }

        @Test
        @DisplayName("La copia de FalPersona preserva idSuj e idBie")
        void copia_preserva_idSuj_idBie() {
            LocalDateTime now = LocalDateTime.of(2026, 7, 21, 10, 0);
            FalPersona p = new FalPersona(1L, TipoPersona.FISICA, now, "test");
            p.setIdSuj(20);
            p.setIdBie(12345);
            FalPersona c = p.copia();
            assertThat(c.getIdSuj()).isEqualTo(20);
            assertThat(c.getIdBie()).isEqualTo(12345);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Java usa Integer — FalActaContravencion
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Java FalActaContravencion — idSujI/idBieI/idSujC/idBieC son Integer")
    class FalActaContravencionJava {

        @Test
        @DisplayName("idSujI es Integer")
        void idSujI_es_Integer() throws Exception {
            Field f = FalActaContravencion.class.getDeclaredField("idSujI");
            assertThat(f.getType()).isEqualTo(Integer.class);
        }

        @Test
        @DisplayName("idBieI es Integer")
        void idBieI_es_Integer() throws Exception {
            Field f = FalActaContravencion.class.getDeclaredField("idBieI");
            assertThat(f.getType()).isEqualTo(Integer.class);
        }

        @Test
        @DisplayName("idSujC es Integer")
        void idSujC_es_Integer() throws Exception {
            Field f = FalActaContravencion.class.getDeclaredField("idSujC");
            assertThat(f.getType()).isEqualTo(Integer.class);
        }

        @Test
        @DisplayName("idBieC es Integer")
        void idBieC_es_Integer() throws Exception {
            Field f = FalActaContravencion.class.getDeclaredField("idBieC");
            assertThat(f.getType()).isEqualTo(Integer.class);
        }

        @Test
        @DisplayName("La copia de FalActaContravencion preserva idSujI, idBieI, idSujC, idBieC")
        void copia_preserva_id_suj_bie() {
            LocalDateTime now = LocalDateTime.of(2026, 7, 21, 10, 0);
            FalActaContravencion ctv = new FalActaContravencion(
                    1L, OrigenNomenclatura.CATASTRO, false, now, "test");
            ctv.setSujetoInmueble(1, 100);
            ctv.setSujetoComercio(2, 200);
            FalActaContravencion c = ctv.copia();
            assertThat(c.getIdSujI()).isEqualTo(1);
            assertThat(c.getIdBieI()).isEqualTo(100);
            assertThat(c.getIdSujC()).isEqualTo(2);
            assertThat(c.getIdBieC()).isEqualTo(200);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Java usa Integer — FalActaSnapshot
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Java FalActaSnapshot — idBieI/idBieC son Integer")
    class FalActaSnapshotJava {

        @Test
        @DisplayName("idBieI es Integer")
        void idBieI_es_Integer() throws Exception {
            Field f = FalActaSnapshot.class.getDeclaredField("idBieI");
            assertThat(f.getType()).isEqualTo(Integer.class);
        }

        @Test
        @DisplayName("idBieC es Integer")
        void idBieC_es_Integer() throws Exception {
            Field f = FalActaSnapshot.class.getDeclaredField("idBieC");
            assertThat(f.getType()).isEqualTo(Integer.class);
        }

        @Test
        @DisplayName("La copia de FalActaSnapshot preserva idBieI e idBieC")
        void copia_preserva_idBieI_idBieC() {
            FalActaSnapshot snap = new FalActaSnapshot(1L);
            snap.setIdBieI(500);
            snap.setIdBieC(600);
            FalActaSnapshot c = snap.copia();
            assertThat(c.getIdBieI()).isEqualTo(500);
            assertThat(c.getIdBieC()).isEqualTo(600);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fronteras de id_suj e id_bie
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Fronteras id_suj (1-255) e id_bie (1-9999999)")
    class Fronteras {

        @Test
        @DisplayName("id_suj=1 es frontera inferior valida")
        void id_suj_frontera_inferior() {
            LocalDateTime now = LocalDateTime.of(2026, 7, 21, 10, 0);
            FalPersona p = new FalPersona(1L, TipoPersona.FISICA, now, "test");
            p.setIdSuj(1);
            assertThat(p.getIdSuj()).isEqualTo(1);
        }

        @Test
        @DisplayName("id_suj=255 es frontera superior valida")
        void id_suj_frontera_superior() {
            LocalDateTime now = LocalDateTime.of(2026, 7, 21, 10, 0);
            FalPersona p = new FalPersona(1L, TipoPersona.FISICA, now, "test");
            p.setIdSuj(255);
            assertThat(p.getIdSuj()).isEqualTo(255);
        }

        @Test
        @DisplayName("id_bie=1 es frontera inferior valida")
        void id_bie_frontera_inferior() {
            LocalDateTime now = LocalDateTime.of(2026, 7, 21, 10, 0);
            FalPersona p = new FalPersona(1L, TipoPersona.FISICA, now, "test");
            p.setIdSuj(20);
            p.setIdBie(1);
            assertThat(p.getIdBie()).isEqualTo(1);
        }

        @Test
        @DisplayName("id_bie=9999999 es frontera superior valida")
        void id_bie_frontera_superior() {
            LocalDateTime now = LocalDateTime.of(2026, 7, 21, 10, 0);
            FalPersona p = new FalPersona(1L, TipoPersona.FISICA, now, "test");
            p.setIdSuj(20);
            p.setIdBie(9_999_999);
            assertThat(p.getIdBie()).isEqualTo(9_999_999);
        }
    }
}
