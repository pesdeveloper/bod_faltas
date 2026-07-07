package ar.gob.malvinas.faltas.core.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de contrato frontend-ready para GET /demo/actas/{codigo}.
 * Cubre los 15 puntos de cobertura requeridos por el slice 8F-7.
 * Slice 8F-7.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("8F-7: GET /demo/actas/{codigo} - drill-down acta demo")
class DemoActaDetalleContractTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("1. GET /demo/actas/{codigo} con codigo valido devuelve 200")
    void codigo_valido_devuelve_200() throws Exception {
        mvc.perform(get("/demo/actas/ACT-001-LABRADA")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("2. Response tiene campos raiz requeridos")
    void response_tiene_campos_raiz() throws Exception {
        mvc.perform(get("/demo/actas/ACT-001-LABRADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").isString())
                .andExpect(jsonPath("$.titulo").isString())
                .andExpect(jsonPath("$.descripcion").isString())
                .andExpect(jsonPath("$.casoUsoPrincipal").isString())
                .andExpect(jsonPath("$.acta").isMap())
                .andExpect(jsonPath("$.timeline").isArray())
                .andExpect(jsonPath("$.documentos").isArray())
                .andExpect(jsonPath("$.demo").isMap())
                .andExpect(jsonPath("$.links").isMap());
    }

    @Test
    @DisplayName("3. acta contiene actaId, bloqueActual, situacionAdministrativa, cerrable")
    void acta_contiene_estado_frontend_ready() throws Exception {
        mvc.perform(get("/demo/actas/ACT-001-LABRADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acta.actaId").isNumber())
                .andExpect(jsonPath("$.acta.bloqueActual").isString())
                .andExpect(jsonPath("$.acta.situacionAdministrativa").isString())
                .andExpect(jsonPath("$.acta.cerrable").isBoolean());
    }

    @Test
    @DisplayName("4. timeline no vacio para ACT-001 (genera ACTLAB)")
    void timeline_no_vacio_para_act001() throws Exception {
        mvc.perform(get("/demo/actas/ACT-001-LABRADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeline").isArray())
                .andExpect(jsonPath("$.timeline.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("4b. timeline no vacio para ACT-012 (fallo condenatorio)")
    void timeline_no_vacio_para_act012() throws Exception {
        mvc.perform(get("/demo/actas/ACT-012-FALLO-COND-DICTADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeline.length()").value(org.hamcrest.Matchers.greaterThan(2)));
    }

    @Test
    @DisplayName("5. timeline orden estable: primer evento orden=1")
    void timeline_orden_estable() throws Exception {
        mvc.perform(get("/demo/actas/ACT-004-PENDIENTE-NOTIFICACION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeline[0].orden").value(1))
                .andExpect(jsonPath("$.timeline[0].tipoEvento").isString());
    }

    @Test
    @DisplayName("5b. timeline ordenado: cada evento tiene orden mayor al anterior")
    void timeline_orden_incrementado() throws Exception {
        MvcResult result = mvc.perform(get("/demo/actas/ACT-006-ANAL-LISTA-FALLO"))
                .andExpect(status().isOk()).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode timeline = json.get("timeline");
        assertThat(timeline).isNotNull();
        assertThat(timeline.size()).isGreaterThan(1);
        for (int i = 1; i < timeline.size(); i++) {
            assertThat(timeline.get(i).get("orden").asInt())
                    .isGreaterThan(timeline.get(i - 1).get("orden").asInt());
        }
    }

    @Test
    @DisplayName("6. ACT-003 tiene documentos")
    void documentos_presentes_en_caso_documental() throws Exception {
        mvc.perform(get("/demo/actas/ACT-003-DOC-PENDIENTE-FIRMA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentos.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("6b. documento tiene campos documentoId, tipoDocu, estadoDocu, mock")
    void documento_tiene_campos_requeridos() throws Exception {
        mvc.perform(get("/demo/actas/ACT-003-DOC-PENDIENTE-FIRMA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentos[0].documentoId").isNumber())
                .andExpect(jsonPath("$.documentos[0].tipoDocu").isString())
                .andExpect(jsonPath("$.documentos[0].estadoDocu").isString())
                .andExpect(jsonPath("$.documentos[0].mock").value(true));
    }

    @Test
    @DisplayName("7. storageKey no usa file:// ni s3:// (guardrail mock)")
    void storageKey_no_usa_storage_real() throws Exception {
        MvcResult result = mvc.perform(get("/demo/actas/ACT-027-DOC-ADJUNTO-CONVALIDADO"))
                .andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("file://").doesNotContain("s3://");
    }

    @Test
    @DisplayName("8. hashDocu contiene mock cuando existe")
    void hashDocu_es_mock() throws Exception {
        MvcResult result = mvc.perform(get("/demo/actas/ACT-027-DOC-ADJUNTO-CONVALIDADO"))
                .andExpect(status().isOk()).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode documentos = json.get("documentos");
        if (documentos != null && documentos.size() > 0) {
            for (JsonNode doc : documentos) {
                JsonNode hash = doc.get("hashDocu");
                if (hash != null && !hash.isNull()) {
                    assertThat(hash.asText()).contains("mock");
                }
            }
        }
    }

    @Test
    @DisplayName("9. codigo inexistente devuelve 404")
    void codigo_inexistente_devuelve_404() throws Exception {
        mvc.perform(get("/demo/actas/ACT-999-INEXISTENTE")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("9b. codigo desconocido devuelve error 4xx (no 500)")
    void codigo_basura_no_devuelve_500() throws Exception {
        mvc.perform(get("/demo/actas/CODIGO-QUE-NO-EXISTE"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("10. GET repetido es idempotente")
    void get_repetido_es_idempotente() throws Exception {
        MvcResult r1 = mvc.perform(get("/demo/actas/ACT-015-CONDENA-FIRME"))
                .andExpect(status().isOk()).andReturn();
        MvcResult r2 = mvc.perform(get("/demo/actas/ACT-015-CONDENA-FIRME"))
                .andExpect(status().isOk()).andReturn();
        JsonNode j1 = objectMapper.readTree(r1.getResponse().getContentAsString());
        JsonNode j2 = objectMapper.readTree(r2.getResponse().getContentAsString());
        assertThat(j1.get("acta").get("bloqueActual").asText())
                .isEqualTo(j2.get("acta").get("bloqueActual").asText());
        assertThat(j1.get("timeline").size()).isEqualTo(j2.get("timeline").size());
        assertThat(j1.get("documentos").size()).isEqualTo(j2.get("documentos").size());
    }

    @Test
    @DisplayName("11. dataset-funcional incluye detallePath por acta")
    void dataset_incluye_detalle_path() throws Exception {
        mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actas[0].detallePath").isString())
                .andExpect(jsonPath("$.actas[0].detallePath")
                        .value(org.hamcrest.Matchers.startsWith("/demo/actas/")));
    }

    @Test
    @DisplayName("11b. detallePath apunta al codigo del acta")
    void dataset_detalle_path_apunta_al_codigo() throws Exception {
        MvcResult result = mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk()).andReturn();
        JsonNode primerActa = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("actas").get(0);
        assertThat(primerActa.get("detallePath").asText())
                .isEqualTo("/demo/actas/" + primerActa.get("codigo").asText());
    }

    @Test
    @DisplayName("12. detalle funciona sin reset habilitado")
    void detalle_funciona_sin_reset_habilitado() throws Exception {
        mvc.perform(get("/demo/actas/ACT-007-PAGVOL-SOLICITADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acta.actaId").isNumber())
                .andExpect(jsonPath("$.timeline.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("13. reset disabled devuelve 404")
    void reset_disabled_devuelve_404() throws Exception {
        mvc.perform(post("/demo/dev/reset").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("14a. GET /demo/documentos/graph sigue funcionando")
    void graph_sigue_funcionando() throws Exception {
        mvc.perform(get("/demo/documentos/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completo").value(true));
    }

    @Test
    @DisplayName("14b. GET /demo/actas/dataset-funcional sigue funcionando")
    void dataset_sigue_funcionando() throws Exception {
        mvc.perform(get("/demo/actas/dataset-funcional"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActasMock")
                        .value(org.hamcrest.Matchers.greaterThanOrEqualTo(31)));
    }

    @Test
    @DisplayName("15. guardrail: docs/spec-as-source no existe en raiz del repo")
    void spec_as_source_no_en_raiz() {
        assertThat(new File("../../docs/spec-as-source").exists()).isFalse();
    }

    @Test
    @DisplayName("Extra: ACT-021 (bloqueante) tiene timeline")
    void act021_bloqueante_tiene_timeline() throws Exception {
        mvc.perform(get("/demo/actas/ACT-021-BLOQUEANTE-ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeline.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("Extra: ACT-020 (paralizada) tiene situacion PARALIZADA")
    void act020_paralizada_estado_correcto() throws Exception {
        MvcResult result = mvc.perform(get("/demo/actas/ACT-020-PARALIZADA"))
                .andExpect(status().isOk()).andReturn();
        assertThat(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("acta").get("situacionAdministrativa").asText()).isEqualTo("PARALIZADA");
    }

    @Test
    @DisplayName("Extra: ACT-017 (condena pagada) tiene bloque CERR")
    void act017_condena_pagada_bloque_cerr() throws Exception {
        MvcResult result = mvc.perform(get("/demo/actas/ACT-017-CONDENA-FIRME-PAGADA"))
                .andExpect(status().isOk()).andReturn();
        assertThat(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("acta").get("bloqueActual").asText()).isEqualTo("CERR");
    }

    @Test
    @DisplayName("Extra: demo.mock=true y source=DATASET_FUNCIONAL")
    void demo_mock_siempre_true() throws Exception {
        mvc.perform(get("/demo/actas/ACT-001-LABRADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demo.mock").value(true))
                .andExpect(jsonPath("$.demo.source").value("DATASET_FUNCIONAL"));
    }

    @Test
    @DisplayName("Extra: links.self apunta al codigo consultado")
    void links_self_apunta_al_codigo() throws Exception {
        mvc.perform(get("/demo/actas/ACT-011-ABSUELTO-CERRADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links.self").value("/demo/actas/ACT-011-ABSUELTO-CERRADO"))
                .andExpect(jsonPath("$.links.dataset").value("/demo/actas/dataset-funcional"))
                .andExpect(jsonPath("$.links.graph").value("/demo/documentos/graph"));
    }

    @Test
    @DisplayName("Extra: ACT-029 (reingreso) es consultable")
    void act029_reingreso_es_consultable() throws Exception {
        mvc.perform(get("/demo/actas/ACT-029-REINGRESO-PARA-REVISION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("ACT-029-REINGRESO-PARA-REVISION"));
    }

    @ParameterizedTest(name = "Codigo representativo consultable: {0}")
    @ValueSource(strings = {
        "ACT-001-LABRADA",
        "ACT-002-EN-ENRIQUECIMIENTO",
        "ACT-010-FALLO-ABS-DICTADO",
        "ACT-015-CONDENA-FIRME",
        "ACT-018-GESTION-EXTERNA",
        "ACT-020-PARALIZADA",
        "ACT-021-BLOQUEANTE-ACTIVO",
        "ACT-027-DOC-ADJUNTO-CONVALIDADO"
    })
    @DisplayName("Todo codigo representativo del dataset es consultable")
    void codigos_representativos_son_consultables(String codigo) throws Exception {
        mvc.perform(get("/demo/actas/" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.acta.actaId").isNumber());
    }
}
