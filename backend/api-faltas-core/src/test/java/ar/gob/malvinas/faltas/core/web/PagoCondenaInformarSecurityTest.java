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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de seguridad para POST /api/faltas/actas/{id}/pago-condena/informar.
 *
 * Verifica que:
 * - sin Bearer -> 401
 * - con firma incorrecta -> 401
 * - alg=none -> 401
 * - JWT valido -> no rechazado por autenticacion (status distinto de 401)
 * - GET sin Bearer -> no es 401 (filtro no protege GET)
 * - JWT valido + body ausente -> 400 (autenticacion superada, Bean Validation rechaza)
 * - JWT valido + monto ausente -> 400 (autenticacion superada, Bean Validation rechaza)
 * - JWT valido + referenciaPago blank -> 400 (autenticacion superada, Bean Validation rechaza)
 *
 * Spring Security activo. No desactivar.
 *
 * Slice GAP-CONFORMIDAD-PAGO-CONDENA-INFORMAR-001 (CMD-FALLO-007).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("IT seguridad: POST actas/{id}/pago-condena/informar requiere Bearer JWT firmado")
class PagoCondenaInformarSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String URL = "/api/faltas/actas/99/pago-condena/informar";
    private static final String BODY = """
            {"monto": 3000.00, "referenciaPago": "REF-001"}
            """;

    @Test
    @DisplayName("POST sin Bearer -> 401")
    void sinBearer_responde_401() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST con JWT firma incorrecta -> 401")
    void conFirmaIncorrecta_responde_401() throws Exception {
        String token = JwtTestSupport.tokenFirmaIncorrecta("test-actor");
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST con JWT alg=none -> 401")
    void conAlgNone_responde_401() throws Exception {
        String token = JwtTestSupport.tokenAlgNone("test-actor");
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST con JWT valido -> no es 401 (autenticacion superada)")
    void conJwtValido_no_es_401() throws Exception {
        String token = JwtTestSupport.token("test-actor");
        MvcResult result = mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isNotEqualTo(401);
    }

    @Test
    @DisplayName("GET sin Bearer a la misma URI -> NO es 401 (filtro no protege GET)")
    void get_sinBearer_no_es_401() throws Exception {
        MvcResult result = mockMvc.perform(get(URL)).andReturn();
        assertThat(result.getResponse().getStatus()).isNotEqualTo(401);
    }

    @Test
    @DisplayName("JWT valido + body ausente -> 400 (autenticacion superada, Bean Validation rechaza)")
    void conJwtValido_bodyAusente_responde_400() throws Exception {
        String token = JwtTestSupport.token("test-actor");
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("JWT valido + monto ausente/null -> 400 (autenticacion superada, Bean Validation rechaza)")
    void conJwtValido_montoAusente_responde_400() throws Exception {
        String token = JwtTestSupport.token("test-actor");
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"referenciaPago": "REF-001"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("JWT valido + referenciaPago blank -> 400 (autenticacion superada, Bean Validation rechaza)")
    void conJwtValido_referenciaPagoBlank_responde_400() throws Exception {
        String token = JwtTestSupport.token("test-actor");
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 3000.00, "referenciaPago": "   "}
                                """))
                .andExpect(status().isBadRequest());
    }
}
