package ar.gob.malvinas.faltas.core.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;

import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de GET /demo/health - Slice 8F-8 (cierre GAP-8).
 *
 * 16 tests que validan shape, valores clave, checks de readiness y guardrails.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "faltas.demo.enabled=true")
@DisplayName("8F-8: Contrato GET /demo/health")
class DemoHealthContractTest {

    @Autowired
    MockMvc mvc;

    // =========================================================================
    // 1. HTTP status
    // =========================================================================

    @Test
    @DisplayName("1. health_devuelve_200")
    void health_devuelve_200() throws Exception {
        mvc.perform(get("/demo/health"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 2-3. Campos raiz y valores de status
    // =========================================================================

    @Test
    @DisplayName("2. health_campos_raiz_clave")
    void health_campos_raiz_clave() throws Exception {
        mvc.perform(get("/demo/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.demoReady").isBoolean())
                .andExpect(jsonPath("$.fhEjecucion").isNotEmpty())
                .andExpect(jsonPath("$.versionDemo").isNotEmpty())
                .andExpect(jsonPath("$.dataset").exists())
                .andExpect(jsonPath("$.documentos").exists())
                .andExpect(jsonPath("$.reset").exists())
                .andExpect(jsonPath("$.endpoints").isArray())
                .andExpect(jsonPath("$.warnings").isArray());
    }

    @Test
    @DisplayName("3. health_status_up_y_demo_ready_true")
    void health_status_up_y_demo_ready_true() throws Exception {
        mvc.perform(get("/demo/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.demoReady").value(true))
                .andExpect(jsonPath("$.versionDemo").value("8F-8"));
    }

    // =========================================================================
    // 4-5. Dataset
    // =========================================================================

    @Test
    @DisplayName("4. health_dataset_reporta_37_actas")
    void health_dataset_reporta_37_actas() throws Exception {
        mvc.perform(get("/demo/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataset.ready").value(true))
                .andExpect(jsonPath("$.dataset.totalActasMock").value(37))
                .andExpect(jsonPath("$.dataset.coberturaCompleta").isBoolean());
    }

    @Test
    @DisplayName("5. health_dataset_reporta_detalle_disponible")
    void health_dataset_reporta_detalle_disponible() throws Exception {
        mvc.perform(get("/demo/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataset.detalleDisponible").value(true));
    }

    // =========================================================================
    // 6-7. Documentos / plantillas
    // =========================================================================

    @Test
    @DisplayName("6. health_documentos_reporta_8_plantillas_y_graph_disponible")
    void health_documentos_reporta_8_plantillas_y_graph_disponible() throws Exception {
        mvc.perform(get("/demo/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentos.ready").value(true))
                .andExpect(jsonPath("$.documentos.totalPlantillasMock").value(8))
                .andExpect(jsonPath("$.documentos.graphDisponible").value(true));
    }

    @Test
    @DisplayName("7. health_documentos_no_reporta_storage_real")
    void health_documentos_no_reporta_storage_real() throws Exception {
        MvcResult result = mvc.perform(get("/demo/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentos.storageReal").value(false))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("s3://").doesNotContain("file://");
    }

    // =========================================================================
    // 8-9. Reset
    // =========================================================================

    @Test
    @DisplayName("8. health_reset_default_disabled")
    void health_reset_default_disabled() throws Exception {
        mvc.perform(get("/demo/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reset.enabled").value(false))
                .andExpect(jsonPath("$.reset.defaultSeguro").value(true));
    }

    @Test
    @DisplayName("9. health_reset_endpoint_protegido")
    void health_reset_endpoint_protegido() throws Exception {
        mvc.perform(get("/demo/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reset.endpoint").value("/demo/dev/reset"));
    }

    // =========================================================================
    // 10-14. Endpoints demo listados
    // =========================================================================

    @Test
    @DisplayName("10. health_lista_endpoint_graph")
    void health_lista_endpoint_graph() throws Exception {
        MvcResult result = mvc.perform(get("/demo/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpoints").isArray())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("/demo/documentos/graph");
    }

    @Test
    @DisplayName("11. health_lista_endpoint_dataset")
    void health_lista_endpoint_dataset() throws Exception {
        MvcResult result = mvc.perform(get("/demo/health"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("/demo/actas/dataset-funcional");
    }

    @Test
    @DisplayName("12. health_lista_endpoint_detalle_acta")
    void health_lista_endpoint_detalle_acta() throws Exception {
        MvcResult result = mvc.perform(get("/demo/health"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("/demo/actas/{codigo}");
    }

    @Test
    @DisplayName("13. health_lista_endpoint_reset")
    void health_lista_endpoint_reset() throws Exception {
        MvcResult result = mvc.perform(get("/demo/health"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("/demo/dev/reset");
    }

    @Test
    @DisplayName("14. health_lista_endpoint_health")
    void health_lista_endpoint_health() throws Exception {
        MvcResult result = mvc.perform(get("/demo/health"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("/demo/health");
    }

    // =========================================================================
    // 15. Warnings
    // =========================================================================

    @Test
    @DisplayName("15. health_warnings_array_estable")
    void health_warnings_array_estable() throws Exception {
        mvc.perform(get("/demo/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings").isArray());
    }

    // =========================================================================
    // 16. Guardrail: docs/spec-as-source no existe en raiz del repo
    // =========================================================================

    @Test
    @DisplayName("16. guardrail_docs_spec_as_source_no_en_raiz")
    void guardrail_docs_spec_as_source_no_en_raiz() {
        File raiz = new File("../../docs/spec-as-source");
        assertThat(raiz).doesNotExist();
    }
}