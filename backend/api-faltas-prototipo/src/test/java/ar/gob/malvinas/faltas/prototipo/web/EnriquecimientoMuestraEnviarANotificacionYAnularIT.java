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
 * Regla operativa: en bandeja {@code ACTAS_EN_ENRIQUECIMIENTO} con
 * {@code estadoProceso=EN_CURSO}, {@code situacionAdministrativa=ACTIVA} y
 * {@code accionPendiente=COMPLETAR_ENRIQUECIMIENTO}, las acciones UI
 * {@code enviarANotificacion=true} y {@code anularActa=true} deben estar
 * habilitadas. Las acciones materiales ({@code cumplimientoMaterial},
 * {@code resolucionBloqueante}) permanecen deshabilitadas si no existe
 * resultado habilitante, incluso si hay bloqueantes pendientes.
 *
 * <p>Caso: {@code ACTA-0024} (tránsito, bloqueantes materiales activos,
 * sin resultado final).
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class EnriquecimientoMuestraEnviarANotificacionYAnularIT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0024";

    @Autowired
    private MockMvc mvc;

    @Test
    void enriquecimientoActivo_accionesUi_enviarYAnularHabilitados_materialesApagados()
            throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ACTAS_EN_ENRIQUECIMIENTO"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("EN_CURSO"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ACTIVA"))
                .andExpect(jsonPath("$.accionPendiente").value("COMPLETAR_ENRIQUECIMIENTO"))
                .andExpect(jsonPath("$.accionesUi.enviarANotificacion").value(true))
                .andExpect(jsonPath("$.accionesUi.anularActa").value(true))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(false))
                .andExpect(jsonPath("$.accionesUi.resolucionBloqueante").value(false))
                .andExpect(jsonPath("$.accionesUi.cierre").value(false))
                .andExpect(jsonPath("$.accionesUi.archivoReingreso").value(false));
    }
}
