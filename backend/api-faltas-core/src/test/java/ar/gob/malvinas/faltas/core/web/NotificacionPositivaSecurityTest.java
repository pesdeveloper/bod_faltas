package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.support.JwtTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de seguridad para POST /api/faltas/notificaciones/{id}/positiva.
 *
 * Verifica que:
 * - sin Bearer -> 401
 * - con firma incorrecta -> 401
 * - alg=none -> 401
 * - JWT valido -> no rechazado por autenticacion (status distinto de 401)
 *
 * Spring Security activo. No desactivar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("IT seguridad: POST notificaciones/{id}/positiva requiere Bearer JWT firmado")
class NotificacionPositivaSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String BODY = """
            {
              "intentoId": 1
            }
            """;

    @Test
    @DisplayName("POST sin Bearer -> 401")
    void sinBearer_responde_401() throws Exception {
        mockMvc.perform(post("/api/faltas/notificaciones/1/positiva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST con JWT firma incorrecta -> 401")
    void conFirmaIncorrecta_responde_401() throws Exception {
        String token = JwtTestSupport.tokenFirmaIncorrecta("test-actor");
        mockMvc.perform(post("/api/faltas/notificaciones/1/positiva")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST con JWT alg=none -> 401")
    void conAlgNone_responde_401() throws Exception {
        String token = JwtTestSupport.tokenAlgNone("test-actor");
        mockMvc.perform(post("/api/faltas/notificaciones/1/positiva")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST con JWT valido -> no es 401 (autenticacion superada)")
    void conJwtValido_no_es_401() throws Exception {
        String token = JwtTestSupport.token("test-actor");
        MvcResult result = mockMvc.perform(post("/api/faltas/notificaciones/1/positiva")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isNotEqualTo(401);
    }
}
