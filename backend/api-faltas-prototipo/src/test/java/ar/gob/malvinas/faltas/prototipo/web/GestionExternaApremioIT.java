package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Circuito APREMIO diferenciado: pago en apremio y reingreso sin pago.
 *
 * <p>Usa ACTA-0130 (GESTION_EXTERNA/APREMIO, CONDENA_FIRME, monto=75000)
 * para probar ambas salidas del circuito de apremio sin necesidad de
 * derivar primero.
 *
 * <p>Criterios de aceptación:
 * <ul>
 *   <li>Pago en apremio: situacionPagoCondena=CONFIRMADO, tipoGestionExterna=null,
 *       resultadoFinal=CONDENA_FIRME, cerrable=true, pagoCondena=false.</li>
 *   <li>Reingreso sin pago: vuelve a análisis, tipoGestionExterna=null,
 *       situacionPagoCondena=PENDIENTE, pagoCondena=true (acción INFORMAR disponible),
 *       cerrable=false.</li>
 *   <li>Eventos correctos en ambos casos.</li>
 *   <li>No rompe ACTA-0029 (pago condena directo sin gestión externa).</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class GestionExternaApremioIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0130";

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void reset() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
    }

    // ── Estado inicial ──────────────────────────────────────────────────────────

    @Test
    void actaExisteEnDatasetConEstadoInicialApremio() throws Exception {
        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ACTA))
                .andExpect(jsonPath("$.bandejaActual").value("GESTION_EXTERNA"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("EN_GESTION_EXTERNA"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("GESTION_EXTERNA"))
                .andExpect(jsonPath("$.tipoGestionExterna").value("APREMIO"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.montoCondena").value(75000))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"));
    }

    // ── Pago en apremio ─────────────────────────────────────────────────────────

    @Test
    void registrarPagoEnApremio_dejaSituacionConfirmadaYCerrable() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/apremio-registrar-pago"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("OK"))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ACTIVA"))
                .andExpect(jsonPath("$.tipoGestionExterna").isEmpty())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles").isEmpty())
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles").isEmpty());
    }

    @Test
    void registrarPagoEnApremio_registraEventoCorrecto() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/apremio-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("PAGO_EN_APREMIO_REGISTRADO")));
    }

    @Test
    void registrarPagoEnApremio_permiteSerrarDepues() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/apremio-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/cerrar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));
    }

    // ── Reingreso sin pago ──────────────────────────────────────────────────────

    @Test
    void apremioReingresarSinPago_vuelveAAnalisisConPagoCondena() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/apremio-reingresar-sin-pago"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("OK"))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.accionPendiente").value("REVISION_POST_GESTION_EXTERNA"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ACTIVA"))
                .andExpect(jsonPath("$.tipoGestionExterna").isEmpty())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles[0]").value("INFORMAR"))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles").isEmpty());
    }

    @Test
    void apremioReingresarSinPago_registraEventoCorrecto() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/apremio-reingresar-sin-pago"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento",
                        hasItem("ACTA_REINGRESADA_DESDE_APREMIO_SIN_PAGO")));
    }

    // ── Conflictos ──────────────────────────────────────────────────────────────

    @Test
    void registrarPagoEnApremio_desdeApremio_segundaVezConflict() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/apremio-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/apremio-registrar-pago"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void apremioAcciones_noAplicanAActaJuzgado() throws Exception {
        mvc.perform(post(B + "/actas/ACTA-0017/acciones/apremio-registrar-pago"))
                .andExpect(status().is4xxClientError());

        mvc.perform(post(B + "/actas/ACTA-0017/acciones/apremio-reingresar-sin-pago"))
                .andExpect(status().is4xxClientError());
    }

    // ── Sanidad: no rompe ACTA-0029 ─────────────────────────────────────────────

    @Test
    void acta0029_circuito_no_usa_gestion_externa() throws Exception {
        mvc.perform(get(B + "/actas/ACTA-0029"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.tipoGestionExterna").isEmpty());
    }
}
