package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dirección — acción compuesta cuando el fallo condenatorio está firmado pero
 * aún no fue notificado positivamente (camino B).
 *
 * <p>El infractor se presenta en Dirección, consiente la condena y paga sin
 * esperar la notificación formal previa. La acción registra en un solo paso:
 * <ol>
 *   <li>Notificación positiva presencial del fallo
 *       ({@code FALLO_CONDENATORIO_NOTIFICADO_PRESENCIAL}).</li>
 *   <li>Consentimiento presencial de condena
 *       ({@code CONDENA_CONSENTIDA_PRESENCIAL}).</li>
 *   <li>Registro de pago de condena informado
 *       ({@code PAGO_CONDENA_REGISTRADO_PRESENCIAL}).</li>
 * </ol>
 *
 * <p>Reglas verificadas:
 * <ul>
 *   <li>accionesUi.consentirCondenaYRegistrarPago=true antes de ejecutar.</li>
 *   <li>resultadoFinal pasa a CONDENA_FIRME.</li>
 *   <li>situacionPagoCondena pasa a INFORMADO.</li>
 *   <li>situacionPago pasa a PENDIENTE_CONFIRMACION.</li>
 *   <li>tipoPago pasa a CONDENA.</li>
 *   <li>montoCondena se conserva.</li>
 *   <li>Evento FALLO_CONDENATORIO_NOTIFICADO_PRESENCIAL registrado.</li>
 *   <li>Eventos CONDENA_CONSENTIDA_PRESENCIAL y PAGO_CONDENA_REGISTRADO_PRESENCIAL registrados.</li>
 *   <li>accionesUi.consentirCondenaYRegistrarPago=false después de ejecutar.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ConsentirCondenaYRegistrarPagoConFalloFirmadoNoNotificadoIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";

    @Autowired
    private MockMvc mvc;

    /**
     * Setup: fallo dictado y firmado, sin notificación portal ni presencial.
     * El acta queda en PENDIENTE_NOTIFICACION con resultadoFinal=SIN_RESULTADO_FINAL.
     */
    private void prepararFalloFirmadoNoNotificado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":3500}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0030-02"))
                .andExpect(status().isOk());
        // No se llama al portal ni a registrar-notificacion-positiva:
        // el fallo está firmado pero sin notificación positiva.
    }

    @Test
    void falloFirmadoNoNotificado_accionUiHabilitada() throws Exception {
        prepararFalloFirmadoNoNotificado();

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesUi.consentirCondenaYRegistrarPago").value(true));
    }

    @Test
    void falloFirmadoNoNotificado_transicionaACondenaFirme() throws Exception {
        prepararFalloFirmadoNoNotificado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENA_FIRME"));
    }

    @Test
    void falloFirmadoNoNotificado_situacionPagoCondenaEsInformado() throws Exception {
        prepararFalloFirmadoNoNotificado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"));
    }

    @Test
    void falloFirmadoNoNotificado_situacionPagoEsPendienteConfirmacion() throws Exception {
        prepararFalloFirmadoNoNotificado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("PENDIENTE_CONFIRMACION"));
    }

    @Test
    void falloFirmadoNoNotificado_tipoPagoEsCondena() throws Exception {
        prepararFalloFirmadoNoNotificado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPago").value("CONDENA"));
    }

    @Test
    void falloFirmadoNoNotificado_montoCondenaSeConserva() throws Exception {
        prepararFalloFirmadoNoNotificado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoCondena").value(3500));
    }

    @Test
    void falloFirmadoNoNotificado_registraEventoNotificacionPresencial() throws Exception {
        prepararFalloFirmadoNoNotificado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("FALLO_CONDENATORIO_NOTIFICADO_PRESENCIAL")));
    }

    @Test
    void falloFirmadoNoNotificado_registraEventosConsentimientoYPago() throws Exception {
        prepararFalloFirmadoNoNotificado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("CONDENA_CONSENTIDA_PRESENCIAL")))
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("PAGO_CONDENA_REGISTRADO_PRESENCIAL")));
    }

    @Test
    void falloFirmadoNoNotificado_accionUiApagadaDespues() throws Exception {
        prepararFalloFirmadoNoNotificado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesUi.consentirCondenaYRegistrarPago").value(false));
    }
}
