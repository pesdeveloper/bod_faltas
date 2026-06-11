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
 * Portal infractor — consentir-condena rechaza si hay apelacion presentada.
 *
 * <p>Reglas verificadas:
 * <ul>
 *   <li>Si el infractor ya presento apelacion, POST consentir-condena devuelve 409.</li>
 *   <li>resultadoFinal y situacionPagoCondena no cambian.</li>
 *   <li>puedeConsentirCondena=false cuando hay apelacion presentada.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PortalConsentirCondenaRechazaSiHayApelacionPendienteIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";
    private static final String QR = "QR-ACTA-0030-DEMO";

    @Autowired
    private MockMvc mvc;

    private void prepararCondenadoConApelacion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":3500}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0030-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/registrar-apelacion"))
                .andExpect(status().isOk());
    }

    @Test
    void conApelacion_consentirCondena_devuelve409() throws Exception {
        prepararCondenadoConApelacion();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/consentir-condena"))
                .andExpect(status().isConflict());
    }

    @Test
    void conApelacion_consentirCondena_noModificaResultadoFinal() throws Exception {
        prepararCondenadoConApelacion();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/consentir-condena"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"));
    }

    @Test
    void conApelacion_consentirCondena_noModificaSituacionPagoCondena() throws Exception {
        prepararCondenadoConApelacion();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/consentir-condena"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("NO_APLICA"));
    }

    @Test
    void conApelacion_puedeConsentirCondena_esFalse() throws Exception {
        prepararCondenadoConApelacion();

        mvc.perform(get(B + "/infractor/actas/" + QR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedeConsentirCondena").value(false));
    }
}
