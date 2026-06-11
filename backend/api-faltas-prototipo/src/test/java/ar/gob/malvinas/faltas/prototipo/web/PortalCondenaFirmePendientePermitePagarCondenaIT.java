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
 * Portal infractor — CONDENA_FIRME con situacionPagoCondena=PENDIENTE.
 *
 * <p>Reglas verificadas:
 * <ul>
 *   <li>resultadoFinal=CONDENA_FIRME, situacionPagoCondena=PENDIENTE, montoCondena > 0.</li>
 *   <li>Portal muestra puedePagarCondena=true.</li>
 *   <li>Portal no permite pago voluntario ni apelación.</li>
 *   <li>mensajeVisible indica condena firme con monto pendiente.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PortalCondenaFirmePendientePermitePagarCondenaIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";
    private static final String QR = "QR-ACTA-0030-DEMO";

    @Autowired
    private MockMvc mvc;

    private void prepararCondenaFirme() throws Exception {
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
    }

    @Test
    void condenaFirmePendiente_puedePagarCondena() throws Exception {
        prepararCondenaFirme();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.puedePagarCondena").value(true));
    }

    @Test
    void condenaFirmePendiente_montoCondenaVisible() throws Exception {
        prepararCondenaFirme();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoCondena").value(35000));
    }

    @Test
    void condenaFirmePendiente_noPermitePagoVoluntario() throws Exception {
        prepararCondenaFirme();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedePagar").value(false))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false));
    }

    @Test
    void condenaFirmePendiente_noPermiteApelacion() throws Exception {
        prepararCondenaFirme();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false));
    }

    @Test
    void condenaFirmePendiente_mensajeVisibleCorrecto() throws Exception {
        prepararCondenaFirme();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensajeVisible").value(
                        "La condena se encuentra firme y registra un monto pendiente de pago."));
    }
}
