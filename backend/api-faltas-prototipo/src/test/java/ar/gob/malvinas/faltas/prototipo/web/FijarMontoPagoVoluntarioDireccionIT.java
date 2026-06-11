package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo: portal solicita pago voluntario → Dirección fija monto.
 *
 * <p>Caso principal ACTA-0016 (acta validada en PENDIENTE_ANALISIS,
 * situacionPago=SIN_PAGO al inicio; el portal no admite solicitar sobre
 * actas en revisión D1/D2):
 * <ol>
 *   <li>Portal solicita pago voluntario: situacionPago=SOLICITADO, monto=null.</li>
 *   <li>Dirección ve acción FIJAR_MONTO disponible; no ve INFORMAR.</li>
 *   <li>Dirección llama fijar-monto-pago-voluntario con monto=1000.</li>
 *   <li>montoPagoVoluntario=1000, accionPendiente limpio.</li>
 *   <li>Ahora aparece acción INFORMAR; ya no aparece FIJAR_MONTO.</li>
 *   <li>Portal puede informar pago.</li>
 * </ol>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class FijarMontoPagoVoluntarioDireccionIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0016";
    private static final String QR = "QR-ACTA-0016-DEMO";

    @Autowired
    private MockMvc mvc;

    /**
     * Caso A: después de solicitar desde portal, la vista admin muestra
     * acción FIJAR_MONTO (no INFORMAR) porque el monto es null.
     */
    @Test
    void desdePortal_despuesDeSolicitar_accionFijarMontoDisponible() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("SOLICITADO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").doesNotExist())
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles", hasItem("FIJAR_MONTO")))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles", not(hasItem("INFORMAR"))));
    }

    /**
     * Caso B: Dirección fija monto → 200 con monto persistido.
     */
    @Test
    void fijarMonto_devuelve200ConMontoFijado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":1000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoPagoVoluntario").value(1000));
    }

    /**
     * Caso B (cont.): después de fijar monto, el detalle admin refleja
     * montoPagoVoluntario=1000 y resultadoFinal invariante.
     */
    @Test
    void fijarMonto_detalleAdminActualizado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":1000}"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("SOLICITADO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(1000))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.estaCerrada").value(false))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));
    }

    /**
     * Caso B (cont.): después de fijar monto, la acción disponible pasa a
     * INFORMAR y desaparece FIJAR_MONTO.
     */
    @Test
    void fijarMonto_accionInformarDisponible_fijarMontoDesaparece() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":1000}"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles", hasItem("INFORMAR")))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles", not(hasItem("FIJAR_MONTO"))));
    }

    /**
     * Caso C: portal después de monto fijado muestra montoPagoVoluntario
     * y puedePagar=true.
     */
    @Test
    void fijarMonto_portalMuestraMontoYPuedePagar() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":1000}"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoPagoVoluntario").value(1000))
                .andExpect(jsonPath("$.puedePagar").value(true))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false));
    }

    /**
     * Caso C (cont.): portal puede informar pago después de que Dirección
     * fijó el monto.
     */
    @Test
    void fijarMonto_portalPuedeInformarPago() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":1000}"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-pago-informado"))
                .andExpect(status().isOk());
    }

    /**
     * Caso D: monto <= 0 devuelve 400.
     */
    @Test
    void fijarMonto_montoNegativo_devuelve400() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":-1}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Caso D (cont.): monto cero devuelve 400.
     */
    @Test
    void fijarMonto_montoCero_devuelve400() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":0}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Caso D (cont.): body ausente devuelve 400.
     */
    @Test
    void fijarMonto_sinBody_devuelve400() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Caso E: fijar monto en acta cerrada devuelve 409.
     */
    @Test
    void fijarMonto_actaCerrada_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/ACTA-0008/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":1000}"))
                .andExpect(status().isConflict());
    }

    /**
     * Caso E (cont.): fijar monto en acta archivada devuelve 409.
     */
    @Test
    void fijarMonto_actaArchivada_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/ACTA-0007/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":1000}"))
                .andExpect(status().isConflict());
    }

    /**
     * Caso E (cont.): fijar monto en acta en gestión externa devuelve 409.
     */
    @Test
    void fijarMonto_actaEnGestionExterna_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/ACTA-0017/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":1000}"))
                .andExpect(status().isConflict());
    }

    /**
     * Caso E (cont.): fijar monto cuando situacionPago=SIN_PAGO devuelve 409
     * (solo se puede fijar cuando hay solicitud pendiente).
     */
    @Test
    void fijarMonto_sinSolicitudPrevia_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":1000}"))
                .andExpect(status().isConflict());
    }

    /**
     * Caso F: informar pago sin monto fijado sigue devolviendo 409.
     */
    @Test
    void sinMontoFijado_informarPago_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-pago-informado"))
                .andExpect(status().isConflict());
    }

    /**
     * Verifica que el evento PAGO_VOLUNTARIO_MONTO_FIJADO se registra.
     */
    @Test
    void fijarMonto_registraEventoPagoVoluntarioMontoFijado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":1000}"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("PAGO_VOLUNTARIO_MONTO_FIJADO")))
                .andExpect(jsonPath("$[*].descripcion", hasItem(containsString("1000"))));
    }
}
