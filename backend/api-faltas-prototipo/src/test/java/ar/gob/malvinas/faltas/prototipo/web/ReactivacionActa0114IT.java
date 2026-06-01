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
 * Contrato de reactivacion de ACTA-0114 (caso demo PARALIZADAS).
 *
 * <p>Verifica el circuito completo:
 * <ol>
 *   <li>Estado inicial: acta en PARALIZADAS con accionPendiente de paralizacion.</li>
 *   <li>POST reactivar-acta: respuesta OK, bandeja PENDIENTE_ANALISIS.</li>
 *   <li>Estado posterior: fuera de PARALIZADAS, situacion ACTIVA, accion pendiente
 *       REVISION_POST_REACTIVACION.</li>
 *   <li>POST reactivar-acta sobre acta ya no paralizada: 409 Conflict.</li>
 * </ol>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ReactivacionActa0114IT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0114";

    @Autowired
    private MockMvc mvc;

    @Test
    void estadoInicial_actaEnParalizadas_conAccionParalizacion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PARALIZADAS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PARALIZADA"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("PARALIZADA"))
                .andExpect(jsonPath("$.accionPendiente").value("PARALIZACION_ESPERA_DOCUMENTAL"))
                .andExpect(jsonPath("$.estaCerrada").value(false));
    }

    @Test
    void reactivarActa_devuelveOkYBandejaPendienteAnalisis() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/reactivar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("OK"))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.accionPendiente").value("REVISION_POST_REACTIVACION"));
    }

    @Test
    void estadoPostReactivacion_saleDeParalizadasYQuedaOperable() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/reactivar-acta"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ACTIVA"))
                .andExpect(jsonPath("$.accionPendiente").value("REVISION_POST_REACTIVACION"))
                .andExpect(jsonPath("$.estaCerrada").value(false));
    }

    @Test
    void reactivarActaYaReactivada_devuelveConflict() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/reactivar-acta"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/reactivar-acta"))
                .andExpect(status().isConflict());
    }

    @Test
    void reactivarActaNoParalizada_devuelveConflict() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/ACTA-0001/acciones/reactivar-acta"))
                .andExpect(status().isConflict());
    }

    @Test
    void reactivarActaInexistente_devuelveNotFound() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/ACTA-XXXX/acciones/reactivar-acta"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postReactivacion_eventoRegistrado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/reactivar-acta"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tipoEvento == 'ACTA_REACTIVADA_DESDE_PARALIZADAS')]").exists());
    }

    @Test
    void postReactivacion_actaVisibleEnPendienteAnalisis() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/reactivar-acta"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/bandejas/PENDIENTE_ANALISIS/actas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + ID + "')]").exists());
    }

    @Test
    void postReactivacion_actaNoVisibleEnParalizadas() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/reactivar-acta"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/bandejas/PARALIZADAS/actas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + ID + "')]").doesNotExist());
    }
}
