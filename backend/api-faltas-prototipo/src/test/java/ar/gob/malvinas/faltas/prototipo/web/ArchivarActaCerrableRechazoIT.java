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
 * Regla operativa: archivo y cierre son salidas mutuamente excluyentes
 * desde {@code PENDIENTE_ANALISIS}. Si la acta ya cumple condición de
 * cierre operativo ({@code cerrabilidad.cerrable == true}), el endpoint
 * de archivo debe rechazar con 409 (Conflict) y corresponde cerrar el
 * acta, no archivarla.
 *
 * <p>El test cubre los tres casos del slice:
 * <ul>
 *   <li>{@code ACTA-0026}: precarga en {@code PENDIENTE_ANALISIS} con
 *       {@code PAGO_CONFIRMADO} y sin pendientes bloqueantes
 *       ({@code cerrable == true}) → archivar-acta responde 409 y la
 *       bandeja no transiciona; cerrar-acta sigue funcionando.</li>
 *   <li>{@code ACTA-0021}: precarga en {@code PENDIENTE_ANALISIS} con
 *       {@code PAGO_CONFIRMADO} y dos pendientes bloqueantes activos
 *       ({@code LIBERACION_RODADO}, {@code ENTREGA_DOCUMENTACION};
 *       sin medida preventiva — {@code cerrable == false}) → archivar-acta
 *       sigue habilitado (regla previa intacta).</li>
 *   <li>{@code ACTA-0007}: precarga en {@code ARCHIVO} con
 *       {@code permiteReingreso == true} → reingresar-acta sigue
 *       funcionando.</li>
 * </ul>
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ArchivarActaCerrableRechazoIT {

    private static final String B = "/api/prototipo";

    @Autowired
    private MockMvc mvc;

    @Test
    void archivarActa_enPendienteAnalisisCerrable_responde409YnoTransiciona() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0026";

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("PAGO_CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(0));

        mvc.perform(post(B + "/actas/" + id + "/acciones/archivar-acta"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.estaCerrada").value(false));

        mvc.perform(post(B + "/actas/" + id + "/acciones/cerrar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"))
                .andExpect(jsonPath("$.estaCerrada").value(true));
    }

    @Test
    void archivarActa_enPendienteAnalisisNoCerrable_sigueHabilitado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0021";

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("PAGO_CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(2));

        mvc.perform(post(B + "/actas/" + id + "/acciones/archivar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"));
    }

    @Test
    void reingresarActa_desdeArchivoConPermiteReingreso_sigueFuncionando() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0007";

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.permiteReingreso").value(true));

        mvc.perform(post(B + "/actas/" + id + "/acciones/reingresar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));
    }
}
