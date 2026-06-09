package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;

/**
 * Solicitud de pago voluntario iniciada por el infractor desde el portal
 * ({@code POST /api/prototipo/infractor/actas/{codigoQr}/acciones/solicitar-pago-voluntario}).
 *
 * <p>Caso principal: ACTA-0024 en ACTAS_EN_ENRIQUECIMIENTO con
 * {@code situacionPago=SIN_PAGO}. Tras la accion:
 * <ul>
 *   <li>situacionPago != SIN_PAGO;</li>
 *   <li>evento PAGO_VOLUNTARIO_SOLICITADO registrado con canal PORTAL_INFRACTOR;</li>
 *   <li>resultadoFinal sigue SIN_RESULTADO_FINAL;</li>
 *   <li>bloqueantes materiales siguen presentes;</li>
 *   <li>puedeSolicitarPagoVoluntario=false (ya solicitado);</li>
 *   <li>acta no cerrable todavia.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class SolicitarPagoVoluntarioPortalIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0024";
    private static final String QR = "QR-ACTA-0024-DEMO";

    @Autowired
    private MockMvc mvc;

    @Test
    void solicitar_estadoInicial_puedeSolicitarPagoVoluntario_esTrue() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(true))
                .andExpect(jsonPath("$.puedePagar").value(false));
    }

    @Test
    void solicitar_devuelveActaInfractorActualizado_conSituacionPagoSolicitado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoQr").value(QR))
                .andExpect(jsonPath("$.actaId").doesNotExist())
                .andExpect(jsonPath("$.situacionPago").value(not("SIN_PAGO")))
                .andExpect(jsonPath("$.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false))
                .andExpect(jsonPath("$.puedePagar").value(false));
    }

    @Test
    void solicitar_registraEventoPagoVoluntarioSolicitadoConCanalPortal() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("PAGO_VOLUNTARIO_SOLICITADO")))
                .andExpect(jsonPath("$[*].descripcion", hasItem(containsString("PORTAL_INFRACTOR"))));
    }

    @Test
    void solicitar_actualizaSituacionPagoEnVistaAdmin() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value(not("SIN_PAGO")))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.estaCerrada").value(false))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));
    }

    @Test
    void solicitar_bloqueantesMaterilesSignuenPresentes() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre", hasItem("ENTREGA_DOCUMENTACION")))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre", hasItem("LIBERACION_RODADO")))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));
    }

    @Test
    void solicitar_noNoAfectaSituacionPagoCondena() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("NO_APLICA"));
    }

    @Test
    void solicitar_rechazadoSiYaFueSolicitado_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isConflict());
    }

    @Test
    void solicitar_rechazadoSiActaEnGestionExterna_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0017-DEMO/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isConflict());
    }

    @Test
    void solicitar_rechazadoSiActaArchivada_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0007-DEMO/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isConflict());
    }

    @Test
    void solicitar_rechazadoSiActaCerrada_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0008-DEMO/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isConflict());
    }

    /**
     * Caso B: después de solicitar desde portal, mensajeVisible indica que la solicitud
     * fue registrada y que Dirección evaluará. Portal no muestra acción de pago.
     */
    @Test
    void solicitar_desdePortal_mensajeVisibleIndicaEsperaEvaluacion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("SOLICITADO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").doesNotExist())
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false))
                .andExpect(jsonPath("$.puedePagar").value(false))
                .andExpect(jsonPath("$.mensajeVisible").value(containsString("Direcci")));
    }

    /**
     * Caso C: intento directo de informar pago sin monto fijado devuelve 409
     * con mensaje explicativo.
     */
    @Test
    void solicitar_desdePortal_intentarInformarPagoSinMonto_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-pago-informado"))
                .andExpect(status().isConflict());
    }

    /**
     * Caso D: después de solicitar desde portal, el estado admin debe conservar
     * accionPendiente=EVALUAR_PAGO_VOLUNTARIO, bloqueantes materiales y
     * resultadoFinal=SIN_RESULTADO_FINAL.
     */
    @Test
    void solicitar_desdePortal_estadoAdminInvariante() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.accionPendiente").value("EVALUAR_PAGO_VOLUNTARIO"))
                .andExpect(jsonPath("$.situacionPago").value("SOLICITADO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").doesNotExist())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("NO_APLICA"))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre", hasItem("ENTREGA_DOCUMENTACION")))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre", hasItem("LIBERACION_RODADO")));
    }

    /**
     * Demo admin: con monto fijado sí aparece acción INFORMAR en acciones disponibles.
     * Verifica que el flujo positivo (admin fija monto) habilita informar pago.
     */
    @Test
    void conMontoFijadoPorAdmin_accionInformarDisponible() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5000.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoPagoVoluntario").value(5000.00));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("SOLICITADO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(5000.00))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles", hasItem("INFORMAR")));

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-pago-informado"))
                .andExpect(status().isOk());
    }

    @Test
    void codigoQrInexistente_devuelve404() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-INEXISTENTE-9999-DEMO/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isNotFound());
    }

    @Test
    void responseCiudadano_noDevuelveActaIdInterno() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actaId").doesNotExist())
                .andExpect(jsonPath("$.acta").value("A-2026-0024"))
                .andExpect(jsonPath("$.codigoQr").value(QR));
    }
}
