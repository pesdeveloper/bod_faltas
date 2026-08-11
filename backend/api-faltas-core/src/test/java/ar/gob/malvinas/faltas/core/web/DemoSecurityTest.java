package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.support.JwtTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guardrails de seguridad para el modulo demo.
 *
 * DEC-010: seguridad cerrada por defecto, excepciones publicas explicitas por endpoint exacto.
 *
 * Reglas validadas:
 * - GET /demo/health es el unico endpoint demo publico (permitAll).
 * - GET /demo/actas/dataset-funcional, GET /demo/actas/{codigo},
 *   GET /demo/documentos/graph, POST /demo/dev/reset requieren JWT.
 * - El 401 del filtro y del entrypoint produce el mismo ErrorResponse canonico.
 * - No existe el matcher wildcard /demo/** en SecurityConfig.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "faltas.demo.enabled=true",
        "faltas.demo.reset.enabled=true"
})
@DisplayName("Demo: politica de seguridad por endpoint exacto (DEC-010)")
class DemoSecurityTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    // =========================================================================
    // 1. GET /demo/health sin JWT: 200 (permitAll, solo status UP/DOWN)
    // =========================================================================

    @Test
    @DisplayName("GET /demo/health sin JWT devuelve 200 (publico, sin datos operativos)")
    void health_sin_jwt_es_200() throws Exception {
        mvc.perform(get("/demo/health"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 2. GET /demo/health con JWT: 200 (publico + autenticado: ambos pasan)
    // =========================================================================

    @Test
    @DisplayName("GET /demo/health con JWT devuelve 200")
    void health_con_jwt_es_200() throws Exception {
        String token = JwtTestSupport.token("demo-test");
        mvc.perform(get("/demo/health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 3. GET /demo/actas/dataset-funcional sin JWT: 401 + ErrorResponse canonico
    // =========================================================================

    @Test
    @DisplayName("GET /demo/actas/dataset-funcional sin JWT devuelve 401 con ErrorResponse canonico")
    void dataset_sin_jwt_es_401_con_error_response() throws Exception {
        MvcResult result = mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("codigoError").asText())
                .as("codigoError debe ser ACTOR_NO_AUTENTICADO")
                .isEqualTo("ACTOR_NO_AUTENTICADO");
        assertThat(body.get("mensaje").asText())
                .as("mensaje no debe contener datos sensibles")
                .isNotBlank()
                .doesNotContain("password")
                .doesNotContain("secret")
                .doesNotContain("token");
    }

    // =========================================================================
    // 4. GET /demo/actas/{codigo} sin JWT: 401
    // =========================================================================

    @Test
    @DisplayName("GET /demo/actas/{codigo} sin JWT devuelve 401")
    void detalle_acta_sin_jwt_es_401() throws Exception {
        mvc.perform(get("/demo/actas/ACT-001-LABRADA"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 5. GET /demo/documentos/graph sin JWT: 401
    // =========================================================================

    @Test
    @DisplayName("GET /demo/documentos/graph sin JWT devuelve 401")
    void graph_sin_jwt_es_401() throws Exception {
        mvc.perform(get("/demo/documentos/graph"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 6. POST /demo/dev/reset sin JWT: 401 + ErrorResponse canonico
    //    (endpoint de mutacion de estado: el mas critico)
    // =========================================================================

    @Test
    @DisplayName("POST /demo/dev/reset sin JWT devuelve 401 con ErrorResponse canonico")
    void reset_sin_jwt_es_401_con_error_response() throws Exception {
        MvcResult result = mvc.perform(post("/demo/dev/reset"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("codigoError").asText())
                .as("codigoError debe ser ACTOR_NO_AUTENTICADO")
                .isEqualTo("ACTOR_NO_AUTENTICADO");
        assertThat(body.get("mensaje").asText())
                .as("mensaje no debe contener datos sensibles")
                .isNotBlank()
                .doesNotContain("password")
                .doesNotContain("secret")
                .doesNotContain("token");
    }

    // =========================================================================
    // 7. Guardrail estructural: no existe /demo/** como permitAll en SecurityConfig
    // =========================================================================

    @Test
    @DisplayName("Guardrail: no existe /demo/** con permitAll en SecurityConfig")
    void no_existe_demo_wildcard_publico_en_security_config() throws IOException {
        Path securityConfig = Path.of("src", "main", "java",
                "ar", "gob", "malvinas", "faltas", "core",
                "infrastructure", "security", "SecurityConfig.java");
        assertThat(Files.exists(securityConfig))
                .as("SecurityConfig.java debe existir en la ruta canonica")
                .isTrue();
        String contenido = Files.readString(securityConfig);
        assertThat(contenido)
                .as("No debe existir .requestMatchers(\"/demo/**\").permitAll() — wildcard publico prohibido")
                .doesNotContain("\"/demo/**\").permitAll()");
    }
}
