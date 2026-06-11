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
 * Regla: pendientes materiales NO son ejecutables antes de un resultado habilitante.
 *
 * <p>ACTA-0024 parte con {@code resultadoFinal=SIN_RESULTADO_FINAL},
 * {@code situacionPago=SIN_PAGO} y bloqueantes materiales activos
 * ({@code ENTREGA_DOCUMENTACION} y {@code LIBERACION_RODADO}).
 *
 * <p>Comportamiento esperado:
 * <ul>
 *   <li>{@code accionesUi.cumplimientoMaterial=false}</li>
 *   <li>{@code accionesUi.resolucionBloqueante=false}</li>
 *   <li>{@code accionesUi.pagoVoluntario=true}</li>
 *   <li>Intentar ejecutar cumplimiento material por endpoint devuelve 409.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class MaterialesNoEjecutablesSinResultadoHabilitanteIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0024";

    @Autowired
    private MockMvc mvc;

    @Test
    void estadoInicial_accionesUi_materialesApagados_pagoVoluntarioPrendido() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(false))
                .andExpect(jsonPath("$.accionesUi.resolucionBloqueante").value(false))
                .andExpect(jsonPath("$.accionesUi.pagoVoluntario").value(true))
                .andExpect(jsonPath("$.accionesUi.cierre").value(false));
    }

    @Test
    void estadoInicial_bloqueantesVisibles_peroNoEjecutables() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]").exists());
    }

    @Test
    void intentarCumplirDocumentacion_sinHabilitante_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA
                        + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isConflict());
    }

    @Test
    void intentarCumplirRodado_sinHabilitante_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA
                        + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isConflict());
    }

    @Test
    void intentarCumplirRodado_estadoNoModificado_despues409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA
                        + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(false));
    }
}
