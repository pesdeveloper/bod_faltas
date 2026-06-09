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
 * Tests del flujo de pago voluntario desde el portal infractor:
 * POST /api/prototipo/infractor/actas/{codigoQr}/acciones/pagar-voluntario.
 *
 * <p>Criterio de aceptacion principal (ACTA-0024):
 * <ol>
 *   <li>Portal solicita pago voluntario.</li>
 *   <li>Direccion fija monto 5000.</li>
 *   <li>Portal muestra boton Pagar (puedePagar=true).</li>
 *   <li>Portal presiona Pagar.</li>
 *   <li>Portal ya no muestra boton Pagar (puedePagar=false).</li>
 *   <li>Portal muestra "Pago en proceso de acreditacion."</li>
 *   <li>Direccion muestra confirmar/observar pago.</li>
 *   <li>Direccion no puede archivar mientras el pago esta en proceso.</li>
 *   <li>Direccion confirma pago.</li>
 *   <li>resultadoFinal=PAGO_CONFIRMADO.</li>
 *   <li>Como siguen bloqueantes materiales, cierre=false.</li>
 * </ol>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class InformarPagoVoluntarioPortalIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0024";
    private static final String QR = "QR-ACTA-0024-DEMO";

    @Autowired
    private MockMvc mvc;

    // -------------------------------------------------------------------------
    // Caso A: pagar sin monto fijado devuelve 409
    // -------------------------------------------------------------------------

    @Test
    void casoA_pagarSinMonto_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        // Infractor solicita pago voluntario (sin monto)
        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("SOLICITADO"));

        // Intento de pagar sin monto fijado
        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isConflict());
    }

    @Test
    void casoA_pagarSinMonto_estadoQuedaSolicitado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("SOLICITADO"))
                .andExpect(jsonPath("$.puedePagar").value(false));
    }

    // -------------------------------------------------------------------------
    // Caso B: pagar con monto fijado
    // -------------------------------------------------------------------------

    @Test
    void casoB_pagarConMontoFijado_devuelve200() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoQr").value(QR))
                .andExpect(jsonPath("$.actaId").doesNotExist())
                .andExpect(jsonPath("$.puedePagar").value(false))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false));
    }

    @Test
    void casoB_pagarConMontoFijado_situacionPagoEsPendienteConfirmacion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("PENDIENTE_CONFIRMACION"));
    }

    @Test
    void casoB_portalMuestraMensajePagoEnProceso() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("PENDIENTE_CONFIRMACION"))
                .andExpect(jsonPath("$.puedePagar").value(false))
                .andExpect(jsonPath("$.mensajeVisible").value(containsString("proceso de acreditaci")));
    }

    @Test
    void casoB_adminMuestraConfirmarYObservar() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles", hasItem("CONFIRMAR")))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles", hasItem("OBSERVAR")))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles", not(hasItem("INFORMAR"))))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles", not(hasItem("FIJAR_MONTO"))))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("NO_APLICA"));
    }

    @Test
    void casoB_archivoBloqueadoMientrasPagoEnProceso() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isOk());

        // El archivo debe ser rechazado mientras el pago está en proceso de acreditación
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/archivar-acta"))
                .andExpect(status().isConflict());
    }

    @Test
    void casoB_bloqueantesMaterialesSignuenPresentes() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre", hasItem("ENTREGA_DOCUMENTACION")))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre", hasItem("LIBERACION_RODADO")))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));
    }

    @Test
    void casoB_eventoPortalRegistradoConPortalInfractor() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("PAGO_VOLUNTARIO_INFORMADO_PORTAL")))
                .andExpect(jsonPath("$[*].descripcion", hasItem(containsString("PORTAL_INFRACTOR"))));
    }

    // -------------------------------------------------------------------------
    // Caso C: duplicado devuelve 409
    // -------------------------------------------------------------------------

    @Test
    void casoC_pagarDosVeces_segundaLlamadaDevuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isConflict());
    }

    // -------------------------------------------------------------------------
    // Caso D: confirmar desde Dirección
    // -------------------------------------------------------------------------

    @Test
    void casoD_confirmarDesdeDireccion_resultadoFinalPagoConfirmado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-informado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("PAGO_CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre", hasItem("ENTREGA_DOCUMENTACION")))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre", hasItem("LIBERACION_RODADO")));
    }

    @Test
    void casoD_confirmarDesdeDireccion_portalMuestraPagoConfirmado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-informado"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"))
                .andExpect(jsonPath("$.puedePagar").value(false))
                .andExpect(jsonPath("$.mensajeVisible").value(containsString("confirmado")));
    }

    // -------------------------------------------------------------------------
    // Caso E: rechazos para actas en estados terminales
    // -------------------------------------------------------------------------

    @Test
    void casoE_actaArchivada_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0007-DEMO/acciones/pagar-voluntario"))
                .andExpect(status().isConflict());
    }

    @Test
    void casoE_actaCerrada_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0008-DEMO/acciones/pagar-voluntario"))
                .andExpect(status().isConflict());
    }

    @Test
    void casoE_actaEnGestionExterna_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0017-DEMO/acciones/pagar-voluntario"))
                .andExpect(status().isConflict());
    }

    @Test
    void casoE_qrInexistente_devuelve404() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-INEXISTENTE-9999-DEMO/acciones/pagar-voluntario"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Tests adicionales: confirmación externa
    // -------------------------------------------------------------------------

    @Test
    void confirmarExterno_desdePortalInformado_devuelve200() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-voluntario-externo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origen\":\"PORTAL_INFRACTOR\",\"monto\":5000.00,\"referenciaPago\":\"REF-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"));
    }

    @Test
    void confirmarExterno_resultadoFinalPagoConfirmado_bloqueantesSignuenPresentes() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-voluntario-externo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origen\":\"PORTAL_INFRACTOR\",\"monto\":5000.00}"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("PAGO_CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre", hasItem("ENTREGA_DOCUMENTACION")))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre", hasItem("LIBERACION_RODADO")));
    }

    @Test
    void confirmarExterno_eventoExternoRegistrado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-voluntario-externo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origen\":\"GATEWAY_PAGO\",\"monto\":5000.00}"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("PAGO_VOLUNTARIO_CONFIRMADO_EXTERNO")));
    }

    @Test
    void confirmarExterno_sinPagoIniciado_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());
        // No se llama a pagar-voluntario -> no hay pago iniciado

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-voluntario-externo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origen\":\"PORTAL_INFRACTOR\",\"monto\":5000.00}"))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmarExterno_sinMontoFijado_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-voluntario-externo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origen\":\"PORTAL_INFRACTOR\",\"monto\":5000.00}"))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmarExterno_conMontoDiferente_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isOk());

        // Monto diferente al fijado
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-voluntario-externo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origen\":\"PORTAL_INFRACTOR\",\"monto\":3000.00}"))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmarExterno_duplicado_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-voluntario-externo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origen\":\"PORTAL_INFRACTOR\",\"monto\":5000.00}"))
                .andExpect(status().isOk());

        // Segunda confirmación: duplicado
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-voluntario-externo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origen\":\"PORTAL_INFRACTOR\",\"monto\":5000.00}"))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmarExterno_actaCerrada_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/ACTA-0008/acciones/confirmar-pago-voluntario-externo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origen\":\"PORTAL_INFRACTOR\",\"monto\":5000.00}"))
                .andExpect(status().isConflict());
    }

    // -------------------------------------------------------------------------
    // Validación: respuesta ciudadana no expone actaId interno
    // -------------------------------------------------------------------------

    @Test
    void responseCiudadano_noExponeActaIdInterno() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-voluntario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actaId").doesNotExist())
                .andExpect(jsonPath("$.codigoQr").value(QR));
    }
}
