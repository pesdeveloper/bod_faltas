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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobertura de seguridad HTTP para el módulo Geo.
 *
 * DEC-010: anyRequest().authenticated() — excepciones públicas acotadas por endpoint exacto.
 *
 * Clasificación final de endpoints Geo:
 *
 * públicos (permitAll) por endpoint exacto:
 *   GET /api/geo/provincias
 *   GET /api/geo/unidades-territoriales
 *   GET /api/geo/localidades
 *   GET /api/geo/calles
 *   GET /api/geo/local/malvinas/localidades
 *   GET /api/geo/local/malvinas/calles
 *   GET /api/geo/local/malvinas/parcela/geom — geometría pura (GeoJSON), sin idSuj/idBie/titularidad/datos fiscales
 *
 * authenticated() explícito:
 *   GET /api/geo/local/malvinas/parcela/detalle — contiene IdSuj, IdBie, comercios, inmuebles
 *   POST /api/geo/geocodificar — requiere actor para auditoría
 *   POST /api/geo/reverse — requiere actor para auditoría
 *   POST /api/geo/geocodificar-esquina — requiere actor para auditoría
 *   POST /api/geo/geocodificar-entre-calles — requiere actor para auditoría
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("Geo: política de seguridad por endpoint exacto (DEC-010)")
class GeoSecurityTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    // =========================================================================
    // Catálogos públicos (permitAll) — 6 GET sin JWT: no deben retornar 401
    // El servicio retorna 200 con [] cuando PostGIS no está disponible en test
    // =========================================================================

    @Test
    @DisplayName("GET /api/geo/provincias sin JWT: no es 401 (permitAll)")
    void provincias_sin_jwt_no_es_401() throws Exception {
        mvc.perform(get("/api/geo/provincias"))
                .andExpect(status().is(org.hamcrest.Matchers.not(401)));
    }

    @Test
    @DisplayName("GET /api/geo/unidades-territoriales sin JWT: no es 401 (permitAll)")
    void unidades_territoriales_sin_jwt_no_es_401() throws Exception {
        mvc.perform(get("/api/geo/unidades-territoriales"))
                .andExpect(status().is(org.hamcrest.Matchers.not(401)));
    }

    @Test
    @DisplayName("GET /api/geo/calles sin JWT: no es 401 (permitAll)")
    void calles_sin_jwt_no_es_401() throws Exception {
        mvc.perform(get("/api/geo/calles")
                        .param("q", "SAN")
                        .param("idLoc", "LP"))
                .andExpect(status().is(org.hamcrest.Matchers.not(401)));
    }

    @Test
    @DisplayName("GET /api/geo/local/malvinas/localidades sin JWT: no es 401 (permitAll)")
    void localidades_malvinas_sin_jwt_no_es_401() throws Exception {
        mvc.perform(get("/api/geo/local/malvinas/localidades"))
                .andExpect(status().is(org.hamcrest.Matchers.not(401)));
    }

    @Test
    @DisplayName("GET /api/geo/local/malvinas/calles sin JWT: no es 401 (permitAll)")
    void calles_malvinas_sin_jwt_no_es_401() throws Exception {
        mvc.perform(get("/api/geo/local/malvinas/calles")
                        .param("q", "SAN"))
                .andExpect(status().is(org.hamcrest.Matchers.not(401)));
    }

    @Test
    @DisplayName("GET /api/geo/local/malvinas/parcela/geom sin JWT: no es 401 (permitAll — geometria pura, sin datos personales/fiscales)")
    void parcela_geom_sin_jwt_no_es_401() throws Exception {
        mvc.perform(get("/api/geo/local/malvinas/parcela/geom")
                        .param("circ", "4").param("secc", "H")
                        .param("quint", "0").param("fracc", "0")
                        .param("manz", "105B").param("parc", "13"))
                .andExpect(status().is(org.hamcrest.Matchers.not(401)));
    }

    // =========================================================================
    // parcela/detalle — authenticated(): sin JWT debe retornar 401 con ErrorResponse
    // =========================================================================

    @Test
    @DisplayName("GET /api/geo/local/malvinas/parcela/detalle sin JWT: 401 con ErrorResponse canonico")
    void parcela_detalle_sin_jwt_es_401_con_error_response() throws Exception {
        MvcResult result = mvc.perform(get("/api/geo/local/malvinas/parcela/detalle")
                        .param("circ", "4").param("secc", "H")
                        .param("quint", "0").param("fracc", "0")
                        .param("manz", "105B").param("parc", "13"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("codigoError").asText())
                .as("codigoError debe ser ACTOR_NO_AUTENTICADO")
                .isEqualTo("ACTOR_NO_AUTENTICADO");
    }

    // =========================================================================
    // 4 POST Geo — authenticated(): sin JWT deben retornar 401 con ErrorResponse
    // =========================================================================

    @Test
    @DisplayName("POST /api/geo/geocodificar sin JWT: 401 con ErrorResponse canonico")
    void geocodificar_sin_jwt_es_401() throws Exception {
        MvcResult result = mvc.perform(post("/api/geo/geocodificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calle\":\"SAN MARTIN\",\"altura\":100,\"idLoc\":\"LP\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("codigoError").asText()).isEqualTo("ACTOR_NO_AUTENTICADO");
    }

    @Test
    @DisplayName("POST /api/geo/reverse sin JWT: 401")
    void reverse_sin_jwt_es_401() throws Exception {
        mvc.perform(post("/api/geo/reverse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lat\":-51.7,\"lon\":-69.2}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/geo/geocodificar-esquina sin JWT: 401")
    void geocodificar_esquina_sin_jwt_es_401() throws Exception {
        mvc.perform(post("/api/geo/geocodificar-esquina")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTcaCalle1\":\"1\",\"idLocCalle1\":\"LP\",\"idTcaCalle2\":\"2\",\"idLocCalle2\":\"LP\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/geo/geocodificar-entre-calles sin JWT: 401")
    void geocodificar_entre_calles_sin_jwt_es_401() throws Exception {
        mvc.perform(post("/api/geo/geocodificar-entre-calles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTcaObj\":\"1\",\"idLocObj\":\"LP\",\"idTcaL1\":\"2\",\"idLocL1\":\"LP\",\"idTcaL2\":\"3\",\"idLocL2\":\"LP\"}"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Guardrail: no existe /api/geo/** como permitAll en SecurityConfig
    // =========================================================================

    @Test
    @DisplayName("Guardrail: no existe /api/geo/** con permitAll en SecurityConfig")
    void no_existe_geo_wildcard_publico_en_security_config() throws IOException {
        Path securityConfig = Path.of("src", "main", "java",
                "ar", "gob", "malvinas", "faltas", "core",
                "infrastructure", "security", "SecurityConfig.java");
        assertThat(Files.exists(securityConfig))
                .as("SecurityConfig.java debe existir")
                .isTrue();
        String contenido = Files.readString(securityConfig);
        assertThat(contenido)
                .as("No debe existir .requestMatchers(\"/api/geo/**\").permitAll() — wildcard público prohibido")
                .doesNotContain("\"/api/geo/**\").permitAll()");
    }
}
