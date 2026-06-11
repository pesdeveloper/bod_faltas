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
 * Regla: absolución habilita materiales pero no cierra el acta si quedan
 * pendientes bloqueantes.
 *
 * <p>Flujo con ACTA-0024:
 * <ol>
 *   <li>Admin mueve a PENDIENTE_ANALISIS via registrar-solicitud-pago-voluntario.</li>
 *   <li>Marcar resultado final ABSUELTO.</li>
 *   <li>Verificar {@code accionesUi.cumplimientoMaterial=true},
 *       {@code accionesUi.resolucionBloqueante=true},
 *       {@code cerrabilidad.cerrable=false},
 *       {@code accionesUi.cierre=false}.</li>
 * </ol>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class AbsolucionHabilitaMaterialesPeroNoCierreSiPendientesIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0024";

    @Autowired
    private MockMvc mvc;

    /**
     * Mueve ACTA-0024 a PENDIENTE_ANALISIS con monto fijado (estado previo
     * requerido por marcar-resultado-final-absuelto).
     */
    private void prepararEnAnalisis() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());
    }

    @Test
    void absuelto_accionesUi_materialesPrendidos() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararEnAnalisis();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/marcar-resultado-final-absuelto"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("ABSUELTO"))
                .andExpect(jsonPath("$.accionesUi.resolucionBloqueante").value(true))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(false));
    }

    @Test
    void absuelto_conMaterialesPendientes_noCerrable() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararEnAnalisis();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/marcar-resultado-final-absuelto"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.accionesUi.cierre").value(false));
    }

    @Test
    void absuelto_bloqueantesPresentes() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararEnAnalisis();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/marcar-resultado-final-absuelto"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]").exists());
    }
}
