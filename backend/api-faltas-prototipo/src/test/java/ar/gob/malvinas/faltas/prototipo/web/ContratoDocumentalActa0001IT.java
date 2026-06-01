package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato documental ACTA-0001: pieza única NOTIFICACION_ACTA.
 *
 * <p>Recorrido completo: PENDIENTES_RESOLUCION_REDACCION →
 * {@code generar-notificacion-acta} → PENDIENTE_FIRMA →
 * {@code firmar-documento/DOC-0001-02} → PENDIENTE_NOTIFICACION.
 *
 * <p>Semilla: {@code MockDataFactory#cargarActa0001}.
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ContratoDocumentalActa0001IT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0001";
    /**
     * DOC-0001-01 es FOTO_INFRACCION (precargado). Tras generar-notificacion-acta
     * se agrega DOC-0001-02 (NOTIFICACION_ACTA).
     */
    private static final String DOC_NOTIFICACION_ACTA = "DOC-0001-02";

    @Autowired
    private MockMvc mvc;

    @Test
    void estadoInicial_bandejaRedaccion_piezaNotificacionRequerida_generadasVacio() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTES_RESOLUCION_REDACCION"))
                .andExpect(jsonPath("$.piezasRequeridas[0]").value("NOTIFICACION_ACTA"))
                .andExpect(jsonPath("$.piezasGeneradas.length()").value(0))
                .andExpect(jsonPath("$.tieneNotificaciones").value(false))
                .andExpect(jsonPath("$.estaCerrada").value(false));
    }

    @Test
    void generarNotificacionActa_pasaAPendienteFirmaConEstadoAgregador() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-notificacion-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_FIRMA_PIEZAS"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.piezasGeneradas", hasItem("NOTIFICACION_ACTA")));
    }

    @Test
    void documentoTrasGenerar_existeNotificacionActaPendienteFirma() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-notificacion-acta"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].id").value(DOC_NOTIFICACION_ACTA))
                .andExpect(jsonPath("$[1].tipoDocumento").value("NOTIFICACION_ACTA"))
                .andExpect(jsonPath("$[1].estadoDocumento").value("PENDIENTE_FIRMA"));
    }

    @Test
    void firmarNotificacionActa_pasaAPendienteNotificacion_noACerradas() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-notificacion-acta"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/" + DOC_NOTIFICACION_ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"))
                .andExpect(jsonPath("$.estaCerrada").value(false));
    }

    @Test
    void estadoFinal_notificable_documentoFirmado_notificacionPendienteSinResultado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTES_RESOLUCION_REDACCION"))
                .andExpect(jsonPath("$.piezasRequeridas[0]").value("NOTIFICACION_ACTA"))
                .andExpect(jsonPath("$.piezasGeneradas.length()").value(0));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-notificacion-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_FIRMA_PIEZAS"));

        mvc.perform(get(B + "/actas/" + ID + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].id").value(DOC_NOTIFICACION_ACTA))
                .andExpect(jsonPath("$[1].tipoDocumento").value("NOTIFICACION_ACTA"))
                .andExpect(jsonPath("$[1].estadoDocumento").value("PENDIENTE_FIRMA"));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/" + DOC_NOTIFICACION_ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"))
                .andExpect(jsonPath("$.tieneNotificaciones").value(true))
                .andExpect(jsonPath("$.estaCerrada").value(false))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));

        mvc.perform(get(B + "/actas/" + ID + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].tipoDocumento").value("NOTIFICACION_ACTA"))
                .andExpect(jsonPath("$[1].estadoDocumento").value("FIRMADO"));

        mvc.perform(get(B + "/actas/" + ID + "/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].resultado", hasItem("SIN_RESULTADO")));
    }
}
