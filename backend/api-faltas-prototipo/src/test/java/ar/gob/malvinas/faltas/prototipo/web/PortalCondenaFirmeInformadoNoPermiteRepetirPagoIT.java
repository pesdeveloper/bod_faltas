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
 * Portal infractor — CONDENA_FIRME con situacionPagoCondena=INFORMADO.
 *
 * <p>Reglas verificadas:
 * <ul>
 *   <li>Portal no permite pagar nuevamente (puedePagarCondena=false).</li>
 *   <li>POST pagar-condena devuelve 409.</li>
 *   <li>mensajeVisible indica pago en proceso de acreditación.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PortalCondenaFirmeInformadoNoPermiteRepetirPagoIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";
    private static final String QR = "QR-ACTA-0030-DEMO";

    @Autowired
    private MockMvc mvc;

    private void prepararCondenaFirmeInformada() throws Exception {
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
    void informado_portalNoPermiteRepetirPago() throws Exception {
        prepararCondenaFirmeInformada();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.puedePagarCondena").value(false));
    }

    @Test
    void informado_postPagarCondena_devuelve409() throws Exception {
        prepararCondenaFirmeInformada();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-condena"))
                .andExpect(status().isConflict());
    }

    @Test
    void informado_mensajeVisibleIndicaProcesoAcreditacion() throws Exception {
        prepararCondenaFirmeInformada();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensajeVisible").value(
                        "Pago de condena en proceso de acreditaci\u00f3n. Direcci\u00f3n de Faltas verificar\u00e1 la acreditaci\u00f3n."));
    }
}
