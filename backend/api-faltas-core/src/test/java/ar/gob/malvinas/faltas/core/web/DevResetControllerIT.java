package ar.gob.malvinas.faltas.core.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integracion del endpoint POST /demo/dev/reset.
 *
 * Requiere faltas.demo.reset.enabled=true.
 *
 * Slice 8F-5.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"faltas.demo.reset.enabled=true", "faltas.demo.enabled=true"})
@DisplayName("IT 8F-5: POST /demo/dev/reset - reset in-memory")
class DevResetControllerIT {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("1. POST /demo/dev/reset responde 200 cuando esta habilitado")
    void reset_responde_200() throws Exception {
        mvc.perform(post("/demo/dev/reset").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("2. response contiene ejecutado=true")
    void response_ejecutado_true() throws Exception {
        mvc.perform(post("/demo/dev/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ejecutado").value(true));
    }

    @Test
    @DisplayName("3. response informa repositorios reseteados (>= 24)")
    void response_informa_repositorios_reseteados() throws Exception {
        mvc.perform(post("/demo/dev/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoriosReseteados").value(org.hamcrest.Matchers.greaterThanOrEqualTo(24)));
    }

    @Test
    @DisplayName("4. response lista repositorios reseteados no vacia")
    void response_lista_repositorios() throws Exception {
        mvc.perform(post("/demo/dev/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositorios").isArray())
                .andExpect(jsonPath("$.repositorios.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(24)));
    }

    @Test
    @DisplayName("5. segundo reset tambien responde 200 (idempotente)")
    void segundo_reset_tambien_ok() throws Exception {
        mvc.perform(post("/demo/dev/reset")).andExpect(status().isOk());
        mvc.perform(post("/demo/dev/reset")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("6. tercer reset devuelve ejecutado=true y 0 errores")
    void tercer_reset_ok() throws Exception {
        mvc.perform(post("/demo/dev/reset"));
        mvc.perform(post("/demo/dev/reset"));
        mvc.perform(post("/demo/dev/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ejecutado").value(true))
                .andExpect(jsonPath("$.errores").value(0));
    }

    @Test
    @DisplayName("7. luego del reset GET /demo/documentos/graph sigue funcionando")
    void graph_demo_funciona_post_reset() throws Exception {
        mvc.perform(post("/demo/dev/reset")).andExpect(status().isOk());

        mvc.perform(get("/demo/documentos/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCasos").value(8))
                .andExpect(jsonPath("$.casosExitosos").value(8))
                .andExpect(jsonPath("$.completo").value(true));
    }

    @Test
    @DisplayName("8. luego del reset GET /demo/actas/dataset-funcional sigue funcionando")
    void dataset_funcional_funciona_post_reset() throws Exception {
        mvc.perform(post("/demo/dev/reset")).andExpect(status().isOk());

        mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActasMock").value(37));
    }

    @Test
    @DisplayName("9. response contiene modo=memory")
    void response_modo_memory() throws Exception {
        mvc.perform(post("/demo/dev/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("memory"));
    }

    @Test
    @DisplayName("10. response contiene fhReset no nulo")
    void response_fh_reset_presente() throws Exception {
        mvc.perform(post("/demo/dev/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fhReset").isNotEmpty());
    }

    @Test
    @DisplayName("11. response contiene plantillasRecreadas=8")
    void response_plantillas_recreadas() throws Exception {
        mvc.perform(post("/demo/dev/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plantillasRecreadas").value(8));
    }

    @Test
    @DisplayName("12. response contiene casosDatasetFuncional=37")
    void response_casos_dataset_31() throws Exception {
        mvc.perform(post("/demo/dev/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casosDatasetFuncional").value(37));
    }

    @Test
    @DisplayName("13. el response no contiene referencias a SQL ni JDBC ni MariaDB")
    void response_no_contiene_sql() throws Exception {
        MvcResult result = mvc.perform(post("/demo/dev/reset"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body.toLowerCase()).doesNotContain("jdbc");
        assertThat(body.toLowerCase()).doesNotContain("mariadb");
        assertThat(body.toLowerCase()).doesNotContain("sql");
    }

    @Test
    @DisplayName("14. luego de reset+graph+reset: graph sigue funcionando (doble ciclo)")
    void doble_ciclo_reset_graph() throws Exception {
        mvc.perform(post("/demo/dev/reset")).andExpect(status().isOk());
        mvc.perform(get("/demo/documentos/graph")).andExpect(status().isOk());
        mvc.perform(post("/demo/dev/reset")).andExpect(status().isOk());
        mvc.perform(get("/demo/documentos/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casosExitosos").value(8));
    }
}