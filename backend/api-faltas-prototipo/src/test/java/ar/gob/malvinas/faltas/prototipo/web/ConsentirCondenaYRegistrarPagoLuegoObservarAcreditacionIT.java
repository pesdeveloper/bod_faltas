package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dirección — flujo: consentir-condena-y-registrar-pago seguido de
 * observar-pago-condena (observar acreditación).
 *
 * <p>Reglas verificadas:
 * <ul>
 *   <li>Tras observar: situacionPagoCondena=OBSERVADO.</li>
 *   <li>Tras observar: cerrable=false.</li>
 *   <li>Tras observar: accionesPagoCondenaDisponibles incluye INFORMAR (reintento disponible).</li>
 *   <li>Tras observar: accionesPagoCondenaDisponibles no incluye CONFIRMAR ni OBSERVAR.</li>
 *   <li>resultadoFinal permanece CONDENA_FIRME.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ConsentirCondenaYRegistrarPagoLuegoObservarAcreditacionIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";
    private static final String QR = "QR-ACTA-0030-DEMO";

    @Autowired
    private MockMvc mvc;

    private void prepararInformado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":3500}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0030-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/consentir-condena-y-registrar-pago"))
                .andExpect(status().isOk());
    }

    @Test
    void observarAcreditacion_situacionPagoCondenaEsObservado() throws Exception {
        prepararInformado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/observar-pago-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("OBSERVADO"));
    }

    @Test
    void observarAcreditacion_actaNoEsCerrable() throws Exception {
        prepararInformado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/observar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));
    }

    @Test
    void observarAcreditacion_resultadoFinalSigueCondenaFirme() throws Exception {
        prepararInformado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/observar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"));
    }

    @Test
    void observarAcreditacion_informarPagoCondenaDisponibleParaReintento() throws Exception {
        prepararInformado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/observar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles", hasItem("INFORMAR")));
    }

    @Test
    void observarAcreditacion_confirmarYObservarNoDisponibles() throws Exception {
        prepararInformado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/observar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles", not(hasItem("CONFIRMAR"))))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles", not(hasItem("OBSERVAR"))));
    }
}
