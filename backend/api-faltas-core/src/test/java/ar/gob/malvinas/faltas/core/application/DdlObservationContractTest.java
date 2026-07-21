package ar.gob.malvinas.faltas.core.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ar.gob.malvinas.faltas.core.application.DdlTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardrails del contrato de observaciones polimórficas centralizadas.
 *
 * <p>Verifica sin conexión a base de datos que {@code database/ddl/create-bod-faltas-domain.sql}
 * implementa correctamente el contrato de {@code fal_observacion} definido en
 * {@code observaciones.md} (SPEC-MODEL-DDL-CLOSURE-001).
 *
 * <ul>
 *   <li>OBS-G1: {@code fal_observacion} tiene las columnas y tipos exactos del contrato.</li>
 *   <li>OBS-G2: {@code observacion VARCHAR(512)} — límite exacto.</li>
 *   <li>OBS-G3: CHECKs {@code chk_obs_texto} y {@code chk_obs_baja_coherente} presentes.</li>
 *   <li>OBS-G4: {@code id_acta_contexto} → {@code fal_acta(id) ON DELETE CASCADE}.</li>
 *   <li>OBS-G5: exactamente 21 triggers {@code AFTER DELETE} para observaciones (códigos 2–22).</li>
 *   <li>OBS-G6: nombres de triggers siguen la convención {@code trg_<tabla>_ad_observaciones}.</li>
 *   <li>OBS-G7: campos libres prohibidos eliminados de las tablas correspondientes.</li>
 * </ul>
 *
 * <p>No usa Mockito, no usa base de datos. Solo lectura de archivos en {@code database/}.
 */
@DisplayName("Guardrails OBS: contrato de observaciones polimórficas centralizadas")
class DdlObservationContractTest {

    private static Path dbRoot;
    private static String ddlCompleto;
    private static Map<String, String> bloquesPorTabla;

    @BeforeAll
    static void cargarDdl() throws IOException {
        dbRoot = resolveDbRoot();
        ddlCompleto = leer(dbRoot.resolve("ddl/create-bod-faltas-domain.sql"));
        List<String> bloques = extraerBloquesDdl(ddlCompleto);
        bloquesPorTabla = bloques.stream()
                .collect(Collectors.toMap(DdlTestSupport::extraerNombreTabla, b -> b));
    }

    private String cuerpo(String tabla) {
        String bloque = bloquesPorTabla.get(tabla);
        assertThat(bloque).as("Bloque DDL de '%s' debe existir", tabla).isNotNull();
        return extraerCuerpo(bloque);
    }

    private void assertColumnContains(String tabla, String fragment, String descripcion) {
        String cuerpo = cuerpo(tabla);
        boolean found = cuerpo.lines()
                .filter(l -> !l.strip().startsWith("--"))
                .anyMatch(l -> l.contains(fragment));
        assertThat(found)
                .as("Tabla '%s': esperado '%s' (%s)", tabla, fragment, descripcion)
                .isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("OBS-G1: fal_observacion tiene columnas y tipos exactos")
    class G1_ColumnasFalObservacion {

        @Test
        @DisplayName("entidad_tipo SMALLINT NOT NULL, entidad_id BIGINT NOT NULL")
        void entidadTipo_y_entidadId() {
            assertColumnContains("fal_observacion", "entidad_tipo        SMALLINT       NOT NULL", "polimorfismo tipo");
            assertColumnContains("fal_observacion", "entidad_id          BIGINT         NOT NULL", "polimorfismo id");
        }

        @Test
        @DisplayName("id_acta_contexto BIGINT NULL")
        void idActaContexto_nullable() {
            assertColumnContains("fal_observacion", "id_acta_contexto    BIGINT         NULL", "contexto de acta nullable");
        }

        @Test
        @DisplayName("origen_observacion SMALLINT NOT NULL")
        void origenObservacion_smallint() {
            assertColumnContains("fal_observacion", "origen_observacion  SMALLINT       NOT NULL", "origen SMALLINT");
        }

        @Test
        @DisplayName("si_activa BOOLEAN NOT NULL DEFAULT TRUE")
        void siActiva_booleanDefault() {
            assertColumnContains("fal_observacion", "si_activa           BOOLEAN        NOT NULL DEFAULT TRUE", "baja logica");
        }

        @Test
        @DisplayName("fh_baja DATETIME(6) NULL, id_user_baja CHAR(36) NULL")
        void baja_nullable() {
            assertColumnContains("fal_observacion", "fh_baja             DATETIME(6)    NULL", "timestamp baja");
            assertColumnContains("fal_observacion", "id_user_baja        CHAR(36)       NULL", "actor baja");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("OBS-G2: observacion VARCHAR(512) — límite exacto cerrado")
    class G2_LongitudObservacion {

        @Test
        @DisplayName("observacion es VARCHAR(512) NOT NULL")
        void observacion_varchar512() {
            assertColumnContains("fal_observacion", "observacion         VARCHAR(512)   NOT NULL", "limite 512 cerrado");
        }

        @Test
        @DisplayName("observacion no es VARCHAR(1000) ni VARCHAR(500)")
        void observacion_noLongitudAnterior() {
            String cuerpo = cuerpo("fal_observacion");
            assertThat(cuerpo)
                    .as("observacion no debe ser VARCHAR(1000)")
                    .doesNotContain("observacion         VARCHAR(1000)")
                    .doesNotContain("observacion         VARCHAR(500)");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("OBS-G3: CHECKs de texto y coherencia de baja presentes")
    class G3_Checks {

        @Test
        @DisplayName("chk_obs_texto CHECK BETWEEN 1 AND 512")
        void chkObsTexto() {
            String bloque = bloquesPorTabla.get("fal_observacion");
            assertThat(bloque)
                    .as("chk_obs_texto debe contener BETWEEN 1 AND 512")
                    .contains("chk_obs_texto")
                    .contains("BETWEEN 1 AND 512");
        }

        @Test
        @DisplayName("chk_obs_baja_coherente CHECK (si_activa / fh_baja / id_user_baja)")
        void chkObsBajaCoherente() {
            String bloque = bloquesPorTabla.get("fal_observacion");
            assertThat(bloque)
                    .as("chk_obs_baja_coherente debe existir")
                    .contains("chk_obs_baja_coherente");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("OBS-G4: id_acta_contexto → fal_acta(id) ON DELETE CASCADE")
    class G4_FkCascade {

        @Test
        @DisplayName("FK fk_obs_acta_contexto usa ON DELETE CASCADE")
        void fkActaContexto_cascade() {
            String bloque = bloquesPorTabla.get("fal_observacion");
            assertThat(bloque)
                    .as("FK de id_acta_contexto debe ser ON DELETE CASCADE")
                    .containsPattern("(?i)id_acta_contexto[\\s\\S]{0,200}ON DELETE CASCADE");
        }

        @Test
        @DisplayName("fal_observacion no tiene FK física por entidad_id (sin referencia directa)")
        void sinFkFisicaEntidadId() {
            String cuerpo = cuerpo("fal_observacion");
            long fkCountEntidadId = cuerpo.lines()
                    .filter(l -> l.toUpperCase().contains("FOREIGN KEY") && l.contains("entidad_id"))
                    .count();
            assertThat(fkCountEntidadId)
                    .as("entidad_id no debe tener FK física (polimorfismo)")
                    .isEqualTo(0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("OBS-G5: exactamente 21 triggers AFTER DELETE para observaciones (códigos 2–22)")
    class G5_ConteoTriggers {

        @Test
        @DisplayName("21 triggers _ad_observaciones en el script")
        void conteo_triggers_ad() {
            Matcher m = Pattern.compile("CREATE\\s+(?:DEFINER\\s*=\\s*\\S+\\s+)?TRIGGER\\s+(\\w+)",
                    Pattern.CASE_INSENSITIVE).matcher(ddlCompleto);
            List<String> triggerObs = new java.util.ArrayList<>();
            while (m.find()) {
                String nombre = m.group(1);
                if (nombre.endsWith("_ad_observaciones")) {
                    triggerObs.add(nombre);
                }
            }
            assertThat(triggerObs)
                    .as("Deben existir exactamente 21 triggers _ad_observaciones (códigos 2-22 de EntidadTipoObservada)")
                    .hasSize(21);
        }

        @Test
        @DisplayName("fal_acta (código 1) no tiene trigger: ON DELETE CASCADE lo cubre")
        void falActa_sinTrigger() {
            assertThat(ddlCompleto).doesNotContain("trg_fal_acta_ad_observaciones");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("OBS-G6: nombres de triggers siguen convención trg_<tabla>_ad_observaciones")
    class G6_NombresTriggers {

        @Test
        @DisplayName("Los 21 triggers esperados por nombre están presentes")
        void triggers_esperados_por_nombre() {
            List<String> esperados = List.of(
                    "trg_fal_persona_ad_observaciones",                       // código 2
                    "trg_fal_persona_domicilio_ad_observaciones",             // código 3
                    "trg_fal_documento_ad_observaciones",                     // código 4
                    "trg_fal_acta_evidencia_ad_observaciones",                // código 5
                    "trg_fal_notificacion_ad_observaciones",                  // código 6
                    "trg_fal_notificacion_intento_ad_observaciones",          // código 7
                    "trg_fal_acta_fallo_ad_observaciones",                    // código 8
                    "trg_fal_acta_apelacion_ad_observaciones",                // código 9
                    "trg_fal_acta_gestion_externa_ad_observaciones",          // código 10
                    "trg_fal_acta_paralizacion_ad_observaciones",             // código 11
                    "trg_fal_acta_archivo_ad_observaciones",                  // código 12
                    "trg_fal_acta_medida_preventiva_ad_observaciones",        // código 13
                    "trg_fal_acta_bloqueante_cierre_material_ad_observaciones", // código 14
                    "trg_fal_acta_articulo_infringido_ad_observaciones",      // código 15
                    "trg_fal_acta_valorizacion_ad_observaciones",             // código 16
                    "trg_fal_acta_obligacion_pago_ad_observaciones",          // código 17
                    "trg_fal_acta_forma_pago_ad_observaciones",               // código 18
                    "trg_fal_acta_plan_pago_ref_ad_observaciones",            // código 19
                    "trg_fal_acta_pago_movimiento_ad_observaciones",          // código 20
                    "trg_num_talonario_ad_observaciones",                     // código 21
                    "trg_num_talonario_movimiento_ad_observaciones"           // código 22
            );
            List<String> faltantes = esperados.stream()
                    .filter(nombre -> !ddlCompleto.contains("TRIGGER " + nombre))
                    .collect(Collectors.toList());
            assertThat(faltantes)
                    .as("Triggers de observación faltantes en el DDL")
                    .isEmpty();
        }

        @Test
        @DisplayName("Todos los triggers _ad_observaciones son AFTER DELETE FOR EACH ROW")
        void triggers_afterDelete() {
            Pattern p = Pattern.compile(
                    "TRIGGER\\s+(trg_\\w+_ad_observaciones)[\\s\\S]*?(?=TRIGGER|$)",
                    Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(ddlCompleto);
            List<String> malFormados = new java.util.ArrayList<>();
            while (m.find()) {
                String fragmento = m.group(0);
                if (!fragmento.toUpperCase().contains("AFTER DELETE")) {
                    malFormados.add(m.group(1));
                }
            }
            assertThat(malFormados)
                    .as("Triggers que no son AFTER DELETE")
                    .isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("OBS-G7: campos libres prohibidos eliminados de sus tablas")
    class G7_CamposProhibidos {

        private void assertCampoEliminado(String tabla, String columna) {
            String cuerpo = cuerpo(tabla);
            boolean presente = cuerpo.lines()
                    .filter(l -> !l.strip().startsWith("--"))
                    .filter(l -> {
                        String s = l.strip();
                        return s.startsWith(columna + " ") || s.startsWith(columna + "\t");
                    })
                    .findAny().isPresent();
            assertThat(presente)
                    .as("Tabla '%s': columna '%s' debe estar eliminada (centralizada en fal_observacion)", tabla, columna)
                    .isFalse();
        }

        @Test @DisplayName("num_talonario: sin obs_talonario")
        void numTalonario_sinObs() { assertCampoEliminado("num_talonario", "obs_talonario"); }

        @Test @DisplayName("num_talonario_movimiento: sin observacion embebida")
        void numTalonarioMovimiento_sinObs() { assertCampoEliminado("num_talonario_movimiento", "observacion"); }

        @Test @DisplayName("fal_acta_articulo_infringido: sin observaciones embebidas")
        void falActaArticuloInfringido_sinObs() {
            assertCampoEliminado("fal_acta_articulo_infringido", "observaciones");
        }

        @Test @DisplayName("fal_persona: sin campo observacion embebido")
        void falPersona_sinObs() { assertCampoEliminado("fal_persona", "observacion"); }

        @Test @DisplayName("fal_acta_apelacion: sin observaciones ni observaciones_resolucion embebidos")
        void falActaApelacion_sinObs() {
            assertCampoEliminado("fal_acta_apelacion", "observaciones");
            assertCampoEliminado("fal_acta_apelacion", "observaciones_resolucion");
        }
    }
}
