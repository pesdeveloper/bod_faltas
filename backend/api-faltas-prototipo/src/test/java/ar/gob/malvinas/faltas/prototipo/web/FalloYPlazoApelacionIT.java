package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Circuito jurídico mínimo: fallo (absolutorio o condenatorio), firma del
 * fallo reutilizando {@code firmar-documento/{documentoId}}, notificación
 * positiva del fallo reutilizando {@code registrar-notificacion-positiva}
 * (detectada como notificación de fallo cuando hay fallo firmado pendiente
 * de notificación), y vencimiento mock del plazo de apelación.
 *
 * <p>Reglas fuertes verificadas:
 * <ul>
 *   <li>fallo absolutorio notificado → {@code resultadoFinal ABSUELTO},
 *       cerrabilidad {@code cerrable true}, portal/infractor
 *       {@code puedePresentarApelacion false};</li>
 *   <li>fallo condenatorio notificado → {@code resultadoFinal CONDENADO},
 *       plazo de apelación abierto, portal/infractor
 *       {@code puedePresentarApelacion true};</li>
 *   <li>vencimiento del plazo sin apelación → {@code CONDENA_FIRME},
 *       portal/infractor {@code puedePresentarApelacion false};</li>
 *   <li>dictar fallo se rechaza con 409 si hay bloqueantes materiales
 *       pendientes o si el acta no está en {@code PENDIENTE_ANALISIS}
 *       (incluyendo {@code CERRADAS}, {@code ARCHIVO},
 *       {@code GESTION_EXTERNA});</li>
 *   <li>NO se genera ni se devuelve EM/RC/Cmte/Pref/Nro ni emisión de
 *       deuda ni recibo (alcance estricto del slice).</li>
 * </ul>
 */
// El null-analysis de JDT marca falsos positivos sobre APIs externas de
// test como MockMvc. Se mantiene la validación funcional intacta.
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class FalloYPlazoApelacionIT {

    private static final String B = "/api/prototipo";
    /** Acta sin fallo previo, en PENDIENTE_ANALISIS y sin bloqueantes materiales. */
    private static final String ACTA_ANALISIS_LIMPIA = "ACTA-0006";
    private static final String MONTO_CONDENA_VALIDO_JSON = "{\"montoCondena\":12345.67}";

    @Autowired
    private MockMvc mvc;

    private static RequestBuilder postDictarFalloCondenatorio(String actaId) {
        return post(B + "/actas/" + actaId + "/acciones/dictar-fallo-condenatorio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MONTO_CONDENA_VALIDO_JSON);
    }

    private static RequestBuilder postDictarFalloCondenatorio(String actaId, String bodyJson) {
        return post(B + "/actas/" + actaId + "/acciones/dictar-fallo-condenatorio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyJson);
    }

    @Test
    void dictarFalloAbsolutorio_desdeAnalisisValido_generaDocPendienteFirma() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/dictar-fallo-absolutorio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoDocumento").value("FALLO_ABSOLUTORIO"))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_FIRMA"));

        mvc.perform(get(B + "/actas/" + ACTA_ANALISIS_LIMPIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"));
    }

    @Test
    void firmarYNotificarFalloAbsolutorio_dejaResultadoAbsueltoCerrableYsinApelacion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/dictar-fallo-absolutorio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentoId").value("DOC-0006-02"));

        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/firmar-documento/DOC-0006-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_ENVIO"));

        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        mvc.perform(get(B + "/actas/" + ACTA_ANALISIS_LIMPIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("ABSUELTO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true));

        String codigoQr = "QR-" + ACTA_ANALISIS_LIMPIA + "-DEMO";
        mvc.perform(get(B + "/infractor/actas/" + codigoQr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("ABSUELTO"))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false))
                .andExpect(jsonPath("$.puedePagar").value(false));
    }

    @Test
    void dictarFalloCondenatorio_desdeAnalisisValido_generaDocPendienteFirma() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(postDictarFalloCondenatorio(ACTA_ANALISIS_LIMPIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoDocumento").value("FALLO_CONDENATORIO"))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.montoCondena").value(12345.67));

        mvc.perform(get(B + "/actas/" + ACTA_ANALISIS_LIMPIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.montoCondena").value(12345.67))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(nullValue()))
                .andExpect(jsonPath("$.em").doesNotExist())
                .andExpect(jsonPath("$.EM").doesNotExist())
                .andExpect(jsonPath("$.rc").doesNotExist())
                .andExpect(jsonPath("$.RC").doesNotExist())
                .andExpect(jsonPath("$.cmte").doesNotExist())
                .andExpect(jsonPath("$.pref").doesNotExist())
                .andExpect(jsonPath("$.nro").doesNotExist());
    }

    @Test
    void dictarFalloCondenatorio_montoNull_rechaza400() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(postDictarFalloCondenatorio(ACTA_ANALISIS_LIMPIA, "{\"montoCondena\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dictarFalloCondenatorio_montoCero_rechaza400() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(postDictarFalloCondenatorio(ACTA_ANALISIS_LIMPIA, "{\"montoCondena\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dictarFalloCondenatorio_montoNegativo_rechaza400() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(postDictarFalloCondenatorio(ACTA_ANALISIS_LIMPIA, "{\"montoCondena\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dictarFalloCondenatorio_sinBody_rechaza400() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/dictar-fallo-condenatorio"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void firmarYNotificarFalloCondenatorio_abrePlazoApelacionYportalDevuelvePuedeTrue() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(postDictarFalloCondenatorio(ACTA_ANALISIS_LIMPIA))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/firmar-documento/DOC-0006-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"));

        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        mvc.perform(get(B + "/actas/" + ACTA_ANALISIS_LIMPIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.montoCondena").value(12345.67))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(nullValue()));

        String codigoQr = "QR-" + ACTA_ANALISIS_LIMPIA + "-DEMO";
        mvc.perform(get(B + "/infractor/actas/" + codigoQr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.montoCondena").value(12345.67))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(true))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false))
                .andExpect(jsonPath("$.puedePagar").value(false));
    }

    @Test
    void registrarVencimientoPlazoApelacion_marcaCondenaFirmeYquitaApelacion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(postDictarFalloCondenatorio(ACTA_ANALISIS_LIMPIA))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/firmar-documento/DOC-0006-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/registrar-vencimiento-plazo-apelacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENA_FIRME"));

        mvc.perform(get(B + "/actas/" + ACTA_ANALISIS_LIMPIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"));

        String codigoQr = "QR-" + ACTA_ANALISIS_LIMPIA + "-DEMO";
        mvc.perform(get(B + "/infractor/actas/" + codigoQr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false))
                .andExpect(jsonPath("$.puedePagar").value(false));

        // Vencimiento doble: ya no aplica (plazo ya cerrado).
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/registrar-vencimiento-plazo-apelacion"))
                .andExpect(status().isConflict());
    }

    @Test
    void dictarFallo_rechazadoSiHayBloqueantesMaterialesPendientes() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        // Origina un bloqueante material posterior al labrado sobre acta en análisis.
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/registrar-medida-preventiva-posterior"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA_ANALISIS_LIMPIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(1))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]")
                        .value("LEVANTAMIENTO_MEDIDA_PREVENTIVA"));

        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/dictar-fallo-absolutorio"))
                .andExpect(status().isConflict());
        mvc.perform(postDictarFalloCondenatorio(ACTA_ANALISIS_LIMPIA))
                .andExpect(status().isConflict());
    }

    @Test
    void dictarFallo_rechazadoSiActaCerradaArchivadaOgestionExterna() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        // ACTA-0008: CERRADAS.
        mvc.perform(post(B + "/actas/ACTA-0008/acciones/dictar-fallo-absolutorio"))
                .andExpect(status().isConflict());
        mvc.perform(postDictarFalloCondenatorio("ACTA-0008"))
                .andExpect(status().isConflict());

        // ACTA-0007: ARCHIVO.
        mvc.perform(post(B + "/actas/ACTA-0007/acciones/dictar-fallo-absolutorio"))
                .andExpect(status().isConflict());
        mvc.perform(postDictarFalloCondenatorio("ACTA-0007"))
                .andExpect(status().isConflict());

        // ACTA-0017: GESTION_EXTERNA.
        mvc.perform(post(B + "/actas/ACTA-0017/acciones/dictar-fallo-absolutorio"))
                .andExpect(status().isConflict());
        mvc.perform(postDictarFalloCondenatorio("ACTA-0017"))
                .andExpect(status().isConflict());

        // Doble dictado: si ya hay fallo previo, rechazar.
        mvc.perform(postDictarFalloCondenatorio(ACTA_ANALISIS_LIMPIA))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/dictar-fallo-absolutorio"))
                .andExpect(status().isConflict());
        mvc.perform(postDictarFalloCondenatorio(ACTA_ANALISIS_LIMPIA))
                .andExpect(status().isConflict());
    }

    @Test
    void dictarFalloAbsolutorio_noExigeMontoCondena() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/dictar-fallo-absolutorio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoCondena").value(nullValue()));

        mvc.perform(get(B + "/actas/" + ACTA_ANALISIS_LIMPIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoCondena").value(nullValue()));
    }

    @Test
    void archivarActa_despuesFalloCondenatorioNotificado_rechaza409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(postDictarFalloCondenatorio(ACTA_ANALISIS_LIMPIA))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/firmar-documento/DOC-0006-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/archivar-acta"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ACTA_ANALISIS_LIMPIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"));
    }

    @Test
    void actaInfractorResponse_postFalloCondenatorio_noDevuelveEMnRCnCmteNiPrefNiNro() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(postDictarFalloCondenatorio(ACTA_ANALISIS_LIMPIA))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/firmar-documento/DOC-0006-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS_LIMPIA + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk());

        String codigoQr = "QR-" + ACTA_ANALISIS_LIMPIA + "-DEMO";
        mvc.perform(get(B + "/infractor/actas/" + codigoQr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedePresentarApelacion").value(true))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false))
                .andExpect(jsonPath("$.puedePagar").value(false))
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
