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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Presentación de apelación desde portal infractor por código QR
 * ({@code POST /api/prototipo/infractor/actas/{codigoQr}/acciones/registrar-apelacion}).
 *
 * <p>Reglas fuertes verificadas:
 * <ul>
 *   <li>busca por {@code codigoQr}, no por {@code actaId};</li>
 *   <li>canal fijado internamente como {@code PORTAL_INFRACTOR} (body opcional,
 *       canal enviado por cliente ignorado);</li>
 *   <li>response ciudadano {@link ar.gob.malvinas.faltas.prototipo.web.dto.ActaInfractorResponse}
 *       sin {@code actaId} interno ni comprobantes EM/RC/Cmte/Pref/Nro;</li>
 *   <li>mismas precondiciones y rechazos que el endpoint administrativo.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class RegistrarApelacionInfractorPorCodigoQrIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0006";

    @Autowired
    private MockMvc mvc;

    @Test
    void registrarApelacionPorCodigoQr_conPlazoAbierto_devuelveActaInfractorActualizado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConPlazoAbierto();

        String codigoQr = codigoQrDeActa(ACTA);
        mvc.perform(post(B + "/infractor/actas/" + codigoQr + "/acciones/registrar-apelacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoQr").value(codigoQr))
                .andExpect(jsonPath("$.actaId").doesNotExist())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false))
                .andExpect(jsonPath("$.estadoVisible").value("EN_TRAMITE"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estaCerrada").value(false));
    }

    @Test
    void registrarApelacionPorCodigoQr_registraEventoConCanalPortalInfractor() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConPlazoAbierto();

        String codigoQr = codigoQrDeActa(ACTA);
        mvc.perform(post(B + "/infractor/actas/" + codigoQr + "/acciones/registrar-apelacion"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("APELACION_PRESENTADA")))
                .andExpect(jsonPath("$[*].descripcion", hasItem(containsString("PORTAL_INFRACTOR"))));
    }

    @Test
    void codigoQrInexistente_devuelve404() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-INEXISTENTE-DEMO/acciones/registrar-apelacion"))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrarApelacionPorCodigoQr_rechazadaSiResultadoFinalNoEsCondenado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-absolutorio"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0006-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk());

        String codigoQr = codigoQrDeActa(ACTA);
        mvc.perform(post(B + "/infractor/actas/" + codigoQr + "/acciones/registrar-apelacion"))
                .andExpect(status().isConflict());
    }

    @Test
    void registrarApelacionPorCodigoQr_rechazadaSiPlazoVencioYestaCondenaFirme() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConPlazoAbierto();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-vencimiento-plazo-apelacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENA_FIRME"));

        String codigoQr = codigoQrDeActa(ACTA);
        mvc.perform(post(B + "/infractor/actas/" + codigoQr + "/acciones/registrar-apelacion"))
                .andExpect(status().isConflict());
    }

    @Test
    void registrarApelacionPorCodigoQr_rechazadaSiDuplicada() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConPlazoAbierto();

        String codigoQr = codigoQrDeActa(ACTA);
        mvc.perform(post(B + "/infractor/actas/" + codigoQr + "/acciones/registrar-apelacion"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + codigoQr + "/acciones/registrar-apelacion"))
                .andExpect(status().isConflict());
    }

    @Test
    void registrarApelacionPorCodigoQr_rechazadaSiActaCerradaArchivadaOgestionExterna() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0008-DEMO/acciones/registrar-apelacion"))
                .andExpect(status().isConflict());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0007-DEMO/acciones/registrar-apelacion"))
                .andExpect(status().isConflict());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0017-DEMO/acciones/registrar-apelacion"))
                .andExpect(status().isConflict());
    }

    @Test
    void responseCiudadano_noDevuelveActaIdInternoNiEMnRCnCmteNiPrefNiNro() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConPlazoAbierto();

        String codigoQr = codigoQrDeActa(ACTA);
        mvc.perform(post(B + "/infractor/actas/" + codigoQr + "/acciones/registrar-apelacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actaId").doesNotExist())
                .andExpect(jsonPath("$.acta").value("A-2026-0006"))
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

    @Test
    void registrarApelacionPorCodigoQr_ignoraCanalEnviadoPorCliente_yUsaPortalInfractor() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConPlazoAbierto();

        String codigoQr = codigoQrDeActa(ACTA);
        mvc.perform(post(B + "/infractor/actas/" + codigoQr + "/acciones/registrar-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"PRESENCIAL_DIRECCION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false));

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].descripcion", hasItem(containsString("PORTAL_INFRACTOR"))));
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

        String codigoQr = codigoQrDeActa(ACTA);
        mvc.perform(get(B + "/infractor/actas/" + codigoQr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(true));
    }

    private static String codigoQrDeActa(String actaId) {
        return "QR-" + actaId + "-DEMO";
    }
}
