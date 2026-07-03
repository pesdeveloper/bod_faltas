package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integraciÃ³n con contexto Spring real.
 * Cubre el flujo HTTP completo: labrar â†’ firma â†’ notificaciÃ³n positiva.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("IT: endpoints REST del flujo de faltas")
class FlujoCoreIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Test
    @DisplayName("Flujo HTTP completo: labrar â†’ completar captura â†’ generar doc â†’ firmar â†’ enviar â†’ positiva")
    void flujo_http_completo() throws Exception {
        // 1. Labrar acta
        String labrarBody = mapper.writeValueAsString(Map.of(
                "tipoActa", "TRANSITO",
                "idDependencia", "DEP-001",
                "idInspector", "INS-001",
                "fechaActa", LocalDate.now().toString(),
                "domicilioHecho", "Belgrano 200",
                "infractorDocumento", "87654321"
        ));

        MvcResult labrarResult = mvc.perform(post("/api/faltas/actas/labrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(labrarBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoEvento").value("ACTLAB"))
                .andReturn();

        String idActa = extractField(labrarResult, "idActa");
        assertThat(idActa).isNotBlank();

        // 2. Verificar snapshot inicial
        mvc.perform(get("/api/faltas/actas/{id}/snapshot", idActa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codBandeja").value("ACTAS_EN_ENRIQUECIMIENTO"))
                .andExpect(jsonPath("$.bloqueActual").value("CAPT"));

        // 3. Completar captura
        mvc.perform(post("/api/faltas/actas/{id}/completar-captura", idActa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoEvento").value("ACTCAP"));

        // 4. Generar documento
        String docBody = mapper.writeValueAsString(Map.of(
                "tipoDocumento", TipoDocu.ACTA_INFRACCION.name(),
                "descripcion", "Acta de infraccion"
        ));

        MvcResult docResult = mvc.perform(post("/api/faltas/actas/{id}/documentos", idActa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(docBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoEvento").value("DOCGEN"))
                .andReturn();

        String idDoc = extractField(docResult, "idEntidadAfectada");

        // 5. Verificar bandeja PENDIENTE_FIRMA
        mvc.perform(get("/api/faltas/actas/{id}/snapshot", idActa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codBandeja").value("PENDIENTE_FIRMA"));

        // 6. Firmar documento
        String firmarBody = mapper.writeValueAsString(Map.of(
                "firmante", "Inspector Rodriguez",
                "tipoFirma", "DIGITAL"
        ));

        mvc.perform(post("/api/faltas/documentos/{id}/firmar", idDoc)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firmarBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoEvento").value("DOCFIR"));

        // 7. Verificar bandeja PENDIENTE_NOTIFICACION
        mvc.perform(get("/api/faltas/actas/{id}/snapshot", idActa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codBandeja").value("PENDIENTE_NOTIFICACION"));

        // 8. Enviar notificaciÃ³n
        String notifBody = mapper.writeValueAsString(Map.of(
                "idDocumento", idDoc,
                "canal", "EMAIL"
        ));

        MvcResult notifResult = mvc.perform(post("/api/faltas/actas/{id}/notificaciones/enviar", idActa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notifBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoEvento").value("NOTENV"))
                .andReturn();

        String idNotif = extractField(notifResult, "idEntidadAfectada");

        // 9. Registrar notificaciÃ³n positiva
        mvc.perform(post("/api/faltas/notificaciones/{id}/positiva", idNotif)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoEvento").value("NOTPOS"));

        // 10. Verificar estado final: ANAL
        mvc.perform(get("/api/faltas/actas/{id}/snapshot", idActa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codBandeja").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.bloqueActual").value("ANAL"));

        // 11. Verificar timeline
        mvc.perform(get("/api/faltas/actas/{id}/timeline", idActa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    @DisplayName("Error 422 al intentar notificar documento sin firma")
    void error_al_notificar_sin_firma() throws Exception {
        String labrarBody = mapper.writeValueAsString(Map.of(
                "tipoActa", "TRANSITO", "idDependencia", "DEP-001",
                "idInspector", "INS-001", "infractorDocumento", "99999999"
        ));
        MvcResult labrarResult = mvc.perform(post("/api/faltas/actas/labrar")
                        .contentType(MediaType.APPLICATION_JSON).content(labrarBody))
                .andReturn();
        String idActa = extractField(labrarResult, "idActa");

        mvc.perform(post("/api/faltas/actas/{id}/completar-captura", idActa)
                .contentType(MediaType.APPLICATION_JSON).content("{}"));

        String docBody = mapper.writeValueAsString(Map.of(
                "tipoDocumento", "ACTA_INFRACCION", "descripcion", "test"));
        MvcResult docResult = mvc.perform(post("/api/faltas/actas/{id}/documentos", idActa)
                .contentType(MediaType.APPLICATION_JSON).content(docBody)).andReturn();
        String idDoc = extractField(docResult, "idEntidadAfectada");

        String notifBody = mapper.writeValueAsString(Map.of("idDocumento", idDoc, "canal", "EMAIL"));
        mvc.perform(post("/api/faltas/actas/{id}/notificaciones/enviar", idActa)
                        .contentType(MediaType.APPLICATION_JSON).content(notifBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Error 404 para acta inexistente")
    void error_404_acta_inexistente() throws Exception {
        mvc.perform(get("/api/faltas/actas/{id}", "no-existe"))
                .andExpect(status().isNotFound());
    }

    private String extractField(MvcResult result, String field) throws Exception {
        String json = result.getResponse().getContentAsString();
        return mapper.readTree(json).get(field).asText();
    }
}

