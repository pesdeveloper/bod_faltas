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
 * Portal infractor — CONDENA_FIRME con situacionPagoCondena=OBSERVADO (reintento permitido).
 *
 * <p>Reglas verificadas:
 * <ul>
 *   <li>Portal permite pagar condena nuevamente (puedePagarCondena=true).</li>
 *   <li>POST pagar-condena pasa a INFORMADO.</li>
 *   <li>Dirección vuelve a tener acciones CONFIRMAR y OBSERVAR.</li>
 *   <li>mensajeVisible indica que el pago fue observado y puede reintentar.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PortalCondenaFirmeObservadoPermiteReintentoIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";
    private static final String QR = "QR-ACTA-0030-DEMO";

    @Autowired
    private MockMvc mvc;

    private void prepararCondenaFirmeObservada() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":35000}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0030-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-vencimiento-plazo-apelacion"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-condena"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/observar-pago-condena"))
                .andExpect(status().isOk());
    }

    @Test
    void observado_portalPermiteReintentoPago() throws Exception {
        prepararCondenaFirmeObservada();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.puedePagarCondena").value(true));
    }

    @Test
    void observado_mensajeVisibleIndicaReintento() throws Exception {
        prepararCondenaFirmeObservada();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensajeVisible").value(
                        "El pago de condena fue observado. Puede reintentar el pago."));
    }

    @Test
    void observado_postPagarCondena_pasaAInformado() throws Exception {
        prepararCondenaFirmeObservada();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedePagarCondena").value(false));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"));
    }

    @Test
    void observado_reintento_direccionQuedaConAccionesConfirmarYObservar() throws Exception {
        prepararCondenaFirmeObservada();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles", hasItem("CONFIRMAR")))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles", hasItem("OBSERVAR")));
    }
}
