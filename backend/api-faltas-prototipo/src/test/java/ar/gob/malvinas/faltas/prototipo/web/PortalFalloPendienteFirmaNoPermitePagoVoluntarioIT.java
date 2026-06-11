package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Portal infractor — bloqueo de pago voluntario cuando existe
 * {@code FALLO_CONDENATORIO:PENDIENTE_FIRMA} (ACTA-0030).
 *
 * <p>Regla de dominio: si ya se dictó un fallo condenatorio, aunque todavía
 * esté pendiente de firma, el portal no debe permitir solicitar ni pagar
 * voluntariamente. El documento no es visible para el infractor hasta que
 * esté firmado y notificado.
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PortalFalloPendienteFirmaNoPermitePagoVoluntarioIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";
    private static final String QR = "QR-ACTA-0030-DEMO";

    @Autowired
    private MockMvc mvc;

    private void prepararFalloCondenatorioPendienteFirma() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":1200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"));
    }

    @Test
    void falloPendienteFirma_get_noOfreceSolicitarPagoVoluntario() throws Exception {
        prepararFalloCondenatorioPendienteFirma();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acta").value("A-2026-0030"))
                .andExpect(jsonPath("$.estadoVisible").value("EN_TRAMITE"))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false));
    }

    @Test
    void falloPendienteFirma_get_noOfrecePagar() throws Exception {
        prepararFalloCondenatorioPendienteFirma();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedePagar").value(false));
    }

    @Test
    void falloPendienteFirma_get_documentosFalloNoVisibles() throws Exception {
        prepararFalloCondenatorioPendienteFirma();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentos", hasSize(0)));
    }

    @Test
    void falloPendienteFirma_get_mensajeResolucionEnProceso() throws Exception {
        prepararFalloCondenatorioPendienteFirma();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensajeVisible",
                        containsString("resolución en proceso de formalización")));
    }

    @Test
    void falloPendienteFirma_post_solicitarPagoVoluntario_devuelve409() throws Exception {
        prepararFalloCondenatorioPendienteFirma();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isConflict());
    }

    @Test
    void falloPendienteFirma_post_solicitarPagoVoluntario_noModificaSituacionPago() throws Exception {
        prepararFalloCondenatorioPendienteFirma();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"));
    }

    @Test
    void falloPendienteFirma_post_solicitarPagoVoluntario_noAgregaEventoPagoSolicitado() throws Exception {
        prepararFalloCondenatorioPendienteFirma();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento",
                        not(org.hamcrest.Matchers.hasItem("PAGO_VOLUNTARIO_SOLICITADO"))));
    }

    @Test
    void falloPendienteFirma_post_solicitarPagoVoluntario_noModificaAccionPendiente() throws Exception {
        prepararFalloCondenatorioPendienteFirma();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionPendiente").value(not("EVALUAR_PAGO_VOLUNTARIO")));
    }

    @Test
    void falloPendienteFirma_post_pagarVoluntario_devuelve409() throws Exception {
        prepararFalloCondenatorioPendienteFirma();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isConflict());
    }
}
