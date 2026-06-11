package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Portal infractor — caso canónico ACTA-0030 con
 * {@code FALLO_CONDENATORIO:FIRMADO} pendiente de notificación
 * (bandeja {@code PENDIENTE_NOTIFICACION}, estado {@code PENDIENTE_ENVIO}).
 *
 * <p>Reglas verificadas (antes de abrir el fallo):
 * <ul>
 *   <li>el portal NO ofrece "Solicitar pago voluntario" ni "Pagar";</li>
 *   <li>el portal muestra el fallo condenatorio como documento visible y
 *       notificable, pendiente de notificación;</li>
 *   <li>{@code resultadoFinal} sigue {@code SIN_RESULTADO_FINAL} y
 *       {@code montoCondena} se conserva como monto del fallo (no deuda
 *       firme);</li>
 *   <li>el mensaje visible indica documentación pendiente de
 *       notificación.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PortalFalloFirmadoPendienteNotificacionIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";
    private static final String QR = "QR-ACTA-0030-DEMO";

    @Autowired
    private MockMvc mvc;

    private void prepararFalloFirmadoPendienteNotificacion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":1200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentoId").value("DOC-0030-02"));
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0030-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"));
    }

    @Test
    void falloFirmadoPendiente_portalNoOfrecePagoVoluntarioNiPago() throws Exception {
        prepararFalloFirmadoPendienteNotificacion();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acta").value("A-2026-0030"))
                .andExpect(jsonPath("$.estadoVisible").value("EN_TRAMITE"))
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false))
                .andExpect(jsonPath("$.puedePagar").value(false))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false))
                .andExpect(jsonPath("$.mensajeVisible", containsString("pendiente de notificación")));
    }

    @Test
    void falloFirmadoPendiente_portalMuestraDocumentoVisibleNotificable() throws Exception {
        prepararFalloFirmadoPendienteNotificacion();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentos", hasSize(1)))
                .andExpect(jsonPath("$.documentos[0].tipo").value("FALLO_CONDENATORIO"))
                .andExpect(jsonPath("$.documentos[0].titulo").value("Fallo condenatorio"))
                .andExpect(jsonPath("$.documentos[0].estadoDocumento").value("FIRMADO"))
                .andExpect(jsonPath("$.documentos[0].estadoNotificacion").value("PENDIENTE_NOTIFICACION"))
                .andExpect(jsonPath("$.documentos[0].visible").value(true))
                .andExpect(jsonPath("$.documentos[0].notificable").value(true))
                .andExpect(jsonPath("$.documentos[0].notificado").value(false))
                .andExpect(jsonPath("$.documentos[0].puedeAbrir").value(true));
    }

    @Test
    void falloFirmadoPendiente_montoCondenaEsMontoDelFalloNoDeudaFirme() throws Exception {
        prepararFalloFirmadoPendienteNotificacion();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoCondena").value(1200))
                .andExpect(jsonPath("$.resultadoFinal").value("SIN_RESULTADO_FINAL"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_ENVIO"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("NO_APLICA"));
    }
}
