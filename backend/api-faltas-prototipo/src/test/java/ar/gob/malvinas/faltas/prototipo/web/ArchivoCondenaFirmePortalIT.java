package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validación end-to-end del circuito archivo/reingreso con condena firme exigible.
 *
 * <p>Cubre:
 * <ol>
 *   <li>ACTA-0133 en ARCHIVO: solo accionesUi.archivoReingreso disponible para Dirección.</li>
 *   <li>ACTA-0133 portal: estadoVisible=ARCHIVADA, puedePagarCondena=true, mensaje correcto.</li>
 *   <li>ACTA-0133 portal: pagar-condena desde ARCHIVO informa pago sin reingresar.</li>
 *   <li>ACTA-0133: reingresar desde ARCHIVO recupera pagoCondena y gestionExterna.</li>
 *   <li>ACTA-0134 en ARCHIVO: estado normalizado + solo archivoReingreso disponible.</li>
 *   <li>ACTA-0134: reingresar desde ARCHIVO recupera confirmar/observar pago.</li>
 * </ol>
 *
 * <p>Regla de dominio: archivo no es cierre. Mientras el acta está en ARCHIVO con
 * permiteReingreso=true, la única acción interna de Dirección es el reingreso.
 * El portal puede permitir pago de condena si existe CONDENA_FIRME con situación
 * PENDIENTE u OBSERVADO y monto > 0, sin que eso modifique la bandeja.
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ArchivoCondenaFirmePortalIT {

    private static final String B = "/api/prototipo";

    @Autowired
    private MockMvc mvc;

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: ArchivoCondenaFirmePendientePermiteSoloReingresoDireccionIT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ACTA-0133 en ARCHIVO con CONDENA_FIRME + PENDIENTE:
     * accionesUi.archivoReingreso=true; acciones operativas apagadas;
     * cerrable=false.
     */
    @Test
    void archivoCondenaFirmePendiente_permiteSoloReingresoDireccion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/ACTA-0133"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.permiteReingreso").value(true))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.accionesUi.archivoReingreso").value(true))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles").isEmpty())
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: PortalArchivadaConCondenaPendientePermitePagarCondenaIT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ACTA-0133 portal: estadoVisible=ARCHIVADA, resultadoFinal=CONDENA_FIRME,
     * situacionPagoCondena=PENDIENTE, puedePagarCondena=true; mensaje explica que
     * puede regularizar el pago desde el portal.
     */
    @Test
    void portalArchivadaCondenaFirmePendiente_permitePagarCondena() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String qr = "QR-ACTA-0133-DEMO";
        mvc.perform(get(B + "/infractor/actas/" + qr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVisible").value("ARCHIVADA"))
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.puedePagarCondena").value(true))
                .andExpect(jsonPath("$.puedeSolicitarPagoVoluntario").value(false))
                .andExpect(jsonPath("$.puedePagar").value(false))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false))
                .andExpect(jsonPath("$.mensajeVisible").value(
                        "La actuaci\u00f3n se encuentra archivada administrativamente, pero registra una"
                                + " condena firme pendiente de pago. Puede regularizar el pago desde este portal."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: PortalArchivadaPagarCondenaInformaPagoSinReingresarIT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ACTA-0133 en ARCHIVO: POST pagar-condena informa el pago sin modificar la
     * bandeja. La acta conserva bandeja=ARCHIVO y permiteReingreso=true.
     * Dirección sigue mostrando solo archivoReingreso disponible.
     */
    @Test
    void portalArchivada_pagarCondena_informaPagoSinReingresar() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String qr = "QR-ACTA-0133-DEMO";

        // Pagar condena desde portal estando archivada
        mvc.perform(post(B + "/infractor/actas/" + qr + "/acciones/pagar-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVisible").value("ARCHIVADA"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"))
                .andExpect(jsonPath("$.puedePagarCondena").value(false));

        // GET Dirección: bandeja sigue en ARCHIVO, permiteReingreso=true
        mvc.perform(get(B + "/actas/ACTA-0133"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"))
                .andExpect(jsonPath("$.situacionPago").value("PENDIENTE_CONFIRMACION"))
                .andExpect(jsonPath("$.tipoPago").value("CONDENA"))
                .andExpect(jsonPath("$.permiteReingreso").value(true))
                .andExpect(jsonPath("$.accionesUi.archivoReingreso").value(true))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles").isEmpty())
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty());

        // Portal: mensaje para archivada con pago informado
        mvc.perform(get(B + "/infractor/actas/" + qr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVisible").value("ARCHIVADA"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"))
                .andExpect(jsonPath("$.mensajeVisible").value(
                        "La actuaci\u00f3n se encuentra archivada administrativamente."
                                + " El pago de condena se encuentra en proceso de verificaci\u00f3n"
                                + " por Direcci\u00f3n de Faltas."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4: ReingresarCondenaFirmePendienteRecuperaPagoYGestionIT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ACTA-0133: reingresar desde ARCHIVO con CONDENA_FIRME + PENDIENTE.
     * Post-reingreso: bandeja=PENDIENTE_ANALISIS, situacionAdministrativa=ACTIVA,
     * pagoCondena=[INFORMAR], gestionExterna=[APREMIO, JUZGADO_DE_PAZ],
     * accionesUi.archivoReingreso=false.
     */
    @Test
    void reingresarCondenaFirmePendiente_recuperaPagoYGestion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/ACTA-0133/acciones/reingresar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        mvc.perform(get(B + "/actas/ACTA-0133"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ACTIVA"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.accionesUi.archivoReingreso").value(false))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles[0]").value("INFORMAR"))
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles[*]")
                        .value(containsInAnyOrder("APREMIO", "JUZGADO_DE_PAZ")))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5: ArchivoCondenaFirmeInformadoPermiteSoloReingresoDireccionIT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ACTA-0134 en ARCHIVO con CONDENA_FIRME + INFORMADO:
     * mock normalizado (situacionPago=PENDIENTE_CONFIRMACION, tipoPago=CONDENA);
     * accionesUi.archivoReingreso=true; confirmar/observar apagados mientras archivada.
     */
    @Test
    void archivoCondenaFirmeInformado_permiteSoloReingresoDireccion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/ACTA-0134"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.situacionPago").value("PENDIENTE_CONFIRMACION"))
                .andExpect(jsonPath("$.tipoPago").value("CONDENA"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"))
                .andExpect(jsonPath("$.permiteReingreso").value(true))
                .andExpect(jsonPath("$.accionesUi.archivoReingreso").value(true))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles").isEmpty())
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 6: ReingresarCondenaFirmeInformadoRecuperaConfirmarObservarIT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ACTA-0134: reingresar desde ARCHIVO con CONDENA_FIRME + INFORMADO.
     * Post-reingreso: bandeja=PENDIENTE_ANALISIS, situacionAdministrativa=ACTIVA,
     * accionesPagoCondena=[CONFIRMAR, OBSERVAR], gestionExterna=[],
     * accionesUi.archivoReingreso=false.
     */
    @Test
    void reingresarCondenaFirmeInformado_recuperaConfirmarObservar() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/ACTA-0134/acciones/reingresar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        mvc.perform(get(B + "/actas/ACTA-0134"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ACTIVA"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPago").value("PENDIENTE_CONFIRMACION"))
                .andExpect(jsonPath("$.tipoPago").value("CONDENA"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"))
                .andExpect(jsonPath("$.accionesUi.archivoReingreso").value(false))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles[*]")
                        .value(containsInAnyOrder("CONFIRMAR", "OBSERVAR")))
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));
    }
}
