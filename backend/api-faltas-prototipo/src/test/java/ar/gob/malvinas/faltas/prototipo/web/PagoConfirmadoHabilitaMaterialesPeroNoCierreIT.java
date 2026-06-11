package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regla: pago voluntario confirmado habilita materiales pero no cierre
 * mientras queden pendientes bloqueantes.
 *
 * <p>ACTA-0021 tiene {@code resultadoFinal=PAGO_CONFIRMADO} precargado y dos
 * bloqueantes materiales ({@code LIBERACION_RODADO} y
 * {@code ENTREGA_DOCUMENTACION}).
 *
 * <p>Comportamiento esperado:
 * <ul>
 *   <li>{@code accionesUi.cumplimientoMaterial=true}</li>
 *   <li>{@code accionesUi.resolucionBloqueante=true}</li>
 *   <li>{@code accionesUi.cierre=false}</li>
 *   <li>{@code cerrabilidad.cerrable=false}</li>
 *   <li>Los bloqueantes siguen presentes.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PagoConfirmadoHabilitaMaterialesPeroNoCierreIT {

    private static final String B = "/api/prototipo";
    /**
     * ACTA-0021: pago confirmado precargado + rodado retenido + documentacion retenida.
     */
    private static final String ACTA = "ACTA-0021";

    @Autowired
    private MockMvc mvc;

    @Test
    void pagoConfirmado_accionesUi_materialesPrendidos() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("PAGO_CONFIRMADO"))
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(true))
                .andExpect(jsonPath("$.accionesUi.resolucionBloqueante").value(true));
    }

    @Test
    void pagoConfirmado_conMaterialesPendientes_noCerrable() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.accionesUi.cierre").value(false));
    }

    @Test
    void pagoConfirmado_bloqueantesPresentes() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre").isArray())
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]").exists());
    }

    @Test
    void pagoConfirmado_resultadoFinalYSituacionCorrectos() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("PAGO_CONFIRMADO"))
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"))
                .andExpect(jsonPath("$.tipoPago").value(not("CONDENA")));
    }
}
