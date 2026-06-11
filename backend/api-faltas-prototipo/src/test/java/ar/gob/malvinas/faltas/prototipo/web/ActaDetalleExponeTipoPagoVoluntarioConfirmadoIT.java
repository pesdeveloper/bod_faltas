package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dirección/admin — detalle de acta con pago voluntario confirmado.
 *
 * <p>Parte de ACTA-0120 (pre-cargada con pago informado / PENDIENTE_CONFIRMACION).
 * Tras confirmar, verifica que el JSON expone:
 * <ul>
 *   <li>{@code situacionPago = CONFIRMADO}</li>
 *   <li>{@code tipoPago = VOLUNTARIO}</li>
 *   <li>{@code situacionPagoCondena = NO_APLICA}</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ActaDetalleExponeTipoPagoVoluntarioConfirmadoIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0120";

    @Autowired
    private MockMvc mvc;

    @Test
    void detalleExponeTipoPagoVoluntario() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-informado"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"))
                .andExpect(jsonPath("$.tipoPago").value("VOLUNTARIO"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("NO_APLICA"));
    }
}
