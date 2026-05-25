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
 * Acceso ciudadano/infractor por código QR ({@code GET
 * /api/prototipo/infractor/actas/{codigoQr}}): el portal del infractor
 * consulta el acta usando un código opaco estable, no usando el
 * {@code actaId} interno.
 *
 * <p>Reglas fuertes verificadas:
 * <ul>
 *   <li>cada acta mock expone un código estable con formato opaco
 *       (no es el {@code actaId} crudo);</li>
 *   <li>404 si el código no coincide con ninguna acta;</li>
 *   <li>flags {@code puedeSolicitarPagoVoluntario} / {@code puedePagar}
 *       reflejan exactamente las condiciones del slice:
 *       sin monto y bandeja operable → solicitar; con monto, no confirmado
 *       y bandeja operable → pagar; pago confirmado → no pagar;</li>
 *   <li>NO se devuelven EM/RC/Cmte/Pref/Nro ni emisión de deuda ni recibo
 *       (la materialización de comprobantes es slice posterior);</li>
 *   <li>{@code puedePresentarApelacion} queda en {@code false} hasta
 *       implementar fallo/notificación de fallo/recurso.</li>
 * </ul>
 */
// El null-analysis de JDT marca falsos positivos sobre APIs externas de
// test como MockMvc y MediaType. Se mantiene la validación funcional intacta.
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class AccesoInfractorPorCodigoQrIT {

    private static final String B = "/api/prototipo";
    private static final String BODY_MONTO_VALIDO = "{\"monto\":12345.67}";

    @Autowired
    private MockMvc mvc;

    @Test
    void actaMockExponeCodigoQrEstable_yEndpointDevuelve200() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        // Código estable derivado deterministicamente del actaId, con
        // formato opaco (no es el actaId crudo).
        String codigoQr = "QR-ACTA-0003-DEMO";
        mvc.perform(get(B + "/infractor/actas/" + codigoQr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoQr").value(codigoQr))
                .andExpect(jsonPath("$.acta").value("A-2026-0003"))
                .andExpect(jsonPath("$.puedeConsultarEstado").value(true))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false));
    }

    @Test
    void codigoQrInexistente_devuelve404() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/infractor/actas/QR-ACTA-INEXISTENTE-DEMO"))
                .andExpect(status().isNotFound());

        mvc.perform(get(B + "/infractor/actas/QR-ACTA-0003"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actaSinMonto_enBandejaOperable_permiteSolicitarPero_noPagar() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        // ACTA-0016: PENDIENTE_ANALISIS, SIN_PAGO, sin monto fijado;
        // bandeja operable (no terminal ni externa).
        String codigoQr = "QR-ACTA-0016-DEMO";

        mvc.perform(get(B + "/infractor/actas/" + codigoQr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoQr").value(codigoQr))
                .andExpect(jsonPath("$.acta").value("A-2026-0016"))
                .andExpect(jsonPath("$.estadoVisible").value("EN_TRAMITE"))
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").doesNotExist())
                .andExpect(jsonPath("$.puedeConsultarEstado").value(true))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(true))
                .andExpect(jsonPath("$.puedePagar").value(false))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false));
    }

    @Test
    void actaConMontoFijado_noConfirmado_permitePagarPero_noSolicitar() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String actaId = "ACTA-0016";
        String codigoQr = "QR-" + actaId + "-DEMO";

        // Fija monto desde la acción administrativa (Dirección de Faltas).
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_MONTO_VALIDO))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/infractor/actas/" + codigoQr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVisible").value("EN_TRAMITE"))
                .andExpect(jsonPath("$.situacionPago").value("SOLICITADO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(12345.67))
                .andExpect(jsonPath("$.puedeConsultarEstado").value(true))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false))
                .andExpect(jsonPath("$.puedePagar").value(true))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false));
    }

    @Test
    void actaConPagoConfirmado_noPermitePagar() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String actaId = "ACTA-0016";
        String codigoQr = "QR-" + actaId + "-DEMO";

        // Recorre el circuito completo de pago voluntario hasta CONFIRMADO.
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_MONTO_VALIDO))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/registrar-pago-informado"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/adjuntar-comprobante-pago-informado")
                        .param("nombreArchivo", "comprobante_demo.pdf"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/confirmar-pago-informado"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/infractor/actas/" + codigoQr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"))
                .andExpect(jsonPath("$.resultadoFinal").value("PAGO_CONFIRMADO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(12345.67))
                .andExpect(jsonPath("$.puedeConsultarEstado").value(true))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false))
                .andExpect(jsonPath("$.puedePagar").value(false));
    }

    @Test
    void actaEnBandejaTerminal_noPermiteSolicitarNiPagar() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        // ACTA-0008: CERRADAS, estaCerrada = true.
        mvc.perform(get(B + "/infractor/actas/QR-ACTA-0008-DEMO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVisible").value("CERRADA"))
                .andExpect(jsonPath("$.puedeConsultarEstado").value(true))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false))
                .andExpect(jsonPath("$.puedePagar").value(false));

        // ACTA-0007: ARCHIVO.
        mvc.perform(get(B + "/infractor/actas/QR-ACTA-0007-DEMO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVisible").value("ARCHIVADA"))
                .andExpect(jsonPath("$.puedeConsultarEstado").value(true))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false))
                .andExpect(jsonPath("$.puedePagar").value(false));

        // ACTA-0017: GESTION_EXTERNA.
        mvc.perform(get(B + "/infractor/actas/QR-ACTA-0017-DEMO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVisible").value("EN_GESTION_EXTERNA"))
                .andExpect(jsonPath("$.puedeConsultarEstado").value(true))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false))
                .andExpect(jsonPath("$.puedePagar").value(false));
    }

    @Test
    void responseNoContiene_EM_RC_Cmte_Pref_Nro_emisionDeuda_niRecibo() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String actaId = "ACTA-0016";
        String codigoQr = "QR-" + actaId + "-DEMO";

        // Recorrido completo de pago voluntario para asegurar que aun
        // habiendo monto + situación CONFIRMADO + cualquier marca interna,
        // el response ciudadano no inventa ni filtra comprobantes.
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_MONTO_VALIDO))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/registrar-pago-informado"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/adjuntar-comprobante-pago-informado")
                        .param("nombreArchivo", "comprobante_demo.pdf"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/confirmar-pago-informado"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/infractor/actas/" + codigoQr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.em").doesNotExist())
                .andExpect(jsonPath("$.EM").doesNotExist())
                .andExpect(jsonPath("$.rc").doesNotExist())
                .andExpect(jsonPath("$.RC").doesNotExist())
                .andExpect(jsonPath("$.cmte").doesNotExist())
                .andExpect(jsonPath("$.pref").doesNotExist())
                .andExpect(jsonPath("$.nro").doesNotExist())
                .andExpect(jsonPath("$.emisionDeuda").doesNotExist())
                .andExpect(jsonPath("$.recibo").doesNotExist())
                .andExpect(jsonPath("$.comprobante").doesNotExist())
                .andExpect(jsonPath("$.pagoInformado").doesNotExist());
    }
}
