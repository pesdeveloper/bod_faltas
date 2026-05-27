package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class SubBandejaDinamicaIT {

    private static final String B = "/api/prototipo";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void bandejas_devuelveCantidadPorBandejaYSubBandejasSinCero() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        MvcResult result = mvc.perform(get(B + "/bandejas")).andExpect(status().isOk()).andReturn();
        JsonNode bandejas = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(bandejas.size() > 0);

        for (JsonNode bandeja : bandejas) {
            assertTrue(bandeja.get("cantidad").asInt() > 0);
            assertTrue(bandeja.has("label"));
            JsonNode subBandejas = bandeja.get("subBandejas");
            assertTrue(subBandejas.isArray());
            int sumaSub = 0;
            for (JsonNode sub : subBandejas) {
                assertTrue(sub.get("cantidad").asInt() > 0);
                assertTrue(sub.has("label"));
                sumaSub += sub.get("cantidad").asInt();
                assertTrue(!"TODAS".equalsIgnoreCase(sub.get("codigo").asText()));
            }
            assertEquals(bandeja.get("cantidad").asInt(), sumaSub, "Suma sub-bandejas vs padre: " + bandeja.get("codigo"));
        }
    }

    @Test
    void bandejas_noIncluyeSubBandejaTodas() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/bandejas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].subBandejas[*].codigo").value(not(hasItem("TODAS"))));
    }

    @Test
    void cadaActaTieneUnaSolaSubBandejaPrimariaEnListado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        Set<String> vistos = new HashSet<>();
        for (String bandeja : new String[] {
            "ACTAS_EN_ENRIQUECIMIENTO",
            "PENDIENTE_PREPARACION_DOCUMENTAL",
            "PENDIENTE_FIRMA",
            "PENDIENTE_NOTIFICACION",
            "EN_NOTIFICACION",
            "PENDIENTE_ANALISIS",
            "PENDIENTES_RESOLUCION_REDACCION",
            "GESTION_EXTERNA",
            "ARCHIVO",
            "CERRADAS"
        }) {
            MvcResult r = mvc.perform(get(B + "/bandejas/" + bandeja + "/actas"))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode actas = objectMapper.readTree(r.getResponse().getContentAsString());
            for (JsonNode acta : actas) {
                String id = acta.get("id").asText();
                assertTrue(vistos.add(id), "Acta duplicada en listados: " + id);
                assertTrue(acta.has("subBandeja"));
                assertTrue(acta.has("subBandejaLabel"));
                assertTrue(acta.has("chip"));
                assertTrue(acta.has("accionPrincipal"));
                assertTrue(acta.has("prioridadSubBandeja"));
                assertTrue(acta.has("chipsSecundarios"));
            }
        }
    }

    @Test
    void listadoSinSubBandeja_devuelveTodasLasActasDeLaBandeja() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        MvcResult sinFiltro = mvc.perform(get(B + "/bandejas/PENDIENTE_ANALISIS/actas"))
                .andExpect(status().isOk())
                .andReturn();
        int total = objectMapper.readTree(sinFiltro.getResponse().getContentAsString()).size();

        MvcResult bandejas = mvc.perform(get(B + "/bandejas")).andExpect(status().isOk()).andReturn();
        JsonNode resumen = objectMapper.readTree(bandejas.getResponse().getContentAsString());
        int cantidadPadre = 0;
        for (JsonNode b : resumen) {
            if ("PENDIENTE_ANALISIS".equals(b.get("codigo").asText())) {
                cantidadPadre = b.get("cantidad").asInt();
            }
        }
        assertEquals(cantidadPadre, total);
    }

    @Test
    void listadoConSubBandeja_filtraSoloActasClasificadas() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/bandejas/PENDIENTE_ANALISIS/actas")
                        .param("subBandeja", "ANALISIS_LISTO_DERIVAR_EXTERNA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].subBandeja").value(everyItem(org.hamcrest.Matchers.is("ANALISIS_LISTO_DERIVAR_EXTERNA"))))
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0015')]").exists());

        mvc.perform(get(B + "/bandejas/PENDIENTE_ANALISIS/actas")
                        .param("subBandeja", "SUB_BANDEJA_INEXISTENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void acta0024_enriquecimientoConBloqueosMateriales_chipYAccionOperativos() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/bandejas/ACTAS_EN_ENRIQUECIMIENTO/actas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0024')].chip").value(hasItem("Pendientes materiales")))
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0024')].accionPrincipal").value(hasItem("Gestionar bloqueos")))
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0024')].chip").value(not(hasItem("D1"))))
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0024')].accionPrincipal").value(not(hasItem("Completar acta"))));
    }

    @Test
    void acta0004TrasNotificacionPositiva_clasificaAnalisisNotifPositiva() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/ACTA-0004/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/bandejas/PENDIENTE_ANALISIS/actas")
                        .param("subBandeja", "ANALISIS_NOTIF_POSITIVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0004')].subBandeja")
                        .value(hasItem("ANALISIS_NOTIF_POSITIVA")))
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0004')].subBandejaLabel")
                        .value(hasItem("Notificación fehaciente positiva")))
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0004')].chip")
                        .value(hasItem("Notificada positiva")))
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0004')].accionPrincipal")
                        .value(hasItem("Analizar expediente")));
    }

    @Test
    void gestionExterna_clasificaPorApremioOJuzgadoDePaz() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/bandejas/GESTION_EXTERNA/actas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0017')].subBandeja")
                        .value(hasItem("EXT_JUZGADO_PAZ")));

        mvc.perform(post(B + "/actas/ACTA-0015/acciones/derivar-a-apremio"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/bandejas/GESTION_EXTERNA/actas")
                        .param("subBandeja", "EXT_APREMIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0015')].subBandeja")
                        .value(hasItem("EXT_APREMIO")));
    }

    @Test
    void enNotificacion_clasificaPorCanal() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/bandejas/EN_NOTIFICACION/actas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0005')].subBandeja")
                        .value(hasItem("NOTIF_EN_CORREO_POSTAL")));

        mvc.perform(get(B + "/bandejas/PENDIENTE_NOTIFICACION/actas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0004')].subBandeja")
                        .value(hasItem("NOTIF_ACTA_LISTA_ENVIO")));
    }

    @Test
    void bandejas_subBandejasCantidadMayorQueCero() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/bandejas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].subBandejas[*].cantidad").value(everyItem(greaterThan(0))));
    }
}
