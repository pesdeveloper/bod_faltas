package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Portal infractor — POST pagar-condena rechazado cuando resultadoFinal=CONDENADO (no firme).
 *
 * <p>Reglas verificadas:
 * <ul>
 *   <li>POST pagar-condena devuelve 409 cuando resultadoFinal es CONDENADO.</li>
 *   <li>No cambia situacionPagoCondena.</li>
 *   <li>No agrega evento de pago condena.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PortalPagarCondenaRechazaCondenadoNoFirmeIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";
    private static final String QR = "QR-ACTA-0030-DEMO";

    @Autowired
    private MockMvc mvc;

    private void prepararCondenado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":35000}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0030-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());
    }

    @Test
    void condenado_postPagarCondena_devuelve409() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-condena"))
                .andExpect(status().isConflict());
    }

    @Test
    void condenado_noModificaSituacionPagoCondena() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-condena"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("NO_APLICA"));
    }

    @Test
    void condenado_noAgregaEventoDePagoCondena() throws Exception {
        prepararCondenado();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-condena"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", not(hasItem("PAGO_CONDENA_INFORMADO"))));
    }
}
