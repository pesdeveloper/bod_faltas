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
 * Presentación mock de apelación/recurso sobre fallo condenatorio notificado
 * con plazo de apelación abierto.
 *
 * <p>Reglas fuertes verificadas:
 * <ul>
 *   <li>registro por {@code PORTAL_INFRACTOR} o {@code PRESENCIAL_DIRECCION}
 *       mantiene {@code resultadoFinal CONDENADO} y desactiva
 *       {@code puedePresentarApelacion};</li>
 *   <li>no pasa a {@code CONDENA_FIRME} ni cierra el acta;</li>
 *   <li>rechazos por resultado distinto de {@code CONDENADO}, plazo cerrado,
 *       apelación duplicada, vencimiento con apelación presentada, canal
 *       inválido y bandejas terminales/externas;</li>
 *   <li>NO se genera ni devuelve EM/RC/Cmte/Pref/Nro ni emisión de deuda ni
 *       recibo (alcance estricto del slice).</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class RegistrarApelacionIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0006";

    @Autowired
    private MockMvc mvc;

    @Test
    void registrarApelacion_porPortalInfractor_conPlazoAbierto_ok() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConPlazoAbierto();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"PORTAL_INFRACTOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OK"))
                .andExpect(jsonPath("$.canal").value("PORTAL_INFRACTOR"))
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENADO"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estaCerrada").value(false));

        String codigoQr = "QR-" + ACTA + "-DEMO";
        mvc.perform(get(B + "/infractor/actas/" + codigoQr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false));
    }

    @Test
    void registrarApelacion_porPresencialDireccion_conPlazoAbierto_ok() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConPlazoAbierto();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"PRESENCIAL_DIRECCION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OK"))
                .andExpect(jsonPath("$.canal").value("PRESENCIAL_DIRECCION"))
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENADO"));

        String codigoQr = "QR-" + ACTA + "-DEMO";
        mvc.perform(get(B + "/infractor/actas/" + codigoQr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false));
    }

    @Test
    void registrarApelacion_rechazadaSiResultadoFinalNoEsCondenado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-absolutorio"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0006-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("ABSUELTO"));

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"PORTAL_INFRACTOR\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registrarApelacion_rechazadaSiPlazoVencioYestaCondenaFirme() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConPlazoAbierto();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-vencimiento-plazo-apelacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENA_FIRME"));

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"PORTAL_INFRACTOR\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registrarApelacion_rechazadaSiDuplicada() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConPlazoAbierto();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"PORTAL_INFRACTOR\"}"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"PRESENCIAL_DIRECCION\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registrarVencimientoPlazo_rechazadoSiYaHayApelacionPresentada() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConPlazoAbierto();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"PORTAL_INFRACTOR\"}"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-vencimiento-plazo-apelacion"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"));
    }

    @Test
    void registrarApelacion_rechazadaSiCanalNullOinvalido() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConPlazoAbierto();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"CORREO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarApelacion_rechazadaSiActaCerradaArchivadaOgestionExterna() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/ACTA-0008/acciones/registrar-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"PORTAL_INFRACTOR\"}"))
                .andExpect(status().isConflict());

        mvc.perform(post(B + "/actas/ACTA-0007/acciones/registrar-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"PORTAL_INFRACTOR\"}"))
                .andExpect(status().isConflict());

        mvc.perform(post(B + "/actas/ACTA-0017/acciones/registrar-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"PORTAL_INFRACTOR\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void actaInfractorResponse_postApelacion_noDevuelveEMnRCnCmteNiPrefNiNro() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioNotificadoConPlazoAbierto();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"PORTAL_INFRACTOR\"}"))
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

        String codigoQr = "QR-" + ACTA + "-DEMO";
        mvc.perform(get(B + "/infractor/actas/" + codigoQr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(true));
    }
}
