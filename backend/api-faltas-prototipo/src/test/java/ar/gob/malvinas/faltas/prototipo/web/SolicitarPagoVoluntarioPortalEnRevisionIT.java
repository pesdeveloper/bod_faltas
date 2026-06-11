package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Portal infractor — acta en revisión (D1/D2, bandeja
 * {@code ACTAS_EN_ENRIQUECIMIENTO}): el portal reconoce el acta y muestra
 * el mensaje informativo, pero NO admite acciones sustantivas. El POST
 * {@code solicitar-pago-voluntario} debe rechazarse con 409 sin modificar
 * el expediente (situación de pago, monto, acción pendiente ni eventos).
 *
 * <p>Caso canónico: ACTA-0024 (nace en enriquecimiento).
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class SolicitarPagoVoluntarioPortalEnRevisionIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0024";
    private static final String QR = "QR-ACTA-0024-DEMO";

    private static final String MENSAJE_RECHAZO =
            "No se puede solicitar pago voluntario porque el acta se encuentra en revisión.";

    @Autowired
    private MockMvc mvc;

    @Test
    void actaEnRevision_getPortalRespondeEnRevisionSinAcciones() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVisible").value("EN_REVISION"))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false))
                .andExpect(jsonPath("$.puedePagar").value(false))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false))
                .andExpect(jsonPath("$.puedeConfirmarVisualizacionNotificacion").value(false));
    }

    @Test
    void actaEnRevision_postSolicitarPagoVoluntario_devuelve409ConMensaje() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isConflict())
                .andExpect(status().reason(MENSAJE_RECHAZO));
    }

    @Test
    void actaEnRevision_postRechazado_noCambiaSituacionPagoMontoNiAccionPendiente() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        MvcResult antes = mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ACTAS_EN_ENRIQUECIMIENTO"))
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").doesNotExist())
                .andReturn();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isConflict());

        MvcResult despues = mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(
                antes.getResponse().getContentAsString(),
                despues.getResponse().getContentAsString(),
                "El POST rechazado no debe modificar el expediente (pago, monto, acción pendiente)");
    }

    @Test
    void actaEnRevision_postRechazado_noRegistraEventosDePago() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        MvcResult eventosAntes = mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andReturn();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", not(hasItem("PAGO_VOLUNTARIO_SOLICITADO"))));

        MvcResult eventosDespues = mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(
                eventosAntes.getResponse().getContentAsString(),
                eventosDespues.getResponse().getContentAsString(),
                "El POST rechazado no debe agregar eventos al expediente");
    }
}
