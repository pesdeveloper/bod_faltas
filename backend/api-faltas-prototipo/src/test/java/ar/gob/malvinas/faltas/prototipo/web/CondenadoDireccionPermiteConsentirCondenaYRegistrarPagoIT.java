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
 * Dirección — cuando el acta tiene resultadoFinal=CONDENADO, montoCondena&gt;0,
 * situacionPagoCondena=NO_APLICA, situacionPago=SIN_PAGO y sin apelación
 * presentada, accionesUi.consentirCondenaYRegistrarPago debe ser true.
 *
 * <p>Reglas verificadas:
 * <ul>
 *   <li>accionesUi.consentirCondenaYRegistrarPago=true en estado CONDENADO válido.</li>
 *   <li>accionesUi.archivoReingreso=false (no está en ARCHIVO).</li>
 *   <li>La acción coexiste con apelacionPresencial y vencimientoPlazoApelacion
 *       (calculados localmente en el frontend).</li>
 *   <li>resultadoFinal sigue siendo CONDENADO antes de ejecutar la acción.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class CondenadoDireccionPermiteConsentirCondenaYRegistrarPagoIT {

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
    void condenado_accionesUi_consentirCondenaYRegistrarPago_esTrue() throws Exception {
        prepararCondenado();

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesUi.consentirCondenaYRegistrarPago").value(true));
    }

    @Test
    void condenado_accionesUi_archivoReingreso_esFalse() throws Exception {
        prepararCondenado();

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesUi.archivoReingreso").value(false));
    }

    @Test
    void condenado_resultadoFinalSigueCondenado() throws Exception {
        prepararCondenado();

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"));
    }

    @Test
    void condenado_situacionPagoCondenaEsNoAplica() throws Exception {
        prepararCondenado();

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("NO_APLICA"));
    }
}
