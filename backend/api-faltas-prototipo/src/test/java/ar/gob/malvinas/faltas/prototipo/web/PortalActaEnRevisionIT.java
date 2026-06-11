package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Portal infractor — acta ingresada pero todavía no validada como
 * notificable (D1/D2, bandeja {@code ACTAS_EN_ENRIQUECIMIENTO}).
 *
 * <p>Reglas verificadas:
 * <ul>
 *   <li>el portal reconoce el número de acta;</li>
 *   <li>muestra exactamente el mensaje de revisión;</li>
 *   <li>no expone documentos notificables;</li>
 *   <li>no habilita pago voluntario, pago, apelación ni confirmación de
 *       notificación;</li>
 *   <li>no expone montos ni detalle operativo.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PortalActaEnRevisionIT {

    private static final String B = "/api/prototipo";

    private static final String MENSAJE_EN_REVISION =
            "El acta se encuentra en revisión. "
                    + "Será notificado cuando la documentación esté validada y disponible.";

    @Autowired
    private MockMvc mvc;

    @Test
    void actaEnEnriquecimiento_portalSoloMuestraMensajeDeRevision() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        // ACTA-0025: D1/D2, bandeja ACTAS_EN_ENRIQUECIMIENTO.
        mvc.perform(get(B + "/infractor/actas/QR-ACTA-0025-DEMO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acta").value("A-2026-0025"))
                .andExpect(jsonPath("$.estadoVisible").value("EN_REVISION"))
                .andExpect(jsonPath("$.mensajeVisible").value(MENSAJE_EN_REVISION))
                .andExpect(jsonPath("$.documentos", hasSize(0)))
                .andExpect(jsonPath("$.puedeConsultarEstado").value(true))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false))
                .andExpect(jsonPath("$.puedePagar").value(false))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false))
                .andExpect(jsonPath("$.puedeConfirmarVisualizacionNotificacion").value(false))
                .andExpect(jsonPath("$.montoPagoVoluntario").doesNotExist())
                .andExpect(jsonPath("$.montoCondena").doesNotExist())
                .andExpect(jsonPath("$.situacionPago").doesNotExist())
                .andExpect(jsonPath("$.resultadoFinal").doesNotExist())
                .andExpect(jsonPath("$.actaId").doesNotExist());
    }

    @Test
    void actaEnRevision_noPermiteAbrirDocumentos() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0025-DEMO/documentos/ACTA_FIRMADA/ver"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actaEnRevision_otroCasoEnriquecimiento_tambienMuestraMensaje() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        // ACTA-0024: también en ACTAS_EN_ENRIQUECIMIENTO al inicio.
        mvc.perform(get(B + "/infractor/actas/QR-ACTA-0024-DEMO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVisible").value("EN_REVISION"))
                .andExpect(jsonPath("$.mensajeVisible").value(MENSAJE_EN_REVISION))
                .andExpect(jsonPath("$.documentos", hasSize(0)))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false))
                .andExpect(jsonPath("$.puedePagar").value(false));
    }
}
