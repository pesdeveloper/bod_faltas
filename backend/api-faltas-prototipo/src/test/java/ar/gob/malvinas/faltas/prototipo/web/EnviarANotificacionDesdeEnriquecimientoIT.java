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
 * Regla operativa: {@code POST /actas/{id}/acciones/enviar-a-notificacion}
 * mueve el acta desde {@code ACTAS_EN_ENRIQUECIMIENTO} a
 * {@code PENDIENTE_NOTIFICACION} con estado {@code PENDIENTE_ENVIO}.
 *
 * <p>Los bloqueantes materiales pendientes no se pierden y siguen
 * presentes. Las acciones {@code enviarANotificacion} y {@code anularActa}
 * quedan apagadas post-transición; {@code notificacion} debe habilitarse
 * (derivado del estado PENDIENTE_NOTIFICACION). Se registra evento
 * {@code ACTA_ENVIADA_A_NOTIFICACION}.
 *
 * <p>Caso: {@code ACTA-0024} (tránsito, dos bloqueantes materiales).
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class EnviarANotificacionDesdeEnriquecimientoIT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0024";

    @Autowired
    private MockMvc mvc;

    @Test
    void enviarANotificacion_desdeEnriquecimiento_transitaAPendienteNotificacion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ACTAS_EN_ENRIQUECIMIENTO"))
                .andExpect(jsonPath("$.accionesUi.enviarANotificacion").value(true));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/enviar-a-notificacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("OK"))
                .andExpect(jsonPath("$.actaId").value(ID))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_ENVIO"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_ENVIO"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ACTIVA"))
                .andExpect(jsonPath("$.accionPendiente").isEmpty())
                .andExpect(jsonPath("$.accionesUi.falloFondo").value(false))
                .andExpect(jsonPath("$.accionesUi.enviarANotificacion").value(false))
                .andExpect(jsonPath("$.accionesUi.anularActa").value(false));

        mvc.perform(get(B + "/actas/" + ID + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tipoEvento == 'ACTA_ENVIADA_A_NOTIFICACION')]").isNotEmpty());
    }

    @Test
    void enviarANotificacion_desdeEnriquecimiento_bloqueantesMaterialesSeConservan() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/enviar-a-notificacion"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"))
                .andExpect(jsonPath("$.hechosMateriales.ejes.length()").value(3))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(false))
                .andExpect(jsonPath("$.accionesUi.resolucionBloqueante").value(false));
    }

    @Test
    void notificacionPositiva_habilitaFalloSoloEnAnalisis() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/enviar-a-notificacion"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_ENVIO"))
                .andExpect(jsonPath("$.accionesUi.falloFondo").value(false));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.accionesUi.falloFondo").value(true))
                .andExpect(jsonPath("$.accionesUi.pagoVoluntario").value(true));
    }
}
