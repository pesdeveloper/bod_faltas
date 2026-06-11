package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Portal infractor — JSON expone {@code tipoPago=CONDENA} una vez informado el pago.
 *
 * <p>Regla verificada: en CONDENA_FIRME, desde el momento en que el infractor
 * informa el pago de condena, el portal debe exponer {@code tipoPago=CONDENA}
 * (no {@code NO_APLICA}) en la respuesta de {@code GET /infractor/actas/{qr}}.
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PortalInfractorExponeTipoPagoCondenaIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";
    private static final String QR = "QR-ACTA-0030-DEMO";

    @Autowired
    private MockMvc mvc;

    private void prepararCondenaFirmeYPagoInformado() throws Exception {
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
    }

    @Test
    void portalExponeTipoPagoCondena_cuandoPagoInformado() throws Exception {
        prepararCondenaFirmeYPagoInformado();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPago").value("CONDENA"));
    }

    @Test
    void portalExponeTipoPagoCondena_cuandoPagoConfirmado() throws Exception {
        prepararCondenaFirmeYPagoInformado();
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPago").value("CONDENA"))
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"))
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENA_FIRME"));
    }
}
