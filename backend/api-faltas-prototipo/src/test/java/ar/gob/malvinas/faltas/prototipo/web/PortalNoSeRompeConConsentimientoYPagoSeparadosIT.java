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
 * Regresion — el flujo del portal infractor (consentir + pagar separados)
 * sigue funcionando correctamente luego de agregar la acción combinada de
 * Dirección.
 *
 * <p>Flujo verificado:
 * <ol>
 *   <li>CONDENADO → portal consentir-condena → CONDENA_FIRME + PENDIENTE.</li>
 *   <li>CONDENA_FIRME + PENDIENTE → portal pagar-condena → INFORMADO.</li>
 * </ol>
 *
 * <p>Reglas verificadas:
 * <ul>
 *   <li>Tras consentir: puedeConsentirCondena=false.</li>
 *   <li>Tras consentir: puedePagarCondena=true.</li>
 *   <li>Tras consentir: resultadoFinal=CONDENA_FIRME, situacionPagoCondena=PENDIENTE.</li>
 *   <li>Tras pagar: situacionPagoCondena=INFORMADO.</li>
 *   <li>Tras pagar: resultadoFinal sigue CONDENA_FIRME.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PortalNoSeRompeConConsentimientoYPagoSeparadosIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";
    private static final String QR = "QR-ACTA-0030-DEMO";

    @Autowired
    private MockMvc mvc;

    private void prepararCondenado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":3500}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0030-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());
    }

    @Test
    void portalConsentir_resultadoFinalEsCondenaFirme() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/consentir-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENA_FIRME"));
    }

    @Test
    void portalConsentir_situacionPagoCondenaEsPendiente() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/consentir-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"));
    }

    @Test
    void portalConsentirLuegoPagar_situacionPagoCondenaEsInformado() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/consentir-condena"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"));
    }

    @Test
    void portalConsentirLuegoPagar_resultadoFinalSigueCondenaFirme() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/consentir-condena"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENA_FIRME"));
    }

    @Test
    void portalConsentir_puedePagarCondena() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/consentir-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedePagarCondena").value(true));
    }

    @Test
    void portalConsentir_noPuedeConsentirNuevamente() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/consentir-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedeConsentirCondena").value(false));
    }
}
