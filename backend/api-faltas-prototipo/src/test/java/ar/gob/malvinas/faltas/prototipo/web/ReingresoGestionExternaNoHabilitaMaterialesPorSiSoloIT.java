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
 * Regla: reingreso desde gestión externa (Apremio/Juzgado) sin pago confirmado
 * NO habilita materiales por si solo.
 *
 * <p>ACTA-0140 simula el estado post-reingreso: {@code resultadoFinal=CONDENA_FIRME}
 * con {@code situacionPagoCondena} sin confirmar y bloqueantes materiales presentes.
 * El habilitante material para CONDENA_FIRME requiere
 * {@code situacionPagoCondena=CONFIRMADO}; sin esa condicion los materiales
 * quedan no ejecutables.
 *
 * <p>Comportamiento esperado:
 * <ul>
 *   <li>{@code accionesUi.cumplimientoMaterial=false}</li>
 *   <li>{@code accionesUi.resolucionBloqueante=false}</li>
 *   <li>Intentar ejecutar cumplimiento devuelve 409.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ReingresoGestionExternaNoHabilitaMaterialesPorSiSoloIT {

    private static final String B = "/api/prototipo";
    /**
     * ACTA-0140: CONDENA_FIRME + sin pago condena confirmado + materiales pendientes.
     */
    private static final String ACTA = "ACTA-0140";

    @Autowired
    private MockMvc mvc;

    @Test
    void condenaFirmeSinPagoConfirmado_accionesUi_materialesApagados() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(false))
                .andExpect(jsonPath("$.accionesUi.resolucionBloqueante").value(false));
    }

    @Test
    void condenaFirmeSinPagoConfirmado_bloqueantesPresentes() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]").exists());
    }

    @Test
    void condenaFirmeSinPagoConfirmado_intentarCumplirDocumentacion_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA
                        + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isConflict());
    }

    @Test
    void condenaFirmeSinPagoConfirmado_intentarCumplirRodado_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA
                        + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isConflict());
    }
}
