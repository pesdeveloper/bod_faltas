package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Portal infractor — apertura del fallo condenatorio pendiente de
 * notificación ({@code POST
 * /infractor/actas/{codigoQr}/documentos/FALLO_CONDENATORIO/ver}).
 *
 * <p>Reglas verificadas (caso canónico ACTA-0030):
 * <ul>
 *   <li>la visualización registra notificación positiva por canal
 *       {@code PORTAL_INFRACTOR};</li>
 *   <li>{@code resultadoFinal} pasa a {@code CONDENADO};</li>
 *   <li>{@code situacionPagoCondena} queda {@code NO_APLICA};</li>
 *   <li>{@code montoCondena} se conserva como monto del fallo;</li>
 *   <li>se habilita la apelación (plazo abierto);</li>
 *   <li>no se habilita pago de condena ni pago voluntario.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PortalNotificaFalloAlVerDocumentoIT {

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
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0030-02"))
                .andExpect(status().isOk());
    }

    @Test
    void verFallo_pasaACondenadoConApelacionHabilitadaYSinPagoCondena() throws Exception {
        prepararFalloFirmadoPendienteNotificacion();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.montoCondena").value(1200))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(true))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false))
                .andExpect(jsonPath("$.puedePagar").value(false))
                .andExpect(jsonPath("$.documentos[0].tipo").value("FALLO_CONDENATORIO"))
                .andExpect(jsonPath("$.documentos[0].estadoNotificacion").value("NOTIFICADO"))
                .andExpect(jsonPath("$.documentos[0].notificado").value(true))
                .andExpect(jsonPath("$.actaId").doesNotExist());
    }

    @Test
    void verFallo_registraNotificacionPositivaPorCanalPortalInfractor() throws Exception {
        prepararFalloFirmadoPendienteNotificacion();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("NO_APLICA"))
                .andExpect(jsonPath("$.notificaciones[*].canalTipificado", hasItem("PORTAL_INFRACTOR")))
                .andExpect(jsonPath(
                        "$.notificaciones[?(@.canalTipificado == 'PORTAL_INFRACTOR')].tipo",
                        hasItem("FALLO_CONDENATORIO")))
                .andExpect(jsonPath(
                        "$.notificaciones[?(@.canalTipificado == 'PORTAL_INFRACTOR')].resultado",
                        hasItem("POSITIVA")));

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("FALLO_CONDENATORIO_NOTIFICADO")))
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("NOTIFICACION_PORTAL_VISUALIZADA")));
    }

    @Test
    void verFallo_expedienteSaleDePendienteNotificacion() throws Exception {
        prepararFalloFirmadoPendienteNotificacion();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_REVISION"));
    }

    @Test
    void verFallo_despuesPuedeApelarDesdePortal() throws Exception {
        prepararFalloFirmadoPendienteNotificacion();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/registrar-apelacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false));
    }

    @Test
    void verDocumentoInexistenteONoVisible_devuelve404() throws Exception {
        prepararFalloFirmadoPendienteNotificacion();

        // Documento interno (informe), nunca visible para el infractor.
        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/INFORME_ALCOHOTEST/ver"))
                .andExpect(status().isNotFound());

        // Tipo notificable pero sin documento en el expediente.
        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_ABSOLUTORIO/ver"))
                .andExpect(status().isNotFound());

        // QR inexistente.
        mvc.perform(post(B + "/infractor/actas/QR-ACTA-9999-DEMO/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isNotFound());
    }
}
