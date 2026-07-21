package ar.gob.malvinas.faltas.core.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integracion con contexto Spring real.
 * Verifica el endpoint GET /demo/documentos/graph del graph demo documental 8F-4.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "faltas.demo.enabled=true")
@DisplayName("IT 8F-4: GET /demo/documentos/graph - graph demo documental completo")
class DocumentoGraphDemoIT {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("1. GET /demo/documentos/graph devuelve 200 con 8 casos exitosos")
    void graph_demo_devuelve_8_casos_exitosos() throws Exception {
        mvc.perform(get("/demo/documentos/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCasos").value(8))
                .andExpect(jsonPath("$.casosExitosos").value(8))
                .andExpect(jsonPath("$.casosFallidos").value(0))
                .andExpect(jsonPath("$.completo").value(true))
                .andExpect(jsonPath("$.casos").isArray())
                .andExpect(jsonPath("$.casos.length()").value(8));
    }

    @Test
    @DisplayName("2. El primer caso es CASO-01 EMITIR_FALLO con estadoRedaccion CONFIRMADA")
    void primer_caso_correcto() throws Exception {
        mvc.perform(get("/demo/documentos/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casos[0].codigoCaso").value("CASO-01"))
                .andExpect(jsonPath("$.casos[0].accionDocumental").value("EMITIR_FALLO"))
                .andExpect(jsonPath("$.casos[0].tipoDocu").value("ACTO_ADMINISTRATIVO"))
                .andExpect(jsonPath("$.casos[0].estadoRedaccion").value("CONFIRMADA"))
                .andExpect(jsonPath("$.casos[0].mock").value(true))
                .andExpect(jsonPath("$.casos[0].exitoso").value(true))
                .andExpect(jsonPath("$.casos[0].redaccionCompleta").value(true));
    }

    @Test
    @DisplayName("3. El ultimo caso es CASO-08 EMITIR_RESOLUTORIO_BLOQUEANTE")
    void ultimo_caso_correcto() throws Exception {
        mvc.perform(get("/demo/documentos/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casos[7].codigoCaso").value("CASO-08"))
                .andExpect(jsonPath("$.casos[7].accionDocumental").value("EMITIR_RESOLUTORIO_BLOQUEANTE"))
                .andExpect(jsonPath("$.casos[7].tipoDocu").value("RESOLUTORIO_BLOQUEANTE"))
                .andExpect(jsonPath("$.casos[7].estadoRedaccion").value("CONFIRMADA"))
                .andExpect(jsonPath("$.casos[7].exitoso").value(true));
    }

    @Test
    @DisplayName("4. Todos los casos tienen storageKey mock:// y hashDocu sha256-mock- en respuesta JSON")
    void todos_tienen_metadatos_mock_en_json() throws Exception {
        MvcResult result = mvc.perform(get("/demo/documentos/graph"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("mock://");
        assertThat(body).contains("sha256-mock-");
        assertThat(body).doesNotContain("s3://");
        assertThat(body).doesNotContain("file://");
    }

    @Test
    @DisplayName("5. fhEjecucion esta presente en la respuesta")
    void fh_ejecucion_presente() throws Exception {
        mvc.perform(get("/demo/documentos/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fhEjecucion").isNotEmpty());
    }

    @Test
    @DisplayName("6. Todos los casos tienen exitoso=true en la respuesta")
    void todos_los_casos_exitosos_en_json() throws Exception {
        MvcResult result = mvc.perform(get("/demo/documentos/graph"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"exitoso\":true");
        assertThat(body).doesNotContain("\"exitoso\":false");
    }
}
