package ar.gob.malvinas.faltas.core.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;

import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de contrato frontend-ready para los endpoints demo.
 *
 * Valida shape de JSON, campos clave, guardrails de reset y CORS.
 *
 * Slice 8F-6.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "faltas.demo.enabled=true")
@DisplayName("8F-6: Contrato frontend-ready de endpoints demo")
class DemoContractTest {

    @Autowired
    MockMvc mvc;

    // =========================================================================
    // GET /demo/documentos/graph - shape contract
    // =========================================================================

    @Test
    @DisplayName("graph: campos raiz clave para frontend presentes")
    void graph_campos_raiz_clave() throws Exception {
        mvc.perform(get("/demo/documentos/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCasos").isNumber())
                .andExpect(jsonPath("$.casosExitosos").isNumber())
                .andExpect(jsonPath("$.casosFallidos").isNumber())
                .andExpect(jsonPath("$.completo").isBoolean())
                .andExpect(jsonPath("$.fhEjecucion").isNotEmpty())
                .andExpect(jsonPath("$.casos").isArray());
    }

    @Test
    @DisplayName("graph: caso tiene IDs de navegacion (actaId, documentoId, redaccionId)")
    void graph_caso_ids_navegacion() throws Exception {
        mvc.perform(get("/demo/documentos/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casos[0].actaId").isNumber())
                .andExpect(jsonPath("$.casos[0].documentoId").isNumber())
                .andExpect(jsonPath("$.casos[0].redaccionId").isNumber());
    }

    @Test
    @DisplayName("graph: caso tiene campos de presentacion (codigoCaso, descripcionCaso, accionDocumental, tipoDocu)")
    void graph_caso_campos_presentacion() throws Exception {
        mvc.perform(get("/demo/documentos/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casos[0].codigoCaso").isString())
                .andExpect(jsonPath("$.casos[0].descripcionCaso").isString())
                .andExpect(jsonPath("$.casos[0].accionDocumental").isString())
                .andExpect(jsonPath("$.casos[0].tipoDocu").isString());
    }

    @Test
    @DisplayName("graph: caso exitoso tiene storageKey mock y hashDocu mock")
    void graph_caso_exitoso_storage_mock() throws Exception {
        mvc.perform(get("/demo/documentos/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casos[0].storageKey").isString())
                .andExpect(jsonPath("$.casos[0].hashDocu").isString())
                .andExpect(jsonPath("$.casos[0].mock").value(true))
                .andExpect(jsonPath("$.casos[0].exitoso").value(true));
    }

    @Test
    @DisplayName("graph: no expone storage real (s3://, file://)")
    void graph_no_storage_real() throws Exception {
        MvcResult result = mvc.perform(get("/demo/documentos/graph"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("s3://");
        assertThat(body).doesNotContain("file://");
    }

    // =========================================================================
    // GET /demo/actas/dataset-funcional - shape contract
    // =========================================================================

    @Test
    @DisplayName("dataset: campos raiz clave para frontend presentes")
    void dataset_campos_raiz_clave() throws Exception {
        mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActasMock").isNumber())
                .andExpect(jsonPath("$.totalCasosUsoCubiertos").isNumber())
                .andExpect(jsonPath("$.totalDocumentosEsperados").isNumber())
                .andExpect(jsonPath("$.coberturaCompletaSegunDominioActual").isBoolean())
                .andExpect(jsonPath("$.actas").isArray())
                .andExpect(jsonPath("$.casosUsoCubiertos").isArray())
                .andExpect(jsonPath("$.casosUsoPendientes").isArray())
                .andExpect(jsonPath("$.advertencias").isArray());
    }

    @Test
    @DisplayName("dataset: acta tiene campos de presentacion (codigo, titulo, descripcion, casoUsoPrincipal)")
    void dataset_acta_campos_presentacion() throws Exception {
        mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actas[0].codigo").isString())
                .andExpect(jsonPath("$.actas[0].titulo").isString())
                .andExpect(jsonPath("$.actas[0].descripcion").isString())
                .andExpect(jsonPath("$.actas[0].casoUsoPrincipal").isString());
    }

    @Test
    @DisplayName("dataset: acta tiene campos de estado (bloqueEsperado, situacionEsperada, bandejaEsperada)")
    void dataset_acta_campos_estado() throws Exception {
        mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actas[0].bloqueEsperado").isString())
                .andExpect(jsonPath("$.actas[0].situacionEsperada").isString())
                .andExpect(jsonPath("$.actas[0].bandejaEsperada").isString())
                .andExpect(jsonPath("$.actas[0].cerrableEsperado").isBoolean());
    }

    @Test
    @DisplayName("dataset: totalActasMock coincide con largo del array actas")
    void dataset_total_coincide_con_array() throws Exception {
        MvcResult result = mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andReturn();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        int total = json.get("totalActasMock").asInt();
        int arraySize = json.get("actas").size();
        assertThat(total).isEqualTo(arraySize);
    }

    // =========================================================================
    // POST /demo/dev/reset - guardrails
    // =========================================================================

    @Test
    @DisplayName("reset: GET /demo/dev/reset devuelve 405 (solo POST permitido)")
    void reset_get_devuelve_405() throws Exception {
        mvc.perform(get("/demo/dev/reset"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("reset: POST /demo/dev/reset sin habilitar devuelve 404")
    void reset_deshabilitado_devuelve_404() throws Exception {
        mvc.perform(post("/demo/dev/reset").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("demo: GET /demo/documentos/graph funciona sin reset habilitado")
    void graph_funciona_sin_reset() throws Exception {
        mvc.perform(get("/demo/documentos/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completo").value(true));
    }

    @Test
    @DisplayName("demo: GET /demo/actas/dataset-funcional funciona sin reset habilitado")
    void dataset_funciona_sin_reset() throws Exception {
        mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActasMock").value(org.hamcrest.Matchers.greaterThanOrEqualTo(31)));
    }

    // =========================================================================
    // Guardrail: docs/spec-as-source no en raiz del repo
    // =========================================================================

    @Test
    @DisplayName("guardrail: docs/spec-as-source no existe en raiz del repo")
    void spec_as_source_no_en_raiz() {
        File prohibited = new File("../../docs/spec-as-source");
        assertThat(prohibited.exists())
                .as("docs/spec-as-source NO debe existir en la raiz del repo")
                .isFalse();
    }
}
