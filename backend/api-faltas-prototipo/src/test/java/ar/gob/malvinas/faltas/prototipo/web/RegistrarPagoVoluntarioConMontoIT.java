package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acción administrativa "Pago voluntario": Dirección de Faltas fija el
 * monto del acta y deja habilitado el pago voluntario.
 *
 * <p>Reglas fuertes verificadas:
 * <ul>
 *   <li>monto obligatorio y &gt; 0 (rechazo con 400);</li>
 *   <li>rechazo con 409 desde bandejas terminales/externas
 *       ({@code CERRADAS}, {@code ARCHIVO}, {@code GESTION_EXTERNA});</li>
 *   <li>persistencia del monto en el snapshot ({@code montoPagoVoluntario});</li>
 *   <li>NO se generan ni referencian comprobantes
 *       (sin EM, sin RC, sin Cmte/Pref/Nro): los comprobantes sólo se
 *       materializan en el proceso externo de pago (slice futuro).</li>
 * </ul>
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class RegistrarPagoVoluntarioConMontoIT {

    private static final String B = "/api/prototipo";
    private static final String BODY_MONTO_VALIDO = "{\"monto\":12345.67}";

    @Autowired
    private MockMvc mvc;

    @Test
    void desdePendienteAnalisisSinPago_conMontoValido_habilitaYPersisteMonto() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0016";

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").doesNotExist());

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_MONTO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.actaId").value(id))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(12345.67));

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.situacionPago").value("SOLICITADO"))
                .andExpect(jsonPath("$.accionPendiente").value("EVALUAR_PAGO_VOLUNTARIO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(12345.67))
                .andExpect(jsonPath("$.estaCerrada").value(false))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.pagoInformado").doesNotExist());
    }

    @Test
    void rechazaMontoCero_400() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0016";

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":0}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").doesNotExist());
    }

    @Test
    void rechazaMontoNegativo_400() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0016";

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":-1.0}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").doesNotExist());
    }

    @Test
    void rechazaMontoNull_400() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0016";

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":null}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").doesNotExist());
    }

    @Test
    void rechazaActaCerradas_409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0008";

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"))
                .andExpect(jsonPath("$.estaCerrada").value(true));

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_MONTO_VALIDO))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(jsonPath("$.montoPagoVoluntario").doesNotExist());
    }

    @Test
    void rechazaActaArchivo_409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0007";

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"));

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_MONTO_VALIDO))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(jsonPath("$.montoPagoVoluntario").doesNotExist());
    }

    @Test
    void rechazaActaGestionExterna_409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0017";

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(jsonPath("$.bandejaActual").value("GESTION_EXTERNA"));

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_MONTO_VALIDO))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(jsonPath("$.montoPagoVoluntario").doesNotExist());
    }

    @Test
    void flujoPosteriorInformarAdjuntarConfirmar_conservaMonto_y_noGeneraComprobantesReales() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0016";

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_MONTO_VALIDO))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-pago-informado"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(jsonPath("$.situacionPago").value("PAGO_INFORMADO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(12345.67))
                .andExpect(jsonPath("$.pagoInformado.comprobante").doesNotExist());

        mvc.perform(post(B + "/actas/" + id + "/acciones/adjuntar-comprobante-pago-informado")
                        .param("nombreArchivo", "comprobante_demo.pdf"))
                .andExpect(status().isOk());

        // El comprobante mock NO debe mapear a referencias reales tipo
        // EM-14-NNNNNNNN ni RC-14-NNNNNNNN (esa materialización se modela
        // luego, en el proceso externo de pago).
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(jsonPath("$.situacionPago").value("PENDIENTE_CONFIRMACION"))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(12345.67))
                .andExpect(jsonPath("$.pagoInformado.comprobante.id").value("COMP-" + id))
                .andExpect(jsonPath("$.pagoInformado.comprobante.id")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.startsWith("EM-"))))
                .andExpect(jsonPath("$.pagoInformado.comprobante.id")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.startsWith("RC-"))));

        mvc.perform(post(B + "/actas/" + id + "/acciones/confirmar-pago-informado"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(12345.67));

        // No se inventan campos Cmte/Pref/Nro en el snapshot.
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(jsonPath("$.cmte").doesNotExist())
                .andExpect(jsonPath("$.pref").doesNotExist())
                .andExpect(jsonPath("$.nro").doesNotExist())
                .andExpect(jsonPath("$.emisionDeuda").doesNotExist())
                .andExpect(jsonPath("$.recibo").doesNotExist());
    }
}
