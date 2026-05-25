package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PendienteNotificacionBandejaTrasCorreoIT {

    private static final String B = "/api/prototipo";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listarPendienteNotificacion_trasGenerarYAnularLoteCorreo_noDevuelve500() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioFirmado("ACTA-0030", "DOC-0030-02");

        MvcResult lote = mvc.perform(post(B + "/notificaciones/correo/lotes/generar"))
                .andExpect(status().isOk())
                .andReturn();
        String loteId = objectMapper.readTree(lote.getResponse().getContentAsString()).get("loteId").asText();

        mvc.perform(post(B + "/notificaciones/correo/lotes/" + loteId + "/anular"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/bandejas/PENDIENTE_NOTIFICACION/actas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0030')].id").value(hasItem("ACTA-0030")))
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0030')].estadoProcesoActual")
                        .value(hasItem("PENDIENTE_ENVIO")));
    }

    @Test
    void listarPendienteNotificacion_conNotificacionListaParaEnvio_cargaResumen() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioFirmado("ACTA-0030", "DOC-0030-02");

        mvc.perform(get(B + "/bandejas/PENDIENTE_NOTIFICACION/actas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0030')].estadoProcesoActual")
                        .value(hasItem("PENDIENTE_ENVIO")))
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0030')].cerrabilidad.resultadoFinal")
                        .value(hasItem("SIN_RESULTADO_FINAL")));
    }

    @Test
    void listarPendienteNotificacion_trasGenerarLoteSinAnular_devuelveActa0030EnEnNotificacion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioFirmado("ACTA-0030", "DOC-0030-02");

        mvc.perform(post(B + "/notificaciones/correo/lotes/generar")).andExpect(status().isOk());

        mvc.perform(get(B + "/bandejas/PENDIENTE_NOTIFICACION/actas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0030')]").doesNotExist());

        mvc.perform(get(B + "/bandejas/EN_NOTIFICACION/actas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'ACTA-0030')].id").value(hasItem("ACTA-0030")));
    }

    @Test
    void listarPendienteNotificacion_listadoYDetalleEnParalelo_trasOperarCorreo_noDevuelve500() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioFirmado("ACTA-0030", "DOC-0030-02");

        MvcResult lote = mvc.perform(post(B + "/notificaciones/correo/lotes/generar"))
                .andExpect(status().isOk())
                .andReturn();
        String loteId = objectMapper.readTree(lote.getResponse().getContentAsString()).get("loteId").asText();
        mvc.perform(post(B + "/notificaciones/correo/lotes/" + loteId + "/anular"))
                .andExpect(status().isOk());

        Thread listado = new Thread(() -> {
            try {
                mvc.perform(get(B + "/bandejas/PENDIENTE_NOTIFICACION/actas"))
                        .andExpect(status().isOk());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        Thread detalle = new Thread(() -> {
            try {
                mvc.perform(get(B + "/actas/ACTA-0030")).andExpect(status().isOk());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        listado.start();
        detalle.start();
        listado.join();
        detalle.join();
    }

    @Test
    void listarPendienteNotificacion_trasProcesarRespuestaCorreo_noDevuelve500() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioFirmado("ACTA-0030", "DOC-0030-02");

        mvc.perform(post(B + "/notificaciones/correo/lotes/generar")).andExpect(status().isOk());
        mvc.perform(post(B + "/notificaciones/correo/respuestas/procesar-demo")).andExpect(status().isOk());

        mvc.perform(get(B + "/bandejas/PENDIENTE_NOTIFICACION/actas"))
                .andExpect(status().isOk());
    }

    private void prepararFalloCondenatorioFirmado(String actaId, String documentoId) throws Exception {
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":12345.67}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/firmar-documento/" + documentoId))
                .andExpect(status().isOk());
    }
}
