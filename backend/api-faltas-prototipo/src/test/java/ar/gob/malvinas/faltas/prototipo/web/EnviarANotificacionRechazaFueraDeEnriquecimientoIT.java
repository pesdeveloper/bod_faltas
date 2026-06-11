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
 * Regla de rechazo: {@code POST .../enviar-a-notificacion} responde 409
 * (Conflict) cuando el acta NO está en {@code ACTAS_EN_ENRIQUECIMIENTO}
 * activa. La bandeja no transiciona y el estado permanece igual.
 *
 * <p>Casos cubiertos:
 * <ul>
 *   <li>{@code ACTA-0004}: {@code PENDIENTE_NOTIFICACION}</li>
 *   <li>{@code ACTA-0006}: {@code PENDIENTE_ANALISIS}</li>
 *   <li>{@code ACTA-0007}: {@code ARCHIVO}</li>
 *   <li>{@code ACTA-0008}: {@code CERRADAS}</li>
 *   <li>{@code ACTA-0017}: {@code GESTION_EXTERNA}</li>
 * </ul>
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class EnviarANotificacionRechazaFueraDeEnriquecimientoIT {

    private static final String B = "/api/prototipo";

    @Autowired
    private MockMvc mvc;

    @Test
    void enviarANotificacion_enPendienteNotificacion_responde409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/ACTA-0004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"));

        mvc.perform(post(B + "/actas/ACTA-0004/acciones/enviar-a-notificacion"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/ACTA-0004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"));
    }

    @Test
    void enviarANotificacion_enPendienteAnalisis_responde409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/ACTA-0006"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        mvc.perform(post(B + "/actas/ACTA-0006/acciones/enviar-a-notificacion"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/ACTA-0006"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));
    }

    @Test
    void enviarANotificacion_enArchivo_responde409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/ACTA-0007"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"));

        mvc.perform(post(B + "/actas/ACTA-0007/acciones/enviar-a-notificacion"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/ACTA-0007"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"));
    }

    @Test
    void enviarANotificacion_enCerradas_responde409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/ACTA-0008"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));

        mvc.perform(post(B + "/actas/ACTA-0008/acciones/enviar-a-notificacion"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/ACTA-0008"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));
    }

    @Test
    void enviarANotificacion_enGestionExterna_responde409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/ACTA-0017"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("GESTION_EXTERNA"));

        mvc.perform(post(B + "/actas/ACTA-0017/acciones/enviar-a-notificacion"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/ACTA-0017"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("GESTION_EXTERNA"));
    }
}
