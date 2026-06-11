package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Portal infractor — estado CONDENADO (fallo notificado, condena aún no firme).
 *
 * <p>Reglas verificadas:
 * <ul>
 *   <li>resultadoFinal=CONDENADO, situacionPagoCondena=NO_APLICA, montoCondena > 0.</li>
 *   <li>Portal permite presentar apelación.</li>
 *   <li>Portal NO muestra puedePagarCondena=true (condena no firme).</li>
 *   <li>Portal NO muestra puedePagar ni puedeSolicitarPagoVoluntario.</li>
 *   <li>mensajeVisible indica que puede apelar dentro del plazo.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PortalCondenadoNoFirmeNoPermitePagoCondenaIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";
    private static final String QR = "QR-ACTA-0030-DEMO";

    @Autowired
    private MockMvc mvc;

    private void prepararCondenado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":35000}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0030-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());
    }

    @Test
    void condenado_portalPermiteApelacion() throws Exception {
        prepararCondenado();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(true));
    }

    @Test
    void condenado_portalNoPermitePagoCondena() throws Exception {
        prepararCondenado();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.puedePagarCondena").value(false));
    }

    @Test
    void condenado_portalNoPermitePagoVoluntario() throws Exception {
        prepararCondenado();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedePagar").value(false))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false));
    }

    @Test
    void condenado_montoCondenaVisible() throws Exception {
        prepararCondenado();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoCondena").value(35000));
    }

    @Test
    void condenado_mensajeVisibleIndicaApelacion() throws Exception {
        prepararCondenado();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensajeVisible").value(
                        "El fallo fue notificado. Puede presentar apelaci\u00f3n dentro del plazo correspondiente"
                                + " o consentir la condena para avanzar al pago."));
    }

    @Test
    void condenado_postPagarCondena_devuelve409() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-condena"))
                .andExpect(status().isConflict());
    }
}
