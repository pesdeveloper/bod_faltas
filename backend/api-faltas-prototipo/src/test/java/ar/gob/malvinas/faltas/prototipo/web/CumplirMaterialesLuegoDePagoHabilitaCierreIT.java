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
 * Regla: al cumplir todos los materiales pendientes luego del habilitante, el acta
 * se vuelve cerrable y puede cerrarse.
 *
 * <p>ACTA-0021 parte con {@code resultadoFinal=PAGO_CONFIRMADO} y dos bloqueantes
 * ({@code LIBERACION_RODADO} y {@code ENTREGA_DOCUMENTACION}).
 *
 * <p>Flujo verificado:
 * <ol>
 *   <li>Cumplir {@code ENTREGA_DOCUMENTACION} (atomico: el store crea el resolutorio).</li>
 *   <li>Emitir resolutorio documental para {@code LIBERACION_RODADO}.</li>
 *   <li>Cumplir {@code LIBERACION_RODADO}.</li>
 *   <li>Verificar {@code cerrable=true} y {@code accionesUi.cierre=true}.</li>
 *   <li>Cerrar acta: {@code bandeja=CERRADAS}, {@code situacionAdministrativa=CERRADA}.</li>
 * </ol>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class CumplirMaterialesLuegoDePagoHabilitaCierreIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0021";

    @Autowired
    private MockMvc mvc;

    private void cumplirMateriales() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA
                        + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA
                        + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA
                        + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk());
    }

    @Test
    void cumplirDocumentacion_devuelveOk() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA
                        + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk());
    }

    @Test
    void cumplirRodadoSinResolutorioPrevio_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA
                        + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isConflict());
    }

    @Test
    void cumplirTodosMateriales_habilitaCierre() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        cumplirMateriales();

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.accionesUi.cierre").value(true))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(0));
    }

    @Test
    void cumplirTodosMateriales_luegoCerrarActa_bandejaCerradas() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        cumplirMateriales();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/cerrar-acta"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("CERRADA"))
                .andExpect(jsonPath("$.estaCerrada").value(true))
                .andExpect(jsonPath("$.permiteReingreso").value(false));
    }

    @Test
    void cumplirTodosMateriales_luegoCerrarActa_accionesUiApagadas() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        cumplirMateriales();
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/cerrar-acta")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesUi.cierre").value(false))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(false))
                .andExpect(jsonPath("$.accionesUi.pagoVoluntario").value(false));
    }
}
