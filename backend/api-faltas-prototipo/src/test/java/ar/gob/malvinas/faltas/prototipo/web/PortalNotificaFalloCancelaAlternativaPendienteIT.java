package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Portal infractor — notificación alternativa superada por portal.
 *
 * <p>Caso canónico ACTA-0030: al firmar el fallo condenatorio se prepara
 * una notificación postal pendiente (NOT-0030-03, CORREO_POSTAL /
 * SIN_RESULTADO). Si luego el infractor abre el fallo desde el portal:
 * <ul>
 *   <li>se registra la notificación positiva PORTAL_INFRACTOR;</li>
 *   <li>la notificación postal pendiente de la misma pieza queda
 *       formalmente {@code SUPERADA_POR_PORTAL} / {@code SIN_EFECTO}
 *       (no fallida: superada por notificación positiva de portal);</li>
 *   <li>no queda ninguna notificación pendiente operativa por esa pieza
 *       (sale del circuito de correo postal);</li>
 *   <li>{@code resultadoFinal=CONDENADO} y
 *       {@code situacionPagoCondena=NO_APLICA};</li>
 *   <li>queda evento de auditoría {@code NOTIFICACION_SUPERADA_POR_PORTAL}.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PortalNotificaFalloCancelaAlternativaPendienteIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";
    private static final String QR = "QR-ACTA-0030-DEMO";
    private static final String NOTIF_POSTAL = "NOT-0030-03";

    @Autowired
    private MockMvc mvc;

    private void prepararFalloFirmadoConPostalPendiente() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":1200}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0030-02"))
                .andExpect(status().isOk());

        // Precondición: notificación alternativa postal pendiente de la pieza fallo.
        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.notificaciones[?(@.id == '" + NOTIF_POSTAL + "')].canalTipificado",
                        hasItem("CORREO_POSTAL")))
                .andExpect(jsonPath(
                        "$.notificaciones[?(@.id == '" + NOTIF_POSTAL + "')].tipo",
                        hasItem("FALLO_CONDENATORIO")))
                .andExpect(jsonPath(
                        "$.notificaciones[?(@.id == '" + NOTIF_POSTAL + "')].resultado",
                        hasItem("SIN_RESULTADO")));
    }

    @Test
    void verFallo_marcaAlternativaPostalComoSuperadaPorPortal() throws Exception {
        prepararFalloFirmadoConPostalPendiente();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENADO"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("NO_APLICA"))
                // Notificación portal positiva registrada.
                .andExpect(jsonPath(
                        "$.notificaciones[?(@.canalTipificado == 'PORTAL_INFRACTOR')].tipo",
                        hasItem("FALLO_CONDENATORIO")))
                .andExpect(jsonPath(
                        "$.notificaciones[?(@.canalTipificado == 'PORTAL_INFRACTOR')].resultado",
                        hasItem("POSITIVA")))
                // La postal de la misma pieza queda superada, no fallida.
                .andExpect(jsonPath(
                        "$.notificaciones[?(@.id == '" + NOTIF_POSTAL + "')].resultado",
                        hasItem("SUPERADA_POR_PORTAL")))
                .andExpect(jsonPath(
                        "$.notificaciones[?(@.id == '" + NOTIF_POSTAL + "')].estado",
                        hasItem("SIN_EFECTO")))
                // No queda ninguna notificación pendiente por la pieza fallo.
                .andExpect(jsonPath(
                        "$.notificaciones[?(@.tipo == 'FALLO_CONDENATORIO' && @.resultado == 'SIN_RESULTADO')]",
                        empty()));
    }

    @Test
    void verFallo_registraEventoDeAuditoriaDeSuperacion() throws Exception {
        prepararFalloFirmadoConPostalPendiente();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("FALLO_CONDENATORIO_NOTIFICADO")))
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("NOTIFICACION_PORTAL_VISUALIZADA")))
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("NOTIFICACION_SUPERADA_POR_PORTAL")));
    }

    @Test
    void verFallo_alternativaSuperadaSaleDelCircuitoDeCorreoPostal() throws Exception {
        prepararFalloFirmadoConPostalPendiente();

        // Antes de abrir desde portal: la postal pendiente es candidata a lote.
        mvc.perform(get(B + "/notificaciones/correo/listas-para-lote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.notificacionId == '" + NOTIF_POSTAL + "')]", hasSize(1)));

        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());

        // Después: la notificación superada ya no aparece como pendiente operativa.
        mvc.perform(get(B + "/notificaciones/correo/listas-para-lote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.notificacionId == '" + NOTIF_POSTAL + "')]", empty()));
    }

    @Test
    void verFallo_segundaApertura_sigueIdempotenteYNoTocaLaSuperada() throws Exception {
        prepararFalloFirmadoConPostalPendiente();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                // Una sola notificación portal positiva (sin duplicar).
                .andExpect(jsonPath(
                        "$.notificaciones[?(@.canalTipificado == 'PORTAL_INFRACTOR')]",
                        hasSize(1)))
                .andExpect(jsonPath(
                        "$.notificaciones[?(@.id == '" + NOTIF_POSTAL + "')].resultado",
                        hasItem("SUPERADA_POR_PORTAL")));
    }
}
