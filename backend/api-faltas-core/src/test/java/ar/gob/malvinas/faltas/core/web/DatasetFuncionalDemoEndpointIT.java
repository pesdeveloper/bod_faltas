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
 * Test de integracion del endpoint GET /demo/actas/dataset-funcional.
 *
 * Verifica que el endpoint devuelve la matriz de cobertura funcional correctamente
 * sin modificar estado, sin ejecutar flujos, sin usar base real.
 *
 * Slice 8F-4B.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "faltas.demo.enabled=true")
@DisplayName("IT 8F-4B: GET /demo/actas/dataset-funcional - dataset funcional del dominio")
class DatasetFuncionalDemoEndpointIT {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("1. GET /demo/actas/dataset-funcional devuelve 200")
    void endpoint_devuelve_200() throws Exception {
        mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("2. Devuelve totalActasMock mayor a cero")
    void devuelve_total_actas_mock_mayor_a_cero() throws Exception {
        mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActasMock").isNumber())
                .andExpect(jsonPath("$.totalActasMock").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("3. Devuelve totalCasosUsoCubiertos mayor a cero")
    void devuelve_casos_uso_cubiertos() throws Exception {
        mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCasosUsoCubiertos").isNumber())
                .andExpect(jsonPath("$.totalCasosUsoCubiertos").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("4. Devuelve totalDocumentosEsperados mayor a cero")
    void devuelve_total_documentos_esperados() throws Exception {
        mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocumentosEsperados").isNumber())
                .andExpect(jsonPath("$.totalDocumentosEsperados").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("5. Devuelve lista de actas no vacia")
    void devuelve_lista_actas_no_vacia() throws Exception {
        mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actas").isArray())
                .andExpect(jsonPath("$.actas.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("6. Devuelve lista de casosUsoCubiertos no vacia")
    void devuelve_lista_casos_uso_cubiertos_no_vacia() throws Exception {
        mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casosUsoCubiertos").isArray())
                .andExpect(jsonPath("$.casosUsoCubiertos.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("7. Devuelve casosUsoPendientes como array")
    void devuelve_casos_pendientes_como_array() throws Exception {
        mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casosUsoPendientes").isArray());
    }

    @Test
    @DisplayName("8. La respuesta contiene ACT-001-LABRADA en la lista de actas")
    void respuesta_contiene_acta_001() throws Exception {
        MvcResult result = mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("ACT-001-LABRADA");
    }

    @Test
    @DisplayName("9. La respuesta no contiene storage real ni SQL")
    void respuesta_no_contiene_storage_real_ni_sql() throws Exception {
        MvcResult result = mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("s3://");
        assertThat(body).doesNotContain("file://");
        assertThat(body).doesNotContain("DROP TABLE");
        assertThat(body).doesNotContain("CREATE TABLE");
    }

    @Test
    @DisplayName("10. Dos llamadas consecutivas devuelven el mismo totalActasMock")
    void dos_llamadas_devuelven_mismo_resultado() throws Exception {
        MvcResult r1 = mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk()).andReturn();
        MvcResult r2 = mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk()).andReturn();

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode j1 = mapper.readTree(r1.getResponse().getContentAsString());
        com.fasterxml.jackson.databind.JsonNode j2 = mapper.readTree(r2.getResponse().getContentAsString());
        assertThat(j1.get("totalActasMock").asInt()).isEqualTo(j2.get("totalActasMock").asInt());
    }

    @Test
    @DisplayName("11. La respuesta contiene las 6 actas nuevas de 8F-4B-R1")
    void respuesta_contiene_actas_8f4b_r1() throws Exception {
        MvcResult result = mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("ACT-026-NOTIFICACION-NEGATIVA");
        assertThat(body).contains("ACT-027-DOC-ADJUNTO-CONVALIDADO");
        assertThat(body).contains("ACT-028-ABSOLUCION-FIRME-CERRADA");
        assertThat(body).contains("ACT-029-REINGRESO-PARA-REVISION");
        assertThat(body).contains("ACT-030-PAGO-CONDENA-OBSERVADO");
        assertThat(body).contains("ACT-031-PAGO-CONDENA-CON-DESCUENTO");
    }

    @Test
    @DisplayName("12. totalActasMock es al menos 31 (8F-4B-R1 completo)")
    void total_actas_al_menos_31() throws Exception {
        mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActasMock").value(org.hamcrest.Matchers.greaterThanOrEqualTo(31)));
    }
}