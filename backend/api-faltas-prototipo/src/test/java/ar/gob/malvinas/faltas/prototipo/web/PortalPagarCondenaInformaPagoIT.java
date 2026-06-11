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
 * Portal infractor — POST pagar-condena: informa pago de condena firme.
 *
 * <p>Reglas verificadas:
 * <ul>
 *   <li>situacionPagoCondena pasa a INFORMADO (no CONFIRMADO).</li>
 *   <li>Evento PAGO_CONDENA_INFORMADO con canal PORTAL_INFRACTOR registrado.</li>
 *   <li>mensajeVisible indica pago en proceso de acreditación.</li>
 *   <li>Dirección queda con acciones CONFIRMAR y OBSERVAR disponibles.</li>
 *   <li>Gestión externa no habilitada mientras pago está INFORMADO.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PortalPagarCondenaInformaPagoIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0030";
    private static final String QR = "QR-ACTA-0030-DEMO";

    @Autowired
    private MockMvc mvc;

    private void prepararCondenaFirme() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":35000}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0030-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/" + QR + "/documentos/FALLO_CONDENATORIO/ver"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-vencimiento-plazo-apelacion"))
                .andExpect(status().isOk());
    }

    @Test
    void pagarCondena_cambiaASituacionInformado() throws Exception {
        prepararCondenaFirme();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.puedePagarCondena").value(false));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"));
    }

    @Test
    void pagarCondena_noConfirmaAutomaticamente() throws Exception {
        prepararCondenaFirme();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"))
                .andExpect(jsonPath("$.estaCerrada").value(false));
    }

    @Test
    void pagarCondena_registraEventoConCanalPortalInfractor() throws Exception {
        prepararCondenaFirme();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("PAGO_CONDENA_INFORMADO")))
                .andExpect(jsonPath("$[*].descripcion", hasItem(containsString("PORTAL_INFRACTOR"))));
    }

    @Test
    void pagarCondena_mensajeVisibleIndicaAcreditacion() throws Exception {
        prepararCondenaFirme();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensajeVisible").value(
                        "Pago de condena en proceso de acreditaci\u00f3n. Direcci\u00f3n de Faltas verificar\u00e1 la acreditaci\u00f3n."));
    }

    @Test
    void pagarCondena_direccionQuedaConAccionesConfirmarYObservar() throws Exception {
        prepararCondenaFirme();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles", hasItem("CONFIRMAR")))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles", hasItem("OBSERVAR")));
    }

    @Test
    void pagarCondena_gestionExternaNoHabilitadaMientrasPagoInformado() throws Exception {
        prepararCondenaFirme();

        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty());
    }
}
