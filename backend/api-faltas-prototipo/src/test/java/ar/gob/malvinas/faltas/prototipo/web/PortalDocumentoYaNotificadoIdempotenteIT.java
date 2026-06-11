package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Portal infractor — idempotencia de la apertura de documentos ya
 * notificados: abrir dos veces el mismo documento no duplica
 * notificaciones, no agrega eventos y no rompe el estado del expediente.
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PortalDocumentoYaNotificadoIdempotenteIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";
    private static final String QR = "QR-ACTA-0030-DEMO";

    @Autowired
    private MockMvc mvc;

    private void prepararFalloNotificadoPorPortal() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":1200}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0030-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENADO"));
    }

    @Test
    void verDocumentoYaNotificado_devuelve200SoloConsultaSinCambios() throws Exception {
        prepararFalloNotificadoPorPortal();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.montoCondena").value(1200))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(true))
                .andExpect(jsonPath("$.puedePagar").value(false))
                .andExpect(jsonPath("$.documentos", hasSize(1)))
                .andExpect(jsonPath("$.documentos[0].estadoNotificacion").value("NOTIFICADO"))
                .andExpect(jsonPath("$.documentos[0].puedeAbrir").value(true));
    }

    @Test
    void verDocumentoDosVeces_noDuplicaNotificacionesNiEventos() throws Exception {
        prepararFalloNotificadoPorPortal();

        MvcResult antes = mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult eventosAntes = mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andReturn();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());

        MvcResult despues = mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult eventosDespues = mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(
                antes.getResponse().getContentAsString(),
                despues.getResponse().getContentAsString(),
                "El detalle admin no debe cambiar al reabrir un documento ya notificado");
        assertEquals(
                eventosAntes.getResponse().getContentAsString(),
                eventosDespues.getResponse().getContentAsString(),
                "No deben agregarse eventos al reabrir un documento ya notificado");
    }

    /**
     * Regla "notificación alternativa superada por portal": la postal
     * pendiente (NOT-0030-03) queda SUPERADA_POR_PORTAL al abrir el fallo;
     * la segunda apertura no la vuelve a tocar ni duplica la notificación
     * portal positiva.
     */
    @Test
    void verDocumentoDosVeces_noVuelveATocarLaAlternativaSuperada() throws Exception {
        prepararFalloNotificadoPorPortal();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.notificaciones[?(@.canalTipificado == 'PORTAL_INFRACTOR')]",
                        hasSize(1)))
                .andExpect(jsonPath(
                        "$.notificaciones[?(@.id == 'NOT-0030-03')].resultado",
                        hasItem("SUPERADA_POR_PORTAL")))
                .andExpect(jsonPath(
                        "$.notificaciones[?(@.id == 'NOT-0030-03')].estado",
                        hasItem("SIN_EFECTO")));
    }

    @Test
    void verDocumentoDosVeces_estadoJuridicoEstable() throws Exception {
        prepararFalloNotificadoPorPortal();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("NO_APLICA"))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));
    }
}
