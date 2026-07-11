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
 * IT de seguridad para POST /api/faltas/documentos/{id}/firmar-real.
 *
 * Verifica que:
 * - sin Bearer -> 401 (bloqueado por JwtActorFilter/SecurityConfig)
 * - con Bearer valido (JWT firmado HS256) -> no es rechazado por autenticacion
 *
 * Spring Security activo (no desactivar en este test).
 * FIX-FALLO-NOTI-01-R2: usa JWT firmado real en lugar de alg=none.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("IT seguridad: POST firmar-real requiere Bearer JWT firmado")
class DocumentoFirmaCallbackSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    private static final String BODY = """
            {
              "seqFirmaReq": 1,
              "idFirmante": 1,
              "tipoFirma": "DIGITAL",
              "referenciaFirmaExt": "security-it-ref",
              "hashDocumento": "security-it-hash"
            }
            """;

    @Test
    @DisplayName("POST sin Bearer -> 401")
    void sinBearer_responde_401() throws Exception {
        mockMvc.perform(post("/api/faltas/documentos/1/firmar-real")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST con Bearer JWT firmado valido -> no es rechazado por autenticacion")
    void conBearerFirmado_no_es_401() throws Exception {
        String token = JwtTestSupport.token("firmas-service-it");
        MvcResult result = mockMvc.perform(post("/api/faltas/documentos/1/firmar-real")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isNotEqualTo(401);
    }

    @Test
    @DisplayName("POST con JWT firma incorrecta -> 401")
    void conBearerFirmaIncorrecta_es_401() throws Exception {
        String token = JwtTestSupport.tokenFirmaIncorrecta("firmas-service-it");
        mockMvc.perform(post("/api/faltas/documentos/1/firmar-real")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST con JWT alg=none -> 401")
    void conBearerAlgNone_es_401() throws Exception {
        String token = JwtTestSupport.tokenAlgNone("firmas-service-it");
        mockMvc.perform(post("/api/faltas/documentos/1/firmar-real")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }
}
