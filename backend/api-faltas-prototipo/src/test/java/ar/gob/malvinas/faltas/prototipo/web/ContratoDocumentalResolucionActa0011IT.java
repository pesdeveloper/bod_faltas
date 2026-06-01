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
 * Contrato documental ACTA-0011: pieza única RESOLUCION.
 *
 * <p>Recorrido completo: PENDIENTES_RESOLUCION_REDACCION →
 * {@code generar-resolucion} → PENDIENTE_FIRMA →
 * {@code firmar-documento/DOC-0011-02} → PENDIENTE_NOTIFICACION.
 *
 * <p>ACTA-0011 tiene pre-cargado DOC-0011-01 (ACTA_FIRMADA/FIRMADO) y
 * NOT-0011-01 (POSTAL/ENTREGADA). Al firmar la resolución, la acta pasa
 * a PENDIENTE_NOTIFICACION; dado que ya existe una notificación
 * ACTA_INFRACCION pre-cargada, no se crea una nueva.
 *
 * <p>Semilla: {@code MockDataFactory#cargarActa0011}.
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ContratoDocumentalResolucionActa0011IT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0011";
    /**
     * DOC-0011-01 es ACTA_FIRMADA (precargado). Tras generar-resolucion
     * se agrega DOC-0011-02 (RESOLUCION).
     */
    private static final String DOC_RESOLUCION = "DOC-0011-02";

    @Autowired
    private MockMvc mvc;

    @Test
    void estadoInicial_bandejaRedaccion_piezaResolucionRequerida_generadasVacio() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTES_RESOLUCION_REDACCION"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_RESOLUCION"))
                .andExpect(jsonPath("$.piezasRequeridas[0]").value("RESOLUCION"))
                .andExpect(jsonPath("$.piezasGeneradas.length()").value(0))
                .andExpect(jsonPath("$.estaCerrada").value(false));
    }

    @Test
    void documentoInicial_actaFirmadaFirmado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("DOC-0011-01"))
                .andExpect(jsonPath("$[0].tipoDocumento").value("ACTA_FIRMADA"))
                .andExpect(jsonPath("$[0].estadoDocumento").value("FIRMADO"));
    }

    @Test
    void notificacionInicial_postalEntregada() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID + "/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("NOT-0011-01"))
                .andExpect(jsonPath("$[0].canal").value("POSTAL"))
                .andExpect(jsonPath("$[0].estadoNotificacion").value("ENTREGADA"))
                .andExpect(jsonPath("$[0].resultado").value("POSITIVA"));
    }

    @Test
    void acta0011_aparece_en_bandeja_pendientesResolucionRedaccion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/bandejas/PENDIENTES_RESOLUCION_REDACCION/actas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(ID)));
    }

    @Test
    void generarResolucion_pasaAPendienteFirma() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-resolucion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_FIRMA_PIEZAS"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.piezasGeneradas", hasItem("RESOLUCION")));
    }

    @Test
    void documentosTrasGenerarResolucion_existeResolucionPendienteFirma() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-resolucion"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].id").value(DOC_RESOLUCION))
                .andExpect(jsonPath("$[1].tipoDocumento").value("RESOLUCION"))
                .andExpect(jsonPath("$[1].estadoDocumento").value("PENDIENTE_FIRMA"));
    }

    @Test
    void firmarResolucion_pasaAPendienteNotificacion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-resolucion"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/" + DOC_RESOLUCION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"))
                .andExpect(jsonPath("$.estaCerrada").value(false));
    }

    @Test
    void flujoCompleto_deRedaccionANotificacion_documentoResolucionFirmado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTES_RESOLUCION_REDACCION"))
                .andExpect(jsonPath("$.piezasRequeridas[0]").value("RESOLUCION"))
                .andExpect(jsonPath("$.piezasGeneradas.length()").value(0));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-resolucion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_FIRMA_PIEZAS"));

        mvc.perform(get(B + "/actas/" + ID + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].id").value(DOC_RESOLUCION))
                .andExpect(jsonPath("$[1].tipoDocumento").value("RESOLUCION"))
                .andExpect(jsonPath("$[1].estadoDocumento").value("PENDIENTE_FIRMA"));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/" + DOC_RESOLUCION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"))
                .andExpect(jsonPath("$.tieneNotificaciones").value(true))
                .andExpect(jsonPath("$.estaCerrada").value(false));

        mvc.perform(get(B + "/actas/" + ID + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].tipoDocumento").value("RESOLUCION"))
                .andExpect(jsonPath("$[1].estadoDocumento").value("FIRMADO"));
    }

    @Test
    void contrato_estadoCoherente_noCerrada_tieneNotificacion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTES_RESOLUCION_REDACCION"))
                .andExpect(jsonPath("$.piezasRequeridas[0]").value("RESOLUCION"))
                .andExpect(jsonPath("$.piezasGeneradas.length()").value(0))
                .andExpect(jsonPath("$.tieneNotificaciones").value(true))
                .andExpect(jsonPath("$.estaCerrada").value(false));

        mvc.perform(get(B + "/actas/" + ID + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoDocumento").value("ACTA_FIRMADA"))
                .andExpect(jsonPath("$[0].estadoDocumento").value("FIRMADO"));

        mvc.perform(get(B + "/actas/" + ID + "/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resultado").value("POSITIVA"));
    }
}
