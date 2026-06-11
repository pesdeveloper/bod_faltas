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
 * Dirección — la acción compuesta consentir-condena-y-registrar-pago rechaza
 * (409) cuando el fallo condenatorio existe pero aún no fue firmado.
 *
 * <p>El fallo en estado {@code PENDIENTE_FIRMA} no es suficiente para habilitar
 * la acción: se requiere que el documento esté efectivamente firmado.
 *
 * <p>Reglas verificadas:
 * <ul>
 *   <li>accionesUi.consentirCondenaYRegistrarPago=false cuando fallo pendiente de firma.</li>
 *   <li>POST consentir-condena-y-registrar-pago devuelve 409 en ese estado.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ConsentirCondenaYRegistrarPagoRechazaSiFalloNoFirmadoIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";

    @Autowired
    private MockMvc mvc;

    /**
     * Setup: fallo dictado pero no firmado todavía.
     * El acta queda en PENDIENTE_FIRMA con el fallo en PENDIENTE_FIRMA.
     */
    private void prepararFalloNofirmado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":3500}"))
                .andExpect(status().isOk());
        // No se firma el documento: el fallo queda en PENDIENTE_FIRMA.
    }

    @Test
    void falloNoFirmado_accionUiDeshabilitada() throws Exception {
        prepararFalloNofirmado();

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesUi.consentirCondenaYRegistrarPago").value(false));
    }

    @Test
    void falloNoFirmado_rechazaConConflict() throws Exception {
        prepararFalloNofirmado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isConflict());
    }
}
