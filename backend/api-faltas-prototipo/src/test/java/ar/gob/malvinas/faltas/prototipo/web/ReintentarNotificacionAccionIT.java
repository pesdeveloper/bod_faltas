package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recorrido demo de la acción "reintentar notificación" sobre ACTA-0005.
 * Verifica que el endpoint
 * {@code POST /api/prototipo/actas/{id}/acciones/reintentar-notificacion}
 * acepta dos marcas operativas como precondición:
 * <ol>
 *   <li>{@code accionPendiente = REINTENTAR_NOTIFICACION} (notificación
 *       no entregada): registra evento {@code NOTIFICACION_REINTENTADA}
 *       y devuelve el acta a {@code PENDIENTE_NOTIFICACION}.</li>
 *   <li>{@code accionPendiente = EVALUAR_NOTIFICACION_VENCIDA}
 *       (notificación vencida por plazo): registra evento
 *       {@code NOTIFICACION_REINTENTADA_POST_VENCIMIENTO} y devuelve el
 *       acta a {@code PENDIENTE_NOTIFICACION}.</li>
 * </ol>
 * En ambos casos limpia {@code accionPendiente} y no toca pago ni
 * resultado final. Si la precondición no se cumple (otra bandeja u otra
 * marca operativa), la acción responde 409.
 */
// El null-analysis de JDT marca falsos positivos sobre APIs externas de test como MockMvc y Hamcrest.
// Se mantiene la validación funcional intacta.
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ReintentarNotificacionAccionIT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0005";

    @Autowired
    private MockMvc mvc;

    @Test
    void reintentarNotificacion_desdePendienteAnalisisConMarca_vuelveAPendienteNotificacion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("EN_NOTIFICACION"));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-notificacion-negativa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.accionPendiente").value("REINTENTAR_NOTIFICACION"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.accionPendiente").value("REINTENTAR_NOTIFICACION"))
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/reintentar-notificacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actaId").value(ID))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_ENVIO"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_ENVIO"))
                .andExpect(jsonPath("$.accionPendiente").value(nullValue()))
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"));

        mvc.perform(get(B + "/actas/" + ID + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("NOTIFICACION_NO_ENTREGADA")))
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("NOTIFICACION_REINTENTADA")));
    }

    /**
     * Caso simétrico al reintento por no entrega: si la acta volvió a
     * análisis por vencimiento de plazo
     * ({@code accionPendiente = EVALUAR_NOTIFICACION_VENCIDA}), el mismo
     * endpoint {@code /acciones/reintentar-notificacion} debe aceptar la
     * acción y devolver el caso al circuito de notificación. El evento
     * más específico {@code NOTIFICACION_REINTENTADA_POST_VENCIMIENTO}
     * preserva la distinción del origen en la timeline.
     */
    @Test
    void reintentarNotificacion_desdePendienteAnalisisConNotificacionVencida_vuelveAPendienteNotificacion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("EN_NOTIFICACION"));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-notificacion-vencida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.accionPendiente").value("EVALUAR_NOTIFICACION_VENCIDA"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.accionPendiente").value("EVALUAR_NOTIFICACION_VENCIDA"))
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/reintentar-notificacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actaId").value(ID))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_ENVIO"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_ENVIO"))
                .andExpect(jsonPath("$.accionPendiente").value(nullValue()))
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"));

        mvc.perform(get(B + "/actas/" + ID + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("NOTIFICACION_VENCIDA")))
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("NOTIFICACION_REINTENTADA_POST_VENCIMIENTO")));
    }

    @Test
    void reintentarNotificacion_desdeEnNotificacionSinMarca_respondeConflict() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("EN_NOTIFICACION"));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/reintentar-notificacion"))
                .andExpect(status().isConflict());
    }
}
