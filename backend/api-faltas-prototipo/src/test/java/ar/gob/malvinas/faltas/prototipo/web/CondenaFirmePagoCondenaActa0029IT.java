package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.BeforeEach;
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
 * Circuito ACTA-0029: condena firme + pago condena + cierre sin gestión externa.
 *
 * <p>Flujo completo: acta en PENDIENTE_ANALISIS → dictar fallo condenatorio → firmar →
 * registrar notificación positiva → vencimiento plazo apelación → CONDENA_FIRME →
 * informar pago condena → confirmar pago condena → cerrar.
 *
 * <p>El circuito no pasa por GESTION_EXTERNA ni APREMIO.
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class CondenaFirmePagoCondenaActa0029IT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0029";
    private static final String MONTO_CONDENA_JSON = "{\"montoCondena\":1700}";
    private static final String DOC_FALLO = "DOC-0029-02";

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void reset() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
    }

    @Test
    void actaExisteEnDatasetConEstadoInicialLimpio() throws Exception {
        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ACTA))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"));
    }

    @Test
    void vencimientoPlazoApelacion_dejActaEnCondenaFirme() throws Exception {
        avanzarACondenaFirme();

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.montoCondena").value(1700))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles[0]").value("INFORMAR"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));
    }

    @Test
    void informarYConfirmarPagoCondena_habilitaCierre() throws Exception {
        avanzarACondenaFirme();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/informar-pago-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"));

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("CONFIRMADO"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles").isEmpty())
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty());
    }

    @Test
    void cerrar_despuesDePagoCondenaConfirmado_quedaEnCERRADAS() throws Exception {
        avanzarACondenaFirme();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/informar-pago-condena"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/cerrar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.montoCondena").value(1700))
                .andExpect(jsonPath("$.situacionPagoCondena").value("CONFIRMADO"));
    }

    @Test
    void circuito_noDeriva_a_gestionExterna_ni_apremio() throws Exception {
        avanzarACondenaFirme();

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value(not("GESTION_EXTERNA")));

        mvc.perform(get(B + "/bandejas/GESTION_EXTERNA/actas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(ACTA))));
    }

    @Test
    void eventosRegistranElCircuito() throws Exception {
        avanzarACondenaFirme();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/informar-pago-condena"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("FALLO_CONDENATORIO_DICTADO")))
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("FALLO_CONDENATORIO_NOTIFICADO")))
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("PLAZO_APELACION_VENCIDO")))
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("PAGO_CONDENA_INFORMADO")))
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("PAGO_CONDENA_CONFIRMADO")));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void avanzarACondenaFirme() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MONTO_CONDENA_JSON))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/" + DOC_FALLO))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-vencimiento-plazo-apelacion"))
                .andExpect(status().isOk());
    }
}
