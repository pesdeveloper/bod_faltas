package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reglas de disponibilidad de acciones de pago de condena y gestión externa.
 *
 * <p>Cubre los escenarios validados durante la validación integral de los
 * circuitos ACTA-0028 y ACTA-0030:
 * <ul>
 *   <li>A. ABSUELTO con montoCondena histórico: no hay pago ni gestión externa.</li>
 *   <li>B. CONDENADO no firme: no hay pago ni gestión externa, sí opciones de apelación.</li>
 *   <li>C. CONDENA_FIRME + PENDIENTE: pagoCondena y gestionExterna disponibles.</li>
 *   <li>D. CONDENA_FIRME + INFORMADO: solo confirmar/observar, sin gestión externa.</li>
 *   <li>E. CONDENA_FIRME + OBSERVADO: pagoCondena y gestionExterna disponibles.</li>
 *   <li>F. CONDENA_FIRME + CONFIRMADO: cierre habilitado, sin gestión externa, accionPendiente limpiado.</li>
 *   <li>G. Defensa backend: derivar con INFORMADO o CONFIRMADO devuelve 409.</li>
 * </ul>
 *
 * <p>Usa ACTA-0029 (empieza en PENDIENTE_ANALISIS) para la mayoría de escenarios.
 * Usa ACTA-0130 (GESTION_EXTERNA/APREMIO, CONDENA_FIRME) para el escenario F con
 * retorno desde gestión externa.
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ReglasPagoCondenaGestionExternaIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA_ANALISIS = "ACTA-0029";
    private static final String ACTA_GE = "ACTA-0130";
    private static final String MONTO_CONDENA_JSON = "{\"montoCondena\":1700}";
    private static final String DOC_FALLO_0029 = "DOC-0029-02";

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void reset() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
    }

    // ── A. ABSUELTO con montoCondena histórico ───────────────────────────────────

    @Test
    void A_absueltoConMontoHistorico_sinPagoNiGestionExterna() throws Exception {
        avanzarACondenaFirme(ACTA_ANALISIS, DOC_FALLO_0029);

        // Reingresar desde apremio sin pago → vuelve a PENDIENTE con CONDENA_FIRME
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/derivar-a-juzgado-de-paz"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/juzgado-reingresar-absuelto"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA_ANALISIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("ABSUELTO"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("NO_APLICA"))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles").isEmpty())
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true));
    }

    // ── B. CONDENADO notificado pero no firme ────────────────────────────────────

    @Test
    void B_condenadoNoFirme_sinPagoNiGestionExterna() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MONTO_CONDENA_JSON))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/firmar-documento/" + DOC_FALLO_0029))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk());
        // No se llama a registrar-vencimiento-plazo-apelacion: queda en CONDENADO

        mvc.perform(get(B + "/actas/" + ACTA_ANALISIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("NO_APLICA"))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles").isEmpty())
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));
    }

    // ── C. CONDENA_FIRME + PENDIENTE ─────────────────────────────────────────────

    @Test
    void C_condenaFirmePendiente_pagoYGestionExternaDisponibles() throws Exception {
        avanzarACondenaFirme(ACTA_ANALISIS, DOC_FALLO_0029);

        mvc.perform(get(B + "/actas/" + ACTA_ANALISIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles[0]").value("INFORMAR"))
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles[*]")
                        .value(containsInAnyOrder("APREMIO", "JUZGADO_DE_PAZ")))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.motivoNoCerrable")
                        .value("Condena firme pendiente de pago o derivación externa."));
    }

    // ── D. CONDENA_FIRME + INFORMADO ─────────────────────────────────────────────

    @Test
    void D_condenaFirmeInformado_soloConfirmarObservar_sinGestionExterna() throws Exception {
        avanzarACondenaFirme(ACTA_ANALISIS, DOC_FALLO_0029);
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/informar-pago-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Pago de condena informado correctamente."))
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"));

        mvc.perform(get(B + "/actas/" + ACTA_ANALISIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles[*]")
                        .value(containsInAnyOrder("CONFIRMAR", "OBSERVAR")))
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.motivoNoCerrable")
                        .value("Pago de condena informado pendiente de confirmación."));
    }

    // ── E. CONDENA_FIRME + OBSERVADO ─────────────────────────────────────────────

    @Test
    void E_condenaFirmeObservado_pagoYGestionExternaDisponibles() throws Exception {
        avanzarACondenaFirme(ACTA_ANALISIS, DOC_FALLO_0029);
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/informar-pago-condena"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/observar-pago-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Pago de condena observado."))
                .andExpect(jsonPath("$.situacionPagoCondena").value("OBSERVADO"));

        mvc.perform(get(B + "/actas/" + ACTA_ANALISIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("OBSERVADO"))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles[0]").value("INFORMAR"))
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles[*]")
                        .value(containsInAnyOrder("APREMIO", "JUZGADO_DE_PAZ")))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));
    }

    // ── F. CONDENA_FIRME + CONFIRMADO (con retorno desde gestión externa) ────────

    @Test
    void F_condenaFirmeConfirmado_cierreHabilitado_sinGestionExterna_accionPendienteLimpiada()
            throws Exception {
        // ACTA-0130 ya está en GESTION_EXTERNA/APREMIO/CONDENA_FIRME
        mvc.perform(post(B + "/actas/" + ACTA_GE + "/acciones/apremio-reingresar-sin-pago"))
                .andExpect(status().isOk());

        // En este punto: PENDIENTE_ANALISIS, accionPendiente=REVISION_POST_GESTION_EXTERNA
        mvc.perform(get(B + "/actas/" + ACTA_GE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionPendiente").value("REVISION_POST_GESTION_EXTERNA"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles[*]")
                        .value(containsInAnyOrder("APREMIO", "JUZGADO_DE_PAZ")));

        // Informar y confirmar pago condena
        mvc.perform(post(B + "/actas/" + ACTA_GE + "/acciones/informar-pago-condena"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA_GE + "/acciones/confirmar-pago-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Pago de condena confirmado correctamente."))
                .andExpect(jsonPath("$.situacionPagoCondena").value("CONFIRMADO"));

        // Estado esperado: cerrable=true, gestionExterna vacío, accionPendiente=null
        mvc.perform(get(B + "/actas/" + ACTA_GE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.accionPendiente").value(nullValue()))
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty())
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles").isEmpty());
    }

    // ── F2. CONDENA_FIRME + CONFIRMADO (sin gestión externa previa) ──────────────

    @Test
    void F2_condenaFirmeConfirmadoSinGestionExternaPrevia_cierreHabilitado() throws Exception {
        avanzarACondenaFirme(ACTA_ANALISIS, DOC_FALLO_0029);
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/informar-pago-condena"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/confirmar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA_ANALISIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles").isEmpty())
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty());
    }

    // ── G. Defensa backend ───────────────────────────────────────────────────────

    @Test
    void G1_derivarConPagoInformado_devuelve409() throws Exception {
        avanzarACondenaFirme(ACTA_ANALISIS, DOC_FALLO_0029);
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/informar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/derivar-a-apremio"))
                .andExpect(status().isConflict());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/derivar-a-juzgado-de-paz"))
                .andExpect(status().isConflict());
    }

    @Test
    void G2_derivarConPagoConfirmado_devuelve409() throws Exception {
        avanzarACondenaFirme(ACTA_ANALISIS, DOC_FALLO_0029);
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/informar-pago-condena"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/confirmar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/derivar-a-apremio"))
                .andExpect(status().isConflict());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/derivar-a-juzgado-de-paz"))
                .andExpect(status().isConflict());
    }

    @Test
    void G3_derivarConCondenadoNoFirme_devuelve409() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MONTO_CONDENA_JSON))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/firmar-documento/" + DOC_FALLO_0029))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk());
        // No se llama vencimiento: resultadoFinal=CONDENADO

        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/derivar-a-apremio"))
                .andExpect(status().isConflict());
        mvc.perform(post(B + "/actas/" + ACTA_ANALISIS + "/acciones/derivar-a-juzgado-de-paz"))
                .andExpect(status().isConflict());
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private void avanzarACondenaFirme(String actaId, String docFallo) throws Exception {
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MONTO_CONDENA_JSON))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/firmar-documento/" + docFallo))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/registrar-vencimiento-plazo-apelacion"))
                .andExpect(status().isOk());
    }
}
