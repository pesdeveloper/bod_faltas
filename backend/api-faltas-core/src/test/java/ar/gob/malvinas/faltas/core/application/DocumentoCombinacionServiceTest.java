package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoCombinacionResultado;
import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoCombinacionService;
import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoVariableRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Micro-slice 8F-1: Motor DocumentoCombinacionService")
class DocumentoCombinacionServiceTest {

    private DocumentoCombinacionService service;

    @BeforeEach
    void setUp() {
        service = new DocumentoCombinacionService(new DocumentoVariableRegistry());
    }

    @Nested
    @DisplayName("Reemplazos exitosos")
    class ReemplazosExitosos {

        @Test
        @DisplayName("1. Reemplaza variable simple")
        void reemplaza_variable_simple() {
            DocumentoCombinacionResultado r = service.combinar(
                    "Acta: {{acta.nroActa}}", Map.of("acta.nroActa", "ACT-001"));
            assertThat(r.contenidoCombinado()).isEqualTo("Acta: ACT-001");
            assertThat(r.completo()).isTrue();
            assertThat(r.variablesUsadas()).contains("acta.nroActa");
        }

        @Test
        @DisplayName("2. Reemplaza multiples variables")
        void reemplaza_multiples_variables() {
            DocumentoCombinacionResultado r = service.combinar(
                    "{{infractor.nombreCompleto}} doc {{infractor.documento}}",
                    Map.of("infractor.nombreCompleto", "Juan", "infractor.documento", "123",
                           "acta.fechaLabrado", FaltasClockTestSupport.FIXED.now()));
            assertThat(r.contenidoCombinado()).isEqualTo("Juan doc 123");
            assertThat(r.completo()).isTrue();
        }

        @Test
        @DisplayName("3. Reemplaza variable repetida en todas sus ocurrencias")
        void reemplaza_variable_repetida() {
            DocumentoCombinacionResultado r = service.combinar(
                    "{{acta.nroActa}} - {{acta.nroActa}}",
                    Map.of("acta.nroActa", "X"));
            assertThat(r.contenidoCombinado()).isEqualTo("X - X");
            assertThat(r.variablesUsadas()).hasSize(1);
        }

        @Test
        @DisplayName("9. Soporta espacios dentro de llaves")
        void soporta_espacios_dentro_llaves() {
            DocumentoCombinacionResultado r = service.combinar(
                    "{{ acta.nroActa }}", Map.of("acta.nroActa", "Z"));
            assertThat(r.contenidoCombinado()).isEqualTo("Z");
        }

        @Test
        @DisplayName("10. Devuelve set de variables usadas")
        void devuelve_variables_usadas() {
            DocumentoCombinacionResultado r = service.combinar(
                    "{{acta.nroActa}} y {{domicilioInfraccion.texto}}",
                    Map.of("acta.nroActa", "A", "domicilioInfraccion.texto", "B"));
            assertThat(r.variablesUsadas())
                    .containsExactlyInAnyOrder("acta.nroActa", "domicilioInfraccion.texto");
        }

        @Test
        @DisplayName("14. No modifica el string del template original")
        void no_modifica_template_original() {
            String t = "Texto: {{acta.nroActa}}";
            service.combinar(t, Map.of("acta.nroActa", "X"));
            assertThat(t).isEqualTo("Texto: {{acta.nroActa}}");
        }
    }

    @Nested
    @DisplayName("Diagnostico de variables")
    class DiagnosticoVariables {

        @Test
        @DisplayName("4. Detecta variable desconocida")
        void detecta_variable_desconocida() {
            DocumentoCombinacionResultado r = service.combinar(
                    "{{dominio.inventado}}", Map.of());
            assertThat(r.variablesDesconocidas()).contains("dominio.inventado");
            assertThat(r.completo()).isFalse();
        }

        @Test
        @DisplayName("5. Detecta variable requerida faltante")
        void detecta_variable_requerida_faltante() {
            DocumentoCombinacionResultado r = service.combinar(
                    "{{acta.fechaLabrado}}", Map.of());
            assertThat(r.variablesFaltantes()).contains("acta.fechaLabrado");
            assertThat(r.completo()).isFalse();
        }

        @Test
        @DisplayName("6. Variable opcional faltante se reemplaza por vacio")
        void opcional_faltante_reemplaza_vacio() {
            DocumentoCombinacionResultado r = service.combinar(
                    "Lat: {{ubicacion.lat}}", Map.of());
            assertThat(r.contenidoCombinado()).isEqualTo("Lat: ");
            assertThat(r.variablesFaltantes()).isEmpty();
            assertThat(r.completo()).isTrue();
        }

        @Test
        @DisplayName("11. Devuelve diagnostico completo con usadas/faltantes/desconocidas")
        void devuelve_diagnostico_completo() {
            DocumentoCombinacionResultado r = service.combinar(
                    "{{infractor.nombreCompleto}} {{dominio.x}} {{acta.fechaLabrado}}",
                    Map.of());
            assertThat(r.variablesUsadas())
                    .containsExactlyInAnyOrder("infractor.nombreCompleto", "dominio.x", "acta.fechaLabrado");
            assertThat(r.variablesFaltantes())
                    .containsExactlyInAnyOrder("infractor.nombreCompleto", "acta.fechaLabrado");
            assertThat(r.variablesDesconocidas()).containsExactly("dominio.x");
            assertThat(r.completo()).isFalse();
        }
    }

    @Nested
    @DisplayName("Formato de valores")
    class FormatoValores {

        @Test
        @DisplayName("12. Formatea LocalDate como dd/MM/yyyy")
        void formatea_fechas() {
            DocumentoCombinacionResultado r = service.combinar(
                    "Fecha: {{acta.nroActa}}", Map.of("acta.nroActa", LocalDate.of(2024, 3, 15)));
            assertThat(r.contenidoCombinado()).isEqualTo("Fecha: 15/03/2024");
        }

        @Test
        @DisplayName("13. Formatea LocalDateTime como dd/MM/yyyy HH:mm")
        void formatea_fecha_hora() {
            DocumentoCombinacionResultado r = service.combinar(
                    "Labrado: {{acta.fechaLabrado}}",
                    Map.of("acta.fechaLabrado", LocalDateTime.of(2024, 3, 15, 10, 30)));
            assertThat(r.contenidoCombinado()).isEqualTo("Labrado: 15/03/2024 10:30");
        }
    }

    @Nested
    @DisplayName("Guardrails de seguridad")
    class Guardrails {

        @Test
        @DisplayName("7. No ejecuta metodos - sintaxis con parentesis no es reemplazada")
        void no_ejecuta_metodos() {
            DocumentoCombinacionResultado r = service.combinar(
                    "{{acta.nroActa.toUpperCase()}}", Map.of("acta.nroActa", "x"));
            assertThat(r.contenidoCombinado()).isEqualTo("{{acta.nroActa.toUpperCase()}}");
            assertThat(r.variablesUsadas()).isEmpty();
        }

        @Test
        @DisplayName("8. Variable sin punto no es reconocida")
        void variable_sin_punto_no_reconocida() {
            DocumentoCombinacionResultado r = service.combinar(
                    "{{sinpunto}}", Map.of("sinpunto", "valor"));
            assertThat(r.contenidoCombinado()).isEqualTo("{{sinpunto}}");
            assertThat(r.variablesUsadas()).isEmpty();
        }

        @Test
        @DisplayName("15. No usa SpEL ni ScriptEngine")
        void no_usa_spel_ni_script_engine() {
            String cn = DocumentoCombinacionService.class.getName();
            assertThat(cn).doesNotContain("SpEL");
            assertThat(cn).doesNotContain("Script");
        }

        @Test
        @DisplayName("Template vacio devuelve resultado completo vacio")
        void template_vacio() {
            DocumentoCombinacionResultado r = service.combinar("", Map.of());
            assertThat(r.contenidoCombinado()).isEmpty();
            assertThat(r.completo()).isTrue();
        }

        @Test
        @DisplayName("Template sin variables devuelve el mismo contenido")
        void template_sin_variables() {
            DocumentoCombinacionResultado r = service.combinar("Sin variables.", Map.of());
            assertThat(r.contenidoCombinado()).isEqualTo("Sin variables.");
        }

        @Test
        @DisplayName("Falla si template es null")
        void falla_template_null() {
            assertThatThrownBy(() -> service.combinar(null, Map.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Falla si contexto es null")
        void falla_contexto_null() {
            assertThatThrownBy(() -> service.combinar("t", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
