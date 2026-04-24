package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Creación de acta mock mínima en vivo (POST {@code /actas/mock}): tránsito,
 * inspecciones con medida, bromatología con decomiso, numeración ACTA-DEMO.
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class AltaActaMockDemoIT {

    private static final String B = "/api/prototipo";

    @Autowired
    private MockMvc mvc;

    @Test
    void altaMockApareceEnListadoActasEnEnriquecimiento() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(
                        post(B + "/actas/mock")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"dependencia\": \"BROMATOLOGIA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("ACTA-DEMO-0001"))
                .andExpect(jsonPath("$.bandejaActual").value("ACTAS_EN_ENRIQUECIMIENTO"));
        mvc.perform(get(B + "/bandejas/ACTAS_EN_ENRIQUECIMIENTO/actas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem("ACTA-DEMO-0001")))
                .andExpect(jsonPath("$[*].numeroActa", hasItem("ACTA-DEMO-0001")));
    }

    @Test
    void demoTransito_rodadoYDocumentacion_bloqueantesYHechosMateriales() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(
                        post(B + "/actas/mock")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "dependencia": "TRANSITO",
                                  "ejeUrbano": true,
                                  "rodadoRetenidoOSecuestrado": true,
                                  "documentacionRetenida": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("ACTA-DEMO-0001"))
                .andExpect(jsonPath("$.numeroActa").value("ACTA-DEMO-0001"))
                .andExpect(jsonPath("$.bandejaActual").value("ACTAS_EN_ENRIQUECIMIENTO"))
                .andExpect(jsonPath("$.dependenciaDemo").value("TRANSITO"))
                .andExpect(jsonPath("$.tipoActaDemo").value("TRANSITO"))
                .andExpect(jsonPath("$.datosTransito.ejeUrbano").value(true))
                .andExpect(jsonPath("$.datosBromatologia").value(nullValue()))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(2))
                .andExpect(
                        jsonPath("$.cerrabilidad.pendientesBloqueantesCierre")
                                .value(containsInAnyOrder("ENTREGA_DOCUMENTACION", "LIBERACION_RODADO")));

        mvc.perform(get(B + "/actas/ACTA-DEMO-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dependenciaDemo").value("TRANSITO"));

        mvc.perform(get(B + "/actas/ACTA-DEMO-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].clave").value("RODADO"))
                .andExpect(
                        jsonPath("$.hechosMateriales.ejes[1].fase")
                                .value("SITUACION_PENDIENTE_DE_RESOLUTORIO"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[2].clave").value("DOCUMENTACION"))
                .andExpect(
                        jsonPath("$.hechosMateriales.ejes[2].fase")
                                .value("SITUACION_PENDIENTE_DE_RESOLUTORIO"));
    }

    @Test
    void inspecciones_clausura_levantamientoMedidaSinTransito() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(
                        post(B + "/actas/mock")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "dependencia": "INSPECCIONES",
                                  "medidaPreventivaClausura": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("ACTA-DEMO-0001"))
                .andExpect(jsonPath("$.dependenciaDemo").value("INSPECCIONES"))
                .andExpect(jsonPath("$.datosTransito").value(nullValue()))
                .andExpect(
                        jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]")
                                .value("LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].fase")
                                .value("SITUACION_PENDIENTE_DE_RESOLUTORIO"));
    }

    @Test
    void bromatologia_decomiso_visibleSinMedidaNiRodado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(
                        post(B + "/actas/mock")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "dependencia": "BROMATOLOGIA",
                                  "decomisoSustanciasAlimenticias": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("ACTA-DEMO-0001"))
                .andExpect(jsonPath("$.dependenciaDemo").value("BROMATOLOGIA"))
                .andExpect(jsonPath("$.datosBromatologia.decomisoSustanciasAlimenticias").value(true))
                .andExpect(jsonPath("$.datosTransito").value(nullValue()))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(0))
                .andExpect(
                        jsonPath("$.hechosMateriales.ejes[?(@.clave == 'MEDIDA_PREVENTIVA')].fase")
                                .value(hasItem("NO_APLICA")));
    }

    @Test
    void numeracionConsecutiva_dosAltaSinColision() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(
                        post(B + "/actas/mock")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"dependencia\": \"BROMATOLOGIA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroActa").value("ACTA-DEMO-0001"));
        mvc.perform(
                        post(B + "/actas/mock")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"dependencia\": \"BROMATOLOGIA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroActa").value("ACTA-DEMO-0002"));
        mvc.perform(get(B + "/actas/ACTA-DEMO-0001")).andExpect(status().isOk());
        mvc.perform(get(B + "/actas/ACTA-DEMO-0002")).andExpect(status().isOk());
    }
}
