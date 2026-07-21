package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.MomentoNumeracionDocu;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.gob.malvinas.faltas.core.application.DdlTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guardrails DECISION_DDL-DOCU-02 — MomentoNumeracionDocu.
 *
 * <p>Verifica que:
 * <ul>
 *   <li>El enum Java contiene exactamente los 5 valores aprobados con sus codigos correctos.</li>
 *   <li>El bloque DDL real de {@code fal_documento_plantilla} documenta el catalogo completo.</li>
 *   <li>El comentario DDL no contiene el catalogo desactualizado (1=NO_APLICA).</li>
 * </ul>
 * HUMAN_DECISION_CLOSED — FULL-R1.2-CORRECCION-10
 */
@DisplayName("Guardrails MomentoNumeracionDocu (DECISION_DDL-DOCU-02 / FULL-R1.2-CORRECCION-10)")
class MomentoNumeracionDocuGuardrailTest {

    private static Map<String, String> bloquesPorTabla;
    private static String bloqueDocuPlantilla;

    @BeforeAll
    static void cargarDdl() throws IOException {
        Path dbRoot = resolveDbRoot();
        bloquesPorTabla = extraerBloquesDdl(leer(dbRoot.resolve("ddl/create-bod-faltas-domain.sql")))
                .stream()
                .collect(Collectors.toMap(DdlTestSupport::extraerNombreTabla, b -> b));
        bloqueDocuPlantilla = bloquesPorTabla.get("fal_documento_plantilla");
        assertThat(bloqueDocuPlantilla)
                .as("El bloque DDL de fal_documento_plantilla debe existir")
                .isNotNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Enum Java
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Enum Java MomentoNumeracionDocu")
    class EnumJava {

        @Test
        @DisplayName("1. Exactamente 5 valores en el enum")
        void exactamente_cinco_valores() {
            assertThat(MomentoNumeracionDocu.values()).hasSize(5);
        }

        @Test
        @DisplayName("2. NO_APLICA tiene codigo 0")
        void no_aplica_codigo_0() {
            assertThat(MomentoNumeracionDocu.NO_APLICA.codigo()).isEqualTo((short) 0);
        }

        @Test
        @DisplayName("3. AL_CREAR tiene codigo 1")
        void al_crear_codigo_1() {
            assertThat(MomentoNumeracionDocu.AL_CREAR.codigo()).isEqualTo((short) 1);
        }

        @Test
        @DisplayName("4. AL_EMITIR tiene codigo 2")
        void al_emitir_codigo_2() {
            assertThat(MomentoNumeracionDocu.AL_EMITIR.codigo()).isEqualTo((short) 2);
        }

        @Test
        @DisplayName("5. AL_ENVIAR_A_FIRMA tiene codigo 3")
        void al_enviar_a_firma_codigo_3() {
            assertThat(MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA.codigo()).isEqualTo((short) 3);
        }

        @Test
        @DisplayName("6. AL_FIRMAR tiene codigo 4")
        void al_firmar_codigo_4() {
            assertThat(MomentoNumeracionDocu.AL_FIRMAR.codigo()).isEqualTo((short) 4);
        }

        @Test
        @DisplayName("7. Todos los codigos son unicos")
        void codigos_unicos() {
            long distintos = java.util.Arrays.stream(MomentoNumeracionDocu.values())
                    .mapToInt(m -> m.codigo())
                    .distinct()
                    .count();
            assertThat(distintos).isEqualTo(MomentoNumeracionDocu.values().length);
        }

        @Test
        @DisplayName("8. desdeCodigo resuelve los 5 valores por round-trip")
        void round_trip_cinco_valores() {
            for (MomentoNumeracionDocu m : MomentoNumeracionDocu.values()) {
                assertThat(MomentoNumeracionDocu.desdeCodigo(m.codigo()))
                        .as("Round-trip fallo para %s (codigo %d)", m.name(), m.codigo())
                        .isEqualTo(m);
            }
        }

        @Test
        @DisplayName("9. desdeCodigo con codigo desconocido lanza excepcion")
        void codigo_desconocido_lanza_excepcion() {
            assertThatThrownBy(() -> MomentoNumeracionDocu.desdeCodigo((short) 99))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bloque DDL real de fal_documento_plantilla
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Bloque DDL fal_documento_plantilla — comentario momento_numeracion_docu")
    class BloqueDdl {

        @Test
        @DisplayName("10. El bloque contiene la columna momento_numeracion_docu")
        void bloque_contiene_columna() {
            assertThat(bloqueDocuPlantilla).contains("momento_numeracion_docu");
        }

        @Test
        @DisplayName("11. El comentario documenta 0=NO_APLICA")
        void documenta_no_aplica() {
            assertThat(bloqueDocuPlantilla)
                    .as("El comentario de momento_numeracion_docu debe documentar 0=NO_APLICA")
                    .contains("0=NO_APLICA");
        }

        @Test
        @DisplayName("12. El comentario documenta 1=AL_CREAR")
        void documenta_al_crear() {
            assertThat(bloqueDocuPlantilla)
                    .as("El comentario de momento_numeracion_docu debe documentar 1=AL_CREAR")
                    .contains("1=AL_CREAR");
        }

        @Test
        @DisplayName("13. El comentario documenta 2=AL_EMITIR")
        void documenta_al_emitir() {
            assertThat(bloqueDocuPlantilla)
                    .as("El comentario de momento_numeracion_docu debe documentar 2=AL_EMITIR")
                    .contains("2=AL_EMITIR");
        }

        @Test
        @DisplayName("14. El comentario documenta 3=AL_ENVIAR_A_FIRMA")
        void documenta_al_enviar_a_firma() {
            assertThat(bloqueDocuPlantilla)
                    .as("El comentario de momento_numeracion_docu debe documentar 3=AL_ENVIAR_A_FIRMA")
                    .contains("3=AL_ENVIAR_A_FIRMA");
        }

        @Test
        @DisplayName("15. El comentario documenta 4=AL_FIRMAR")
        void documenta_al_firmar() {
            assertThat(bloqueDocuPlantilla)
                    .as("El comentario de momento_numeracion_docu debe documentar 4=AL_FIRMAR")
                    .contains("4=AL_FIRMAR");
        }

        @Test
        @DisplayName("16. El comentario NO contiene el catalogo anterior (1=NO_APLICA)")
        void sin_catalogo_anterior() {
            assertThat(bloqueDocuPlantilla)
                    .as("El catalogo anterior '1=NO_APLICA' no debe aparecer en el bloque de fal_documento_plantilla")
                    .doesNotContain("1=NO_APLICA");
        }

        @Test
        @DisplayName("17. El bloque menciona MomentoNumeracionDocu")
        void menciona_enum() {
            assertThat(bloqueDocuPlantilla).contains("MomentoNumeracionDocu");
        }
    }
}
