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
 * Portal infractor — POST consentir-condena: pasa a CONDENA_FIRME + PENDIENTE.
 *
 * <p>Reglas verificadas:
 * <ul>
 *   <li>resultadoFinal pasa a CONDENA_FIRME.</li>
 *   <li>situacionPagoCondena pasa a PENDIENTE.</li>
 *   <li>montoCondena se conserva.</li>
 *   <li>puedePagarCondena=true luego del consentimiento.</li>
 *   <li>puedePresentarApelacion=false luego del consentimiento.</li>
 *   <li>puedeConsentirCondena=false luego del consentimiento.</li>
 *   <li>Evento CONDENA_CONSENTIDA registrado con referencia a PORTAL_INFRACTOR.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PortalConsentirCondenaPasaAFirmePendienteIT {

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
    void consentirCondena_resultadoFinalPasaACondenaFirme() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/consentir-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENA_FIRME"));
    }

    @Test
    void consentirCondena_situacionPagoCondenaEsPendiente() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/consentir-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"));
    }

    @Test
    void consentirCondena_montoCondenaSeConserva() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/consentir-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoCondena").value(3500));
    }

    @Test
    void consentirCondena_puedePagarCondena() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/consentir-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedePagarCondena").value(true));
    }

    @Test
    void consentirCondena_noPuedePresentarApelacion() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/consentir-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false));
    }

    @Test
    void consentirCondena_noPuedeConsentirNuevamente() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/consentir-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedeConsentirCondena").value(false));
    }

    @Test
    void consentirCondena_registraEventoCondenaConsentida() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/consentir-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("CONDENA_CONSENTIDA")))
                .andExpect(jsonPath("$[*].descripcion", hasItem(containsString("PORTAL_INFRACTOR"))));
    }
}
