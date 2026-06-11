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
 * Dirección — flujo completo: consentir-condena-y-registrar-pago seguido de
 * confirmar-pago-condena (confirmar acreditación).
 *
 * <p>Reglas verificadas:
 * <ul>
 *   <li>Tras confirmar: situacionPagoCondena=CONFIRMADO.</li>
 *   <li>Tras confirmar: situacionPago=CONFIRMADO.</li>
 *   <li>Tras confirmar: tipoPago=CONDENA.</li>
 *   <li>Tras confirmar: cerrable=true (sin bloqueantes materiales).</li>
 *   <li>resultadoFinal permanece CONDENA_FIRME.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ConsentirCondenaYRegistrarPagoLuegoConfirmarAcreditacionIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";
    private static final String QR = "QR-ACTA-0030-DEMO";

    @Autowired
    private MockMvc mvc;

    private void prepararInformado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":3500}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0030-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk());
    }

    @Test
    void confirmarAcreditacion_situacionPagoCondenaEsConfirmado() throws Exception {
        prepararInformado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("CONFIRMADO"));
    }

    @Test
    void confirmarAcreditacion_situacionPagoEsConfirmado() throws Exception {
        prepararInformado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"));
    }

    @Test
    void confirmarAcreditacion_tipoPagoEsCondena() throws Exception {
        prepararInformado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPago").value("CONDENA"));
    }

    @Test
    void confirmarAcreditacion_resultadoFinalSigueCondenaFirme() throws Exception {
        prepararInformado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"));
    }

    @Test
    void confirmarAcreditacion_actaEsCerrable() throws Exception {
        prepararInformado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true));
    }
}
