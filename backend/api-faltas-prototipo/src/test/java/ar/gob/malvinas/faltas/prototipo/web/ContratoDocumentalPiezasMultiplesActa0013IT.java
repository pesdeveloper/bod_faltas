package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato documental ACTA-0013: dos piezas requeridas (NOTIFICACION_ACTA y
 * MEDIDA_PREVENTIVA). El acta permanece en PENDIENTES_RESOLUCION_REDACCION
 * hasta que ambas piezas estén producidas.
 *
 * <p>Recorrido completo: PENDIENTES_RESOLUCION_REDACCION →
 * {@code generar-medida-preventiva} (permanece en redacción) →
 * {@code generar-notificacion-acta} → PENDIENTE_FIRMA →
 * firmar DOC-0013-02 (queda pendiente DOC-0013-03) →
 * firmar DOC-0013-03 → PENDIENTE_NOTIFICACION.
 *
 * <p>Semilla: {@code MockDataFactory#cargarActa0013}.
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ContratoDocumentalPiezasMultiplesActa0013IT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0013";
    /**
     * DOC-0013-01 es ACTA_RETENCION (precargado). Tras generar-medida-preventiva
     * se agrega DOC-0013-02 (MEDIDA_PREVENTIVA), y tras generar-notificacion-acta
     * se agrega DOC-0013-03 (NOTIFICACION_ACTA).
     */
    private static final String DOC_MEDIDA_PREVENTIVA = "DOC-0013-02";
    private static final String DOC_NOTIFICACION_ACTA = "DOC-0013-03";

    @Autowired
    private MockMvc mvc;

    @Test
    void estadoInicial_dosPiezasRequeridas_generadasVacio_bandejaRedaccion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTES_RESOLUCION_REDACCION"))
                .andExpect(jsonPath("$.piezasRequeridas",
                        containsInAnyOrder("NOTIFICACION_ACTA", "MEDIDA_PREVENTIVA")))
                .andExpect(jsonPath("$.piezasGeneradas.length()").value(0));
    }

    @Test
    void generarMedidaPreventiva_permaneceSiAunFaltaNotificacion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-medida-preventiva"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTES_RESOLUCION_REDACCION"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTES_RESOLUCION_REDACCION"))
                .andExpect(jsonPath("$.piezasGeneradas", hasItem("MEDIDA_PREVENTIVA")));
    }

    @Test
    void generarAmbas_pasaAPendienteFirmaConDocsEsperados() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-medida-preventiva"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-notificacion-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_FIRMA_PIEZAS"));

        mvc.perform(get(B + "/actas/" + ID + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].id").value(DOC_MEDIDA_PREVENTIVA))
                .andExpect(jsonPath("$[1].tipoDocumento").value("MEDIDA_PREVENTIVA"))
                .andExpect(jsonPath("$[1].estadoDocumento").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$[2].id").value(DOC_NOTIFICACION_ACTA))
                .andExpect(jsonPath("$[2].tipoDocumento").value("NOTIFICACION_ACTA"))
                .andExpect(jsonPath("$[2].estadoDocumento").value("PENDIENTE_FIRMA"));
    }

    @Test
    void firmarPrimerDoc_permanecePendienteFirmaHastaFirmarSegundo() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        generarAmbas();

        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/" + DOC_MEDIDA_PREVENTIVA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"));
    }

    @Test
    void flujoCompleto_dosPiezas_terminaEnPendienteNotificacionConNotifPendiente() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTES_RESOLUCION_REDACCION"))
                .andExpect(jsonPath("$.piezasRequeridas",
                        containsInAnyOrder("NOTIFICACION_ACTA", "MEDIDA_PREVENTIVA")))
                .andExpect(jsonPath("$.piezasGeneradas.length()").value(0));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-medida-preventiva"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTES_RESOLUCION_REDACCION"));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-notificacion-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_FIRMA_PIEZAS"));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/" + DOC_MEDIDA_PREVENTIVA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/" + DOC_NOTIFICACION_ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"));

        mvc.perform(get(B + "/actas/" + ID + "/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].resultado", hasItem("SIN_RESULTADO")));
    }

    private void generarAmbas() throws Exception {
        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-medida-preventiva"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-notificacion-acta"))
                .andExpect(status().isOk());
    }
}
