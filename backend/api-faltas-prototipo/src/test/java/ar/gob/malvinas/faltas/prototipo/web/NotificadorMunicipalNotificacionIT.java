package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class NotificadorMunicipalNotificacionIT {

    private static final String B = "/api/prototipo";

    @Autowired
    private MockMvc mvc;

    @Test
    void listarNotificacionesNotificadorMunicipal_pendientes() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/notificaciones/notificador-municipal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].notificacionId").value(hasItem("NOT-0031-01")))
                .andExpect(jsonPath("$[*].notificacionId").value(hasItem("NOT-0032-01")))
                .andExpect(jsonPath("$[*].notificacionId").value(hasItem("NOT-0033-01")))
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0031-01')].canal")
                        .value(hasItem("NOTIFICADOR_MUNICIPAL")))
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0031-01')].estado")
                        .value(hasItem("LISTA_PARA_ENVIO")))
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0031-01')].qrNotificacion")
                        .value(hasItem("QR-NOT-NOT-0031-01-DEMO")));
    }

    @Test
    void registrarAcusePositivoFalloCondenatorio_pasaActaACondenado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/notificaciones/notificador-municipal/NOT-0031-01/acuse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"POSITIVA\",\"observacion\":\"Entregada en domicilio\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificacion.resultado").value("POSITIVA"))
                .andExpect(jsonPath("$.notificacion.estado").value("ENTREGADA"))
                .andExpect(jsonPath("$.notificacion.observacion").value("Entregada en domicilio"));

        mvc.perform(get(B + "/actas/ACTA-0031"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0031-01')].resultado").value(hasItem("POSITIVA")));
    }

    @Test
    void registrarAcusePositivoFalloAbsolutorio_pasaActaAAbsuelto() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/notificaciones/notificador-municipal/NOT-0032-01/acuse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"POSITIVA\",\"observacion\":\"Entregada por notificador\"}"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/ACTA-0032"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("ABSUELTO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0032-01')].resultado").value(hasItem("POSITIVA")));
    }

    @Test
    void registrarAcuseNegativo_noCambiaResultadoFinalJuridico() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/notificaciones/notificador-municipal/NOT-0031-01/acuse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"NEGATIVA\",\"observacion\":\"Domicilio no ubicado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificacion.resultado").value("NEGATIVA"));

        mvc.perform(get(B + "/actas/ACTA-0031"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.accionPendiente").value("REINTENTAR_NOTIFICACION"))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0031-01')].resultado").value(hasItem("NEGATIVA")))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal", not("CONDENADO")))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal", not("ABSUELTO")));
    }

    @Test
    void registrarAcuseVencido_noCambiaResultadoFinalJuridico() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/notificaciones/notificador-municipal/NOT-0032-01/acuse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"VENCIDA\",\"observacion\":\"Sin acuse dentro del plazo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificacion.resultado").value("VENCIDA"));

        mvc.perform(get(B + "/actas/ACTA-0032"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.accionPendiente").value("EVALUAR_NOTIFICACION_VENCIDA"))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0032-01')].resultado").value(hasItem("VENCIDA")))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal", not("CONDENADO")))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal", not("ABSUELTO")));
    }

    @Test
    void registrarAcuse_notificacionInexistente_responde404Claro() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/notificaciones/notificador-municipal/NOT-NO-EXISTE/acuse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"POSITIVA\",\"observacion\":\"Sin datos\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrarAcuse_deOtroCanal_respondeConflict() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/notificaciones/notificador-municipal/NOT-0004-01/acuse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"POSITIVA\",\"observacion\":\"No corresponde\"}"))
                .andExpect(status().isConflict());
    }
}
