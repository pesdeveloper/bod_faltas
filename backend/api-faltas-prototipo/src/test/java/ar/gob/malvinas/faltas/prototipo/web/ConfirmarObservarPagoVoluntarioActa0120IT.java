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
 * ACTA-0120: pago voluntario informado con comprobante adjunto → PENDIENTE_CONFIRMACION.
 *
 * <p>Verifica el circuito de verificación interna:
 * <ul>
 *   <li>Estado inicial: {@code PENDIENTE_CONFIRMACION}, acciones disponibles
 *       {@code CONFIRMAR} y {@code OBSERVAR}, accionPendiente
 *       {@code VERIFICAR_PAGO_INFORMADO}, sin resultado final.</li>
 *   <li>Confirmar pago: {@code resultadoFinal = PAGO_CONFIRMADO},
 *       {@code cerrable = true}, sin acciones de pago voluntario
 *       adicionales.</li>
 *   <li>Observar pago (circuito alternativo): {@code situacionPago = OBSERVADO},
 *       resultado final sigue {@code SIN_RESULTADO_FINAL}, no cerrable,
 *       acción {@code INFORMAR} disponible para reinformar.</li>
 * </ul>
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ConfirmarObservarPagoVoluntarioActa0120IT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0120";

    @Autowired
    private MockMvc mvc;

    @Test
    void estadoInicial_esPendienteConfirmacion_conAccionesConfirmarYObservar() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("PENDIENTE_CONFIRMACION"))
                .andExpect(jsonPath("$.accionPendiente").value("VERIFICAR_PAGO_INFORMADO"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles").isArray())
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles[0]").value("CONFIRMAR"))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles[1]").value("OBSERVAR"))
                .andExpect(jsonPath("$.pagoInformado.comprobante.id").value("COMP-0120"));
    }

    @Test
    void confirmarPago_dejaResultadoFinalPagoConfirmadoYCerrable() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-informado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.actaId").value(ACTA))
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("PAGO_CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles").isEmpty());
    }

    @Test
    void observarPago_dejaObservadoSinResultadoFinalYPermiteReinformar() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/observar-pago-informado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.actaId").value(ACTA))
                .andExpect(jsonPath("$.situacionPago").value("OBSERVADO"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("OBSERVADO"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles[0]").value("INFORMAR"));
    }

    @Test
    void confirmarPago_idNoExistente_devuelve404() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/ACTA-XXXX/acciones/confirmar-pago-informado"))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirmarPago_sinPagoInformado_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        // ACTA-0029 está en PENDIENTE_ANALISIS pero sin pago informado (SIN_PAGO)
        mvc.perform(post(B + "/actas/ACTA-0029/acciones/confirmar-pago-informado"))
                .andExpect(status().isConflict());
    }
}
