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

/**
 * Dirección — POST consentir-condena-y-registrar-pago transiciona correctamente
 * a CONDENA_FIRME + INFORMADO.
 *
 * <p>Reglas verificadas:
 * <ul>
 *   <li>resultadoFinal pasa a CONDENA_FIRME.</li>
 *   <li>situacionPagoCondena pasa a INFORMADO.</li>
 *   <li>situacionPago pasa a PENDIENTE_CONFIRMACION.</li>
 *   <li>tipoPago pasa a CONDENA.</li>
 *   <li>montoCondena se conserva.</li>
 *   <li>accionesPagoCondenaDisponibles incluye CONFIRMAR y OBSERVAR.</li>
 *   <li>accionesPagoCondenaDisponibles no incluye INFORMAR (ya informado).</li>
 *   <li>accionesUi.consentirCondenaYRegistrarPago=false (ya ejecutado).</li>
 *   <li>Eventos CONDENA_CONSENTIDA_PRESENCIAL y PAGO_CONDENA_REGISTRADO_PRESENCIAL registrados.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ConsentirCondenaYRegistrarPagoPasaAFirmeInformadoIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";
    private static final String QR = "QR-ACTA-0030-DEMO";

    @Autowired
    private MockMvc mvc;

    private void prepararCondenado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":3500}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0030-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());
    }

    @Test
    void consentirCondenaYRegistrarPago_resultadoFinalEsCondenaFirme() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENA_FIRME"));
    }

    @Test
    void consentirCondenaYRegistrarPago_situacionPagoCondenaEsInformado() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"));
    }

    @Test
    void consentirCondenaYRegistrarPago_situacionPagoEsPendienteConfirmacion() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("PENDIENTE_CONFIRMACION"));
    }

    @Test
    void consentirCondenaYRegistrarPago_tipoPagoEsCondena() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPago").value("CONDENA"));
    }

    @Test
    void consentirCondenaYRegistrarPago_montoCondenaSeConserva() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoCondena").value(3500));
    }

    @Test
    void consentirCondenaYRegistrarPago_accionesConfirmarYObservarDisponibles() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles", hasItem("CONFIRMAR")))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles", hasItem("OBSERVAR")));
    }

    @Test
    void consentirCondenaYRegistrarPago_accionInformarNoDisponible() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles", not(hasItem("INFORMAR"))));
    }

    @Test
    void consentirCondenaYRegistrarPago_accionUiApagada() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesUi.consentirCondenaYRegistrarPago").value(false));
    }

    @Test
    void consentirCondenaYRegistrarPago_registraEventosPresenciales() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("CONDENA_CONSENTIDA_PRESENCIAL")))
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("PAGO_CONDENA_REGISTRADO_PRESENCIAL")));
    }
}
