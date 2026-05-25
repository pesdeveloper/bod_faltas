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
 * Resolución mock de apelación/recurso ya presentado sobre fallo
 * condenatorio notificado.
 *
 * <p>Reglas fuertes verificadas:
 * <ul>
 *   <li>{@code RECHAZADA} → {@code CONDENA_FIRME}, sin cierre automático;</li>
 *   <li>{@code ACEPTADA_ABSUELVE} → {@code ABSUELTO} y cerrabilidad
 *       {@code cerrable true}, sin cierre automático;</li>
 *   <li>rechazos por ausencia de apelación, resolución duplicada, resultado
 *       inválido y bandejas terminales/externas;</li>
 *   <li>{@code puedePresentarApelacion} queda en {@code false} tras resolver;</li>
 *   <li>NO se genera ni devuelve EM/RC/Cmte/Pref/Nro ni emisión de deuda ni
 *       recibo (alcance estricto del slice).</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ResolverApelacionIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0006";

    @Autowired
    private MockMvc mvc;

    @Test
    void resolverApelacion_rechazada_dejaCondenaFirmeSinCerrarActa() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConApelacionPresentada();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/resolver-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"RECHAZADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OK"))
                .andExpect(jsonPath("$.resultadoResolucion").value("RECHAZADA"))
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENA_FIRME"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.estaCerrada").value(false));

        String codigoQr = "QR-" + ACTA + "-DEMO";
        mvc.perform(get(B + "/infractor/actas/" + codigoQr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false));
    }

    @Test
    void resolverApelacion_aceptadaAbsuelve_dejaAbsueltoCerrableSinCerrarActa() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConApelacionPresentada();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/resolver-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"ACEPTADA_ABSUELVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OK"))
                .andExpect(jsonPath("$.resultadoResolucion").value("ACEPTADA_ABSUELVE"))
                .andExpect(jsonPath("$.resultadoFinal").value("ABSUELTO"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("ABSUELTO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.estaCerrada").value(false));

        String codigoQr = "QR-" + ACTA + "-DEMO";
        mvc.perform(get(B + "/infractor/actas/" + codigoQr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("ABSUELTO"))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false));
    }

    @Test
    void resolverApelacion_rechazadaSiNoHayApelacionPresentada() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConPlazoAbierto();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/resolver-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"RECHAZADA\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void resolverApelacion_rechazadaSiDuplicada() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConApelacionPresentada();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/resolver-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"RECHAZADA\"}"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/resolver-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"ACEPTADA_ABSUELVE\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void resolverApelacion_rechazadaSiResultadoNullOinvalido() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConApelacionPresentada();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/resolver-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/resolver-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"ELEVADA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resolverApelacion_rechazadaSiActaCerradaArchivadaOgestionExterna() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/ACTA-0008/acciones/resolver-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"RECHAZADA\"}"))
                .andExpect(status().isConflict());

        mvc.perform(post(B + "/actas/ACTA-0007/acciones/resolver-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"RECHAZADA\"}"))
                .andExpect(status().isConflict());

        mvc.perform(post(B + "/actas/ACTA-0017/acciones/resolver-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"RECHAZADA\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void actaInfractorResponse_postResolucionApelacion_noDevuelveEMnRCnCmteNiPrefNiNro() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConApelacionPresentada();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/resolver-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"RECHAZADA\"}"))
                .andExpect(status().isOk());

        String codigoQr = "QR-" + ACTA + "-DEMO";
        mvc.perform(get(B + "/infractor/actas/" + codigoQr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false))
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

    private void prepararFalloCondenatorioNotificadoConPlazoAbierto() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":12345.67}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0006-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk());
    }

    private void prepararFalloCondenatorioNotificadoConApelacionPresentada() throws Exception {
        prepararFalloCondenatorioNotificadoConPlazoAbierto();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"PORTAL_INFRACTOR\"}"))
                .andExpect(status().isOk());
    }
}
